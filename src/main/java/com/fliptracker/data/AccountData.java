package com.fliptracker.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.GrandExchangeOfferState;

/** Everything saved to disk for one account. */
public class AccountData
{
	public static final int SLOTS = 8;
	public static final int MAX_TRADES = 5000;

	private List<Trade> trades = new ArrayList<>();
	private Map<Integer, LimitWindow> limits = new HashMap<>();
	private SlotSnapshot[] slots = new SlotSnapshot[SLOTS];

	public List<Trade> getTrades()
	{
		return trades;
	}

	public Map<Integer, LimitWindow> getLimits()
	{
		return limits;
	}

	public SlotSnapshot[] getSlots()
	{
		return slots;
	}

	/** Fill in anything missing after loading an older or partial file. */
	public AccountData normalize()
	{
		if (trades == null)
		{
			trades = new ArrayList<>();
		}
		if (limits == null)
		{
			limits = new HashMap<>();
		}
		if (slots == null || slots.length != SLOTS)
		{
			SlotSnapshot[] fixed = new SlotSnapshot[SLOTS];
			if (slots != null)
			{
				System.arraycopy(slots, 0, fixed, 0, Math.min(slots.length, SLOTS));
			}
			slots = fixed;
		}
		return this;
	}

	public void addTrade(Trade trade)
	{
		trades.add(trade);
		if (trades.size() > MAX_TRADES)
		{
			trades.subList(0, trades.size() - MAX_TRADES).clear();
		}
	}

	/** One fill (or partial fill) of a GE offer. */
	public static class Trade
	{
		private int itemId;
		private String itemName;
		private boolean buy;
		private int quantity;
		/** Average price per item for this fill. */
		private long price;
		private long time;

		public Trade()
		{
		}

		public Trade(int itemId, String itemName, boolean buy, int quantity, long price, long time)
		{
			this.itemId = itemId;
			this.itemName = itemName;
			this.buy = buy;
			this.quantity = quantity;
			this.price = price;
			this.time = time;
		}

		public int getItemId()
		{
			return itemId;
		}

		public String getItemName()
		{
			return itemName;
		}

		public boolean isBuy()
		{
			return buy;
		}

		public int getQuantity()
		{
			return quantity;
		}

		public long getPrice()
		{
			return price;
		}

		public long getTime()
		{
			return time;
		}
	}

	/** The 4-hour buy limit window for one item. */
	public static class LimitWindow
	{
		private int itemId;
		private String itemName;
		private long windowStart;
		private int bought;

		public LimitWindow()
		{
		}

		public LimitWindow(int itemId, String itemName, long windowStart, int bought)
		{
			this.itemId = itemId;
			this.itemName = itemName;
			this.windowStart = windowStart;
			this.bought = bought;
		}

		public int getItemId()
		{
			return itemId;
		}

		public String getItemName()
		{
			return itemName;
		}

		public long getWindowStart()
		{
			return windowStart;
		}

		public int getBought()
		{
			return bought;
		}

		public void setBought(int bought)
		{
			this.bought = bought;
		}
	}

	/** Last seen state of a GE slot, so we can work out what changed. */
	public static class SlotSnapshot
	{
		private int itemId;
		private boolean buy;
		private long price;
		private int totalQuantity;
		private int quantitySold;
		private long spent;
		private GrandExchangeOfferState state;

		public SlotSnapshot()
		{
		}

		public SlotSnapshot(int itemId, boolean buy, long price, int totalQuantity, int quantitySold, long spent,
			GrandExchangeOfferState state)
		{
			this.itemId = itemId;
			this.buy = buy;
			this.price = price;
			this.totalQuantity = totalQuantity;
			this.quantitySold = quantitySold;
			this.spent = spent;
			this.state = state;
		}

		public int getItemId()
		{
			return itemId;
		}

		public int getQuantitySold()
		{
			return quantitySold;
		}

		public long getSpent()
		{
			return spent;
		}

		public boolean sameOffer(SlotSnapshot o)
		{
			return o != null && itemId == o.itemId && buy == o.buy
				&& price == o.price && totalQuantity == o.totalQuantity;
		}

		public boolean isFinished()
		{
			return state != GrandExchangeOfferState.BUYING && state != GrandExchangeOfferState.SELLING;
		}
	}
}
