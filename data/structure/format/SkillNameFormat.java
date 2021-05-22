import l2god.listeners.FormatListener;
import l2god.util.Util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillNameFormat implements FormatListener
{
	private static final Pattern pattern = Pattern.compile("\\bskill_txt_begin\\b(.*?)\\bskill_txt_end\\b", Pattern.DOTALL);
	private static final Pattern pattern2 = Pattern.compile("\\bskill_begin\\b(.*?)\\bskill_end\\b", Pattern.DOTALL);

	@Override
	public String decode(String str)
	{
		Matcher m = pattern.matcher(str);
		Map<Integer, String> indexes = new HashMap<>();
		while(m.find())
		{
			Map<String, String> params = Util.stringToMap(m.group(1));
			String name = params.get("str_name");
			indexes.put(Integer.valueOf(params.get("index")), name.substring(1, name.length() - 1));
		}

		StringBuilder builder = new StringBuilder();
		Matcher m2 = pattern2.matcher(str);
		while(m2.find())
		{
			Map<String, String> params = Util.stringToMap(m2.group(1));
			setNameByIndex(indexes, params, "name");
			setNameByIndex(indexes, params, "desc");
			setNameByIndex(indexes, params, "desc_param");
			setNameByIndex(indexes, params, "enchant_name");
			setNameByIndex(indexes, params, "enchant_name_param");
			setNameByIndex(indexes, params, "enchant_desc");
			setNameByIndex(indexes, params, "enchant_desc_param");

			builder.append("skill_begin\t").append(Util.mapToString(params)).append("skill_end").append("\r\n");
		}

		return builder.toString();
	}

	@Override
	public String encode(String str)
	{
		Map<String, String> indexes = new LinkedHashMap<>();
		Map<Long, String> sorted = new TreeMap<>();

		Matcher m2 = pattern2.matcher(str);
		while(m2.find())
		{
			Map<String, String> params = Util.stringToMap(m2.group(1));
			setIndexByName(indexes, params, "name");
			setIndexByName(indexes, params, "desc");
			setIndexByName(indexes, params, "desc_param");
			setIndexByName(indexes, params, "enchant_name");
			setIndexByName(indexes, params, "enchant_name_param");
			setIndexByName(indexes, params, "enchant_desc");
			setIndexByName(indexes, params, "enchant_desc_param");

			String result = "skill_begin\t" + Util.mapToString(params) + "skill_end\r\n";

			int id = Integer.parseInt(params.get("skill_id"));
			int level = Integer.parseInt(params.get("skill_level"));
			int subLevel = Integer.parseInt(params.get("skill_sublevel"));

			sorted.put(getSkillHashCode(id, level, subLevel), result);
		}

		StringBuilder builder = new StringBuilder();
		for(String key : indexes.keySet())
		{
			builder.append("skill_txt_begin\tstr_name=").append(key).append("\tindex=").append(indexes.get(key)).append("\tskill_txt_end\r\n");
		}

		sorted.values().forEach(builder::append);

		return builder.toString();
	}

	private void setNameByIndex(Map<Integer, String> indexes, Map<String, String> params, String paramName)
	{
		params.put(paramName, "[" + indexes.get(Integer.parseInt(params.get(paramName))) + "]");
	}

	private void setIndexByName(Map<String, String> indexes, Map<String, String> params, String paramName)
	{
		String name = params.get(paramName);
		if(indexes.containsKey(name))
		{
			params.put(paramName, indexes.get(name));
			return;
		}

		String index = String.valueOf(indexes.size());
		indexes.put(name, index);
		params.put(paramName, index);
	}

	private static long getSkillHashCode(int skillId, int skillLevel, int subLevel)
	{
		if(subLevel > 0)
			return (skillId * 4294967296L) + (skillLevel * 65536) + subLevel;
		else
			return (skillId * 65536) + skillLevel;
	}
}
