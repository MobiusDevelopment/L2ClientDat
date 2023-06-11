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

import java.util.ArrayList;
import java.util.List;

public class ParamNode
{
	private final ParamNodeType _entityType;
	private final ParamType _type;
	private String _name;
	private int _size;
	private boolean _hidden;
	private String _cycleName;
	private ArrayList<ParamNode> _sub;
	private boolean _isIterator;
	private boolean _skipWriteSize;
	private String _paramIf;
	private String _valIf;
	private String paramMask;
	private int valMask;
	
	ParamNode(String name, ParamNodeType entityType, ParamType type)
	{
		_size = -1;
		_hidden = false;
		_name = name;
		_entityType = entityType;
		_type = type;
	}
	
	public int getSize()
	{
		return _size;
	}
	
	public void setSize(int size)
	{
		_size = size;
	}
	
	void setIterator()
	{
		_isIterator = true;
	}
	
	void setHidden()
	{
		_hidden = true;
	}
	
	boolean isIterator()
	{
		return _isIterator && (_size < 0);
	}
	
	boolean isNameHidden()
	{
		return _hidden;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	ParamNodeType getEntityType()
	{
		return _entityType;
	}
	
	public ParamType getType()
	{
		return _type;
	}
	
	ParamNode copy()
	{
		final ParamNode node = new ParamNode(getName(), getEntityType(), getType());
		if (isNameHidden())
		{
			node.setHidden();
		}
		
		if (isIterator())
		{
			node.setIterator();
		}
		
		node.setSkipWriteSize(isSkipWriteSize());
		node.setCycleName(getCycleName());
		if (getSubNodes() != null)
		{
			final List<ParamNode> list = new ArrayList<>();
			for (ParamNode n : getSubNodes())
			{
				final ParamNode copyN = n.copy();
				list.add(copyN);
			}
			node.addSubNodes(list);
		}
		
		return node;
	}
	
	void addSubNodes(List<ParamNode> n)
	{
		if (_sub == null)
		{
			_sub = new ArrayList<>();
		}
		_sub.addAll(n);
	}
	
	List<ParamNode> getSubNodes()
	{
		return _sub;
	}
	
	@Override
	public String toString()
	{
		return _name + "[" + _entityType + "][" + _cycleName + "][" + _type + "]";
	}
	
	boolean isSkipWriteSize()
	{
		return _skipWriteSize;
	}
	
	void setSkipWriteSize(boolean skipWrite)
	{
		_skipWriteSize = skipWrite;
	}
	
	String getParamIf()
	{
		return _paramIf;
	}
	
	void setParamIf(String paramIf)
	{
		_paramIf = paramIf;
	}
	
	String getValIf()
	{
		return _valIf;
	}
	
	void setValIf(String valIf)
	{
		_valIf = valIf;
	}
	
	String getCycleName()
	{
		return _cycleName;
	}
	
	void setCycleName(String cycleName)
	{
		_cycleName = cycleName;
	}
	
	public String getParamMask()
	{
		return paramMask;
	}
	
	public void setParamMask(String paramMask)
	{
		this.paramMask = paramMask;
	}
	
	public int getValMask()
	{
		return valMask;
	}
	
	public void setValMask(int valMask)
	{
		this.valMask = valMask;
	}
}
