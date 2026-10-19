package com.fliptracker.data;

/** Everything we know about one item's prices, merged from the wiki endpoints. */
public class ItemInfo
{
	/** Each item can be bought up to its limit once per 4 hours: 6 windows a day. */
	public static final int LIMIT_WINDOWS_PER_DAY = 6;

	private final int id;
	private final String name;
	private final boolean members;
	/** GE buy limit per 4 hours, 0 if unknown. */
	private final int limit;

	/** Latest instant-buy price (what you sell at). */
	private final long high;
	/** Latest instant-sell price (what you buy at). */
	private final long low;
	/** Epoch seconds of the latest instant buy / sell. */
	private final long highTime;
	private final long lowTime;

	/** Trades in the last hour (both sides). */
	private final long hourVolume;

	/** 24h average instant-buy / instant-sell prices. */
	private final long dayAvgHigh;
	private final long dayAvgLow;
	/** 24h volumes, split by side. */
	private final long dayHighVolume;
	private final long dayLowVolume;

	public ItemInfo(int id, String name, boolean members, int limit,
		long high, long low, long highTime, long lowTime, long hourVolume,
		long dayAvgHigh, long dayAvgLow, long dayHighVolume, long dayLowVolume)
	{
		this.id = id;
		this.name = name;
		this.members = members;
		this.limit = limit;
		this.high = high;
		this.low = low;
		this.highTime = highTime;
		this.lowTime = lowTime;
		this.hourVolume = hourVolume;
		this.dayAvgHigh = dayAvgHigh;
		this.dayAvgLow = dayAvgLow;
		this.dayHighVolume = dayHighVolume;
		this.dayLowVolume = dayLowVolume;
	}

	public int getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public boolean isMembers()
	{
		return members;
	}

	public int getLimit()
	{
		return limit;
	}

	public long getHigh()
	{
		return high;
	}

	public long getLow()
	{
		return low;
	}

	public long getHighTime()
	{
		return highTime;
	}

	public long getLowTime()
	{
		return lowTime;
	}

	public long getHourVolume()
	{
		return hourVolume;
	}

	public long getDayAvgHigh()
	{
		return dayAvgHigh;
	}

	public long getDayAvgLow()
	{
		return dayAvgLow;
	}

	public long getDayHighVolume()
	{
		return dayHighVolume;
	}

	public long getDayLowVolume()
	{
		return dayLowVolume;
	}

	public boolean hasPrices()
	{
		return high > 0 && low > 0;
	}

	public long tax()
	{
		return GeTax.perItem(id, high);
	}

	/** Margin per item after tax, using the latest prices. */
	public long margin()
	{
		return GeTax.margin(id, low, high);
	}

	public double roi()
	{
		return low > 0 ? margin() * 100.0 / low : 0;
	}

	/** Rough profit you could make right now in one limit window. */
	public long potentialProfit()
	{
		long qty = limit > 0 ? Math.min(limit, hourVolume) : hourVolume;
		return margin() * qty;
	}

	public boolean hasDayData()
	{
		return dayAvgHigh > 0 && dayAvgLow > 0;
	}

	public long dayVolume()
	{
		return dayHighVolume + dayLowVolume;
	}

	/** Margin per item after tax, using 24h average prices. Steadier than the latest snapshot. */
	public long dayMargin()
	{
		return GeTax.margin(id, dayAvgLow, dayAvgHigh);
	}

	public double dayRoi()
	{
		return dayAvgLow > 0 ? dayMargin() * 100.0 / dayAvgLow : 0;
	}

	/**
	 * How many you could realistically flip in 24 hours: limited by the buy limit
	 * (6 windows a day) and by how many trade on each side per day.
	 */
	public long dayFlipQty()
	{
		long byVolume = Math.min(dayLowVolume, dayHighVolume);
		if (limit <= 0)
		{
			return byVolume;
		}
		return Math.min((long) limit * LIMIT_WINDOWS_PER_DAY, byVolume);
	}

	/** Estimated profit from flipping this item for a full day. */
	public long dayProfit()
	{
		return Math.max(0, dayMargin()) * dayFlipQty();
	}

	/** Gold needed to buy one full limit at the 24h average buy price. */
	public long capitalPerLimit()
	{
		long qty = limit > 0 ? limit : dayFlipQty();
		return dayAvgLow * qty;
	}
}
