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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.compiler.Compiler;
import org.l2jmobius.compiler.MemoryClassLoader;

public class Util
{
	private static final Logger LOGGER = Logger.getLogger(Util.class.getName());
	
	public static void printBytes(String paramString, byte[] paramArrayOfByte)
	{
		final StringBuilder stringBuilder = new StringBuilder(paramArrayOfByte.length);
		stringBuilder.append(paramString).append(": [");
		for (byte b : paramArrayOfByte)
		{
			stringBuilder.append(b).append(" ");
		}
		stringBuilder.append("]");
		LOGGER.info(stringBuilder.toString());
	}
	
	public static boolean compareBuffers(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
	{
		if ((paramArrayOfByte1 == null) || (paramArrayOfByte2 == null) || (paramArrayOfByte1.length != paramArrayOfByte2.length))
		{
			return false;
		}
		
		for (byte b = 0; b < paramArrayOfByte1.length; ++b)
		{
			if (paramArrayOfByte1[b] != paramArrayOfByte2[b])
			{
				return false;
			}
		}
		
		return true;
	}
	
	private static String printData(byte[] paramArrayOfByte, int paramInt)
	{
		final StringBuilder stringBuilder = new StringBuilder();
		byte b = 0;
		for (int i = 0; i < paramInt; ++i)
		{
			if ((b % 16) == 0)
			{
				stringBuilder.append(fillHex(i, 4)).append(": ");
			}
			stringBuilder.append(fillHex(paramArrayOfByte[i] & 0xFF, 2)).append(" ");
			++b;
			if (b == 16)
			{
				stringBuilder.append("\t ");
				int b2 = i - 15;
				for (byte b3 = 0; b3 < 16; ++b3)
				{
					final byte b4 = paramArrayOfByte[b2++];
					if ((b4 > 31) && (b4 < 128))
					{
						stringBuilder.append((char) b4);
					}
					else
					{
						stringBuilder.append('.');
					}
				}
				stringBuilder.append("\n");
				b = 0;
			}
		}
		
		int i = paramArrayOfByte.length % 16;
		if (i > 0)
		{
			for (int j = 0; j < (17 - i); ++j)
			{
				stringBuilder.append("\t ");
			}
			int j = paramArrayOfByte.length - i;
			for (byte b5 = 0; b5 < i; ++b5)
			{
				final byte b6 = paramArrayOfByte[j++];
				if ((b6 > 31) && (b6 < 128))
				{
					stringBuilder.append((char) b6);
				}
				else
				{
					stringBuilder.append('.');
				}
			}
			stringBuilder.append("\n");
		}
		
		return stringBuilder.toString();
	}
	
	private static String fillHex(int paramInt1, int paramInt2)
	{
		final StringBuilder str = new StringBuilder(Integer.toHexString(paramInt1));
		for (int i = str.length(); i < paramInt2; ++i)
		{
			str.insert(0, "0");
		}
		return str.toString();
	}
	
	public static String printData(byte[] paramArrayOfByte)
	{
		return printData(paramArrayOfByte, paramArrayOfByte.length);
	}
	
	public static List<File> loadFiles(String paramString1, String paramString2)
	{
		final List<File> arrayList = new ArrayList<>();
		final File file = new File(paramString1);
		final File[] arrayOfFile = file.listFiles();
		if (arrayOfFile != null)
		{
			for (File file2 : arrayOfFile)
			{
				if (file2.isFile())
				{
					if (file2.getName().endsWith(paramString2))
					{
						arrayList.add(file2);
					}
				}
				else if (file2.isDirectory())
				{
					final File file3 = new File(paramString1 + file2.getName() + "/");
					final File[] arrayOfFile2 = file3.listFiles();
					if (arrayOfFile2 != null)
					{
						for (File file4 : arrayOfFile2)
						{
							if (file4.getName().endsWith(paramString2))
							{
								arrayList.add(file4);
							}
						}
					}
				}
			}
		}
		return arrayList;
	}
	
	public static String[] getDirsNames(String paramString1, String paramString2)
	{
		final List<String> arrayList = new ArrayList<>();
		final File file = new File(paramString1);
		final File[] arrayOfFile = file.listFiles();
		if (arrayOfFile == null)
		{
			return null;
		}
		
		for (File file2 : arrayOfFile)
		{
			if (file2.isDirectory() && file2.getName().endsWith(paramString2))
			{
				arrayList.add(file2.getName());
			}
		}
		
		final String[] arrayOfString = new String[arrayList.size()];
		byte b = 0;
		for (String str : arrayList)
		{
			arrayOfString[b] = str;
			++b;
		}
		
		return arrayOfString;
	}
	
	public static String[] getFilesNames(String paramString1, String paramString2)
	{
		final List<String> arrayList = new ArrayList<>();
		final File file = new File(paramString1);
		final File[] arrayOfFile = file.listFiles();
		if (arrayOfFile == null)
		{
			return null;
		}
		
		for (File file2 : arrayOfFile)
		{
			if (file2.isFile() && file2.getName().endsWith(paramString2))
			{
				arrayList.add(file2.getName().replace(paramString2, ""));
			}
		}
		
		final String[] arrayOfString = new String[arrayList.size()];
		byte b = 0;
		for (String str : arrayList)
		{
			arrayOfString[b] = str;
			++b;
		}
		
		return arrayOfString;
	}
	
	public static String printData(ByteBuffer paramByteBuffer)
	{
		final byte[] arrayOfByte = new byte[paramByteBuffer.remaining()];
		paramByteBuffer.get(arrayOfByte);
		final String str = printData(arrayOfByte, arrayOfByte.length);
		paramByteBuffer.position(paramByteBuffer.position() - arrayOfByte.length);
		return str;
	}
	
	public static List<String> splitList(String paramString)
	{
		if (paramString.startsWith("{"))
		{
			paramString = paramString.substring(1, paramString.length() - 1);
		}
		
		int level = 0;
		StringBuilder sb = new StringBuilder();
		final List<String> arrayList = new ArrayList<>();
		for (char part : paramString.toCharArray())
		{
			if ((part == '{') || (part == '['))
			{
				++level;
			}
			else if ((part == '}') || (part == ']'))
			{
				--level;
			}
			else if ((part == ';') && (level == 0))
			{
				arrayList.add(sb.toString());
				sb = new StringBuilder();
				continue; // Skip the rest of the loop and go to the next iteration.
			}
			sb.append(part);
		}
		arrayList.add(sb.toString());
		
		return arrayList;
	}
	
	public static Map<String, String> stringToMap(String paramString)
	{
		final LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
		for (String str : paramString.split("\t"))
		{
			if (str.contains("="))
			{
				final int i = str.indexOf("=");
				final String str2 = str.substring(0, i);
				final String str3 = str.substring(i + 1);
				linkedHashMap.put(str2, str3);
			}
		}
		return linkedHashMap;
	}
	
	public static String mapToString(Map<String, String> paramMap)
	{
		final StringBuilder stringBuilder = new StringBuilder();
		for (Entry<String, String> entry : paramMap.entrySet())
		{
			stringBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("\t");
		}
		return stringBuilder.toString();
	}
	
	public static Object loadJavaClass(String paramString1, String paramString2)
	{
		final File file = new File(paramString2 + paramString1 + ".java");
		if (!file.exists())
		{
			return null;
		}
		
		final Compiler compiler = new Compiler();
		if (compiler.compile(file))
		{
			final MemoryClassLoader classLoader = compiler.getClassLoader();
			for (String name : classLoader.getLoadedClasses())
			{
				if (!name.contains("$"))
				{
					try
					{
						final Class<?> clazz = classLoader.loadClass(name);
						if (!Modifier.isAbstract(clazz.getModifiers()) && paramString1.equals(name))
						{
							return clazz.getDeclaredConstructor().newInstance();
						}
					}
					catch (Exception e)
					{
						LOGGER.log(Level.WARNING, e.getMessage(), e);
						break;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Method to get the stack trace of a Throwable into a String
	 * @param t Throwable to get the stacktrace from
	 * @return stack trace from Throwable as String
	 */
	public static String getStackTrace(Throwable t)
	{
		final StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
	/**
	 * @param array - the array to look into
	 * @param obj - the object to search for
	 * @return {@code true} if the {@code array} contains the {@code obj}, {@code false} otherwise.
	 */
	public static boolean startsWith(String[] array, String obj)
	{
		for (String element : array)
		{
			if (element.startsWith(obj))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param <T>
	 * @param array - the array to look into
	 * @param obj - the object to search for
	 * @return {@code true} if the {@code array} contains the {@code obj}, {@code false} otherwise.
	 */
	public static <T> boolean contains(T[] array, T obj)
	{
		for (T element : array)
		{
			if (element.equals(obj))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param array - the array to look into
	 * @param obj - the integer to search for
	 * @return {@code true} if the {@code array} contains the {@code obj}, {@code false} otherwise
	 */
	public static boolean contains(int[] array, int obj)
	{
		for (int element : array)
		{
			if (element == obj)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param array - the array to look into
	 * @param obj - the object to search for
	 * @param ignoreCase
	 * @return {@code true} if the {@code array} contains the {@code obj}, {@code false} otherwise.
	 */
	public static boolean contains(String[] array, String obj, boolean ignoreCase)
	{
		for (String element : array)
		{
			if (element.equals(obj) || (ignoreCase && element.equalsIgnoreCase(obj)))
			{
				return true;
			}
		}
		return false;
	}
}
