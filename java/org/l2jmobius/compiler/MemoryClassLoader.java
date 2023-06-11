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
package org.l2jmobius.compiler;

import java.util.HashMap;
import java.util.Map;

public class MemoryClassLoader extends ClassLoader
{
	private final Map<String, MemoryByteCode> _classes = new HashMap<>();
	private final Map<String, MemoryByteCode> _loaded = new HashMap<>();
	
	public MemoryClassLoader()
	{
	}
	
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException
	{
		MemoryByteCode mbc = _classes.get(name);
		if (mbc == null)
		{
			mbc = _classes.get(name);
			if (mbc == null)
			{
				return super.findClass(name);
			}
		}
		return defineClass(name, mbc.getBytes(), 0, mbc.getBytes().length);
	}
	
	public void addClass(MemoryByteCode mbc)
	{
		_classes.put(mbc.getName(), mbc);
		_loaded.put(mbc.getName(), mbc);
	}
	
	public MemoryByteCode getClass(String name)
	{
		return _classes.get(name);
	}
	
	public String[] getLoadedClasses()
	{
		return _loaded.keySet().toArray(new String[_loaded.size()]);
	}
	
	public void clear()
	{
		_loaded.clear();
	}
}
