package com.fliptracker.ui;

import com.fliptracker.FlipTrackerConfig;
import com.fliptracker.FlipTrackerPlugin;
import com.fliptracker.data.ItemInfo;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.IconTextField;

/**
 * Lists the best items to flip. In LIVE mode it uses the latest prices; in DAY mode it
 * uses 24h averages and ranks by estimated profit over a full day.
 */
class MarginsPanel extends JPanel
{
	enum Mode
	{
		LIVE, DAY
	}

	private static final String[] LIVE_SORTS = {"Profit per limit", "Margin", "ROI", "Hourly volume"};
	private static final String[] DAY_SORTS = {"24h profit", "Margin", "ROI", "Daily volume"};

	private final FlipTrackerPlugin plugin;
	private final ItemManager itemManager;
	private final Mode mode;
	private final IconTextField search = new IconTextField();
	private final JComboBox<String> sort;
	private final JLabel status = UiUtil.label(" ", UiUtil.MUTED, false);
	private final JPanel list = UiUtil.verticalList();

	MarginsPanel(FlipTrackerPlugin plugin, ItemManager itemManager, Mode mode)
	{
		this.plugin = plugin;
		this.itemManager = itemManager;
		this.mode = mode;

		setLayout(new BorderLayout(0, 4));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel controls = new JPanel(new GridLayout(0, 1, 0, 4));
		controls.setBackground(ColorScheme.DARK_GRAY_COLOR);
		controls.setBorder(new EmptyBorder(0, 0, 2, 0));

		if (mode == Mode.DAY)
		{
			controls.add(UiUtil.label("<html>Best items to flip over 24 hours, from 24h average prices. "
				+ "Hover a row for the full breakdown.</html>", UiUtil.MUTED, false));
		}

		search.setIcon(IconTextField.Icon.SEARCH);
		search.setPreferredSize(new Dimension(0, 30));
		search.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		search.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				refresh();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				refresh();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				refresh();
			}
		});
		controls.add(search);

		JPanel sortRow = new JPanel(new BorderLayout(4, 0));
		sortRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		sort = new JComboBox<>(mode == Mode.DAY ? DAY_SORTS : LIVE_SORTS);
		sort.setFocusable(false);
		sort.addActionListener(e -> refresh());
		sortRow.add(sort, BorderLayout.CENTER);
		JButton reload = new JButton("Refresh");
		reload.setFocusable(false);
		reload.setToolTipText("Fetch the latest prices now");
		reload.addActionListener(e ->
		{
			status.setText("Refreshing...");
			plugin.refreshPricesNow();
		});
		sortRow.add(reload, BorderLayout.EAST);
		controls.add(sortRow);
		controls.add(status);

		add(controls, BorderLayout.NORTH);
		add(UiUtil.scroll(list), BorderLayout.CENTER);

		refresh();
	}

	void refresh()
	{
		Map<Integer, ItemInfo> prices = plugin.getPrices();
		FlipTrackerConfig config = plugin.getConfig();
		String query = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);

		list.removeAll();

		if (prices.isEmpty())
		{
			String err = plugin.getPriceError();
			status.setText(err != null ? "Couldn't load prices" : "Loading prices...");
			list.add(UiUtil.emptyState(err != null
				? "Couldn't reach the OSRS Wiki price API.<br>" + err
				: "Fetching prices from the OSRS Wiki..."));
			revalidateAll();
			return;
		}

		List<ItemInfo> rows = new ArrayList<>();
		for (ItemInfo i : prices.values())
		{
			if (!query.isEmpty())
			{
				if (i.getName().toLowerCase(Locale.ROOT).contains(query) && (mode == Mode.LIVE ? i.hasPrices() : i.hasDayData()))
				{
					rows.add(i);
				}
				continue;
			}
			if (passesFilters(i, config))
			{
				rows.add(i);
			}
		}

		rows.sort(comparator());
		int max = config.maxResults();
		if (rows.size() > max)
		{
			rows = rows.subList(0, max);
		}

		String err = plugin.getPriceError() != null ? " (last refresh failed)" : "";
		status.setText("Updated " + UiUtil.ago(plugin.getPricesUpdatedAt()) + " · " + rows.size() + " shown" + err);

		if (rows.isEmpty())
		{
			list.add(UiUtil.emptyState(query.isEmpty()
				? "Nothing matches your filters.<br>Try lowering the minimum volume or price in the plugin settings."
				: "No items match \"" + query + "\"."));
		}

		int rank = 1;
		for (ItemInfo i : rows)
		{
			UiUtil.addRow(list, mode == Mode.DAY
				? PriceRows.day(plugin, itemManager, i, rank++)
				: PriceRows.live(plugin, itemManager, i));
		}
		revalidateAll();
	}

	private boolean passesFilters(ItemInfo i, FlipTrackerConfig config)
	{
		if (!config.includeMembers() && i.isMembers())
		{
			return false;
		}

		long buyPrice = mode == Mode.DAY ? i.getDayAvgLow() : i.getLow();
		if (buyPrice < config.minPrice() || (config.maxPrice() > 0 && buyPrice > config.maxPrice()))
		{
			return false;
		}

		if (mode == Mode.DAY)
		{
			return i.hasDayData()
				&& i.dayVolume() >= config.minDailyVolume()
				&& i.dayMargin() > 0
				&& i.dayFlipQty() > 0;
		}

		if (!i.hasPrices() || i.margin() <= 0 || i.getHourVolume() < config.minVolume())
		{
			return false;
		}
		int maxAge = config.maxPriceAge();
		if (maxAge > 0)
		{
			long cutoff = System.currentTimeMillis() / 1000 - maxAge * 60L;
			return i.getHighTime() >= cutoff && i.getLowTime() >= cutoff;
		}
		return true;
	}

	private Comparator<ItemInfo> comparator()
	{
		int idx = Math.max(0, sort.getSelectedIndex());
		Comparator<ItemInfo> c;
		if (mode == Mode.DAY)
		{
			switch (idx)
			{
				case 1:
					c = Comparator.comparingLong(ItemInfo::dayMargin);
					break;
				case 2:
					c = Comparator.comparingDouble(ItemInfo::dayRoi);
					break;
				case 3:
					c = Comparator.comparingLong(ItemInfo::dayVolume);
					break;
				default:
					c = Comparator.comparingLong(ItemInfo::dayProfit);
			}
		}
		else
		{
			switch (idx)
			{
				case 1:
					c = Comparator.comparingLong(ItemInfo::margin);
					break;
				case 2:
					c = Comparator.comparingDouble(ItemInfo::roi);
					break;
				case 3:
					c = Comparator.comparingLong(ItemInfo::getHourVolume);
					break;
				default:
					c = Comparator.comparingLong(ItemInfo::potentialProfit);
			}
		}
		return c.reversed();
	}

	private void revalidateAll()
	{
		list.revalidate();
		list.repaint();
	}
}
