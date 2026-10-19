package com.fliptracker.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.QuantityFormatter;

final class UiUtil
{
	static final Color PROFIT = ColorScheme.PROGRESS_COMPLETE_COLOR;
	static final Color LOSS = ColorScheme.PROGRESS_ERROR_COLOR;
	static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;

	private UiUtil()
	{
	}

	/** Exact with commas under 10m, compact above. */
	static String gp(long v)
	{
		return Math.abs(v) >= 10_000_000 ? QuantityFormatter.quantityToStackSize(v) : QuantityFormatter.formatNumber(v);
	}

	static String compact(long v)
	{
		return QuantityFormatter.quantityToStackSize(v);
	}

	static String signedGp(long v)
	{
		return (v > 0 ? "+" : "") + gp(v);
	}

	static Color profitColor(long v)
	{
		return v > 0 ? PROFIT : v < 0 ? LOSS : MUTED;
	}

	static String duration(long ms)
	{
		long s = Math.max(0, ms / 1000);
		long h = s / 3600;
		long m = (s % 3600) / 60;
		if (h > 0)
		{
			return h + "h " + m + "m";
		}
		if (m > 0)
		{
			return m + "m " + (s % 60) + "s";
		}
		return (s % 60) + "s";
	}

	static String ago(long epochMs)
	{
		long ms = System.currentTimeMillis() - epochMs;
		if (ms < 60_000)
		{
			return "just now";
		}
		long m = ms / 60_000;
		if (m < 60)
		{
			return m + "m ago";
		}
		long h = m / 60;
		if (h < 48)
		{
			return h + "h ago";
		}
		return (h / 24) + "d ago";
	}

	static JLabel label(String text, Color color, boolean bold)
	{
		JLabel l = new JLabel(text);
		l.setForeground(color);
		l.setFont(bold ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeSmallFont());
		return l;
	}

	static JLabel header(String text)
	{
		JLabel l = label(text, Color.WHITE, true);
		l.setBorder(new EmptyBorder(8, 2, 4, 2));
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	static JLabel emptyState(String text)
	{
		JLabel l = label("<html><div style='text-align:center'>" + text + "</div></html>", MUTED, false);
		l.setHorizontalAlignment(SwingConstants.CENTER);
		l.setBorder(new EmptyBorder(20, 8, 20, 8));
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	/** Vertical list that doesn't stretch its rows. */
	static JPanel verticalList()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBackground(ColorScheme.DARK_GRAY_COLOR);
		return p;
	}

	static void addRow(JPanel list, JComponent row)
	{
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		list.add(row);
		list.add(Box.createRigidArea(new Dimension(0, 4)));
	}

	static JScrollPane scroll(JPanel list)
	{
		// Track the viewport width so long rows shrink instead of getting clipped on the right.
		JPanel wrapper = new ScrollablePanel();
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(list, BorderLayout.NORTH);

		JScrollPane scroll = new JScrollPane(wrapper);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		return scroll;
	}

	private static class ScrollablePanel extends JPanel implements Scrollable
	{
		ScrollablePanel()
		{
			super(new BorderLayout());
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return visibleRect.height;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
}
