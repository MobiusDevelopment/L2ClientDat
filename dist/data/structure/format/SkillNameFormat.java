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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.l2jmobius.actions.ActionTask;
import org.l2jmobius.listeners.FormatListener;
import org.l2jmobius.util.Util;

public class SkillNameFormat implements FormatListener
{
	private static final Pattern PATTERN = Pattern.compile("\\bskill_txt_begin\\b(.*?)\\bskill_txt_end\\b", Pattern.DOTALL);
	private static final Pattern PATTERN_2 = Pattern.compile("\\bskill_begin\\b(.*?)\\bskill_end\\b", Pattern.DOTALL);
	
	private static class SkillData implements Comparable<SkillData>
	{
		public final int id;
		public final int level;
		public final int subLevel;
		public final String data;
		
		public SkillData(int id, int level, int subLevel, String data)
		{
			this.id = id;
			this.level = level;
			this.subLevel = subLevel;
			this.data = data;
		}
		
		@Override
		public int compareTo(SkillData o)
		{
			int res = Integer.compare(id, o.id);
			if (res == 0)
			{
				res = Integer.compare(level, o.level);
				if (res == 0)
				{
					res = Integer.compare(subLevel, o.subLevel);
				}
			}
			return res;
		}
	}
	
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
		final Map<Integer, String> indexes = new HashMap<>();
		while (m.find())
		{
			final Map<String, String> params = Util.stringToMap(m.group(1));
			final String name = params.get("str_name");
			indexes.put(Integer.valueOf(params.get("index")), name.substring(1, name.length() - 1));
			
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
			setNameByIndex(indexes, params, "name");
			setNameByIndex(indexes, params, "desc");
			setNameByIndex(indexes, params, "desc_param");
			setNameByIndex(indexes, params, "enchant_name");
			setNameByIndex(indexes, params, "enchant_name_param");
			setNameByIndex(indexes, params, "enchant_desc");
			setNameByIndex(indexes, params, "enchant_desc_param");
			
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
		
		final Map<String, String> indexes = new LinkedHashMap<>();
		final List<SkillData> sorted = new ArrayList<>();
		
		final Matcher m2 = PATTERN_2.matcher(str);
		while (m2.find())
		{
			final Map<String, String> params = Util.stringToMap(m2.group(1));
			setIndexByName(indexes, params, "name");
			setIndexByName(indexes, params, "desc");
			setIndexByName(indexes, params, "desc_param");
			setIndexByName(indexes, params, "enchant_name");
			setIndexByName(indexes, params, "enchant_name_param");
			setIndexByName(indexes, params, "enchant_desc");
			setIndexByName(indexes, params, "enchant_desc_param");
			
			final String result = "skill_begin\t" + Util.mapToString(params) + "skill_end\r\n";
			
			final int id = Integer.parseInt(params.get("skill_id"));
			final int level = Integer.parseInt(params.get("skill_level"));
			final int subLevel = Integer.parseInt(params.get("skill_sublevel"));
			
			sorted.add(new SkillData(id, level, subLevel, result));
			
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, progressDiff, progressWeight);
		}
		
		final StringBuilder builder = new StringBuilder();
		for (String key : indexes.keySet())
		{
			builder.append("skill_txt_begin\ttext=").append(key).append("\tindex=").append(indexes.get(key)).append("\tskill_txt_end\r\n");
			
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, progressDiff, progressWeight);
		}
		
		Collections.sort(sorted);
		sorted.forEach((s) -> builder.append(s.data));
		
		return builder.toString();
	}
	
	private void setNameByIndex(Map<Integer, String> indexes, Map<String, String> params, String paramName)
	{
		params.put(paramName, "[" + indexes.get(Integer.parseInt(params.get(paramName))) + "]");
	}
	
	private void setIndexByName(Map<String, String> indexes, Map<String, String> params, String paramName)
	{
		final String name = params.get(paramName);
		if (indexes.containsKey(name))
		{
			params.put(paramName, indexes.get(name));
			return;
		}
		
		final String index = String.valueOf(indexes.size());
		indexes.put(name, index);
		params.put(paramName, index);
	}
}
