package com.fliptracker.ui;

import com.fliptracker.FlipTrackerPlugin;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

public class FlipTrackerPanel extends PluginPanel
{
	private final MarginsPanel live;
	private final MarginsPanel day;
	private final WatchlistPanel watch;
	private final FlipsPanel flips;
	private final LimitsPanel limits;

	public FlipTrackerPanel(FlipTrackerPlugin plugin, ItemManager itemManager)
	{
		super(false);
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(6, 4, 6, 4));

		live = new MarginsPanel(plugin, itemManager, MarginsPanel.Mode.LIVE);
		day = new MarginsPanel(plugin, itemManager, MarginsPanel.Mode.DAY);
		watch = new WatchlistPanel(plugin, itemManager);
		flips = new FlipsPanel(plugin, itemManager);
		limits = new LimitsPanel(plugin, itemManager);

		JPanel display = new JPanel(new BorderLayout());
		display.setBackground(ColorScheme.DARK_GRAY_COLOR);

		MaterialTabGroup tabs = new MaterialTabGroup(display);
		tabs.setLayout(new GridLayout(1, 5, 0, 0));
		tabs.setBorder(new EmptyBorder(0, 0, 8, 0));

		MaterialTab liveTab = tab(tabs, "Live", "Best flips right now", live);
		tab(tabs, "24h", "Best items to flip over 24 hours", day);
		tab(tabs, "Watch", "Your watchlist", watch);
		tab(tabs, "Flips", "Your flip log and profit", flips);
		tab(tabs, "Limits", "Buy limit timers", limits);
		tabs.select(liveTab);

		add(tabs, BorderLayout.NORTH);
		add(display, BorderLayout.CENTER);
	}

	private static MaterialTab tab(MaterialTabGroup group, String name, String tooltip, JPanel content)
	{
		MaterialTab tab = new MaterialTab(name, group, content);
		tab.setToolTipText(tooltip);
		tab.setFont(FontManager.getRunescapeSmallFont());
		group.addTab(tab);
		return tab;
	}

	public void refreshPrices()
	{
		live.refresh();
		day.refresh();
		watch.refresh();
		flips.refresh(); // unrealised profit uses current prices
	}

	public void refreshFlips()
	{
		flips.refresh();
	}

	public void refreshLimits()
	{
		limits.refresh();
	}

	public void shutDown()
	{
		limits.stop();
	}
}
