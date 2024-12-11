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
package org.l2jmobius.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.l2jmobius.util.StringUtil;

public class FileLogFormatter extends Formatter
{
	private static final String TAB = "\t";
	
	private final SimpleDateFormat _dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss,SSS");
	
	@Override
	public String format(LogRecord record)
	{
		// Java 1.8
		// return StringUtil.concat(_dateFormat.format(new Date(record.getMillis())), TAB, record.getLevel().getName(), TAB, String.valueOf(record.getThreadID()), TAB, record.getLoggerName(), TAB, record.getMessage(), System.lineSeparator());
		// Java 16
		return StringUtil.concat(_dateFormat.format(new Date(record.getMillis())), TAB, record.getLevel().getName(), TAB, String.valueOf(record.getLongThreadID()), TAB, record.getLoggerName(), TAB, record.getMessage(), System.lineSeparator());
	}
}
