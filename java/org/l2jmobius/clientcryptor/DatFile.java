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
package org.l2jmobius.clientcryptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import org.l2jmobius.clientcryptor.crypt.DatCrypter;
import org.l2jmobius.config.ConfigDebug;

public class DatFile extends File
{
	private ByteBuffer _buff;
	private Footer _foot;
	
	public DatFile(String pathname)
	{
		super(pathname);
	}
	
	public static void encrypt(byte[] buff, String file, DatCrypter crypter) throws Exception
	{
		crypter.aquire();
		final FileOutputStream os = new FileOutputStream(file, false);
		final String header = "Lineage2Ver" + crypter.getCode();
		os.write(header.getBytes(StandardCharsets.UTF_16LE));
		crypter.update(buff);
		final byte[] res = crypter.encryptResult().array();
		os.write(res);
		if (ConfigDebug.DAT_ADD_END_BYTES)
		{
			final byte[] endBytes =
			{
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				100
			};
			os.write(endBytes);
		}
		os.close();
		crypter.release();
	}
	
	public ByteBuffer getBuff()
	{
		return _buff;
	}
	
	public void decrypt(DatCrypter crypter) throws Exception
	{
		loadInfo();
		try (FileInputStream fis = new FileInputStream(this))
		{
			crypter.aquire();
			try
			{
				fis.skip(28L);
				final byte[] buff = new byte[crypter.getChunkSize(fis.available())];
				int len = fis.available() - crypter.getSkipSize();
				while (len > 0)
				{
					len -= fis.read(buff);
					if (!crypter.update(buff))
					{
						_buff = null;
						return;
					}
				}
				_buff = crypter.decryptResult();
			}
			finally
			{
				crypter.release();
			}
		}
		catch (Exception e)
		{
			throw e;
		}
	}
	
	@SuppressWarnings("unused")
	private boolean checkCrc32(DatCrypter crypter)
	{
		if (_foot == null)
		{
			try
			{
				loadInfo();
			}
			catch (IOException e)
			{
				return false;
			}
		}
		try (FileInputStream fis = new FileInputStream(this))
		{
			final CRC32 chksum = new CRC32();
			byte[] buff = new byte[1024];
			int len = fis.available() - 20;
			while (len > 0)
			{
				if (len < 1024)
				{
					buff = new byte[len];
				}
				len -= fis.read(buff);
				chksum.update(buff);
			}
			return chksum.getValue() == _foot.crc32;
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
			return false;
		}
	}
	
	private void loadInfo() throws IOException
	{
		if (!exists() || !canRead())
		{
			throw new IOException("Can not read the dat file");
		}
		
		final FileInputStream fis = new FileInputStream(this);
		if (fis.available() < 28)
		{
			fis.close();
			throw new IOException("Can not read the dat file : too small");
		}
		
		final byte[] head = new byte[28];
		fis.read(head);
		
		final String header = new String(head, StandardCharsets.UTF_16LE);
		if (!header.startsWith("Lineage2Ver"))
		{
			fis.close();
			throw new IOException("Can not read the dat file : wrong header");
		}
		
		if (header.endsWith("111") || header.endsWith("120") || header.endsWith("211") || header.endsWith("212"))
		{
			fis.close();
			return;
		}
		
		if (header.endsWith("311"))
		{
			fis.close();
			return;
		}
		
		if (!header.endsWith("411") && !header.endsWith("412") && !header.endsWith("413") && !header.endsWith("414"))
		{
			fis.close();
			throw new IOException("Can not read the dat file : unknown header : '" + header + "'");
		}
		
		if (fis.available() < 20)
		{
			fis.close();
			throw new IOException("Can not read the dat file : too small");
		}
		
		fis.skip(fis.available() - 20);
		final byte[] foot = new byte[20];
		fis.read(foot);
		int min = foot[4] & 0xFF;
		min += ((foot[5] << 8) & 0xFF00);
		min += ((foot[6] << 16) & 0xFF0000);
		min += ((foot[7] << 24) & 0xFF000000);
		int maj = foot[8] & 0xFF;
		maj += ((foot[9] << 8) & 0xFF00);
		maj += ((foot[10] << 16) & 0xFF0000);
		maj += ((foot[11] << 24) & 0xFF000000);
		long crc = foot[12] & 0xFFL;
		crc += ((foot[13] << 8) & 0xFF00L);
		crc += ((foot[14] << 16) & 0xFF0000L);
		crc += ((foot[15] << 24) & 0xFF000000L);
		_foot = new Footer(crc, min, maj);
		fis.close();
	}
	
	private static class Footer
	{
		long crc32;
		@SuppressWarnings("unused")
		int majorVersion;
		@SuppressWarnings("unused")
		int minorVersion;
		
		Footer(long crc, int maj, int min)
		{
			crc32 = crc;
			majorVersion = maj;
			minorVersion = min;
		}
	}
}
