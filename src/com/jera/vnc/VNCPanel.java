package com.jera.vnc;

import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: jbrewer
 * Date: Sep 12, 2003
 * Time: 7:19:25 PM
 * To change this template use Options | File Templates.
 */
public class VNCPanel extends JComponent implements VNCObserver {
	VNC vnc;
	private BufferedImage screenImage;

	public VNCPanel(VNC theVNC) {
		super();

		vnc = theVNC;

		addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
			}

			public void mousePressed(MouseEvent e) {
				try {
					vnc.sendPointerEvent(1, e.getX(), e.getY());
				}
				catch (IOException e1) {
					e1.printStackTrace();  //To change body of catch statement use Options | File Templates.
				}
			}

			public void mouseReleased(MouseEvent e) {
				try {
					vnc.sendPointerEvent(0, e.getX(), e.getY());
				}
				catch (IOException e1) {
					e1.printStackTrace();  //To change body of catch statement use Options | File Templates.
				}
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}
		});

		addMouseMotionListener(new MouseMotionListener() {
			public void mouseDragged(MouseEvent e) {
				try {
					vnc.sendPointerEvent(1, e.getX(), e.getY());
				}
				catch (IOException e1) {
					e1.printStackTrace();  //To change body of catch statement use Options | File Templates.
				}
			}

			public void mouseMoved(MouseEvent e) {
				try {
					vnc.sendPointerEvent(0, e.getX(), e.getY());
				}
				catch (IOException e1) {
					e1.printStackTrace();  //To change body of catch statement use Options | File Templates.
				}
			}
		});

		addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				try {
					vnc.sendKeyEvent(true, translateKey(e));
				}
				catch (IOException e1) {
					e1.printStackTrace();  //To change body of catch statement use Options | File Templates.
				}
			}

			public void keyReleased(KeyEvent e) {
				try {
					vnc.sendKeyEvent(false, translateKey(e));
				}
				catch (IOException e1) {
					e1.printStackTrace();  //To change body of catch statement use Options | File Templates.
				}
			}
		});
	}

	public void addNotify() {
		super.addNotify();
		requestFocus();
	}

	public void setImage(BufferedImage theImage) {
		screenImage = theImage;
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (screenImage != null) {
			g.drawImage(screenImage, 0, 0, null);
		}
	}

	public void imageChanged() {
		repaint();
	}

	public void setScreenSize(final int width, final int height) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setPreferredSize(new Dimension(width, height));
				revalidate();
			}
		});
	}

	public static int translateKey(KeyEvent awtKey) {
		switch(awtKey.getKeyCode()) {
			case KeyEvent.VK_BACK_SPACE:
				return KeySym.BACKSPACE;
			case KeyEvent.VK_TAB:
				return KeySym.TAB;
			case KeyEvent.VK_ENTER:
				return KeySym.ENTER;
			case KeyEvent.VK_ESCAPE:
				return KeySym.ESCAPE;
			case KeyEvent.VK_INSERT:
				return KeySym.INSERT;
			case KeyEvent.VK_DELETE:
				return KeySym.DELETE;
			case KeyEvent.VK_HOME:
				return KeySym.HOME;
			case KeyEvent.VK_END:
				return KeySym.END;
			case KeyEvent.VK_PAGE_UP:
				return KeySym.PAGE_UP;
			case KeyEvent.VK_PAGE_DOWN:
				return KeySym.PAGE_DOWN;
			case KeyEvent.VK_LEFT:
				return KeySym.LEFT;
			case KeyEvent.VK_UP:
				return KeySym.UP;
			case KeyEvent.VK_RIGHT:
				return KeySym.RIGHT;
			case KeyEvent.VK_DOWN:
				return KeySym.DOWN;
			case KeyEvent.VK_F1:
				return KeySym.F1;
			case KeyEvent.VK_F2:
				return KeySym.F2;
			case KeyEvent.VK_F3:
				return KeySym.F3;
			case KeyEvent.VK_F4:
				return KeySym.F4;
			case KeyEvent.VK_F5:
				return KeySym.F5;
			case KeyEvent.VK_F6:
				return KeySym.F6;
			case KeyEvent.VK_F7:
				return KeySym.F7;
			case KeyEvent.VK_F8:
				return KeySym.F8;
			case KeyEvent.VK_F9:
				return KeySym.F9;
			case KeyEvent.VK_F10:
				return KeySym.F10;
			case KeyEvent.VK_F11:
				return KeySym.F11;
			case KeyEvent.VK_F12:
				return KeySym.F12;
			default:
				return awtKey.getKeyChar();
		}
	}
}
