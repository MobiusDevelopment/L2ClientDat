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

import java.nio.ByteBuffer;

public class BlowFishDatCrypter extends DatCrypter
{
	private boolean _encrypt;
	private final BlowfishEngine _blowfish;
	
	public BlowFishDatCrypter(String name, int code, String key, boolean deCrypt)
	{
		super(name, code);
		_encrypt = false;
		_blowfish = new BlowfishEngine();
		_encrypt = !deCrypt;
		_blowfish.init(_encrypt, key.getBytes());
	}
	
	@Override
	public ByteBuffer decryptResult()
	{
		return null;
	}
	
	@Override
	public ByteBuffer encryptResult()
	{
		return null;
	}
	
	@Override
	public boolean update(byte[] b)
	{
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
