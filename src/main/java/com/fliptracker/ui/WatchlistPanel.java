package com.fliptracker.ui;

import com.fliptracker.FlipTrackerPlugin;
import com.fliptracker.data.ItemInfo;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.IconTextField;

class WatchlistPanel extends JPanel
{
	private final FlipTrackerPlugin plugin;
	private final ItemManager itemManager;
	private final IconTextField add = new IconTextField();
	private final JLabel hint = UiUtil.label("Type an item name and press Enter", UiUtil.MUTED, false);
	private final JPanel list = UiUtil.verticalList();

	WatchlistPanel(FlipTrackerPlugin plugin, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.itemManager = itemManager;

		setLayout(new BorderLayout(0, 4));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel controls = new JPanel(new GridLayout(0, 1, 0, 4));
		controls.setBackground(ColorScheme.DARK_GRAY_COLOR);
		controls.setBorder(new EmptyBorder(0, 0, 2, 0));

		add.setIcon(IconTextField.Icon.SEARCH);
		add.setPreferredSize(new Dimension(0, 30));
		add.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		add.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		add.addActionListener(e -> addByName(add.getText()));
		controls.add(add);
		controls.add(hint);

		add(controls, BorderLayout.NORTH);
		add(UiUtil.scroll(list), BorderLayout.CENTER);
		refresh();
	}

	private void addByName(String text)
	{
		String q = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
		if (q.isEmpty())
		{
			return;
		}

		ItemInfo best = null;
		int bestScore = 0;
		for (ItemInfo i : plugin.getPrices().values())
		{
			String name = i.getName().toLowerCase(Locale.ROOT);
			int score = name.equals(q) ? 3 : name.startsWith(q) ? 2 : name.contains(q) ? 1 : 0;
			if (score > bestScore || (score == bestScore && score > 0 && best != null && name.length() < best.getName().length()))
			{
				best = i;
				bestScore = score;
			}
		}

		if (best == null)
		{
			hint.setText("No item found for \"" + text.trim() + "\"");
			return;
		}
		if (plugin.isWatched(best.getId()))
		{
			hint.setText(best.getName() + " is already on your watchlist");
			return;
		}
		plugin.toggleWatch(best.getId());
		hint.setText("Added " + best.getName());
		add.setText("");
	}

	void refresh()
	{
		list.removeAll();
		List<Integer> ids = plugin.getWatchlist();
		Map<Integer, ItemInfo> prices = plugin.getPrices();

		if (ids.isEmpty())
		{
			list.add(UiUtil.emptyState("Your watchlist is empty.<br>Add items above, or click any item in the Live or 24h tabs."));
		}

		for (Integer id : ids)
		{
			ItemInfo info = prices.get(id);
			if (info == null || !info.hasPrices())
			{
				UiUtil.addRow(list, new ItemRow(itemManager, id, "Item " + id, null, null,
					"No recent price data", null, null)
					.action("Remove from watchlist", () -> plugin.toggleWatch(id)));
				continue;
			}
			UiUtil.addRow(list, PriceRows.live(plugin, itemManager, info));
		}
		list.revalidate();
		list.repaint();
	}
}
