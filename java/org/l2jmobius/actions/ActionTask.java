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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.l2jmobius.L2ClientDat;

public abstract class ActionTask extends SwingWorker<Void, Void> implements PropertyChangeListener
{
	protected final L2ClientDat _l2clientdat;
	private double _progress = 0.0;
	
	public ActionTask(L2ClientDat l2clientdat)
	{
		_l2clientdat = l2clientdat;
		addPropertyChangeListener(this);
	}
	
	public L2ClientDat getL2ClientDat()
	{
		return _l2clientdat;
	}
	
	@Override
	public Void doInBackground()
	{
		_l2clientdat.onStartTask();
		setProgress(0);
		action();
		if (!isCancelled())
		{
			setProgress(100);
		}
		return null;
	}
	
	protected abstract void action();
	
	public final void abort()
	{
		if (isCancelled())
		{
			return;
		}
		
		cancel(true);
		_l2clientdat.onAbortTask();
	}
	
	public double addProgress(double progress, double value, double weight)
	{
		changeProgress(_progress = getWeightValue(value, weight) + progress);
		return _progress;
	}
	
	public void changeProgress(double value)
	{
		final int intValue = (int) Math.max(0.0, Math.min(100.0, value));
		if (intValue > getProgress())
		{
			setProgress(intValue);
		}
	}
	
	public double getCurrentProgress()
	{
		return _progress;
	}
	
	public double getWeightValue(double value, double weight)
	{
		return (value / 100.0) * weight;
	}
	
	@Override
	public void done()
	{
		if (!isCancelled())
		{
			try
			{
				get();
			}
			catch (ExecutionException | InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		_l2clientdat.onStopTask();
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if ("progress".equals(evt.getPropertyName()))
		{
			_l2clientdat.onProgressTask((int) evt.getNewValue());
		}
	}
}
