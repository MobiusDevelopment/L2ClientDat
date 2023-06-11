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
package org.l2jmobius.xml;

public class Variant
{
	private final Object _value;
	private final Class<?> _type;
	
	Variant(Object value, Class<?> type)
	{
		_value = type.cast(value);
		_type = type;
	}
	
	public final boolean isInt()
	{
		return _type == Integer.class;
	}
	
	public final boolean isShort()
	{
		return _type == Short.class;
	}
	
	public final boolean isFloat()
	{
		return _type == Float.class;
	}
	
	public final boolean isDouble()
	{
		return _type == Double.class;
	}
	
	public final int getInt()
	{
		return (int) _value;
	}
	
	public final short getShort()
	{
		return (short) _value;
	}
	
	public final float getFloat()
	{
		return (float) _value;
	}
	
	public double getDouble()
	{
		return (double) _value;
	}
	
	public long getLong()
	{
		return (long) _value;
	}
	
	@Override
	public final String toString()
	{
		return String.valueOf(_value);
	}
}
