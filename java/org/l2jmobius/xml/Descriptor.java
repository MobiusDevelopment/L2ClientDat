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

import java.util.List;

import org.l2jmobius.listeners.FormatListener;

public class Descriptor
{
	private final String _alias;
	private final String _filePattern;
	private final List<ParamNode> _nodes;
	private boolean _isRawData;
	private boolean _isSafePackage;
	private FormatListener _format;
	
	Descriptor(String alias, String filePattern, List<ParamNode> nodes)
	{
		_alias = alias;
		_filePattern = filePattern;
		_nodes = nodes;
		_isRawData = false;
	}
	
	void setIsRawData(boolean value)
	{
		_isRawData = value;
	}
	
	public void setIsSafePackage(boolean value)
	{
		_isSafePackage = value;
	}
	
	boolean isRawData()
	{
		return _isRawData;
	}
	
	public boolean isSafePackage()
	{
		return _isSafePackage;
	}
	
	public String getAlias()
	{
		return _alias;
	}
	
	public String getFilePattern()
	{
		return _filePattern;
	}
	
	List<ParamNode> getNodes()
	{
		return _nodes;
	}
	
	public FormatListener getFormat()
	{
		return _format;
	}
	
	public void setFormat(FormatListener format)
	{
		_format = format;
	}
}
