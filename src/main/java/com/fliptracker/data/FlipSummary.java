package com.fliptracker.data;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Profit worked out from the trade log. Sells are matched to earlier buys of
 * the same item, oldest first (FIFO). Sells with no matching buy are ignored.
 */
public class FlipSummary
{
	public static final FlipSummary EMPTY = new FlipSummary(0, 0, 0, 0, Collections.emptyList(), Collections.emptyList());
	private static final int RECENT_LIMIT = 100;

	private final long totalProfit;
	private final long todayProfit;
	private final int flipCount;
	private final long taxPaid;
	/** Newest first. */
	private final List<CompletedFlip> recent;
	private final List<OpenPosition> open;

	public static class CompletedFlip
	{
		private final int itemId;
		private final String itemName;
		private final int quantity;
		private final long avgBuy;
		private final long sellPrice;
		private final long profit;
		private final long time;

		public CompletedFlip(int itemId, String itemName, int quantity, long avgBuy, long sellPrice, long profit, long time)
		{
			this.itemId = itemId;
			this.itemName = itemName;
			this.quantity = quantity;
			this.avgBuy = avgBuy;
			this.sellPrice = sellPrice;
			this.profit = profit;
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

		public int getQuantity()
		{
			return quantity;
		}

		public long getAvgBuy()
		{
			return avgBuy;
		}

		public long getSellPrice()
		{
			return sellPrice;
		}

		public long getProfit()
		{
			return profit;
		}

		public long getTime()
		{
			return time;
		}
	}

	public static class OpenPosition
	{
		private final int itemId;
		private final String itemName;
		private final int quantity;
		private final long avgBuy;

		public OpenPosition(int itemId, String itemName, int quantity, long avgBuy)
		{
			this.itemId = itemId;
			this.itemName = itemName;
			this.quantity = quantity;
			this.avgBuy = avgBuy;
		}

		public int getItemId()
		{
			return itemId;
		}

		public String getItemName()
		{
			return itemName;
		}

		public int getQuantity()
		{
			return quantity;
		}

		public long getAvgBuy()
		{
			return avgBuy;
		}
	}

	public static FlipSummary compute(List<AccountData.Trade> trades)
	{
		if (trades == null || trades.isEmpty())
		{
			return EMPTY;
		}

		List<AccountData.Trade> sorted = new ArrayList<>(trades);
		sorted.sort(Comparator.comparingLong(AccountData.Trade::getTime));

		long startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
		Map<Integer, Deque<long[]>> lots = new HashMap<>(); // [qty, price]
		Map<Integer, String> names = new HashMap<>();
		List<CompletedFlip> flips = new ArrayList<>();
		long total = 0;
		long today = 0;
		long tax = 0;

		for (AccountData.Trade t : sorted)
		{
			names.put(t.getItemId(), t.getItemName());
			Deque<long[]> queue = lots.computeIfAbsent(t.getItemId(), k -> new ArrayDeque<>());

			if (t.isBuy())
			{
				queue.addLast(new long[]{t.getQuantity(), t.getPrice()});
				continue;
			}

			long remaining = t.getQuantity();
			long matched = 0;
			long cost = 0;
			while (remaining > 0 && !queue.isEmpty())
			{
				long[] lot = queue.peekFirst();
				long take = Math.min(remaining, lot[0]);
				matched += take;
				cost += take * lot[1];
				lot[0] -= take;
				remaining -= take;
				if (lot[0] == 0)
				{
					queue.pollFirst();
				}
			}

			if (matched == 0)
			{
				continue;
			}

			long taxEach = GeTax.perItem(t.getItemId(), t.getPrice());
			long profit = matched * (t.getPrice() - taxEach) - cost;
			total += profit;
			tax += matched * taxEach;
			if (t.getTime() >= startOfToday)
			{
				today += profit;
			}
			flips.add(new CompletedFlip(t.getItemId(), t.getItemName(), (int) matched,
				cost / matched, t.getPrice(), profit, t.getTime()));
		}

		List<OpenPosition> open = new ArrayList<>();
		for (Map.Entry<Integer, Deque<long[]>> e : lots.entrySet())
		{
			long qty = 0;
			long cost = 0;
			for (long[] lot : e.getValue())
			{
				qty += lot[0];
				cost += lot[0] * lot[1];
			}
			if (qty > 0)
			{
				open.add(new OpenPosition(e.getKey(), names.get(e.getKey()), (int) qty, cost / qty));
			}
		}
		open.sort(Comparator.comparing(OpenPosition::getItemName, Comparator.nullsLast(String::compareToIgnoreCase)));

		int count = flips.size();
		Collections.reverse(flips);
		List<CompletedFlip> recent = flips.size() > RECENT_LIMIT ? new ArrayList<>(flips.subList(0, RECENT_LIMIT)) : flips;

		return new FlipSummary(total, today, count, tax,
			Collections.unmodifiableList(recent), Collections.unmodifiableList(open));
	}

	public FlipSummary(long totalProfit, long todayProfit, int flipCount, long taxPaid, List<CompletedFlip> recent, List<OpenPosition> open)
	{
		this.totalProfit = totalProfit;
		this.todayProfit = todayProfit;
		this.flipCount = flipCount;
		this.taxPaid = taxPaid;
		this.recent = recent;
		this.open = open;
	}

	public long getTotalProfit()
	{
		return totalProfit;
	}

	public long getTodayProfit()
	{
		return todayProfit;
	}

	public int getFlipCount()
	{
		return flipCount;
	}

	public long getTaxPaid()
	{
		return taxPaid;
	}

	public List<CompletedFlip> getRecent()
	{
		return recent;
	}

	public List<OpenPosition> getOpen()
	{
		return open;
	}
}
