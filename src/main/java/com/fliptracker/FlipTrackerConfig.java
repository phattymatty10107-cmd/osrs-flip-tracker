package com.fliptracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(FlipTrackerConfig.GROUP)
public interface FlipTrackerConfig extends Config
{
	String GROUP = "fliptracker";

	@ConfigSection(
		name = "Margin finder",
		description = "Filters for the Margins and Best 24h tabs",
		position = 0
	)
	String marginSection = "margins";

	@ConfigSection(
		name = "Notifications",
		description = "Alerts for buy limits and watchlist margins",
		position = 1
	)
	String notifySection = "notifications";

	@Range(min = 1, max = 60)
	@Units(Units.MINUTES)
	@ConfigItem(
		keyName = "refreshMinutes",
		name = "Price refresh",
		description = "How often to pull prices from the OSRS Wiki",
		position = 0,
		section = marginSection
	)
	default int refreshMinutes()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "minVolume",
		name = "Min hourly volume",
		description = "Hide items traded fewer times than this in the last hour (Margins tab)",
		position = 1,
		section = marginSection
	)
	default int minVolume()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "minDailyVolume",
		name = "Min daily volume",
		description = "Hide items traded fewer times than this in the last 24 hours (Best 24h tab)",
		position = 2,
		section = marginSection
	)
	default int minDailyVolume()
	{
		return 1000;
	}

	@ConfigItem(
		keyName = "minPrice",
		name = "Min price",
		description = "Ignore items cheaper than this",
		position = 3,
		section = marginSection
	)
	default int minPrice()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "maxPrice",
		name = "Max price",
		description = "Ignore items more expensive than this (set to your cash stack). 0 = no limit",
		position = 4,
		section = marginSection
	)
	default int maxPrice()
	{
		return 0;
	}

	@Range(max = 1440)
	@Units(Units.MINUTES)
	@ConfigItem(
		keyName = "maxPriceAge",
		name = "Max price age",
		description = "Ignore items whose last instant buy/sell is older than this. 0 = off",
		position = 5,
		section = marginSection
	)
	default int maxPriceAge()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "includeMembers",
		name = "Include members items",
		description = "Show members-only items",
		position = 6,
		section = marginSection
	)
	default boolean includeMembers()
	{
		return true;
	}

	@Range(min = 10, max = 200)
	@ConfigItem(
		keyName = "maxResults",
		name = "Results shown",
		description = "How many items to list",
		position = 7,
		section = marginSection
	)
	default int maxResults()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "notifyLimitReset",
		name = "Buy limit reset",
		description = "Notify when an item's 4-hour buy limit resets",
		position = 0,
		section = notifySection
	)
	default boolean notifyLimitReset()
	{
		return true;
	}

	@ConfigItem(
		keyName = "marginAlert",
		name = "Watchlist margin alert",
		description = "Notify when a watched item's margin (after tax) reaches this many gp. 0 = off",
		position = 1,
		section = notifySection
	)
	default int marginAlert()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "watchlist",
		name = "",
		description = "",
		hidden = true
	)
	default String watchlist()
	{
		return "";
	}
}
