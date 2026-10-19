package com.fliptracker.data;

/**
 * Grand Exchange sales tax: 2% of the sell price per item, rounded down,
 * capped at 5m per item. Items selling for under 50gp are exempt, as are bonds.
 */
public final class GeTax
{
	public static final int BOND_ID = 13190;
	private static final long CAP = 5_000_000L;

	private GeTax()
	{
	}

	public static long perItem(int itemId, long price)
	{
		if (itemId == BOND_ID || price < 50)
		{
			return 0;
		}
		return Math.min(price * 2 / 100, CAP);
	}

	/** Profit per item when buying at {@code buy} and selling at {@code sell}. */
	public static long margin(int itemId, long buy, long sell)
	{
		return sell - perItem(itemId, sell) - buy;
	}
}
