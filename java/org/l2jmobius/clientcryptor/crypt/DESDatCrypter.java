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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class DESDatCrypter extends DatCrypter
{
	private final boolean _encrypt;
	private ByteArrayOutputStream _result;
	private Cipher _cipher;
	
	public DESDatCrypter(String name, int code, String sKey, boolean deCrypt) throws Exception
	{
		super(name, code);
		_encrypt = !deCrypt;
		final byte[] key = sKey.getBytes();
		final byte[] keyXor = new byte[key.length];
		for (int i = 0; i < key.length; ++i)
		{
			keyXor[i % 8] ^= key[i];
		}
		final DESKeySpec dks = new DESKeySpec(keyXor);
		final SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
		final SecretKey desKey = skf.generateSecret(dks);
		(_cipher = Cipher.getInstance("DES/ECB/NoPadding")).init(deCrypt ? 2 : 1, desKey);
	}
	
	@Override
	public ByteBuffer decryptResult()
	{
		return ByteBuffer.wrap(_result.toByteArray());
	}
	
	@Override
	public ByteBuffer encryptResult()
	{
		return ByteBuffer.wrap(_result.toByteArray());
	}
	
	@Override
	public boolean update(byte[] bArray) throws Exception
	{
		if (!_encrypt)
		{
			_result = new ByteArrayOutputStream(bArray.length);
			final byte[] bytes = new byte[8];
			int size;
			for (int position = 0; position < bArray.length; position += size)
			{
				size = Math.min(8, bArray.length - position);
				System.arraycopy(bArray, position, bytes, 0, size);
				_result.write((size == 8) ? _cipher.doFinal(bytes) : bytes, 0, size);
			}
		}
		return true;
	}
	
	@Override
	public int getChunkSize(int available)
	{
		return available;
	}
	
	@Override
	public int getSkipSize()
	{
		return 0;
	}
	
	@Override
	public boolean isEncrypt()
	{
		return _encrypt;
	}
}
