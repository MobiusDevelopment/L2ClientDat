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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.L2ClientDat;
import org.l2jmobius.clientcryptor.crypt.DatCrypter;
import org.l2jmobius.data.GameDataName;

public class MassTxtUnpacker extends ActionTask
{
	private static final Logger LOGGER = Logger.getLogger(MassTxtUnpacker.class.getName());
	
	private final String _chronicle;
	private final String _path;
	
	public MassTxtUnpacker(L2ClientDat l2ClientDat, String chronicle, String path)
	{
		super(l2ClientDat);
		_chronicle = chronicle;
		_path = path;
	}
	
	@Override
	public void action()
	{
		try
		{
			action0();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, null, e);
		}
	}
	
	public void action0()
	{
		L2ClientDat.addLogConsole("Mass unpacker with using " + _chronicle + " chronicles by path [" + _path + "]", true);
		
		final File baseDir = new File(_path);
		if (!baseDir.exists())
		{
			L2ClientDat.addLogConsole("Directory [" + _path + "] does not exists.", true);
			return;
		}
		
		final String unpackDirPath = _path + "/unpacked";
		final File unpackDir = new File(unpackDirPath);
		if (!unpackDir.exists() && !unpackDir.mkdir())
		{
			L2ClientDat.addLogConsole("Cannot create recrypt directory [" + unpackDirPath + "].", true);
			return;
		}
		
		final File[] files = baseDir.listFiles(pathname -> pathname.getName().toLowerCase().endsWith(".dat") || pathname.getName().toLowerCase().endsWith(".ini") || pathname.getName().toLowerCase().endsWith(".htm"));
		if ((files == null) || (files.length == 0))
		{
			L2ClientDat.addLogConsole("Directory [" + _path + "] is empty.", true);
			return;
		}
		
		GameDataName.getInstance().clear();
		final long startTime = System.currentTimeMillis();
		double progress = getCurrentProgress();
		final double progressWeight = 100.0 / files.length;
		
		for (File file : files)
		{
			if (isCancelled())
			{
				L2ClientDat.addLogConsole("Cancelled.", true);
				return;
			}
			
			try (FileInputStream fis = new FileInputStream(file))
			{
				L2ClientDat.addLogConsole("Start unpacking [" + file.getName() + "]...", true);
				if (fis.available() < 28)
				{
					L2ClientDat.addLogConsole("[" + file.getName() + "] is too small.", true);
				}
				else
				{
					final byte[] head = new byte[28];
					fis.read(head);
					fis.close();
					final String header = new String(head, StandardCharsets.UTF_16LE);
					if (!header.matches("Lineage2Ver41[1-4]"))
					{
						L2ClientDat.addLogConsole("[" + file.getName() + "] not encrypted. Skip decrypt.", true);
					}
					else
					{
						final String text = OpenDat.start(this, progressWeight, _chronicle, file, true);
						if (text == null)
						{
							L2ClientDat.addLogConsole("Cannot parse [" + file.getName() + "]", true);
						}
						else if (!text.isEmpty())
						{
							final DatCrypter crypter = OpenDat.getLastDatCrypter(file);
							String charset = "UTF-8";
							String name = file.getName();
							if (crypter.isUseStructure() && file.getName().endsWith(".dat"))
							{
								name = name.replace(".dat", ".txt");
							}
							else if (name.endsWith(".htm"))
							{
								charset = "UTF-16";
							}
							
							Files.write(Paths.get(unpackDirPath, name), text.getBytes(charset));
							L2ClientDat.addLogConsole("Success unpacked [" + file.getName() + "]", true);
						}
					}
				}
			}
			catch (Exception e3)
			{
				LOGGER.log(Level.WARNING, ("[" + file.getName() + "] decrypt failed."));
			}
			finally
			{
				progress = addProgress(progress, progressWeight, 100.0);
			}
		}
		
		final long diffTime = (System.currentTimeMillis() - startTime) / 1000L;
		L2ClientDat.addLogConsole("Completed. Elapsed ".concat(String.valueOf(diffTime)).concat(" sec"), true);
	}
}
