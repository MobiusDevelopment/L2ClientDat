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

public enum ParamNodeType
{
	FOR,
	WRAPPER,
	CONSTANT,
	VARIABLE,
	IF,
	ELSE,
	MASK;
	
	boolean isCycle()
	{
		return this == ParamNodeType.FOR;
	}
	
	public boolean isWrapper()
	{
		return this == ParamNodeType.WRAPPER;
	}
	
	boolean isConstant()
	{
		return this == ParamNodeType.CONSTANT;
	}
	
	boolean isVariable()
	{
		return this == ParamNodeType.VARIABLE;
	}
	
	boolean isIf()
	{
		return this == ParamNodeType.IF;
	}
	
	boolean isElse()
	{
		return this == ParamNodeType.ELSE;
	}
	
	boolean isMask()
	{
		return this == ParamNodeType.MASK;
	}
}
