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
package org.l2jmobius.clientcryptor.crypt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.crypto.Cipher;

import org.l2jmobius.util.DebugUtil;
import org.l2jmobius.util.Util;

public class RSADatCrypter extends DatCrypter
{
	private static final Logger LOGGER = Logger.getLogger(RSADatCrypter.class.getName());
	
	private Cipher _cipher;
	private ByteArrayOutputStream _result;
	private boolean _encrypt;
	
	public RSADatCrypter(String name, int code, String modulus, String exp, boolean deCrypt)
	{
		super(name, code);
		_encrypt = false;
		try
		{
			_cipher = Cipher.getInstance("RSA/ECB/nopadding");
			if (deCrypt)
			{
				final RSAPublicKeySpec keyspec = new RSAPublicKeySpec(new BigInteger(modulus, 16), new BigInteger(exp, 16));
				final RSAPublicKey rsaKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keyspec);
				_cipher.init(2, rsaKey);
			}
			else
			{
				_encrypt = true;
				final RSAPrivateKeySpec keyspec2 = new RSAPrivateKeySpec(new BigInteger(modulus, 16), new BigInteger(exp, 16));
				final RSAPrivateKey rsaKey2 = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keyspec2);
				_cipher.init(1, rsaKey2);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	@Override
	public ByteBuffer decryptResult()
	{
		if (!checkAquired())
		{
			throw new IllegalStateException("Do not even think about using a DatCrypter that you did not aquired");
		}
		
		final byte[] compressed = _result.toByteArray();
		int inflatedSize = compressed[0] & 0xFF;
		inflatedSize += ((compressed[1] << 8) & 0xFF00);
		inflatedSize += ((compressed[2] << 16) & 0xFF0000);
		inflatedSize += ((compressed[3] << 24) & 0xFF000000);
		final ByteArrayInputStream bais = new ByteArrayInputStream(compressed, 4, compressed.length - 4);
		final InflaterInputStream iis = new InflaterInputStream(bais, new Inflater());
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
		final byte[] inflatedResult = new byte[128];
		try
		{
			int len;
			while ((len = iis.read(inflatedResult)) > 0)
			{
				baos.write(inflatedResult, 0, len);
			}
		}
		catch (IOException e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		if (baos.size() != inflatedSize)
		{
			LOGGER.log(Level.WARNING, ("[RSADatCrypter] Hum inflated result does not have the expected length..(" + baos.size() + "!=" + inflatedSize + ")"));
		}
		
		return ByteBuffer.wrap(baos.toByteArray());
	}
	
	@Override
	public ByteBuffer encryptResult()
	{
		if (!checkAquired())
		{
			throw new IllegalStateException("Do not even think about using a DatCrypter that you did not aquired");
		}
		
		final ByteArrayOutputStream result = new ByteArrayOutputStream();
		try
		{
			final ByteArrayInputStream input = new ByteArrayInputStream(_result.toByteArray());
			final byte[] buffer = new byte[124];
			final byte[] block = new byte[128];
			int len;
			while ((len = input.read(buffer)) > 0)
			{
				Arrays.fill(block, (byte) 0);
				block[0] = (byte) ((len >> 24) & 0xFF);
				block[1] = (byte) ((len >> 16) & 0xFF);
				block[2] = (byte) ((len >> 8) & 0xFF);
				block[3] = (byte) (len & 0xFF);
				System.arraycopy(buffer, 0, block, 128 - len - ((124 - len) % 4), len);
				result.write(_cipher.doFinal(block));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		return ByteBuffer.wrap(result.toByteArray());
	}
	
	@Override
	public boolean update(byte[] b) throws Exception
	{
		if (!checkAquired())
		{
			throw new IllegalStateException("Do not even think about using a DatCrypter that you did not aquired");
		}
		
		Exception exception = null;
		try
		{
			if (!_encrypt)
			{
				final byte[] chunk = _cipher.doFinal(b);
				int size = chunk[3];
				size += ((chunk[2] << 8) & 0xFF00);
				size += ((chunk[1] << 16) & 0xFF0000);
				size += ((chunk[0] << 24) & 0xFF000000);
				final int pad = (-size & 0x1) + (-size & 0x2);
				DebugUtil.debug("Size:" + size + " pad:" + pad);
				if (size > 128)
				{
					return false;
				}
				
				_result.write(chunk, 128 - size - pad, size);
				DebugUtil.debug("--- BLOCK:\n" + Util.printData(chunk) + "-----");
			}
			else
			{
				try
				{
					final ByteArrayOutputStream s = new ByteArrayOutputStream(b.length);
					final DeflaterOutputStream dos = new DeflaterOutputStream(s, new Deflater());
					dos.write(b);
					dos.finish();
					dos.close();
					final int l = b.length;
					(_result = new ByteArrayOutputStream(10 + s.toByteArray().length)).write(l & 0xFF);
					_result.write((l & 0xFF00) >> 8);
					_result.write((l & 0xFF0000) >> 16);
					_result.write((l & 0xFF000000) >> 24);
					_result.write(s.toByteArray());
				}
				catch (IOException e)
				{
					exception = e;
				}
			}
		}
		catch (Exception e2)
		{
			exception = e2;
		}
		
		if (exception != null)
		{
			throw exception;
		}
		
		return true;
	}
	
	@Override
	public void aquire()
	{
		super.aquire();
		_result = new ByteArrayOutputStream(128);
	}
	
	@Override
	public boolean isEncrypt()
	{
		return _encrypt;
	}
	
	@Override
	public int getChunkSize(int available)
	{
		return 128;
	}
	
	@Override
	public int getSkipSize()
	{
		return 20;
	}
}
