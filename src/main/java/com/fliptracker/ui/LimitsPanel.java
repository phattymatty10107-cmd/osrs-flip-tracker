package com.fliptracker.ui;

import com.fliptracker.FlipTrackerPlugin;
import com.fliptracker.data.LimitView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;

/** Countdown to each item's 4-hour buy limit reset. */
class LimitsPanel extends JPanel
{
	private final FlipTrackerPlugin plugin;
	private final ItemManager itemManager;
	private final JPanel list = UiUtil.verticalList();
	private final Timer ticker;

	LimitsPanel(FlipTrackerPlugin plugin, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.itemManager = itemManager;

		setLayout(new BorderLayout(0, 4));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(UiUtil.label("<html>Buy limits reset 4 hours after your first buy of an item.</html>",
			UiUtil.MUTED, false), BorderLayout.NORTH);
		add(UiUtil.scroll(list), BorderLayout.CENTER);

		ticker = new Timer(1000, e ->
		{
			if (isShowing())
			{
				refresh();
			}
		});
		ticker.start();
		refresh();
	}

	void stop()
	{
		ticker.stop();
	}

	void refresh()
	{
		list.removeAll();
		List<LimitView> views = plugin.getLimitViews();
		long now = System.currentTimeMillis();

		if (views.isEmpty())
		{
			list.add(UiUtil.emptyState("No active buy limits.<br>They start automatically when you buy something on the GE."));
		}

		for (LimitView v : views)
		{
			boolean ready = now >= v.getResetAt();
			String limitText = v.getLimit() > 0
				? UiUtil.gp(v.getBought()) + " / " + UiUtil.gp(v.getLimit()) + " bought"
				: UiUtil.gp(v.getBought()) + " bought (limit unknown)";
			String left = v.remaining() >= 0 ? UiUtil.gp(v.remaining()) + " more allowed" : null;

			ItemRow row = new ItemRow(itemManager, v.getItemId(), v.getItemName(),
				ready ? "Ready" : UiUtil.duration(v.getResetAt() - now),
				ready ? UiUtil.PROFIT : ColorScheme.GRAND_EXCHANGE_PRICE,
				limitText, ready ? "Limit has reset" : left, null);

			JPanel wrap = new JPanel(new BorderLayout());
			wrap.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			wrap.add(row, BorderLayout.CENTER);
			wrap.add(progress(v, now), BorderLayout.SOUTH);
			UiUtil.addRow(list, wrap);
		}
		list.revalidate();
		list.repaint();
	}

	private static JPanel progress(LimitView v, long now)
	{
		JProgressBar bar = new JProgressBar(0, 1000);
		double frac = Math.min(1.0, (now - v.getWindowStart()) / (double) FlipTrackerPlugin.LIMIT_WINDOW_MS);
		bar.setValue((int) (frac * 1000));
		bar.setPreferredSize(new Dimension(0, 4));
		bar.setBorderPainted(false);
		bar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bar.setForeground(frac >= 1.0 ? UiUtil.PROFIT : new Color(0xC8A030));

		JPanel p = new JPanel(new GridLayout(1, 1));
		p.setBorder(new EmptyBorder(0, 5, 4, 5));
		p.setOpaque(false);
		p.add(bar);
		return p;
	}
}
