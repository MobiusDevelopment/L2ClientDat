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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.l2jmobius.actions.ActionTask;
import org.l2jmobius.listeners.FormatListener;
import org.l2jmobius.util.Util;

public class SkillGrpFormat245 implements FormatListener
{
	private static final Pattern PATTERN = Pattern.compile("\\bskill_autouse_begin\\b(.*?)\\bskill_autouse_end\\b", Pattern.DOTALL);
	private static final Pattern PATTERN_2 = Pattern.compile("\\bskill_begin\\b(.*?)\\bskill_end\\b", Pattern.DOTALL);
	private static final Pattern PATTERN_3 = Pattern.compile("\\bicon_panel_2_begin\\b(.*?)\\bicon_panel_2_end\\b", Pattern.DOTALL);
	
	private static int getSkillLevelMask(int skillLevel, int subSkillLevel)
	{
		return skillLevel | (subSkillLevel << 16);
	}
	
	private static int getSkillLevelFromMask(int skillLevelMask)
	{
		final int mask = 0b1111111111111111;
		return mask & skillLevelMask;
	}
	
	private static int getSubSkillLevelFromMask(int skillLevelMask)
	{
		final int mask = 0b1111111111111111;
		return mask & (skillLevelMask >>> 16);
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
		final Map<Integer, Integer> autoUses = new HashMap<>();
		while (m.find())
		{
			final Map<String, String> params = Util.stringToMap(m.group(1));
			final int skillId = Integer.parseInt(params.get("skill_id"));
			final int autoUseType = Integer.parseInt(params.get("auto_use_type"));
			autoUses.put(skillId, autoUseType);
			
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, progressDiff, progressWeight);
		}
		
		final Matcher m3 = PATTERN_3.matcher(str);
		final Map<Integer, Map<Integer, String>> iconPanels2 = new HashMap<>();
		while (m3.find())
		{
			final Map<String, String> params = Util.stringToMap(m3.group(1));
			final int skillId = Integer.parseInt(params.get("skill_id2"));
			final int skillLvl = Integer.parseInt(params.get("skill_level2"));
			final int skillSubLvl = Integer.parseInt(params.get("skill_sublevel2"));
			final String iconPanel2 = params.get("icon_panel2");
			iconPanels2.computeIfAbsent(skillId, k -> new HashMap<>()).put(getSkillLevelMask(skillLvl, skillSubLvl), iconPanel2);
			
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
			final int skillLvl = Integer.parseInt(params.get("level"));
			final int skillSubLvl = Integer.parseInt(params.get("sublevel"));
			final Integer autoUse = autoUses.get(skillId);
			if (autoUse != null)
			{
				params.put("auto_use_type", String.valueOf(autoUse));
			}
			else
			{
				params.put("auto_use_type", "0");
			}
			
			final String iconPanel2 = iconPanels2.getOrDefault(skillId, Collections.emptyMap()).getOrDefault(getSkillLevelMask(skillLvl, skillSubLvl), "[]");
			params.put("icon_panel_2", iconPanel2);
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
		final Map<Integer, Integer> autoUses = new LinkedHashMap<>();
		final Map<Integer, Map<Integer, String>> iconPanels2 = new LinkedHashMap<>();
		final Matcher m2 = PATTERN_2.matcher(str);
		while (m2.find())
		{
			final Map<String, String> params = Util.stringToMap(m2.group(1));
			final int id = Integer.parseInt(params.get("id"));
			final int level = Integer.parseInt(params.get("level"));
			final int sublevel = Integer.parseInt(params.get("sublevel"));
			final String autoUseType = params.remove("auto_use_type");
			final int type = autoUseType == null ? 0 : Integer.parseInt(autoUseType);
			if (type != 0)
			{
				autoUses.put(id, type);
			}
			
			final String iconPanel2 = params.remove("icon_panel_2");
			if ((iconPanel2 != null) && !iconPanel2.equalsIgnoreCase("[]"))
			{
				iconPanels2.computeIfAbsent(id, k -> new LinkedHashMap<>()).put(getSkillLevelMask(level, sublevel), iconPanel2);
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
			builder.append("skill_autouse_begin\tskill_id=").append(entry.getKey()).append("\tauto_use_type=").append(entry.getValue()).append("\tskill_autouse_end\r\n");
			
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, progressDiff, progressWeight);
		}
		
		for (Entry<Integer, Map<Integer, String>> entry : iconPanels2.entrySet())
		{
			final int skillId = entry.getKey();
			for (Entry<Integer, String> entry1 : entry.getValue().entrySet())
			{
				int skillLvl = getSkillLevelFromMask(entry1.getKey());
				int skillSubLvl = getSubSkillLevelFromMask(entry1.getKey());
				builder.append("icon_panel_2_begin\tskill_id2=").append(skillId).append("\tskill_level2=").append(skillLvl).append("\tskill_sublevel2=").append(skillSubLvl).append("\ticon_panel2=").append(entry1.getValue()).append("\ticon_panel_2_end\r\n");
			}
			
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, progressDiff, progressWeight);
		}
		return builder.toString();
	}
}
