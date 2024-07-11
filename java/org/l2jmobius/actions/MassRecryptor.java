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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.L2ClientDat;
import org.l2jmobius.clientcryptor.DatFile;
import org.l2jmobius.clientcryptor.crypt.DatCrypter;
import org.l2jmobius.clientcryptor.crypt.RSADatCrypter;
import org.l2jmobius.config.ConfigWindow;
import org.l2jmobius.xml.CryptVersionParser;

public class MassRecryptor extends ActionTask
{
	private final String _path;
	
	public MassRecryptor(L2ClientDat l2ClientDat, String path)
	{
		super(l2ClientDat);
		_path = path;
	}
	
	@Override
	protected void action()
	{
		L2ClientDat.addLogConsole("Mass recrypt by path [" + _path + "]", true);
		
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
			L2ClientDat.addLogConsole("Selected encryptor does not have encrypt RSA key.", true);
			return;
		}
		
		final File[] files = baseDir.listFiles(pathname -> encrypter.checkFileExtension(pathname.getName()));
		if ((files == null) || (files.length == 0))
		{
			L2ClientDat.addLogConsole("Directory [" + _path + "] is empty.", true);
			return;
		}
		
		double progress = getCurrentProgress();
		addProgress(progress, 1.0, 100.0);
		
		final String backupDirPath = _path + "/backup";
		final File backupDir = new File(backupDirPath);
		if (!backupDir.exists() && !backupDir.mkdir())
		{
			L2ClientDat.addLogConsole("Cannot create backup directory [" + backupDirPath + "].", true);
			return;
		}
		
		final List<DatCrypter> decryptors = new ArrayList<>();
		for (DatCrypter crypter : CryptVersionParser.getInstance().getDecryptKeys().values())
		{
			if (isCancelled())
			{
				return;
			}
			
			if (!(crypter instanceof RSADatCrypter) || crypter.getName().equals(encrypter.getName()))
			{
				continue;
			}
			
			decryptors.add(crypter);
		}
		
		addProgress(progress, 2.0, 100.0);
		final double progressWeight = 100.0 / files.length;
		final long startTime = System.currentTimeMillis();
		L2ClientDat.addLogConsole("---------------------------------------", true);
		
		for (File file : files)
		{
			if (isCancelled())
			{
				return;
			}
			
			READ:
			{
				try (FileInputStream fis = new FileInputStream(file))
				{
					if (fis.available() < 28)
					{
						L2ClientDat.addLogConsole("The file " + file.getName() + " is too small.", true);
						break READ;
					}
					
					final byte[] head = new byte[28];
					fis.read(head);
					fis.close();
					
					final String header = new String(head, StandardCharsets.UTF_16LE);
					if (!header.matches("Lineage2Ver41[1-4]"))
					{
						L2ClientDat.addLogConsole("File " + file.getName() + " not encrypted. Skip decrypt.", true);
						break READ;
					}
					
					final ByteBuffer buffer = OpenDat.decrypt(file, decryptors, false);
					if (buffer == null)
					{
						break READ;
					}
					
					Files.copy(file.toPath(), new File(backupDir, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
					DatFile.encrypt(buffer.array(), file.getPath(), encrypter);
					L2ClientDat.addLogConsole(file.getName() + " change crypt by " + encrypter.getName() + " encryptor success.", true);
				}
				catch (Exception e)
				{
					L2ClientDat.addLogConsole(file.getName() + " change crypt by " + encrypter.getName() + " encryptor failed!", true);
				}
				finally
				{
					progress = addProgress(progress, progressWeight, 97.0);
				}
				
				L2ClientDat.addLogConsole("---------------------------------------", true);
			}
		}
		
		final long diffTime = (System.currentTimeMillis() - startTime) / 1000L;
		L2ClientDat.addLogConsole("Completed. Elapsed ".concat(String.valueOf(diffTime)).concat(" sec"), true);
	}
}
