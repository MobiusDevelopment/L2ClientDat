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
package org.l2jmobius.xml;

import java.io.File;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.L2ClientDat;
import org.l2jmobius.actions.ActionTask;
import org.l2jmobius.clientcryptor.crypt.DatCrypter;
import org.l2jmobius.config.ConfigDebug;
import org.l2jmobius.config.ConfigWindow;
import org.l2jmobius.data.GameDataName;
import org.l2jmobius.util.ByteReader;
import org.l2jmobius.util.DebugUtil;

public class DescriptorReader
{
	private static final String EQ = "=";
	private static final String TAB = "\t";
	private static final String SEMI = ";";
	private static final String LB = "[";
	private static final String RB = "]";
	
	public DescriptorReader()
	{
	}
	
	public String parseData(ActionTask actionTask, double weight, File currentFile, DatCrypter crypter, Descriptor desc, ByteBuffer data, boolean mass) throws Exception
	{
		boolean error = false;
		double progress = actionTask.getCurrentProgress();
		final boolean hasFormatter = (desc.getFormat() != null) && !ConfigWindow.CURRENT_FORMATTER.equalsIgnoreCase("Disabled");
		String stringData;
		if (desc.isRawData())
		{
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, hasFormatter ? 20.0 : 49.0, weight);
			final StringBuilder builder = new StringBuilder();
			final ParamNode node = desc.getNodes().get(0);
			if (!readVariables(currentFile, crypter, node, new HashMap<>(), data, builder, true, mass))
			{
				if (mass)
				{
					return null;
				}
				
				final String errorMsg = stringData = String.format("Error while parsing variable NAME[%s] TYPE[%s] in file NAME[%s]! Parsed data: %s", node.getName(), node.getType(), currentFile.getName(), builder);
				L2ClientDat.addLogConsole(errorMsg, true);
			}
			else
			{
				stringData = builder.toString();
			}
			progress = actionTask.addProgress(progress, 50.0, weight);
		}
		else
		{
			final Map<String, Variant> vars = new HashMap<>();
			final Data result = parseData(actionTask, actionTask.getWeightValue(69.0, weight), currentFile, crypter, data, null, desc.getNodes(), 1, vars, false, 0, mass);
			if (result != null)
			{
				stringData = result.data.toString().trim();
				error = result.error;
			}
			else
			{
				stringData = "";
				error = true;
			}
			
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, 69.0, weight);
		}
		if (!error && (desc.getFormat() != null) && !ConfigWindow.CURRENT_FORMATTER.equalsIgnoreCase("Disabled"))
		{
			stringData = desc.getFormat().decode(actionTask, actionTask.getWeightValue(30.0, weight), stringData);
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, 30.0, weight);
		}
		actionTask.addProgress(progress, 1.0, weight);
		final int pos = desc.isSafePackage() ? (data.position() + 13) : data.position();
		if (data.limit() > pos)
		{
			if (mass)
			{
				return null;
			}
			
			L2ClientDat.addLogConsole("Unpacked not full " + data.position() + "/" + data.limit() + " diff: " + (data.limit() - pos), true);
		}
		return stringData;
	}
	
	private Data parseData(ActionTask actionTask, double weight, File currentFile, DatCrypter crypter, ByteBuffer data, ParamNode lastNode, List<ParamNode> nodes, int cycleSize, Map<String, Variant> vars, boolean isNameHidden, int cycleNameLevel, boolean mass) throws Exception
	{
		if (cycleSize <= 0)
		{
			return null;
		}
		
		if (cycleSize > 1000000)
		{
			throw new Exception("To much data.");
		}
		
		final Data result = new Data();
		final StringBuilder out = result.data;
		for (int i = 0; i < cycleSize; ++i)
		{
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			final boolean isAddCycleName = !isNameHidden && (lastNode != null) && lastNode.getEntityType().isCycle();
			if (isAddCycleName)
			{
				for (int k = 0; k < cycleNameLevel; ++k)
				{
					out.append(TAB);
				}
				out.append(lastNode.getName().concat("_begin"));
				++cycleNameLevel;
			}
			
			final int nodeSize = nodes.size();
			int nNode = 0;
			for (ParamNode n : nodes)
			{
				if (!n.isIterator())
				{
					++nNode;
				}
			}
			
			if (isNameHidden && (nNode > 1))
			{
				out.append("{");
			}
			
			final double progress = actionTask.getCurrentProgress();
			final double progressWeight = 100.0 / cycleSize / nodeSize;
			for (int j = 0; j < nodeSize; ++j)
			{
				if (actionTask.isCancelled())
				{
					return null;
				}
				
				final ParamNode node = nodes.get(j);
				if (node.getEntityType().isIf())
				{
					final Variant variant = vars.get(node.getParamIf());
					if ((variant != null) && variant.toString().equalsIgnoreCase(node.getValIf()))
					{
						final Data dataResult = parseData(actionTask, actionTask.getWeightValue(progressWeight, weight), currentFile, crypter, data, null, node.getSubNodes(), 1, vars, isNameHidden, cycleNameLevel, mass);
						if (dataResult != null)
						{
							out.append(dataResult.data);
							if (dataResult.error)
							{
								result.error = true;
								break;
							}
						}
					}
				}
				else if (node.getEntityType().isElse())
				{
					final Variant variant = vars.get(node.getParamIf());
					if ((variant != null) && !variant.toString().equalsIgnoreCase(node.getValIf()))
					{
						final Data dataResult = parseData(actionTask, actionTask.getWeightValue(progressWeight, weight), currentFile, crypter, data, null, node.getSubNodes(), 1, vars, isNameHidden, cycleNameLevel, mass);
						if (dataResult != null)
						{
							out.append(dataResult.data);
							if (dataResult.error)
							{
								result.error = true;
								break;
							}
						}
					}
				}
				else if (node.getEntityType().isMask())
				{
					final Variant variant = vars.get(node.getParamMask());
					if (variant != null)
					{
						final int value = Integer.parseInt(variant.toString());
						if ((value & node.getValMask()) == node.getValMask())
						{
							final Data dataResult2 = parseData(actionTask, actionTask.getWeightValue(progressWeight, weight), currentFile, crypter, data, null, node.getSubNodes(), 1, vars, isNameHidden, cycleNameLevel, mass);
							if (dataResult2 != null)
							{
								out.append(dataResult2.data);
								if (dataResult2.error)
								{
									result.error = true;
									break;
								}
							}
						}
					}
				}
				else
				{
					if (!node.isIterator() && !node.getEntityType().isConstant() && !isNameHidden && (node.getEntityType().isWrapper() || node.isNameHidden()))
					{
						out.append(TAB).append(node.getName()).append(EQ);
					}
					if (node.getEntityType().isWrapper())
					{
						final Data dataResult3 = parseData(actionTask, actionTask.getWeightValue(progressWeight, weight), currentFile, crypter, data, null, node.getSubNodes(), 1, vars, true, cycleNameLevel, mass);
						if (dataResult3 != null)
						{
							out.append(dataResult3.data);
							if (dataResult3.error)
							{
								result.error = true;
								break;
							}
						}
					}
					else if (node.getEntityType().isCycle())
					{
						int size;
						if (node.getSize() >= 0)
						{
							size = node.getSize();
						}
						else
						{
							final Variant var2 = vars.get(node.getCycleName());
							if (var2.isInt())
							{
								size = var2.getInt();
							}
							else
							{
								if (!var2.isShort())
								{
									throw new Exception("Wrong cycle variable format for cycle: " + node.getName() + " iterator: " + node.getCycleName());
								}
								size = var2.getShort();
							}
						}
						if (node.isNameHidden())
						{
							out.append("{");
						}
						
						final Data dataResult = parseData(actionTask, actionTask.getWeightValue(progressWeight, weight), currentFile, crypter, data, node, node.getSubNodes(), size, vars, node.isNameHidden(), cycleNameLevel, mass);
						if (dataResult != null)
						{
							out.append(dataResult.data);
							if (dataResult.error)
							{
								result.error = true;
								break;
							}
						}
						
						if (node.isNameHidden())
						{
							out.append("}");
						}
					}
					else if (node.getEntityType().isConstant())
					{
						out.append(node.getName().replace("\\t", TAB).replace("\\r\\n", "\r\n"));
					}
					else if (node.getEntityType().isVariable() && !readVariables(currentFile, crypter, node, vars, data, out, false, mass))
					{
						if (!mass)
						{
							L2ClientDat.addLogConsole(String.format("Error while parsing variable NAME[%s] TYPE[%s] in file NAME[%s]! Parsed data: %s", node.getName(), node.getType(), currentFile.getName(), out.toString()), true);
						}
						
						result.error = true;
						break;
					}
					
					if (!node.isIterator() && !node.getEntityType().isConstant() && isNameHidden && (j != (nodeSize - 1)))
					{
						out.append(SEMI);
					}
					
					if (vars.containsKey(node.getName()))
					{
						DebugUtil.debugPos(data.position(), node.getName(), vars.get(node.getName()));
					}
				}
				
				if (actionTask.isCancelled())
				{
					return null;
				}
				
				actionTask.addProgress(progress, progressWeight, weight);
			}
			if (isNameHidden)
			{
				if (nNode > 1)
				{
					out.append("}");
				}
				if (i < (cycleSize - 1))
				{
					out.append(SEMI);
				}
			}
			if (isAddCycleName)
			{
				if (out.charAt(out.length() - 1) != '\n')
				{
					out.append(TAB);
				}
				out.append(lastNode.getName()).append("_end\r\n");
				--cycleNameLevel;
			}
			
			if (result.error)
			{
				break;
			}
		}
		return result;
	}
	
	private boolean readVariables(File currentFile, DatCrypter crypter, ParamNode node, Map<String, Variant> vars, ByteBuffer data, StringBuilder out, boolean isRaw, boolean mass)
	{
		try
		{
			switch (node.getType())
			{
				case UCHAR:
				{
					final short value = (byte) ByteReader.readChar(data);
					if (!node.isIterator())
					{
						if (ConfigDebug.DAT_REPLACEMENT_ENUMS && node.isEnum())
						{
							out.append(DescriptorParser.getInstance().getEnumNameByIndex(node.getEnumName(), value));
						}
						else
						{
							out.append(value);
						}
					}
					vars.put(node.getName(), new Variant(value, Short.class));
					break;
				}
				case UBYTE:
				{
					final int value = ByteReader.readUByte(data);
					if (!node.isIterator())
					{
						if (ConfigDebug.DAT_REPLACEMENT_ENUMS && node.isEnum())
						{
							out.append(DescriptorParser.getInstance().getEnumNameByIndex(node.getEnumName(), value));
						}
						else
						{
							out.append(value);
						}
					}
					vars.put(node.getName(), new Variant(value, Integer.class));
					break;
				}
				case SHORT:
				{
					final short value = ByteReader.readShort(data);
					if (!node.isIterator())
					{
						if (ConfigDebug.DAT_REPLACEMENT_ENUMS && node.isEnum())
						{
							out.append(DescriptorParser.getInstance().getEnumNameByIndex(node.getEnumName(), value));
						}
						else
						{
							out.append(value);
						}
					}
					vars.put(node.getName(), new Variant(value, Short.class));
					break;
				}
				case USHORT:
				{
					final int value = ByteReader.readShort(data) & 0xFFFF;
					if (!node.isIterator())
					{
						if (ConfigDebug.DAT_REPLACEMENT_ENUMS && node.isEnum())
						{
							out.append(DescriptorParser.getInstance().getEnumNameByIndex(node.getEnumName(), value));
						}
						else
						{
							out.append(value);
						}
					}
					vars.put(node.getName(), new Variant(value, Integer.class));
					break;
				}
				case UINT:
				{
					final int value = ByteReader.readUInt(data);
					if (!node.isIterator())
					{
						if (ConfigDebug.DAT_REPLACEMENT_ENUMS && node.isEnum())
						{
							out.append(DescriptorParser.getInstance().getEnumNameByIndex(node.getEnumName(), value));
						}
						else
						{
							out.append(value);
						}
					}
					vars.put(node.getName(), new Variant(value, Integer.class));
					break;
				}
				case INT:
				{
					final int value = ByteReader.readInt(data);
					if (!node.isIterator())
					{
						if (ConfigDebug.DAT_REPLACEMENT_ENUMS && node.isEnum())
						{
							out.append(DescriptorParser.getInstance().getEnumNameByIndex(node.getEnumName(), value));
						}
						else
						{
							out.append(value);
						}
					}
					vars.put(node.getName(), new Variant(value, Integer.class));
					break;
				}
				case CNTR:
				{
					final int value = ByteReader.readCompactInt(data);
					if (!node.isIterator())
					{
						if (ConfigDebug.DAT_REPLACEMENT_ENUMS && node.isEnum())
						{
							out.append(DescriptorParser.getInstance().getEnumNameByIndex(node.getEnumName(), value));
						}
						else
						{
							out.append(value);
						}
					}
					vars.put(node.getName(), new Variant(value, Integer.class));
					break;
				}
				case UNICODE:
				{
					final String str = ByteReader.readUtfString(data, isRaw);
					if (!isRaw)
					{
						if (!node.isIterator())
						{
							out.append(LB);
							out.append(str);
							out.append(RB);
						}
						vars.put(node.getName(), new Variant(str, String.class));
						break;
					}
					
					out.append(str);
					break;
				}
				case ASCF:
				{
					final String str = ByteReader.readString(data, isRaw);
					if (!isRaw)
					{
						if (!node.isIterator())
						{
							out.append(LB);
							out.append(str);
							out.append(RB);
						}
						vars.put(node.getName(), new Variant(str, String.class));
						break;
					}
					
					out.append(str);
					break;
				}
				case DOUBLE:
				{
					final double value3 = ByteReader.readDouble(data);
					if (!node.isIterator())
					{
						out.append(new BigDecimal(Double.toString(value3)).toPlainString());
					}
					vars.put(node.getName(), new Variant(value3, Double.class));
					break;
				}
				case FLOAT:
				{
					final float value4 = ByteReader.readFloat(data);
					if (!node.isIterator())
					{
						out.append(value4);
					}
					vars.put(node.getName(), new Variant(value4, Float.class));
					break;
				}
				case LONG:
				{
					final long value5 = ByteReader.readLong(data);
					if (!node.isIterator())
					{
						out.append(value5);
					}
					vars.put(node.getName(), new Variant(value5, Long.class));
					break;
				}
				case RGBA:
				{
					final String value6 = ByteReader.readRGBA(data);
					if (!node.isIterator())
					{
						out.append(value6);
					}
					vars.put(node.getName(), new Variant(value6, String.class));
					break;
				}
				case RGB:
				{
					final String value6 = ByteReader.readRGB(data);
					if (!node.isIterator())
					{
						out.append(value6);
					}
					vars.put(node.getName(), new Variant(value6, String.class));
					break;
				}
				case HEX:
				{
					final int value2 = ByteReader.readUByte(data);
					if (!node.isIterator())
					{
						String hex = Integer.toHexString(value2).toUpperCase();
						if (hex.length() == 1)
						{
							hex = "0" + hex;
						}
						out.append(hex);
					}
					vars.put(node.getName(), new Variant(value2, Integer.class));
					break;
				}
				case MAP_INT:
				{
					final int index = ByteReader.readUInt(data);
					if (ConfigDebug.DAT_REPLACEMENT_NAMES)
					{
						final String paramName = GameDataName.getInstance().getString(currentFile, crypter, index, mass);
						if (!node.isIterator())
						{
							out.append(paramName);
						}
						vars.put(node.getName(), new Variant(paramName, String.class));
						break;
					}
					if (!node.isIterator())
					{
						out.append(index);
					}
					vars.put(node.getName(), new Variant(index, Integer.class));
					break;
				}
				default:
				{
					return false;
				}
			}
		}
		catch (Exception e)
		{
			return false;
		}
		
		return true;
	}
	
	private static class Data
	{
		public final StringBuilder data;
		public boolean error;
		
		private Data()
		{
			data = new StringBuilder();
			error = false;
		}
	}
	
	public static DescriptorReader getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DescriptorReader INSTANCE = new DescriptorReader();
	}
}
