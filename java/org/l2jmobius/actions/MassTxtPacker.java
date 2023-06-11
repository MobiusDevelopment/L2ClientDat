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
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.L2ClientDat;
import org.l2jmobius.clientcryptor.DatFile;
import org.l2jmobius.clientcryptor.crypt.DatCrypter;
import org.l2jmobius.clientcryptor.crypt.RSADatCrypter;
import org.l2jmobius.config.ConfigDebug;
import org.l2jmobius.config.ConfigWindow;
import org.l2jmobius.data.GameDataName;
import org.l2jmobius.xml.CryptVersionParser;
import org.l2jmobius.xml.Descriptor;
import org.l2jmobius.xml.DescriptorParser;
import org.l2jmobius.xml.DescriptorWriter;

public class MassTxtPacker extends ActionTask
{
	private static final Logger LOGGER = Logger.getLogger(MassTxtPacker.class.getName());
	
	private final String _chronicle;
	private final String _path;
	
	public MassTxtPacker(L2ClientDat l2ClientDat, String chronicle, String path)
	{
		super(l2ClientDat);
		_chronicle = chronicle;
		_path = path;
	}
	
	@Override
	protected void action()
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
	
	public void action0() throws Exception
	{
		L2ClientDat.addLogConsole("Mass packer with using " + _chronicle + " chronicles by path [" + _path + "]", true);
		
		final File baseDir = new File(_path);
		if (!baseDir.exists())
		{
			L2ClientDat.addLogConsole("Directory [" + _path + "] does not exists.", true);
			return;
		}
		
		final DatCrypter encrypter = CryptVersionParser.getInstance().getEncryptKey(ConfigWindow.CURRENT_ENCRYPT);
		if (!(encrypter instanceof RSADatCrypter))
		{
			L2ClientDat.addLogConsole("Selected encryptor not RSA! Please select RSA encryptor.", true);
			return;
		}
		
		if (!encrypter.isEncrypt())
		{
			L2ClientDat.addLogConsole("Selected encryptor dont have encrypt RSA key.", true);
			return;
		}
		
		final String packDirPath = _path + "/packed";
		final File packDir = new File(packDirPath);
		if (!packDir.exists() && !packDir.mkdir())
		{
			L2ClientDat.addLogConsole("Cannot create directory [" + packDir + "].", true);
			return;
		}
		
		final File[] files = baseDir.listFiles(pathname -> encrypter.checkFileExtension(pathname.getName()));
		if ((files == null) || (files.length == 0))
		{
			L2ClientDat.addLogConsole("Directory [" + _path + "] is empty.", true);
			return;
		}
		
		double progress = getCurrentProgress();
		if (isCancelled())
		{
			return;
		}
		
		progress = addProgress(progress, 2.0, 100.0);
		GameDataName.getInstance().clear();
		final long startTime = System.currentTimeMillis();
		progress = addProgress(progress, 3.0, 100.0);
		final double progressWeight = 90.0 / files.length;
		
		for (File file : files)
		{
			if (file.getName().equalsIgnoreCase("L2GameDataName.txt"))
			{
				pack(this, progressWeight, _chronicle, encrypter, file, packDir);
				break;
			}
		}
		
		for (File file : files)
		{
			if (!file.getName().equalsIgnoreCase("L2GameDataName.txt"))
			{
				pack(this, progressWeight, _chronicle, encrypter, file, packDir);
			}
		}
		
		if (isCancelled())
		{
			return;
		}
		
		progress = addProgress(progress, 90.0, 100.0);
		GameDataName.getInstance().checkAndUpdate(packDir.getPath(), encrypter);
		
		if (isCancelled())
		{
			return;
		}
		
		addProgress(progress, 5.0, 100.0);
		final long diffTime = (System.currentTimeMillis() - startTime) / 1000L;
		L2ClientDat.addLogConsole("Completed. Elapsed ".concat(String.valueOf(diffTime)).concat(" sec"), true);
	}
	
	private static void pack(ActionTask actionTask, double weight, String chronicle, DatCrypter encrypter, File file, File packDir)
	{
		double progress = actionTask.getCurrentProgress();
		L2ClientDat.addLogConsole("Start packing [" + file.getName() + "]...", true);
		
		try
		{
			final File outFile = new File(packDir, file.getName().replace(".txt", ".dat"));
			byte[] buff = null;
			boolean shouldContinue = true;
			
			if (file.getName().endsWith(".dat") || file.getName().endsWith(".txt"))
			{
				final Descriptor desc = DescriptorParser.getInstance().findDescriptorForFile(chronicle, file.getName().replace(".txt", ".dat"));
				if (desc != null)
				{
					final byte[] array = Files.readAllBytes(file.toPath());
					final String joined = new String(array, 0, array.length, StandardCharsets.UTF_8);
					buff = DescriptorWriter.parseData(actionTask, actionTask.getWeightValue(80.0, weight), outFile, encrypter, desc, joined.replace("\n", "\r\n"), true);
					if (actionTask.isCancelled())
					{
						shouldContinue = false;
					}
					progress = actionTask.addProgress(progress, 80.0, weight);
				}
				else
				{
					L2ClientDat.addLogConsole("Not found the structure of the file: " + file.getName(), true);
				}
			}
			else if (file.getName().endsWith(".ini"))
			{
				final byte[] array2 = Files.readAllBytes(file.toPath());
				final String joined2 = new String(array2, 0, array2.length, StandardCharsets.UTF_8);
				buff = joined2.replace("\n", "\r\n").getBytes();
			}
			else
			{
				L2ClientDat.addLogConsole("Unknown file [" + file.getName() + "] type!", true);
				shouldContinue = false;
			}
			
			if (shouldContinue && (buff != null))
			{
				try
				{
					if (ConfigDebug.ENCRYPT)
					{
						DatFile.encrypt(buff, outFile.getPath(), encrypter);
					}
					else
					{
						final FileOutputStream os = new FileOutputStream(outFile, false);
						os.write(buff);
						os.close();
					}
					L2ClientDat.addLogConsole("Success packed [" + file.getName() + "]", true);
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, e.getMessage(), e);
				}
			}
			
			if (actionTask.isCancelled())
			{
				return;
			}
			
			actionTask.addProgress(progress, 20.0, weight);
		}
		catch (Exception e2)
		{
			LOGGER.log(Level.WARNING, e2.getMessage(), e2);
		}
	}
}
