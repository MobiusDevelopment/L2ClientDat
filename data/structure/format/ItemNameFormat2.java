import l2god.listeners.FormatListener;
import l2god.util.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemNameFormat2 implements FormatListener {
	private static final Pattern pattern = Pattern.compile("\\bitem_name_begin\\b(.*?)\\bitem_name_end\\b", Pattern.DOTALL);
	private static final Pattern patternAutoUse = Pattern.compile("\\bitem_name_reuse_begin\\b(.*?)\\bitem_name_reuse_end\\b", Pattern.DOTALL);
	private static final Pattern patternKeepEnchant = Pattern.compile("\\bitem_ex_begin\\b(.*?)\\bitem_ex_end\\b", Pattern.DOTALL);
	private static final boolean isItemOption = true;


	@Override
	public String decode(String str) {
		Matcher m = patternAutoUse.matcher(str);
		Map<Integer, String> automaticUseParams = new LinkedHashMap<>();
		while(m.find()) {
			Map<String, String> params = Util.stringToMap(m.group(1));
			automaticUseParams.put(Integer.valueOf(params.get("item_id")), params.get("type"));
		}

		Matcher m2 = patternKeepEnchant.matcher(str);
		Map<Integer, Map<String, String>> keepEnchantParams = new LinkedHashMap<>();
		while(m2.find()) {
			Map<String, String> params = Util.stringToMap(m2.group(1));
			keepEnchantParams.put(Integer.valueOf(params.get("id")), params);
		}

		StringBuilder builder = new StringBuilder();
		Matcher m3 = pattern.matcher(str);
		while(m3.find()) {
			Map<String, String> params = Util.stringToMap(m3.group(1));
			int itemId = Integer.parseInt(params.get("id"));
			
			if (isItemOption) {
				String option = params.get("item_option");
				params.put("item_option", option.substring(0, option.length() - 1) + ";" + automaticUseParams.getOrDefault(itemId, "0") + "}");
			} else {
				params.put("automatic_use", automaticUseParams.getOrDefault(itemId, "0"));
			}

			Map<String, String> keepParams = keepEnchantParams.get(itemId);
			if (keepParams != null) {
				keepParams.remove("id");
				params.putAll(keepParams);
			} else {
				params.put("keep_type_selection", "normal");
				params.put("keep_type_enchant", "{0;0;0}");
			}

			builder.append("item_name_begin\t").append(Util.mapToString(params)).append("item_name_end").append("\r\n");
		}

		return builder.toString();
	}

	@Override
	public String encode(String str) {
		StringBuilder builder = new StringBuilder();
		List<String> automaticUseParams = new ArrayList<>(1000);
		List<String> keepEnchantParams = new ArrayList<>(1000);

		Matcher m2 = pattern.matcher(str);
		while(m2.find()) {
			Map<String, String> params = Util.stringToMap(m2.group(1));

			String sId = params.get("id");
			String aUse = "0";
			if (isItemOption) {
				String option = params.get("item_option");
				String[] optionParams = option.substring(1, option.length() - 1).split(";");
				aUse = optionParams[5];
			} else {
				aUse = params.getOrDefault("automatic_use", "0");
			}
			
			if (!aUse.equals("0"))
				automaticUseParams.add("\titem_id=" + sId + "\ttype=" + aUse);		

			String keepSelect = params.getOrDefault("keep_type_selection", "normal");
			String keepTypeEnchant = params.getOrDefault("keep_type_enchant", "{0;0;0}");
			keepEnchantParams.add("\tid="+ sId +"\tkeep_type_selection=" + keepSelect + "\t" + "keep_type_enchant=" + keepTypeEnchant);

			builder.append("item_name_begin\t").append(Util.mapToString(params)).append("item_name_end").append("\r\n");
		}

		automaticUseParams.forEach((k) -> builder.append("item_name_reuse_begin\titem_id=").append(k).append("\titem_name_reuse_end\r\n"));
		keepEnchantParams.forEach((k) -> builder.append("item_ex_begin\tid=").append(k).append("\titem_ex_end\r\n"));
		return builder.toString();
	}
}
