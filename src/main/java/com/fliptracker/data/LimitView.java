package com.fliptracker.data;

/** Read-only view of an active buy limit window, for the UI. */
public class LimitView
{
	private final int itemId;
	private final String itemName;
	private final int bought;
	/** 0 if unknown. */
	private final int limit;
	private final long windowStart;
	private final long resetAt;

	public int remaining()
	{
		return limit > 0 ? Math.max(0, limit - bought) : -1;
	}

	public LimitView(int itemId, String itemName, int bought, int limit, long windowStart, long resetAt)
	{
		this.itemId = itemId;
		this.itemName = itemName;
		this.bought = bought;
		this.limit = limit;
		this.windowStart = windowStart;
		this.resetAt = resetAt;
	}

	public int getItemId()
	{
		return itemId;
	}

	public String getItemName()
	{
		return itemName;
	}

	public int getBought()
	{
		return bought;
	}

	public int getLimit()
	{
		return limit;
	}

	public long getWindowStart()
	{
		return windowStart;
	}

	public long getResetAt()
	{
		return resetAt;
	}
}
