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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.l2jmobius.actions.ActionTask;
import org.l2jmobius.clientcryptor.crypt.DatCrypter;
import org.l2jmobius.config.ConfigDebug;
import org.l2jmobius.config.ConfigWindow;
import org.l2jmobius.data.GameDataName;
import org.l2jmobius.util.ByteWriter;
import org.l2jmobius.util.Util;
import org.l2jmobius.xml.exceptions.CycleArgumentException;
import org.l2jmobius.xml.exceptions.PackDataException;

public class DescriptorWriter
{
	private static final Logger LOGGER = Logger.getLogger(DescriptorWriter.class.getName());
	
	private static final byte[] END_FILE_BYTES = new byte[]
	{
		12,
		83,
		97,
		102,
		101,
		80,
		97,
		99,
		107,
		97,
		103,
		101,
		0
	};
	
	public static byte[] parseData(ActionTask actionTask, double weight, File currentFile, DatCrypter crypter, Descriptor desc, String data, boolean mass) throws Exception
	{
		final ByteArrayOutputStream stream = new ByteArrayOutputStream(data.length() / 2);
		double progress = actionTask.getCurrentProgress();
		if ((desc.getFormat() != null) && !ConfigWindow.CURRENT_FORMATTER.equalsIgnoreCase("Disabled"))
		{
			data = desc.getFormat().encode(actionTask, actionTask.getWeightValue(20.0, weight), data);
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, 20.0, weight);
		}
		if (desc.isRawData())
		{
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, 50.0, weight);
			final byte[] bytes = parseNodeValue(currentFile, crypter, data, desc.getNodes().get(0), true, mass);
			if (bytes != null)
			{
				stream.write(bytes);
			}
			else
			{
				LOGGER.log(Level.WARNING, "Failed to parse raw data.");
			}
		}
		else
		{
			final String lines = data.replace("\r\n", "\t");
			final List<WriteData> writeData = new ArrayList<>();
			packData(actionTask, actionTask.getWeightValue(30.0, weight), currentFile, crypter, writeData, lines, new HashMap<>(), new HashMap<>(), desc.getNodes(), mass);
			if (actionTask.isCancelled())
			{
				return null;
			}
			
			progress = actionTask.addProgress(progress, 30.0, weight);
			final double progressWeight = actionTask.getWeightValue(40.0, weight);
			final double progressDiff = 100.0 / writeData.size();
			
			final ByteBuffer buffer = ByteBuffer.allocateDirect(data.length() * 2);
			for (WriteData wr : writeData)
			{
				if (wr.isIterator())
				{
					LOGGER.log(Level.WARNING, ("Found iterator without writed size: " + wr.getParamNode().getName()));
				}
				else
				{
					buffer.put(wr.getBytes());
					if (actionTask.isCancelled())
					{
						return null;
					}
					progress = actionTask.addProgress(progress, progressDiff, progressWeight);
				}
			}
			try
			{
				buffer.flip();
				stream.write(ByteBuffer.allocate(buffer.limit()).put(buffer).array());
				if (actionTask.isCancelled())
				{
					return null;
				}
			}
			catch (IOException e)
			{
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
		}
		
		actionTask.addProgress(progress, 10.0, weight);
		
		if (desc.isSafePackage())
		{
			stream.write(DescriptorWriter.END_FILE_BYTES);
		}
		
		return stream.toByteArray();
	}
	
	private static void packData(ActionTask actionTask, double weight, File currentFile, DatCrypter crypter, List<WriteData> writeData, String lines, Map<String, String> paramMap, Map<ParamNode, String> mapData, List<ParamNode> nodes, boolean mass) throws Exception
	{
		final List<WriteData> subWriteData = new ArrayList<>();
		for (ParamNode node : nodes)
		{
			if ((actionTask != null) && actionTask.isCancelled())
			{
				return;
			}
			
			if (node.isIterator())
			{
				subWriteData.add(new WriteIterator(node));
			}
			else if (node.getEntityType().isCycle())
			{
				if (!node.isNameHidden())
				{
					final Pattern pattern = Pattern.compile("\\b" + node.getName().concat("_begin\\b(.*?)\\b").concat(node.getName()).concat("_end\\b"), 32);
					final Matcher m = pattern.matcher(lines);
					final List<String> list = new ArrayList<>();
					while (m.find())
					{
						list.add(m.group(1));
					}
					
					final double progress = (actionTask != null) ? actionTask.getCurrentProgress() : 0.0;
					final double progressWeight = (actionTask != null) ? actionTask.getWeightValue(100.0 / list.size(), weight) : 0.0;
					writeSize(currentFile, crypter, subWriteData, node, list.size(), mass);
					for (String str : list)
					{
						paramMap.putAll(Util.stringToMap(str));
						packData(actionTask, progressWeight, currentFile, crypter, subWriteData, str, paramMap, mapData, node.getSubNodes(), mass);
					}
					
					if (actionTask == null)
					{
						continue;
					}
					
					if (actionTask.isCancelled())
					{
						return;
					}
					
					actionTask.addProgress(progress, 100.0, weight);
				}
				else
				{
					final String param = getDataString(node, node.getName(), paramMap, mapData);
					if (param == null)
					{
						throw new PackDataException("Not found data for cycle: " + node.getName() + "\r\n-node: " + node + "\r\n\tparam: " + paramMap.get(node.getName()));
					}
					
					if (param.isEmpty() || param.equals("{}"))
					{
						writeSize(currentFile, crypter, subWriteData, node, 0, mass);
					}
					else
					{
						final List<String> subParams = Util.splitList(param);
						final int cycleSize = subParams.size();
						if ((node.getSize() > 0) && (node.getSize() != cycleSize))
						{
							throw new PackDataException("Wrong static cycle count for cycle: " + node.getName() + " size: " + subParams.size() + " params: " + param + "\r\n-node: " + node + "\r\n\tparam: " + paramMap.get(node.getName()));
						}
						
						writeSize(currentFile, crypter, subWriteData, node, cycleSize, mass);
						int nPramNode = 0;
						int nCycleNode = 0;
						for (ParamNode n : node.getSubNodes())
						{
							if (n.getEntityType().isCycle())
							{
								++nCycleNode;
							}
							else
							{
								if (n.isIterator())
								{
									continue;
								}
								
								++nPramNode;
							}
						}
						for (String subParam : subParams)
						{
							int paramIndex = 0;
							final List<String> sub2Params = ((nPramNode > 0) || (nCycleNode > 1)) ? Util.splitList(subParam) : Collections.singletonList(subParam);
							for (ParamNode n2 : node.getSubNodes())
							{
								if (!n2.isIterator() && !n2.getEntityType().isConstant())
								{
									if (paramIndex >= sub2Params.size())
									{
										throw new PackDataException("Wrong param count for cycle: " + node.getName() + ", paramIndex: " + paramIndex + ", params: " + param + "\r\n-node: " + node + "\r\n\tparam: " + paramMap.get(node.getName()));
									}
									mapData.put(n2, sub2Params.get(paramIndex++));
								}
							}
							packData(null, 0.0, currentFile, crypter, subWriteData, lines, paramMap, mapData, node.getSubNodes(), mass);
						}
					}
				}
			}
			else if (node.getEntityType().isWrapper())
			{
				final String param = getDataString(node, node.getName(), paramMap, mapData);
				if (param == null)
				{
					throw new PackDataException("Not found data for wrapper: " + node.getName() + "\r\n-node: " + node + "\r\n\tparam: " + paramMap.get(node.getName()));
				}
				
				final List<String> subParams = Util.splitList(param);
				int paramIndex2 = 0;
				for (ParamNode n3 : node.getSubNodes())
				{
					if (!n3.isIterator() && !n3.getEntityType().isConstant())
					{
						if (paramIndex2 >= subParams.size())
						{
							throw new PackDataException("Wrong param count for wrapper: " + node.getName() + ", paramIndex: " + paramIndex2 + ", params: " + param + "\r\n-node: " + node + "\r\n\tparam: " + paramMap.get(node.getName()));
						}
						
						mapData.put(n3, subParams.get(paramIndex2++));
					}
				}
				packData(null, 0.0, currentFile, crypter, subWriteData, lines, paramMap, mapData, node.getSubNodes(), mass);
			}
			else if (node.getEntityType().isVariable())
			{
				final String param = getDataString(node, node.getName(), paramMap, mapData);
				if (param == null)
				{
					throw new PackDataException("Not found data for variable: " + node.getName() + "\r\n-node: " + node + "\r\n\tparam: " + paramMap.get(node.getName()));
				}
				
				final byte[] bytes = parseNodeValue(currentFile, crypter, param, node, false, mass);
				if (bytes == null)
				{
					throw new PackDataException("Node value is null.\r\n-node: " + node + "\r\n\tparam: " + paramMap.get(node.getName()));
				}
				
				subWriteData.add(new WriteBytes(node, bytes));
			}
			else if (node.getEntityType().isIf())
			{
				final String param = getDataString(node, node.getParamIf(), paramMap, mapData);
				if (param == null)
				{
					throw new PackDataException("Not found data for if: " + node.getParamIf() + "\r\n-node: " + node + "\r\n\tparam: " + paramMap.get(node.getParamIf()));
				}
				
				if (!node.getValIf().equalsIgnoreCase(param))
				{
					continue;
				}
				
				packData(null, 0.0, currentFile, crypter, subWriteData, lines, paramMap, mapData, node.getSubNodes(), mass);
			}
			else if (node.getEntityType().isElse())
			{
				final String param = getDataString(node, node.getParamIf(), paramMap, mapData);
				if (param == null)
				{
					throw new PackDataException("Not found data for else: " + node.getParamIf() + "\r\n-node: " + node + "\r\n\tparam: " + paramMap.get(node.getParamIf()));
				}
				
				if (node.getValIf().equalsIgnoreCase(param))
				{
					continue;
				}
				
				packData(null, 0.0, currentFile, crypter, subWriteData, lines, paramMap, mapData, node.getSubNodes(), mass);
			}
			else
			{
				if (!node.getEntityType().isMask())
				{
					continue;
				}
				
				final String param = getDataString(node, node.getParamMask(), paramMap, mapData);
				if (param == null)
				{
					throw new PackDataException("Not found data for mask: " + node.getParamMask() + "\r\n-node: " + node + "\r\n\tparam: " + paramMap.get(node.getParamMask()));
				}
				
				final int mask = Integer.parseInt(param);
				if ((mask & node.getValMask()) != node.getValMask())
				{
					continue;
				}
				
				packData(null, 0.0, currentFile, crypter, subWriteData, lines, paramMap, mapData, node.getSubNodes(), mass);
			}
		}
		writeData.addAll(subWriteData);
	}
	
	private static byte[] parseNodeValue(File currentFile, DatCrypter crypter, String data, ParamNode node, boolean isRaw, boolean mass)
	{
		final ParamType nodeType = node.getType();
		if (nodeType == null)
		{
			if (!mass)
			{
				LOGGER.log(Level.WARNING, ("Incorrect node type for node " + node));
			}
			return null;
		}
		
		if (ConfigDebug.DAT_REPLACEMENT_ENUMS && node.isEnum())
		{
			data = String.valueOf(DescriptorParser.getInstance().getEnumNameByName(node.getEnumName(), data)).trim();
		}
		
		try
		{
			switch (nodeType)
			{
				case UCHAR:
				{
					return ByteWriter.writeUByte(Byte.parseByte(data));
				}
				case CNTR:
				{
					return ByteWriter.writeCompactInt(Integer.parseInt(data));
				}
				case UBYTE:
				{
					return ByteWriter.writeUByte(Short.parseShort(data));
				}
				case SHORT:
				{
					return ByteWriter.writeShort(Short.parseShort(data));
				}
				case USHORT:
				{
					return ByteWriter.writeUShort(Integer.parseInt(data));
				}
				case UINT:
				case INT:
				{
					return ByteWriter.writeInt(Integer.parseInt(data));
				}
				case UNICODE:
				{
					return ByteWriter.writeUtfString(isRaw ? data : data.substring(1, data.length() - 1), isRaw);
				}
				case ASCF:
				{
					return ByteWriter.writeString(isRaw ? data : data.substring(1, data.length() - 1), isRaw);
				}
				case DOUBLE:
				{
					return ByteWriter.writeDouble(Double.parseDouble(data));
				}
				case FLOAT:
				{
					return ByteWriter.writeFloat(Float.parseFloat(data));
				}
				case LONG:
				{
					return ByteWriter.writeLong(Long.parseLong(data));
				}
				case RGBA:
				{
					return ByteWriter.writeRGBA(data);
				}
				case RGB:
				{
					return ByteWriter.writeRGB(data);
				}
				case HEX:
				{
					return ByteWriter.writeByte((byte) (Integer.parseInt(data, 16) & 0xFF));
				}
				case MAP_INT:
				{
					if (ConfigDebug.DAT_REPLACEMENT_NAMES)
					{
						return ByteWriter.writeInt(GameDataName.getInstance().getId(currentFile, crypter, node, data, mass));
					}
					return ByteWriter.writeInt(Integer.parseInt(data));
				}
				default:
				{
					LOGGER.log(Level.WARNING, ("Unsupported primitive type " + nodeType));
					break;
				}
			}
		}
		catch (Exception e)
		{
			if (!mass)
			{
				LOGGER.log(Level.WARNING, ("Failed to parse value for node " + node + " data: " + data), e);
			}
		}
		
		return null;
	}
	
	private static String getDataString(ParamNode node, String name, Map<String, String> paramMap, Map<ParamNode, String> mapData)
	{
		if ((mapData != null) && mapData.containsKey(node))
		{
			return mapData.get(node);
		}
		return paramMap.get(name);
	}
	
	private static void writeSize(File currentFile, DatCrypter crypter, List<WriteData> writeData, ParamNode node, int cycleSize, boolean mass) throws CycleArgumentException, PackDataException
	{
		if (!node.isSkipWriteSize() && (node.getSize() < 0))
		{
			WriteData iterator = null;
			for (int i = writeData.size() - 1; i >= 0; --i)
			{
				final WriteData wd = writeData.get(i);
				if (wd.isIterator() && wd.getParamNode().getName().equals(node.getCycleName()))
				{
					iterator = wd;
					break;
				}
			}
			
			if (iterator == null)
			{
				throw new CycleArgumentException("Not found iterator for cycle: " + node.getName());
			}
			
			final int index = writeData.indexOf(iterator);
			writeData.remove(index);
			final ParamNode iteratorNode = iterator.getParamNode();
			final byte[] bytes = parseNodeValue(currentFile, crypter, String.valueOf(cycleSize), iteratorNode, false, mass);
			if (bytes == null)
			{
				throw new PackDataException("Cant write size! Node value is null.\r\n-node: " + node);
			}
			
			writeData.add(index, new WriteBytes(iteratorNode, bytes));
		}
	}
	
	private static class WriteData
	{
		final ParamNode paramNode;
		
		WriteData(ParamNode paramNode)
		{
			this.paramNode = paramNode;
		}
		
		boolean isIterator()
		{
			return false;
		}
		
		ParamNode getParamNode()
		{
			return paramNode;
		}
		
		byte[] getBytes()
		{
			return new byte[0];
		}
	}
	
	public static class WriteIterator extends WriteData
	{
		WriteIterator(ParamNode paramNode)
		{
			super(paramNode);
		}
		
		@Override
		public boolean isIterator()
		{
			return true;
		}
	}
	
	private static class WriteBytes extends WriteData
	{
		final byte[] bytes;
		
		WriteBytes(ParamNode paramNode, byte[] bytes)
		{
			super(paramNode);
			this.bytes = bytes;
		}
		
		@Override
		public byte[] getBytes()
		{
			return bytes;
		}
	}
}
