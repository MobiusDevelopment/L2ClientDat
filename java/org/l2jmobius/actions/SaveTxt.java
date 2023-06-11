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
package org.l2jmobius.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.L2ClientDat;
import org.l2jmobius.config.ConfigWindow;

public class SaveTxt extends ActionTask
{
	private static final Logger LOGGER = Logger.getLogger(SaveTxt.class.getName());
	
	private final File _file;
	
	public SaveTxt(L2ClientDat l2ClientDat, File file)
	{
		super(l2ClientDat);
		_file = file;
	}
	
	@Override
	protected void action()
	{
		try
		{
			if (isCancelled())
			{
				return;
			}
			
			changeProgress(15.0);
			final PrintWriter out = new PrintWriter(new FileOutputStream(_file.getPath()), true);
			changeProgress(30.0);
			ConfigWindow.save("FILE_SAVE_CURRENT_DIRECTORY", _file.getParentFile().toString());
			changeProgress(50.0);
			out.print(_l2clientdat.getTextPaneMain().getText());
			changeProgress(90.0);
			out.close();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, null, e);
		}
		
		L2ClientDat.addLogConsole("---------------------------------------", true);
		L2ClientDat.addLogConsole("Saved: " + _file.getPath(), true);
	}
}
