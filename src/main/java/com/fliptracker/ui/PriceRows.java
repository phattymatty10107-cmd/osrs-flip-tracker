package com.fliptracker.ui;

import com.fliptracker.FlipTrackerPlugin;
import com.fliptracker.data.ItemInfo;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.LinkBrowser;

/** Builds rows for items in the Live, 24h and Watch tabs. */
final class PriceRows
{
	private PriceRows()
	{
	}

	static ItemRow live(FlipTrackerPlugin plugin, ItemManager itemManager, ItemInfo i)
	{
		long margin = i.margin();
		String line2 = "Buy " + UiUtil.gp(i.getLow()) + "  Sell " + UiUtil.gp(i.getHigh());
		String line3 = String.format("ROI %.1f%%  Vol %s/h  Lim %s",
			i.roi(), UiUtil.compact(i.getHourVolume()), i.getLimit() > 0 ? UiUtil.compact(i.getLimit()) : "?");

		String tip = "<html><b>" + i.getName() + "</b><br>"
			+ "Buy at (instant sell): " + UiUtil.gp(i.getLow()) + " gp, " + ageOf(i.getLowTime()) + "<br>"
			+ "Sell at (instant buy): " + UiUtil.gp(i.getHigh()) + " gp, " + ageOf(i.getHighTime()) + "<br>"
			+ "GE tax: " + UiUtil.gp(i.tax()) + " gp<br>"
			+ "Margin after tax: " + UiUtil.gp(margin) + " gp<br>"
			+ "Profit for one limit (approx): " + UiUtil.gp(i.potentialProfit()) + " gp<br>"
			+ "Click for options</html>";

		return withActions(plugin, new ItemRow(itemManager, i.getId(), i.getName(),
			UiUtil.signedGp(margin), UiUtil.profitColor(margin), line2, line3, tip), i);
	}

	static ItemRow day(FlipTrackerPlugin plugin, ItemManager itemManager, ItemInfo i, int rank)
	{
		long dayProfit = i.dayProfit();
		String line2 = "Margin " + UiUtil.gp(i.dayMargin()) + " x " + UiUtil.compact(i.dayFlipQty()) + "/day";
		String line3 = String.format("ROI %.1f%%  Vol %s/day  Lim %s",
			i.dayRoi(), UiUtil.compact(i.dayVolume()), i.getLimit() > 0 ? UiUtil.compact(i.getLimit()) : "?");

		String tip = "<html><b>#" + rank + " " + i.getName() + "</b> (24h averages)<br>"
			+ "Avg buy price: " + UiUtil.gp(i.getDayAvgLow()) + " gp<br>"
			+ "Avg sell price: " + UiUtil.gp(i.getDayAvgHigh()) + " gp<br>"
			+ "Margin after tax: " + UiUtil.gp(i.dayMargin()) + " gp<br>"
			+ "Traded in 24h: " + UiUtil.gp(i.getDayLowVolume()) + " instant-sold, "
			+ UiUtil.gp(i.getDayHighVolume()) + " instant-bought<br>"
			+ "Flippable per day: " + UiUtil.gp(i.dayFlipQty())
			+ (i.getLimit() > 0 ? " (limit " + UiUtil.gp(i.getLimit()) + " x 6 windows)" : "") + "<br>"
			+ "Estimated 24h profit: <b>" + UiUtil.gp(dayProfit) + " gp</b><br>"
			+ "Cash needed per limit: " + UiUtil.gp(i.capitalPerLimit()) + " gp<br>"
			+ "Live margin now: " + UiUtil.gp(i.margin()) + " gp<br>"
			+ "Click for options</html>";

		return withActions(plugin, new ItemRow(itemManager, i.getId(), "#" + rank + " " + i.getName(),
			"+" + UiUtil.compact(dayProfit), UiUtil.PROFIT, line2, line3, tip), i);
	}

	private static ItemRow withActions(FlipTrackerPlugin plugin, ItemRow row, ItemInfo i)
	{
		return row
			.action(plugin.isWatched(i.getId()) ? "Remove from watchlist" : "Add to watchlist", () -> plugin.toggleWatch(i.getId()))
			.action("Open price chart", () -> LinkBrowser.browse("https://prices.runescape.wiki/osrs/item/" + i.getId()));
	}

	private static String ageOf(long epochSeconds)
	{
		return epochSeconds > 0 ? UiUtil.ago(epochSeconds * 1000) : "unknown";
	}
}
