import l2god.listeners.FormatListener;
import l2god.util.Util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemNameFormat implements FormatListener {
	private static final Pattern pattern = Pattern.compile("\\bitem_name_begin\\b(.*?)\\bitem_name_end\\b", Pattern.DOTALL);
	private static final Pattern pattern2 = Pattern.compile("\\bautomatic_use_begin\\b(.*?)\\bautomatic_use_end\\b", Pattern.DOTALL);

	@Override
	public String decode(String str) {
		Matcher m = pattern2.matcher(str);
		Map<Integer, String> automaticUseParams = new LinkedHashMap<>();
		while(m.find()) {
			Map<String, String> params = Util.stringToMap(m.group(1));
			automaticUseParams.put(Integer.valueOf(params.get("item_id")), params.get("type"));
		}

		StringBuilder builder = new StringBuilder();
		Matcher m2 = pattern.matcher(str);
		while(m2.find()) {
			Map<String, String> params = Util.stringToMap(m2.group(1));
			int itemId = Integer.parseInt(params.get("id"));
			String option = params.get("item_option");
			params.put("item_option", option.substring(0, option.length() - 1) + ";" + automaticUseParams.getOrDefault(itemId, "0") + "}");
			builder.append("item_name_begin\t").append(Util.mapToString(params)).append("item_name_end").append("\r\n");
		}

		return builder.toString();
	}

	@Override
	public String encode(String str) {
		StringBuilder builder = new StringBuilder();
		Map<String, String> automaticUseParams = new LinkedHashMap<>();
		Matcher m2 = pattern2.matcher(str);
		while(m2.find()) {
			Map<String, String> params = Util.stringToMap(m2.group(1));
			String option = params.get("item_option");
			String[] optionParams = option.substring(1, option.length() - 1).split(";");
			automaticUseParams.put(params.get("id"), optionParams[5]);
			builder.append("item_name_begin\t").append(Util.mapToString(params)).append("item_name_end").append("\r\n");
		}

		automaticUseParams.forEach((k,v) -> builder.append("automatic_use_begin\titem_id=").append(k).append("\ttype=").append(v).append("\tautomatic_use_end\r\n"));
		return builder.toString();
	}
}
