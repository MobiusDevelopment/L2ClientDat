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
package org.l2jmobius;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.SplashScreen;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.l2jmobius.actions.ActionTask;
import org.l2jmobius.actions.MassRecryptor;
import org.l2jmobius.actions.MassTxtPacker;
import org.l2jmobius.actions.MassTxtUnpacker;
import org.l2jmobius.actions.OpenDat;
import org.l2jmobius.actions.SaveDat;
import org.l2jmobius.actions.SaveTxt;
import org.l2jmobius.clientcryptor.crypt.DatCrypter;
import org.l2jmobius.config.ConfigDebug;
import org.l2jmobius.config.ConfigWindow;
import org.l2jmobius.forms.JPopupTextArea;
import org.l2jmobius.util.Util;
import org.l2jmobius.xml.CryptVersionParser;
import org.l2jmobius.xml.DescriptorParser;

public class L2ClientDat extends JFrame
{
	private static final Logger LOGGER = Logger.getLogger(L2ClientDat.class.getName());
	
	private static final String DAT_STRUCTURE_STR = "Structures";
	public static final String ENABLED_STR = "Enabled";
	public static final String DISABLED_STR = "Disabled";
	private static final String OPEN_BTN_STR = "Open";
	private static final String SAVE_TXT_BTN_STR = "Save (TXT)";
	private static final String SAVE_DAT_BTN_STR = "Save (DAT)";
	private static final String DECRYPT_ALL_BTN_STR = "Unpack all";
	private static final String ENCRYPT_ALL_BTN_STR = "Pack all";
	private static final String PATCH_ALL_BTN_STR = "Patch all";
	private static final String SELECT_BTN_STR = "Select";
	private static final String FILE_SELECT_BTN_STR = "File select";
	private static final String ABORT_BTN_STR = "Abort";
	private static final String SOURCE_ENCRYPT_TYPE_STR = "Source";
	public static final String DETECT_STR = "Detect";
	public static final String NO_TRANSLATE_STR = "No translate";
	
	public static boolean DEV_MODE = false;
	
	private static SplashScreen _splashScreen = null;
	private static JTextArea _textPaneLog;
	private final ExecutorService _executorService = Executors.newCachedThreadPool();
	private final JPopupTextArea _textPaneMain;
	private final LineNumberingTextArea _lineNumberingTextArea;
	private final JComboBox<String> _jComboBoxChronicle;
	private final JComboBox<String> _jComboBoxEncrypt;
	private final JComboBox<String> _jComboBoxFormatter;
	private final ArrayList<JPanel> _actionPanels = new ArrayList<>();
	private final JButton _saveTxtButton;
	private final JButton _saveDatButton;
	private final JButton _abortTaskButton;
	private final JProgressBar _progressBar;
	
	private File _currentFileWindow = null;
	private ActionTask _progressTask = null;
	
	public static void main(String[] args)
	{
		// Create log folder.
		final File logFolder = new File(".", "log");
		logFolder.mkdir();
		
		// Create input stream for log file -- or store file data into memory.
		try (InputStream is = new FileInputStream(new File("./config/log.cfg")))
		{
			LogManager.getLogManager().readConfiguration(is);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, null, e);
		}
		
		_splashScreen = SplashScreen.getSplashScreen();
		DEV_MODE = Util.contains((Object[]) args, (Object) "-dev");
		
		ConfigWindow.load();
		ConfigDebug.load();
		
		CryptVersionParser.getInstance().parse();
		DescriptorParser.getInstance().parse();
		
