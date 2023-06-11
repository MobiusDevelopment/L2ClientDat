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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.l2jmobius.actions.ActionTask;
import org.l2jmobius.listeners.FormatListener;
import org.l2jmobius.util.Util;

public class SkillGrpFormat implements FormatListener
{
	private static final Pattern PATTERN = Pattern.compile("\\bskill_autouse_begin\\b(.*?)\\bskill_autouse_end\\b", Pattern.DOTALL);
	private static final Pattern PATTERN_2 = Pattern.compile("\\bskill_begin\\b(.*?)\\bskill_end\\b", Pattern.DOTALL);
	
	@Override
	public String decode(ActionTask actionTask, double progressWeight, String str)
	{
		final int lineCount = str.split("\r\n|\r|\n").length;
		if (lineCount == 0)
		{
			return "";
		}
		
		double progress = actionTask.getCurrentProgress();
		final double progressDiff = 100. / lineCount;
		
		final Matcher m = PATTERN.matcher(str);
		final Map<Integer, Integer> autoUses = new HashMap<>();
		while (m.find())
		{
			final Map<String, String> params = Util.stringToMap(m.group(1));
			final int skillId = Integer.parseInt(params.get("skill_id"));
			final int autoUse = Integer.parseInt(params.get("auto_use"));
			autoUses.put(skillId, autoUse);
			
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, progressDiff, progressWeight);
		}
		
		final StringBuilder builder = new StringBuilder();
		final Matcher m2 = PATTERN_2.matcher(str);
		while (m2.find())
		{
			final Map<String, String> params = Util.stringToMap(m2.group(1));
			final int skillId = Integer.parseInt(params.get("id"));
			params.put("auto_use", String.valueOf(autoUses.getOrDefault(skillId, 0)));
			builder.append("skill_begin\t").append(Util.mapToString(params)).append("skill_end").append("\r\n");
			
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, progressDiff, progressWeight);
		}
		
		return builder.toString();
	}
	
	@Override
	public String encode(ActionTask actionTask, double progressWeight, String str)
	{
		final int lineCount = str.split("\r\n|\r|\n").length;
		if (lineCount == 0)
		{
			return "";
		}
		
		double progress = actionTask.getCurrentProgress();
		final double progressDiff = 100. / lineCount;
		
		final StringBuilder builder = new StringBuilder();
		final Map<Integer, Integer> autoUses = new HashMap<>();
		final Matcher m2 = PATTERN_2.matcher(str);
		while (m2.find())
		{
			final Map<String, String> params = Util.stringToMap(m2.group(1));
			final int id = Integer.parseInt(params.get("id"));
			final String autoUse = params.remove("auto_use");
			if (autoUse != null)
			{
				final int autoUseType = Integer.parseInt(autoUse);
				if (autoUseType != 0)
				{
					autoUses.put(id, autoUseType);
				}
			}
			builder.append("skill_begin\t").append(Util.mapToString(params)).append("skill_end\r\n");
			
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, progressDiff, progressWeight);
		}
		
		for (Entry<Integer, Integer> entry : autoUses.entrySet())
		{
			builder.append("skill_autouse_begin\tskill_id=").append(entry.getKey()).append("\tauto_use=").append(entry.getValue()).append("\tskill_autouse_end\r\n");
			
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, progressDiff, progressWeight);
		}
		return builder.toString();
	}
}
