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
package org.l2jmobius.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.L2ClientDat;
import org.l2jmobius.clientcryptor.DatFile;
import org.l2jmobius.clientcryptor.crypt.DatCrypter;
import org.l2jmobius.data.GameDataName;
import org.l2jmobius.util.DebugUtil;
import org.l2jmobius.xml.CryptVersionParser;
import org.l2jmobius.xml.Descriptor;
import org.l2jmobius.xml.DescriptorParser;
import org.l2jmobius.xml.DescriptorReader;

public class OpenDat extends ActionTask
{
	private static final Logger LOGGER = Logger.getLogger(OpenDat.class.getName());
	
	private static final Map<String, DatCrypter> LAST_DAT_CRYPTERS = new HashMap<>();
	
	protected final String _structureChronicle;
	protected final File _file;
	
	public static DatCrypter getLastDatCrypter(File file)
	{
		return LAST_DAT_CRYPTERS.get(file.getAbsolutePath().toLowerCase());
	}
	
	public OpenDat(L2ClientDat l2ClientDat, String structureChronicle, File file)
	{
		super(l2ClientDat);
		_structureChronicle = structureChronicle;
		_file = file;
	}
	
	@Override
	protected void action()
	{
		_l2clientdat.getTextPaneMain().cleanUp();
		String result = null;
		try
		{
			result = start(this, 100.0, _structureChronicle, _file, false);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, null, e);
		}
		finally
		{
			_l2clientdat.setEditorText((result != null) ? result : "");
			_l2clientdat.getTextPaneMain().discardAllEdits();
		}
	}
	
	public static ByteBuffer decrypt(File file, Collection<DatCrypter> decryptors, boolean mass) throws Exception
	{
		final String fileName = file.getName();
		if (!file.exists())
		{
			if (!mass)
			{
				L2ClientDat.addLogConsole("File " + fileName + " does not exist.", true);
			}
			return null;
		}
		
		if (!file.canRead())
		{
			if (!mass)
			{
				L2ClientDat.addLogConsole("Unable to read " + fileName + " file.", true);
			}
			return null;
		}
		
		final FileInputStream fis = new FileInputStream(file);
		if (fis.available() < 28)
		{
			if (!mass)
			{
				L2ClientDat.addLogConsole("The file " + fileName + " is too small.", true);
			}
			fis.close();
			return null;
		}
		
		boolean crypt = true;
		final byte[] head = new byte[28];
		fis.read(head);
		fis.close();
		final String header = new String(head, StandardCharsets.UTF_16LE);
		if (!header.matches("Lineage2Ver(\\d{3})"))
		{
			if (!mass)
			{
				L2ClientDat.addLogConsole("File " + fileName + " not encrypted. Skip decrypt.", true);
			}
			crypt = false;
		}
		ByteBuffer buffer = null;
		if (crypt)
		{
			final int cryptCode = Integer.parseInt(header.replaceFirst("Lineage2Ver(\\d{3})", "$1"));
			if (!mass)
			{
				L2ClientDat.addLogConsole("File " + fileName + " encrypted. " + header + " decrypt ...", true);
			}
			
			DatCrypter crypter = null;
			for (DatCrypter c : decryptors)
			{
				if (c.getCode() == cryptCode)
				{
					try
					{
						final DatFile dat = new DatFile(file.getPath());
						dat.decrypt(c);
						buffer = dat.getBuff();
						if (buffer != null)
						{
							crypter = c;
							break;
						}
						continue;
					}
					catch (Exception ex)
					{
					}
				}
			}
			
			if (crypter == null)
			{
				if (!mass)
				{
					L2ClientDat.addLogConsole("Error decrypt " + fileName + " file.", true);
				}
				return null;
			}
			
			OpenDat.LAST_DAT_CRYPTERS.put(file.getAbsolutePath().toLowerCase(), crypter);
			DebugUtil.save(buffer, file);
			if (!mass)
			{
				L2ClientDat.addLogConsole("Decrypt " + fileName + " file successfully by " + crypter.getName() + " decrypter.", true);
			}
		}
		else
		{
			try (FileInputStream fIn = new FileInputStream(file))
			{
				final FileChannel fChan = fIn.getChannel();
				final ByteBuffer mBuf = ByteBuffer.allocate((int) fChan.size());
				fChan.read(mBuf);
				buffer = mBuf;
				fChan.close();
			}
			catch (IOException exc)
			{
				if (!mass)
				{
					L2ClientDat.addLogConsole("Error reading" + fileName + "  file.", true);
				}
			}
		}
		return buffer;
	}
	
	public static ByteBuffer decrypt(File file, boolean mass) throws Exception
	{
		return decrypt(file, CryptVersionParser.getInstance().getDecryptKeys().values(), mass);
	}
	
	public static String start(ActionTask actionTask, double weight, String structureChronicle, File file, boolean mass) throws Exception
	{
		final ByteBuffer buffer = decrypt(file, mass);
		if (buffer == null)
		{
			return "";
		}
		
		double progress = actionTask.getCurrentProgress();
		final DatCrypter crypter = getLastDatCrypter(file);
		final String fileName = file.getName();
		String text = null;
		if (fileName.contains(".ini") || fileName.contains(".txt"))
		{
			if (buffer.hasArray())
			{
				text = new String(buffer.array(), 0, buffer.array().length, StandardCharsets.UTF_8);
			}
		}
		else if (fileName.contains(".htm"))
		{
			if (buffer.hasArray())
			{
				text = new String(buffer.array(), 0, buffer.array().length, StandardCharsets.UTF_16);
				if (actionTask.isCancelled())
				{
					return null;
				}
				
				progress = actionTask.addProgress(progress, 99.0, weight);
			}
		}
		else
		{
			if ((crypter == null) || !crypter.isUseStructure())
			{
				if (!mass)
				{
					L2ClientDat.addLogConsole("Unknown file " + fileName + " type!", true);
				}
				return null;
			}
			
			if (!mass)
			{
				L2ClientDat.addLogConsole("Read the file structure ...", true);
			}
			
			final Descriptor desc = DescriptorParser.getInstance().findDescriptorForFile(structureChronicle, fileName);
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, 5.0, weight);
			if (desc != null)
			{
				buffer.position(0);
				DebugUtil.debug("Buffer size: " + buffer.limit());
				if (!mass)
				{
					GameDataName.getInstance().clear();
				}
				text = DescriptorReader.getInstance().parseData(actionTask, actionTask.getWeightValue(94.0, weight), file, crypter, desc, buffer, mass);
				System.gc();
				
				if (actionTask.isCancelled())
				{
					return null;
				}
				
				progress = actionTask.addProgress(progress, 94.0, weight);
			}
			
			if (text == null)
			{
				if (!mass)
				{
					L2ClientDat.addLogConsole("Structure is not found in the directory: " + structureChronicle + " file: " + fileName, true);
				}
				return null;
			}
		}
		
		actionTask.addProgress(progress, 1.0, weight);
		if (!mass)
		{
			L2ClientDat.addLogConsole("Completed.", true);
		}
		
		return text;
	}
}
