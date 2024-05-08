/*
 * This file is part of the L2ClientDat project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.forms;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.undo.UndoManager;

public class JPopupTextArea extends JTextArea
{
	private static final String COPY = "Copy (Ctrl + C)";
	private static final String CUT = "Cut (Ctrl + X)";
	private static final String PASTE = "Paste (Ctrl + V)";
	private static final String DELETE = "Delete";
	private static final String SELECT_ALL = "Select all (Ctrl + A)";
	private static final String GOTO = "Go to (Ctrl + G)";
	private static final String SEARCH = "Search (Ctrl + F)";
	
	private final List<Integer> _lineLength;
	protected final UndoManager _manager;
	
	public JPopupTextArea()
	{
		_lineLength = new ArrayList<>();
		addPopupMenu();
		_manager = new UndoManager();
		getDocument().addUndoableEditListener(_manager);
		
		final Action undo = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (_manager.canUndo())
				{
					_manager.undo();
				}
			}
		};
		
		final Action redo = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (_manager.canRedo())
				{
					_manager.redo();
				}
			}
		};
		
		final InputMap imap = getInputMap();
		imap.put(KeyStroke.getKeyStroke("ctrl Z"), "undo");
		imap.put(KeyStroke.getKeyStroke("ctrl Y"), "redo");
		final ActionMap amap = getActionMap();
		amap.put("undo", undo);
		amap.put("redo", redo);
	}
	
	public void discardAllEdits()
	{
		_manager.discardAllEdits();
	}
	
	public void cleanUp()
	{
		setText("");
		removeAll();
		discardAllEdits();
	}
	
	private void addPopupMenu()
	{
		final JPopupMenu menu = new JPopupMenu();
		final JMenuItem copyItem = new JMenuItem();
		copyItem.setAction(getActionMap().get("copy-to-clipboard"));
		copyItem.setText(COPY);
		
		final JMenuItem cutItem = new JMenuItem();
		cutItem.setAction(getActionMap().get("cut-to-clipboard"));
		cutItem.setText(CUT);
		
		final JMenuItem pasteItem = new JMenuItem();
		pasteItem.setAction(getActionMap().get("paste-from-clipboard"));
		pasteItem.setText(PASTE);
		
		final JMenuItem deleteItem = new JMenuItem();
		deleteItem.setAction(getActionMap().get("delete-previous"));
		deleteItem.setText(DELETE);
		
		final JMenuItem selectAllItem = new JMenuItem();
		selectAllItem.setAction(getActionMap().get("select-all"));
		selectAllItem.setText(SELECT_ALL);
		
		final JMenuItem selectLine = new JMenuItem();
		selectLine.addActionListener(e -> goToLine());
		selectLine.setText(GOTO);
		
		final JMenuItem selectFind = new JMenuItem();
		selectFind.addActionListener(e -> searchString());
		selectFind.setText(SEARCH);
		
		menu.add(copyItem);
		menu.add(cutItem);
		menu.add(pasteItem);
		menu.add(deleteItem);
		menu.add(new JSeparator());
		menu.add(selectAllItem);
		menu.add(selectLine);
		menu.add(selectFind);
		add(menu);
		
		addMouseListener(new PopupTriggerMouseListener(menu, this));
		addKeyListener(new KeyListen());
	}
	
	protected void goToLine()
	{
		int no = 0;
		boolean fnd;
		String lineno;
		do
		{
			fnd = true;
			lineno = JOptionPane.showInputDialog("Line number:");
			try
			{
				no = Integer.parseInt(lineno);
			}
			catch (Exception exp)
			{
				if (lineno != null)
				{
					JOptionPane.showMessageDialog(new Frame(), "Enter a valid line number", "Error", 0);
					fnd = false;
				}
			}
			
			if (no <= 0)
			{
				if (lineno == null)
				{
					continue;
				}
				
				JOptionPane.showMessageDialog(new Frame(), "Enter a valid line number", "Error", 0);
				fnd = false;
			}
		}
		
		while (!fnd);
		if (lineno != null)
		{
			getLinePosition();
			if ((no - 1) >= _lineLength.size())
			{
				JOptionPane.showMessageDialog(new Frame(), "Line number does not exist", "Error", 0);
			}
			else
			{
				try
				{
					requestFocus();
					setCaretPosition(_lineLength.get(no - 1));
				}
				catch (Exception exp)
				{
					JOptionPane.showMessageDialog(new Frame(), "Bad position", "Error", 0);
				}
			}
		}
	}
	
	protected void searchString()
	{
		final String lineno = JOptionPane.showInputDialog("Search string: ");
		if ((lineno == null) || lineno.isEmpty())
		{
			JOptionPane.showMessageDialog(new Frame(), "Enter a empty string", "Error", 0);
			return;
		}
		
		try
		{
			requestFocus();
			final String editorText = getText();
			final int start = editorText.indexOf(lineno, getSelectionEnd());
			if (start != -1)
			{
				setCaretPosition(start);
				moveCaretPosition(start + lineno.length());
				getCaret().setSelectionVisible(true);
			}
		}
		catch (Exception exp)
		{
			JOptionPane.showMessageDialog(new Frame(), "Bad position", "Error", 0);
		}
	}
	
	private void getLinePosition()
	{
		_lineLength.clear();
		final String txt = getText();
		final int width = getWidth();
		final StringTokenizer st = new StringTokenizer(txt, "\n ", true);
		String str = " ";
		int len = 0;
		_lineLength.add(0);
		while (st.hasMoreTokens())
		{
			final String token = st.nextToken();
			final int w = getGraphics().getFontMetrics(getGraphics().getFont()).stringWidth(str + token);
			if ((w > width) || (token.charAt(0) == '\n'))
			{
				len += str.length();
				if (token.charAt(0) == '\n')
				{
					_lineLength.add(len);
				}
				else
				{
					_lineLength.add(len - 1);
				}
				str = token;
			}
			else
			{
				str += token;
			}
		}
	}
	
	private static class PopupTriggerMouseListener extends MouseAdapter
	{
		private final JPopupMenu popup;
		private final JComponent component;
		
		public PopupTriggerMouseListener(JPopupMenu popup, JComponent component)
		{
			this.popup = popup;
			this.component = component;
		}
		
		private void showMenuIfPopupTrigger(MouseEvent e)
		{
			if (e.isPopupTrigger())
			{
				popup.show(component, e.getX() + 3, e.getY() + 3);
			}
		}
		
		@Override
		public void mousePressed(MouseEvent e)
		{
			showMenuIfPopupTrigger(e);
		}
		
		@Override
		public void mouseReleased(MouseEvent e)
		{
			showMenuIfPopupTrigger(e);
		}
	}
	
	private class KeyListen implements KeyListener
	{
		private boolean controlDown;
		private boolean gDown;
		private boolean fDown;
		
		public KeyListen()
		{
		}
		
		@Override
		public void keyTyped(KeyEvent e)
		{
		}
		
		@Override
		public void keyPressed(KeyEvent e)
		{
			if (e.getKeyCode() == 17)
			{
				controlDown = true;
			}
			else if (e.getKeyCode() == 71)
			{
				gDown = true;
			}
			else if (e.getKeyCode() == 70)
			{
				fDown = true;
			}
			
			if (controlDown)
			{
				if (gDown)
				{
					controlDown = false;
					gDown = false;
					goToLine();
				}
				else if (fDown)
				{
					controlDown = false;
					fDown = false;
					searchString();
				}
			}
		}
		
		@Override
		public void keyReleased(KeyEvent e)
		{
			if (e.getKeyCode() == 17)
			{
				controlDown = false;
			}
			else if (e.getKeyCode() == 71)
			{
				gDown = false;
			}
			else if (e.getKeyCode() == 70)
			{
				fDown = false;
			}
		}
	}
}
