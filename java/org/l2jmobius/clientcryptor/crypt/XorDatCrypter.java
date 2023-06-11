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

public class XorDatCrypter extends DatCrypter
{
	private final boolean _encrypt;
	private final int _xorKey;
	private ByteArrayOutputStream _result;
	
	public XorDatCrypter(String name, int code, int key, boolean deCrypt)
	{
		super(name, code);
		_encrypt = !deCrypt;
		_xorKey = key;
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
	public boolean update(byte[] bArray)
	{
		for (byte b : bArray)
		{
			_result.write(b ^ _xorKey);
		}
		return true;
	}
	
	@Override
	public int getChunkSize(int available)
	{
		return 1;
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
	
	@Override
	public void aquire()
	{
		super.aquire();
		_result = new ByteArrayOutputStream(128);
	}
}
