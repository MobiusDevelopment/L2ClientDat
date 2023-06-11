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

public class DescriptorLink
{
	private final String _dir;
	private final String _namePattern;
	private final String _linkFile;
	private final String _linkVersion;
	
	public DescriptorLink(String dir, String namePattern, String linkFile, String linkVersion)
	{
		_dir = dir;
		_namePattern = namePattern;
		_linkFile = linkFile;
		_linkVersion = linkVersion;
	}
	
	public String getFilePattern()
	{
		return _dir;
	}
	
	public String getNamePattern()
	{
		return _namePattern;
	}
	
	public String getLinkFile()
	{
		return _linkFile;
	}
	
	public String getLinkVersion()
	{
		return _linkVersion;
	}
}
