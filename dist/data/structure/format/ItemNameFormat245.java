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

public class ItemNameFormat245 implements FormatListener
{
	private static final Pattern PATTERN = Pattern.compile("\\bitem_autouse_begin\\b(.*?)\\bitem_autouse_end\\b", Pattern.DOTALL);
	private static final Pattern PATTERN_2 = Pattern.compile("\\bitem_name_begin\\b(.*?)\\bitem_name_end\\b", Pattern.DOTALL);
	private static final Pattern PATTERN_3 = Pattern.compile("\\bitem_enchant_begin\\b(.*?)\\bitem_enchant_end\\b", Pattern.DOTALL);
	
	private static class ItemEnchant
	{
		final int keepTypeSelection;
		final String keepTypeEnchant;
		
		public ItemEnchant(int selection, String enchant)
		{
			keepTypeSelection = selection;
			keepTypeEnchant = enchant;
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
		final Map<Integer, Integer> autoUses = new HashMap<>();
		while (m.find())
		{
			final Map<String, String> params = Util.stringToMap(m.group(1));
			final int itemId = Integer.parseInt(params.get("item_id"));
			final int autouseType = Integer.parseInt(params.get("autouse_type"));
			autoUses.put(itemId, autouseType);
			
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, progressDiff, progressWeight);
		}
		
		final Matcher m3 = PATTERN_3.matcher(str);
		final Map<Integer, ItemEnchant> unks = new HashMap<>();
		while (m3.find())
		{
			final Map<String, String> params = Util.stringToMap(m3.group(1));
			final int itemId = Integer.parseInt(params.get("item_ex_id"));
			final int keepTypeSelection = Integer.parseInt(params.get("keep_type_selection"));
			final String keepTypeEnchant = params.get("keep_type_enchant");
			unks.put(itemId, new ItemEnchant(keepTypeSelection, keepTypeEnchant));
			
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
			final int id = Integer.parseInt(params.get("id"));
			params.put("autouse_type", String.valueOf(autoUses.getOrDefault(id, 0)));
			final ItemEnchant itemEnchant = unks.getOrDefault(id, new ItemEnchant(-1, ""));
			params.put("keep_type_selection", String.valueOf(itemEnchant.keepTypeSelection));
			params.put("keep_type_enchant", itemEnchant.keepTypeEnchant);
			builder.append("item_name_begin\t").append(Util.mapToString(params)).append("item_name_end").append("\r\n");
			
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
		final Map<Integer, ItemEnchant> unks = new HashMap<>();
		final Matcher m2 = PATTERN_2.matcher(str);
		while (m2.find())
		{
			final Map<String, String> params = Util.stringToMap(m2.group(1));
			final int id = Integer.parseInt(params.get("id"));
			final String autoUse = params.remove("autouse_type");
			if (autoUse != null)
			{
				int autoUseType = Integer.parseInt(autoUse);
				if (autoUseType != 0)
				{
					autoUses.put(id, autoUseType);
				}
			}
			
			int keepTypeSelectionInt = 0;
			final String keepTypeSelection = params.remove("keep_type_selection");
			if (keepTypeSelection != null)
			{
				keepTypeSelectionInt = Integer.parseInt(keepTypeSelection);
			}
			
			final String keepTypeEnchant = params.remove("keep_type_enchant");
			if ((keepTypeSelectionInt >= 0) && !((keepTypeEnchant == null) || keepTypeEnchant.isEmpty()))
			{
				unks.put(id, new ItemEnchant(keepTypeSelectionInt, keepTypeEnchant));
			}
			builder.append("item_name_begin\t").append(Util.mapToString(params)).append("item_name_end\r\n");
			
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, progressDiff, progressWeight);
		}
		
		for (Entry<Integer, Integer> entry : autoUses.entrySet())
		{
			builder.append("item_autouse_begin\titem_id=").append(entry.getKey()).append("\tautouse_type=").append(entry.getValue()).append("\titem_autouse_end\r\n");
			
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, progressDiff, progressWeight);
		}
		
		for (Entry<Integer, ItemEnchant> entry : unks.entrySet())
		{
			builder.append("item_enchant_begin\titem_ex_id=").append(entry.getKey()).append("\tkeep_type_selection=").append(entry.getValue().keepTypeSelection).append("\tkeep_type_enchant=").append(entry.getValue().keepTypeEnchant).append("\titem_enchant_end\r\n");
			
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, progressDiff, progressWeight);
		}
		return builder.toString();
	}
}
