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
package org.l2jmobius.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ByteWriter
{
	private static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
	private static final Charset DEFAULT_CHARSET = StandardCharsets.US_ASCII;
	private static final Charset UTF_16_LE_CHARSET = StandardCharsets.UTF_16LE;
	
	public static byte[] writeCompactInt(int count)
	{
		return compactIntToByteArray(count);
	}
	
	public static byte[] writeByte(byte value)
	{
		return new byte[]
		{
			value
		};
	}
	
	public static byte[] writeUByte(short value)
	{
		return writeByte((byte) value);
	}
	
	public static byte[] writeInt(int value)
	{
		final byte[] result =
		{
			(byte) (value & 0xFF),
			(byte) ((value & 0xFF00) >> 8),
			(byte) ((value & 0xFF0000) >> 16),
			(byte) ((value & 0xFF000000) >> 24)
		};
		return result;
	}
	
	public static byte[] writeUInt(int value)
	{
		return writeInt(value);
	}
	
	public static byte[] writeShort(int value)
	{
		final byte[] result =
		{
			(byte) (value & 0xFF),
			(byte) ((value & 0xFF00) >> 8)
		};
		return result;
	}
	
	public static byte[] writeUShort(int value)
	{
		return writeShort(value);
	}
	
	public static byte[] writeRGB(String rgb)
	{
		final ByteBuffer buffer = ByteBuffer.allocate(3).order(ByteWriter.BYTE_ORDER);
		buffer.put((byte) Integer.parseInt(rgb.substring(0, 2), 16));
		buffer.put((byte) Integer.parseInt(rgb.substring(2, 4), 16));
		buffer.put((byte) Integer.parseInt(rgb.substring(4, 6), 16));
		return buffer.array();
	}
	
	public static byte[] writeRGBA(String rgba)
	{
		final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteWriter.BYTE_ORDER);
		buffer.put(writeRGB(rgba.substring(0, 6)));
		buffer.put((byte) Integer.parseInt(rgba.substring(6, 8), 16));
		return buffer.array();
	}
	
	public static byte[] writeUtfString(String str, boolean isRaw)
	{
		int size = str.length();
		if (size <= 0)
		{
			return ByteBuffer.allocate(4).order(ByteWriter.BYTE_ORDER).putInt(0).array();
		}
		
		if (!isRaw)
		{
			str = checkAndReplaceNewLine(str);
			size = str.length();
		}
		
		final ByteBuffer buffer = ByteBuffer.allocate((size * 2) + 4).order(ByteWriter.BYTE_ORDER);
		buffer.putInt(size * 2);
		for (int i = 0; i < size; ++i)
		{
			buffer.putChar(str.charAt(i));
		}
		
		return buffer.array();
	}
	
	public static byte[] writeString(String s, boolean isRaw)
	{
		if ((s == null) || s.isEmpty())
		{
			return writeCompactInt(0);
		}
		
		if (!isRaw)
		{
			s = checkAndReplaceNewLine(s);
		}
		s += '\0';
		
		final boolean def = ByteWriter.DEFAULT_CHARSET.newEncoder().canEncode(s);
		final byte[] bytes = s.getBytes(def ? ByteWriter.DEFAULT_CHARSET : ByteWriter.UTF_16_LE_CHARSET);
		final byte[] bSize = compactIntToByteArray(def ? bytes.length : (-bytes.length / 2));
		final ByteBuffer buffer = ByteBuffer.allocate(bytes.length + bSize.length).order(ByteWriter.BYTE_ORDER);
		buffer.put(bSize);
		buffer.put(bytes);
		return buffer.array();
	}
	
	public static byte[] writeDouble(double value)
	{
		final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteWriter.BYTE_ORDER);
		buffer.putDouble(value);
		return buffer.array();
	}
	
	public static byte[] writeFloat(float value)
	{
		final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteWriter.BYTE_ORDER);
		buffer.putFloat(value);
		return buffer.array();
	}
	
	public static byte[] writeLong(long value)
	{
		final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteWriter.BYTE_ORDER);
		buffer.putLong(value);
		return buffer.array();
	}
	
	private static byte[] compactIntToByteArray(int v)
	{
		final boolean negative = v < 0;
		v = Math.abs(v);
		final int[] bytes =
		{
			v & 0x3F,
			(v >> 6) & 0x7F,
			(v >> 13) & 0x7F,
			(v >> 20) & 0x7F,
			(v >> 27) & 0x7F
		};
		
		if (negative)
		{
			bytes[0] |= 0x80;
		}
		
		int size = 5;
		for (int i = 4; (i > 0) && (bytes[i] == 0); --i)
		{
			--size;
		}
		
		final byte[] res = new byte[size];
		for (int j = 0; j < size; ++j)
		{
			if (j != (size - 1))
			{
				bytes[j] |= ((j == 0) ? 64 : 128);
			}
			res[j] = (byte) bytes[j];
		}
		
		return res;
	}
	
	private static String checkAndReplaceNewLine(String str)
	{
		if (str.contains("\\r\\n"))
		{
			str = str.replace("\\r\\n", "\r\n");
		}
		return str;
	}
}
