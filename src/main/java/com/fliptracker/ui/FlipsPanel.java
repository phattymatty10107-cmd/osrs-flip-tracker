package com.fliptracker.ui;

import com.fliptracker.FlipTrackerPlugin;
import com.fliptracker.data.FlipSummary;
import com.fliptracker.data.ItemInfo;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;

/** Profit summary, items you're holding, and completed flips. Filled automatically from GE offers. */
class FlipsPanel extends JPanel
{
	private final FlipTrackerPlugin plugin;
	private final ItemManager itemManager;
	private final JLabel total = stat();
	private final JLabel today = stat();
	private final JLabel count = stat();
	private final JLabel tax = stat();
	private final JPanel list = UiUtil.verticalList();

	FlipsPanel(FlipTrackerPlugin plugin, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.itemManager = itemManager;

		setLayout(new BorderLayout(0, 4));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel stats = new JPanel(new GridLayout(2, 2, 4, 4));
		stats.setBackground(ColorScheme.DARK_GRAY_COLOR);
		stats.add(box("Total profit", total));
		stats.add(box("Today", today));
		stats.add(box("Flips", count));
		stats.add(box("Tax paid", tax));
		add(stats, BorderLayout.NORTH);

		add(UiUtil.scroll(list), BorderLayout.CENTER);

		JButton clear = new JButton("Clear history");
		clear.setFocusable(false);
		clear.addActionListener(e ->
		{
			int answer = JOptionPane.showConfirmDialog(this,
				"Delete all recorded trades and buy limit timers for this account?",
				"Flip Tracker", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (answer == JOptionPane.YES_OPTION)
			{
				plugin.clearHistory();
			}
		});
		add(clear, BorderLayout.SOUTH);

		refresh();
	}

	void refresh()
	{
		FlipSummary s = plugin.getSummary();
		setStat(total, s.getTotalProfit());
		setStat(today, s.getTodayProfit());
		count.setText(String.valueOf(s.getFlipCount()));
		count.setForeground(Color.WHITE);
		tax.setText(UiUtil.gp(s.getTaxPaid()));
		tax.setForeground(Color.WHITE);

		list.removeAll();
		Map<Integer, ItemInfo> prices = plugin.getPrices();

		if (!s.getOpen().isEmpty())
		{
			list.add(UiUtil.header("Holding (" + s.getOpen().size() + ")"));
			for (FlipSummary.OpenPosition p : s.getOpen())
			{
				ItemInfo info = prices.get(p.getItemId());
				String value = null;
				Color color = UiUtil.MUTED;
				String line3 = null;
				if (info != null && info.hasPrices())
				{
					long unreal = (long) p.getQuantity() * (info.getHigh() - info.tax() - p.getAvgBuy());
					value = UiUtil.signedGp(unreal);
					color = UiUtil.profitColor(unreal);
					line3 = "Sell now ~" + UiUtil.gp(info.getHigh()) + " each";
				}
				UiUtil.addRow(list, new ItemRow(itemManager, p.getItemId(), p.getItemName(), value, color,
					UiUtil.gp(p.getQuantity()) + " bought @ " + UiUtil.gp(p.getAvgBuy()), line3,
					"Value shown is unrealised profit if you sold at the current price, after tax"));
			}
		}

		list.add(UiUtil.header("Completed flips"));
		if (s.getRecent().isEmpty())
		{
			list.add(UiUtil.emptyState("No flips yet.<br>Buy and sell on the Grand Exchange and they'll show up here automatically."));
		}
		for (FlipSummary.CompletedFlip f : s.getRecent())
		{
			UiUtil.addRow(list, new ItemRow(itemManager, f.getItemId(), f.getItemName(),
				UiUtil.signedGp(f.getProfit()), UiUtil.profitColor(f.getProfit()),
				UiUtil.gp(f.getQuantity()) + " x " + UiUtil.gp(f.getAvgBuy()) + " -> " + UiUtil.gp(f.getSellPrice()),
				UiUtil.ago(f.getTime()), null));
		}

		list.revalidate();
		list.repaint();
	}

	private static JLabel stat()
	{
		return UiUtil.label("0", Color.WHITE, true);
	}

	private static void setStat(JLabel label, long value)
	{
		label.setText(UiUtil.signedGp(value));
		label.setForeground(UiUtil.profitColor(value));
	}

	private static JPanel box(String title, JLabel value)
	{
		JPanel p = new JPanel(new GridLayout(2, 1));
		p.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		p.setBorder(new EmptyBorder(5, 7, 5, 7));
		p.add(UiUtil.label(title, UiUtil.MUTED, false));
		p.add(value);
		return p;
	}
}
