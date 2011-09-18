package info.svitkine.alexei.wage;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

public class MenuBarRenderer extends JComponent {
	private static final int HEIGHT = 19;
	private static final int PADDING = 6;
	private static final int ITEM_HEIGHT = 19;

	private JMenuBar menubar;
	private int[] offsets;
	private int[] spans;
	private int pressedMenu;
	private int pressedItem;
	
	public MenuBarRenderer(JMenuBar menubar) {
		this.menubar = menubar;
		Font f = new Font("Chicago", Font.PLAIN, 13); 
		setFont(f);
		FontMetrics m = getFontMetrics(f);
		int menus = menubar.getMenuCount();
		offsets = new int[menus];
		spans = new int[offsets.length];
		int x = 20;
		for (int i = 0; i < menubar.getMenuCount(); i++) {
			JMenu menu = menubar.getMenu(i);
			spans[i] = m.stringWidth(menu.getText());
			offsets[i] = x;
			x += spans[i] + 12;
		}
		pressedMenu = -1;
		pressedItem = -1;
	}
	
	private String getAcceleratorString(JMenuItem item) {
		KeyStroke accelerator = item.getAccelerator();
		String text = null;
		if (accelerator != null) {
			text = "      \u2318";
			String t = accelerator.toString();
			text += t.charAt(t.length() - 1);
		}
		return text;
	}
	
	private int calculateMenuWidth(JMenu menu) {
		int maxWidth = 0;
		Font f = getFont();
		for (int j = 0; j < menu.getItemCount(); j++) {
			JMenuItem item = menu.getItem(j);
			if (item != null) {
				f = new Font(f.getFamily(), item.getFont().getStyle(), f.getSize());
				FontMetrics m = getFontMetrics(f);
				String text = item.getText();
				String acceleratorText = getAcceleratorString(item);
				if (acceleratorText != null) {
					text += acceleratorText;
				}
				int width = m.stringWidth(text);
				if (width > maxWidth) {
					maxWidth = width;
				}
			}
		}
		return maxWidth;
	}

	private Rectangle getMenuBounds(int menuIndex) {
		JMenu menu = menubar.getMenu(menuIndex);
		// TODO: cache maxWidth
		int maxWidth = calculateMenuWidth(menu);
		int x = offsets[menuIndex] - PADDING;
		int y = HEIGHT;
		int w = maxWidth + PADDING * 3;
		int h = menu.getItemCount() * 20;
		return new Rectangle(x, y, w, h);
	}
	
	@Override
	public void paint(Graphics g) {
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), HEIGHT);
		g.setColor(Color.BLACK);
		g.fillRect(0, HEIGHT, getWidth(), 1);
		Font f = getFont();
		g.setFont(f);
		// TODO: generalize this... have each 'menu' have bounds and a styled string...
		for (int i = 0; i < menubar.getMenuCount(); i++) {
			JMenu menu = menubar.getMenu(i);
			g.setColor(Color.BLACK);
			if (pressedMenu == i) {
				g.fillRect(offsets[i] - 6, 1, spans[i] + 12, HEIGHT - 1);
				g.setColor(Color.WHITE);
			}
			g.drawString(menu.getText(), offsets[i], 14);
			if (pressedMenu == i) {
				FontMetrics m = getFontMetrics(f);
				Rectangle bounds = getMenuBounds(i);
				g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
				g.setColor(Color.BLACK);
				g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
				int y = 33;
				for (int j = 0; j < menu.getItemCount(); j++) {
					JMenuItem item = menu.getItem(j);
					g.setColor(Color.BLACK);
					if (pressedItem == j) {
						g.fillRect(bounds.x, y - 14, bounds.width, ITEM_HEIGHT);
						g.setColor(Color.WHITE);
					} else if (item != null && !item.isEnabled()) {
						g.setColor(Color.GRAY);
					}
					if (item != null) {
						Graphics2D g2 = ((Graphics2D)g);
						f = new Font(f.getFamily(), item.getFont().getStyle(), f.getSize());
						FontRenderContext frc = g2.getFontRenderContext();
						TextLayout tl = new TextLayout(item.getText(), f, frc);
						tl.draw(g2, offsets[i] + PADDING, y);
						String acceleratorText = getAcceleratorString(item);
						if (acceleratorText != null) {
							int width = m.stringWidth(acceleratorText);
							tl = new TextLayout(acceleratorText, f, frc);
							tl.draw(g2, bounds.x + bounds.width - width - PADDING, y);							
						}
					} else {
						g.drawLine(bounds.x, y - 7, bounds.x + bounds.width, y - 7);
					}
					y += ITEM_HEIGHT;
				}
			}
		}
	}

	private int getMenuAt(int x, int y) {
		if (y < HEIGHT) {
			for (int i = 0; i < menubar.getMenuCount(); i++) {
				if (x > offsets[i] - 6 && x - offsets[i] < spans[i] + 6) {
					return i;
				}
			}
		}
		return -1;
	}
	
	private int getMenuItemAt(int x, int y) {
		if (pressedMenu != -1) {
			Rectangle bounds = getMenuBounds(pressedMenu);
			if (bounds.contains(x, y)) {
				int dy = y - bounds.y;
				int itemIndex = dy / ITEM_HEIGHT;
				if (menubar.getMenu(pressedMenu).getItem(itemIndex).isEnabled()) {
					return itemIndex;
				}
			}
		}
		return -1;
	}

	public boolean handleMouseEvent(MouseEvent event, int type) {
		if (type == MouseEvent.MOUSE_PRESSED || type == MouseEvent.MOUSE_DRAGGED)
			return mousePressed(event);
		else if (type == MouseEvent.MOUSE_RELEASED)
			return mouseReleased(event);
		return false;
	}
	
	private boolean mousePressed(MouseEvent event) {
		int menuIndex = getMenuAt(event.getX(), event.getY());
		if (menuIndex != -1) {
			if (pressedMenu != menuIndex) {
				if (pressedMenu != -1)
					menubar.getMenu(pressedMenu).setSelected(false);
				pressedMenu = menuIndex;
				if (pressedMenu != -1)
					menubar.getMenu(pressedMenu).setSelected(true);
				repaint();
			}
			return true;
		}

		int menuItemIndex = getMenuItemAt(event.getX(), event.getY());
		if (pressedItem != menuItemIndex) {
			pressedItem = menuItemIndex;
			repaint();
		}
		
		return pressedItem != -1;
	}

	private boolean mouseReleased(MouseEvent event) {
		boolean result = false;
		if (pressedMenu != -1 && pressedItem != -1) {
			JMenu menu = menubar.getMenu(pressedMenu);
			JMenuItem item = menu.getItem(pressedItem);
			item.doClick();
			result = true;
		}
		if (pressedMenu != -1)
			menubar.getMenu(pressedMenu).setSelected(false);
		pressedMenu = -1;
		pressedItem = -1;
		repaint();
		return result;
	}
}