		try
		{
			for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
			{
				if ("Nimbus".equals(info.getName()))
				{
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		}
		catch (Exception ex)
		{
			LOGGER.log(Level.SEVERE, null, ex);
		}
		
		EventQueue.invokeLater(L2ClientDat::new);
	}
	
	public L2ClientDat()
	{
		setTitle("L2ClientDat Editor - L2jMobius Edition");
		
		setMinimumSize(new Dimension(1000, 600));
		setSize(new Dimension(ConfigWindow.WINDOW_WIDTH, ConfigWindow.WINDOW_HEIGHT));
		getContentPane().setLayout(new BorderLayout());
		setDefaultCloseOperation(3);
		setLocationRelativeTo(null);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent evt)
			{
				ConfigWindow.save("WINDOW_HEIGHT", String.valueOf(getHeight()));
				ConfigWindow.save("WINDOW_WIDTH", String.valueOf(getWidth()));
				System.exit(0);
			}
		});
		
		final JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BorderLayout());
		
		final JPanel buttonPane2 = new JPanel();
		final JLabel structureLabel = new JLabel(DAT_STRUCTURE_STR);
		buttonPane2.add(structureLabel);
		_jComboBoxChronicle = new JComboBox<>();
		final String[] chronicles = DescriptorParser.getInstance().getChronicleNames().toArray(new String[0]);
		_jComboBoxChronicle.setModel(new DefaultComboBoxModel<>(chronicles));
		_jComboBoxChronicle.setSelectedItem(Util.contains((Object[]) chronicles, (Object) ConfigWindow.CURRENT_CHRONICLE) ? ConfigWindow.CURRENT_CHRONICLE : chronicles[chronicles.length - 1]);
		_jComboBoxChronicle.addActionListener(e -> saveComboBox(_jComboBoxChronicle, "CURRENT_CHRONICLE"));
		buttonPane2.add(_jComboBoxChronicle);
		
		final JLabel encryptLabel = new JLabel("Encrypt:");
		buttonPane2.add(encryptLabel);
		_jComboBoxEncrypt = new JComboBox<>();
		DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>(CryptVersionParser.getInstance().getEncryptKey().keySet().toArray(new String[0]));
		comboBoxModel.insertElementAt(SOURCE_ENCRYPT_TYPE_STR, 0);
		comboBoxModel.setSelectedItem(SOURCE_ENCRYPT_TYPE_STR);
		_jComboBoxEncrypt.setModel(comboBoxModel);
		_jComboBoxEncrypt.setSelectedItem(ConfigWindow.CURRENT_ENCRYPT);
		_jComboBoxEncrypt.addActionListener(e -> saveComboBox(_jComboBoxEncrypt, "CURRENT_ENCRYPT"));
		buttonPane2.add(_jComboBoxEncrypt);
		
		buttonPane.add(buttonPane2, "First");
		_actionPanels.add(buttonPane2);
		final JLabel inputFormatterLabel = new JLabel("Formatter:");
		buttonPane2.add(inputFormatterLabel);
		_jComboBoxFormatter = new JComboBox<>();
		comboBoxModel = new DefaultComboBoxModel<>(new String[]
		{
			"Enabled",
			"Disabled"
		});
		_jComboBoxFormatter.setModel(comboBoxModel);
		_jComboBoxFormatter.setSelectedItem(ConfigWindow.CURRENT_FORMATTER);
		_jComboBoxFormatter.addActionListener(e -> saveComboBox(_jComboBoxFormatter, "CURRENT_FORMATTER"));
		buttonPane2.add(_jComboBoxFormatter);
		
		final JPanel buttonPane3 = new JPanel();
		final JButton open = new JButton();
		open.setText(OPEN_BTN_STR);
		open.addActionListener(this::openSelectFileWindow);
		buttonPane3.add(open);
		(_saveTxtButton = new JButton()).setText(SAVE_TXT_BTN_STR);
		_saveTxtButton.addActionListener(this::saveTxtActionPerformed);
		_saveTxtButton.setEnabled(false);
		buttonPane3.add(_saveTxtButton);
		(_saveDatButton = new JButton()).setText(SAVE_DAT_BTN_STR);
		_saveDatButton.addActionListener(this::saveDatActionPerformed);
		_saveDatButton.setEnabled(false);
		buttonPane3.add(_saveDatButton);
		final JButton massTxtUnpack = new JButton();
		massTxtUnpack.setText(DECRYPT_ALL_BTN_STR);
		massTxtUnpack.addActionListener(this::massTxtUnpackActionPerformed);
		buttonPane3.add(massTxtUnpack);
		final JButton massTxtPack = new JButton();
		massTxtPack.setText(ENCRYPT_ALL_BTN_STR);
		massTxtPack.addActionListener(this::massTxtPackActionPerformed);
		buttonPane3.add(massTxtPack);
		final JButton massRecrypt = new JButton();
		massRecrypt.setText(PATCH_ALL_BTN_STR);
		massRecrypt.addActionListener(this::massRecryptActionPerformed);
		buttonPane3.add(massRecrypt);
		buttonPane.add(buttonPane3);
		_actionPanels.add(buttonPane3);
		
		final JPanel progressPane = new JPanel();
		(_progressBar = new JProgressBar(0, 100)).setPreferredSize(new Dimension(300, 25));
		_progressBar.setValue(0);
		_progressBar.setStringPainted(true);
		progressPane.add(_progressBar);
		
		(_abortTaskButton = new JButton()).setText(ABORT_BTN_STR);
		_abortTaskButton.addActionListener(this::abortActionPerformed);
		_abortTaskButton.setEnabled(false);
		progressPane.add(_abortTaskButton);
		buttonPane.add(progressPane, "Last");
		final JSplitPane jsp = new JSplitPane(0, false);
		jsp.setResizeWeight(0.7);
		jsp.setOneTouchExpandable(true);
		final Font font = new Font(new JLabel().getFont().getName(), 1, 13);
		(_textPaneMain = new JPopupTextArea()).setBackground(new Color(41, 49, 52));
		_textPaneMain.setForeground(Color.WHITE);
		_textPaneMain.setFont(font);
		((AbstractDocument) _textPaneMain.getDocument()).setDocumentFilter(new DocumentFilter()
		{
			@Override
			public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
			{
				final String replacedText = text.replace("\r\n", "\n");
				super.replace(fb, offset, length, replacedText, attrs);
			}
		});
		
		(_lineNumberingTextArea = new LineNumberingTextArea(_textPaneMain)).setBackground(Color.DARK_GRAY);
		_lineNumberingTextArea.setForeground(Color.LIGHT_GRAY);
		_lineNumberingTextArea.setFont(font.deriveFont(12.0f));
		_lineNumberingTextArea.setEditable(false);
		_textPaneMain.getDocument().addDocumentListener(_lineNumberingTextArea);
		
		final JScrollPane jScrollPane1 = new JScrollPane(22, 30);
		jScrollPane1.setAutoscrolls(true);
		jScrollPane1.setViewportView(_textPaneMain);
		jScrollPane1.setRowHeaderView(_lineNumberingTextArea);
		jsp.setTopComponent(jScrollPane1);
		(_textPaneLog = new JPopupTextArea()).setBackground(new Color(41, 49, 52));
		_textPaneLog.setForeground(Color.CYAN);
		_textPaneLog.setEditable(false);
		
		final JScrollPane jScrollPane2 = new JScrollPane();
		jScrollPane2.setViewportView(_textPaneLog);
		jScrollPane2.setAutoscrolls(true);
		jsp.setBottomComponent(jScrollPane2);
		getContentPane().add(buttonPane, "First");
		getContentPane().add(jsp);
		
		// Set icons.
		final List<Image> icons = new ArrayList<>();
		icons.add(new ImageIcon("." + File.separator + "images" + File.separator + "l2jmobius_16x16.png").getImage());
		icons.add(new ImageIcon("." + File.separator + "images" + File.separator + "l2jmobius_32x32.png").getImage());
		icons.add(new ImageIcon("." + File.separator + "images" + File.separator + "l2jmobius_64x64.png").getImage());
		icons.add(new ImageIcon("." + File.separator + "images" + File.separator + "l2jmobius_128x128.png").getImage());
		setIconImages(icons);
		
		pack();
		if (_splashScreen != null)
		{
			_splashScreen.close();
		}
		setVisible(true);
		toFront();
	}
	
	public JPopupTextArea getTextPaneMain()
	{
		return _textPaneMain;
	}
	
	public static void addLogConsole(String log, boolean isLog)
	{
		if (isLog)
		{
			LOGGER.info(log);
		}
		
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(() -> _textPaneLog.append(log + "\n"));
		}
		else
		{
			_textPaneLog.append(log + "\n");
		}
	}
	
	public void setEditorText(String text)
	{
		_lineNumberingTextArea.cleanUp();
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(() -> _textPaneMain.setText(text));
		}
		else
		{
			_textPaneMain.setText(text);
		}
	}
	
	private void massTxtPackActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
		{
			return;
		}
		
		final JFileChooser fileopen = new JFileChooser();
		fileopen.setFileSelectionMode(1);
		fileopen.setAcceptAllFileFilterUsed(false);
		fileopen.setCurrentDirectory(new File(ConfigWindow.FILE_OPEN_CURRENT_DIRECTORY_PACK));
		fileopen.setPreferredSize(new Dimension(600, 600));
		
		final int ret = fileopen.showDialog(null, SELECT_BTN_STR);
		if (ret == 0)
		{
			_currentFileWindow = fileopen.getSelectedFile();
			ConfigWindow.save("FILE_OPEN_CURRENT_DIRECTORY_PACK", _currentFileWindow.getPath());
			addLogConsole("---------------------------------------", true);
			addLogConsole("Selected folder: " + _currentFileWindow.getPath(), true);
			_progressTask = new MassTxtPacker(this, String.valueOf(_jComboBoxChronicle.getSelectedItem()), _currentFileWindow.getPath());
			_executorService.execute(_progressTask);
		}
	}
	
	private void massTxtUnpackActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
		{
			return;
		}
		
		final JFileChooser fileopen = new JFileChooser();
		fileopen.setFileSelectionMode(1);
		fileopen.setAcceptAllFileFilterUsed(false);
		fileopen.setCurrentDirectory(new File(ConfigWindow.FILE_OPEN_CURRENT_DIRECTORY_UNPACK));
		fileopen.setPreferredSize(new Dimension(600, 600));
		
		final int ret = fileopen.showDialog(null, SELECT_BTN_STR);
		if (ret == 0)
		{
			_currentFileWindow = fileopen.getSelectedFile();
			ConfigWindow.save("FILE_OPEN_CURRENT_DIRECTORY_UNPACK", _currentFileWindow.getPath());
			addLogConsole("---------------------------------------", true);
			addLogConsole("selected folder: " + _currentFileWindow.getPath(), true);
			_progressTask = new MassTxtUnpacker(this, String.valueOf(_jComboBoxChronicle.getSelectedItem()), _currentFileWindow.getPath());
			_executorService.execute(_progressTask);
		}
	}
	
	private void massRecryptActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
		{
			return;
		}
		
		final JFileChooser fileopen = new JFileChooser();
		fileopen.setFileSelectionMode(1);
		fileopen.setAcceptAllFileFilterUsed(false);
		fileopen.setCurrentDirectory(new File(ConfigWindow.FILE_OPEN_CURRENT_DIRECTORY));
		fileopen.setPreferredSize(new Dimension(600, 600));
		
		final int ret = fileopen.showDialog(null, SELECT_BTN_STR);
		if (ret == 0)
		{
			_currentFileWindow = fileopen.getSelectedFile();
			ConfigWindow.save("FILE_OPEN_CURRENT_DIRECTORY", _currentFileWindow.getPath());
			addLogConsole("---------------------------------------", true);
			addLogConsole("Selected folder: " + _currentFileWindow.getPath(), true);
			_progressTask = new MassRecryptor(this, _currentFileWindow.getPath());
			_executorService.execute(_progressTask);
		}
	}
	
	private void openSelectFileWindow(ActionEvent evt)
	{
		if (_progressTask != null)
		{
			return;
		}
		
		final JFileChooser fileopen = new JFileChooser();
		fileopen.setFileSelectionMode(0);
		fileopen.setMultiSelectionEnabled(false);
		fileopen.setAcceptAllFileFilterUsed(false);
		fileopen.setFileFilter(new FileNameExtensionFilter(".dat", new String[]
		{
			"dat"
		}));
		fileopen.setFileFilter(new FileNameExtensionFilter(".ini", new String[]
		{
			"ini"
		}));
		fileopen.setFileFilter(new FileNameExtensionFilter(".txt", new String[]
		{
			"txt"
		}));
		fileopen.setFileFilter(new FileNameExtensionFilter(".htm", new String[]
		{
			"htm"
		}));
		fileopen.setFileFilter(new FileNameExtensionFilter(".dat, .ini, .txt, .htm", new String[]
		{
			"dat",
			"ini",
			"txt",
			"htm"
		}));
		fileopen.setSelectedFile(new File(ConfigWindow.LAST_FILE_SELECTED));
		fileopen.setPreferredSize(new Dimension(600, 600));
		
		final int ret = fileopen.showDialog(null, FILE_SELECT_BTN_STR);
		if (ret == 0)
		{
			_currentFileWindow = fileopen.getSelectedFile();
			ConfigWindow.save("LAST_FILE_SELECTED", _currentFileWindow.getAbsolutePath());
			addLogConsole("---------------------------------------", true);
			addLogConsole("Open file: " + _currentFileWindow.getName(), true);
			_progressTask = new OpenDat(this, String.valueOf(_jComboBoxChronicle.getSelectedItem()), _currentFileWindow);
			_executorService.execute(_progressTask);
		}
	}
	
	private void saveTxtActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
		{
			return;
		}
		
		final JFileChooser fileSave = new JFileChooser();
		fileSave.setCurrentDirectory(new File(ConfigWindow.FILE_SAVE_CURRENT_DIRECTORY));
		if (_currentFileWindow != null)
		{
			fileSave.setSelectedFile(new File(_currentFileWindow.getName().split("\\.")[0] + ".txt"));
			fileSave.setFileFilter(new FileNameExtensionFilter(".txt", new String[]
			{
				"txt"
			}));
			fileSave.setAcceptAllFileFilterUsed(false);
			fileSave.setPreferredSize(new Dimension(600, 600));
			final int ret = fileSave.showSaveDialog(null);
			if (ret == 0)
			{
				_progressTask = new SaveTxt(this, fileSave.getSelectedFile());
				_executorService.execute(_progressTask);
			}
		}
		else
		{
			addLogConsole("No open file!", true);
		}
	}
	
	private void saveDatActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
		{
			return;
		}
		
		if (_currentFileWindow != null)
		{
			_progressTask = new SaveDat(this, _currentFileWindow, String.valueOf(_jComboBoxChronicle.getSelectedItem()));
			_executorService.execute(_progressTask);
		}
		else
		{
			addLogConsole("Error saving dat. No file name.", true);
		}
	}
	
	private void abortActionPerformed(ActionEvent evt)
	{
		if (_progressTask == null)
		{
			return;
		}
		
		_progressTask.abort();
		addLogConsole("---------------------------------------", true);
		addLogConsole("Progress aborted.", true);
	}
	
	public DatCrypter getEncryptor(File file)
	{
		DatCrypter crypter = null;
		final String encryptorName = ConfigWindow.CURRENT_ENCRYPT;
		if (encryptorName.equalsIgnoreCase(".") || encryptorName.equalsIgnoreCase(SOURCE_ENCRYPT_TYPE_STR) || encryptorName.trim().isEmpty())
		{
			final DatCrypter lastDatDecryptor = OpenDat.getLastDatCrypter(file);
			if (lastDatDecryptor != null)
			{
				crypter = CryptVersionParser.getInstance().getEncryptKey(lastDatDecryptor.getName());
				if (crypter == null)
				{
					addLogConsole("Not found " + lastDatDecryptor.getName() + " encryptor of the file: " + _currentFileWindow.getName(), true);
				}
			}
		}
		else
		{
			crypter = CryptVersionParser.getInstance().getEncryptKey(encryptorName);
			if (crypter == null)
			{
				addLogConsole("Not found " + encryptorName + " encryptor of the file: " + _currentFileWindow.getName(), true);
			}
		}
		return crypter;
	}
	
	private void saveComboBox(JComboBox<String> jComboBox, String param)
	{
		ConfigWindow.save(param, String.valueOf(jComboBox.getSelectedItem()));
	}
	
	public void onStartTask()
	{
		setCursor(Cursor.getPredefinedCursor(3));
		_progressBar.setValue(0);
		checkButtons();
	}
	
	public void onProgressTask(int val)
	{
		_progressBar.setValue(val);
	}
	
	public void onStopTask()
	{
		_progressTask = null;
		_progressBar.setValue(100);
		checkButtons();
		Toolkit.getDefaultToolkit().beep();
		setCursor(null);
	}
	
	public void onAbortTask()
	{
		if (_progressTask == null)
		{
			return;
		}
		
		_progressTask = null;
		setCursor(null);
		checkButtons();
	}
	
	private void checkButtons()
	{
		if (_progressTask != null)
		{
			_actionPanels.forEach(p ->
			{
				final Component[] array = p.getComponents();
				int i = 0;
				for (int length = array.length; i < length; ++i)
				{
					final Component c = array[i];
					c.setEnabled(false);
				}
				return;
			});
			
			_abortTaskButton.setEnabled(true);
		}
		else
		{
			_actionPanels.forEach(p ->
			{
				final Component[] array2 = p.getComponents();
				int j = 0;
				for (int length2 = array2.length; j < length2; ++j)
				{
					final Component c2 = array2[j];
					if (c2 == _saveTxtButton)
					{
						c2.setEnabled(_currentFileWindow != null);
					}
					else if (c2 == _saveDatButton)
					{
						c2.setEnabled(_currentFileWindow != null);
					}
					else
					{
						c2.setEnabled(true);
					}
				}
				return;
			});
			
			_abortTaskButton.setEnabled(false);
		}
	}
	
	private static class LineNumberingTextArea extends JTextArea implements DocumentListener
	{
		private final JTextArea textArea;
		private int lastLines;
		
		public LineNumberingTextArea(JTextArea area)
		{
			lastLines = 0;
			textArea = area;
		}
		
		public void cleanUp()
		{
			setText("");
			removeAll();
			lastLines = 0;
		}
		
		private void updateText()
		{
			final int length = textArea.getLineCount();
			if (length == lastLines)
			{
				return;
			}
			
			lastLines = length;
			final StringBuilder lineNumbersTextBuilder = new StringBuilder();
			lineNumbersTextBuilder.append("1").append(System.lineSeparator());
			for (int line = 2; line <= length; ++line)
			{
				lineNumbersTextBuilder.append(line).append(System.lineSeparator());
			}
			setText(lineNumbersTextBuilder.toString());
		}
		
		@Override
		public void insertUpdate(DocumentEvent documentEvent)
		{
			updateText();
		}
		
		@Override
		public void removeUpdate(DocumentEvent documentEvent)
		{
			updateText();
		}
		
		@Override
		public void changedUpdate(DocumentEvent documentEvent)
		{
			updateText();
		}
	}
}
