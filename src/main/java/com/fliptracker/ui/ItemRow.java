package com.fliptracker.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;

/** One row in a list: item icon, a title line with a coloured value, and two detail lines. */
class ItemRow extends JPanel
{
	private final JPopupMenu menu = new JPopupMenu();

	ItemRow(ItemManager itemManager, int itemId, String title, String value, Color valueColor,
		String line2, String line3, String tooltip)
	{
		setLayout(new BorderLayout(6, 0));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 5, 5, 6));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(36, 32));
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		if (itemManager != null && itemId > 0)
		{
			itemManager.getImage(itemId).addTo(icon);
		}
		add(icon, BorderLayout.WEST);

		JPanel lines = new JPanel(new GridLayout(line3 == null ? 2 : 3, 1, 0, 1));
		lines.setOpaque(false);
		lines.setMinimumSize(new Dimension(0, 0));

		JPanel top = new JPanel(new BorderLayout(4, 0));
		top.setOpaque(false);
		top.add(UiUtil.label(title, Color.WHITE, false), BorderLayout.CENTER);
		if (value != null)
		{
			top.add(UiUtil.label(value, valueColor, false), BorderLayout.EAST);
		}
		lines.add(top);
		lines.add(UiUtil.label(line2, UiUtil.MUTED, false));
		if (line3 != null)
		{
			lines.add(UiUtil.label(line3, UiUtil.MUTED, false));
		}
		add(lines, BorderLayout.CENTER);

		if (tooltip != null)
		{
			setToolTipText(tooltip);
		}

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON1)
				{
					showMenu(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					showMenu(e);
				}
			}
		});
	}

	ItemRow action(String text, Runnable action)
	{
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(e -> action.run());
		menu.add(item);
		return this;
	}

	private void showMenu(MouseEvent e)
	{
		if (menu.getComponentCount() > 0 && !menu.isVisible())
		{
			menu.show(this, e.getX(), e.getY());
		}
	}
}
