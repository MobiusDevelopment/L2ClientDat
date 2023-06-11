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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.L2ClientDat;
import org.l2jmobius.listeners.FormatListener;
import org.l2jmobius.util.DebugUtil;
import org.l2jmobius.util.Util;
import org.l2jmobius.xml.exceptions.CycleArgumentException;

public class DescriptorParser
{
	private static final Logger LOGGER = Logger.getLogger(DescriptorParser.class.getName());
	
	private final Map<String, Map<String, Descriptor>> _descriptors = new HashMap<>();
	private final Map<String, List<ParamNode>> _definitions = new HashMap<>();
	private final Map<String, List<DescriptorLink>> _links = new LinkedHashMap<>();
	
	public DescriptorParser()
	{
	}
	
	public void parse()
	{
		parseDefinitions();
		Util.loadFiles("./data/structure/", ".xml").forEach(this::parseDescriptor);
	}
	
	private void parseDefinitions()
	{
		final File def = new File("./data/definitions.xml");
		if (def.exists())
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringElementContentWhitespace(true);
			factory.setIgnoringComments(true);
			try
			{
				final Document doc = factory.newDocumentBuilder().parse(def);
				for (Node defsNode = doc.getFirstChild(); defsNode != null; defsNode = doc.getNextSibling())
				{
					if (defsNode.getNodeName().equals("definitions"))
					{
						for (Node defNode = defsNode.getFirstChild(); defNode != null; defNode = defNode.getNextSibling())
						{
							if (defNode.getNodeName().equals("definition"))
							{
								final String defName = defNode.getAttributes().getNamedItem("name").getNodeValue();
								final List<ParamNode> nodes = parseNodes(defNode, true, new HashSet<String>(), "definitions->" + defName, Collections.emptyList());
								_definitions.put(defName, nodes);
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}
	
	private void parseDescriptor(File file)
	{
		if (!file.exists())
		{
			DebugUtil.debug("File " + file.getName() + " not found.");
		}
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringElementContentWhitespace(true);
			factory.setIgnoringComments(true);
			final Document doc = factory.newDocumentBuilder().parse(file);
			for (Node fileNode0 = doc.getFirstChild(); fileNode0 != null; fileNode0 = doc.getNextSibling())
			{
				if (fileNode0.getNodeName().equalsIgnoreCase("list"))
				{
					for (Node fileNode2 = fileNode0.getFirstChild(); fileNode2 != null; fileNode2 = fileNode2.getNextSibling())
					{
						if (fileNode2.getNodeName().equalsIgnoreCase("link"))
						{
							final Node chronicleNode = fileNode0.getAttributes().getNamedItem("name");
							final String dir = (chronicleNode != null) ? chronicleNode.getNodeValue() : file.getName().substring(0, file.getName().length() - 4);
							final String namePattern = fileNode2.getAttributes().getNamedItem("pattern").getNodeValue();
							final String linkFile = fileNode2.getAttributes().getNamedItem("file").getNodeValue();
							final String linkVersion = fileNode2.getAttributes().getNamedItem("version").getNodeValue();
							List<DescriptorLink> list = _links.get(dir);
							if (list == null)
							{
								list = new ArrayList<>();
							}
							list.add(new DescriptorLink(dir, namePattern, linkFile, linkVersion));
							_links.put(dir, list);
						}
						else if (fileNode2.getNodeName().equalsIgnoreCase("file"))
						{
							final String dir2 = file.getName().substring(0, file.getName().length() - 4);
							final String namePattern2 = fileNode2.getAttributes().getNamedItem("pattern").getNodeValue();
							final boolean isRawData = parseBoolNode(fileNode2, "isRaw", false);
							final boolean isSafePackage = parseBoolNode(fileNode2, "isSafePackage", false);
							final String formatName = parseStringNode(fileNode2, "format", null);
							
							DebugUtil.debug("Boot of parsing file: " + namePattern2);
							
							final List<ParamNode> nodes = parseNodes(fileNode2, false, new HashSet<String>(), dir2 + "->" + namePattern2, Collections.emptyList());
							final Descriptor desc = new Descriptor(file.getName(), namePattern2, nodes);
							desc.setIsRawData(isRawData);
							desc.setIsSafePackage(isSafePackage);
							if (formatName != null)
							{
								final Object obj = Util.loadJavaClass(formatName, "./data/structure/format/");
								if (obj == null)
								{
									LOGGER.log(Level.WARNING, ("Format src file '" + formatName + ".java' not found!"));
								}
								else if (obj instanceof FormatListener)
								{
									desc.setFormat((FormatListener) obj);
								}
							}
							
							Map<String, Descriptor> versions = _descriptors.get(dir2);
							if (versions == null)
							{
								versions = new HashMap<>();
							}
							versions.put(namePattern2, desc);
							_descriptors.put(dir2, versions);
							DebugUtil.debug("End of parsing file: " + namePattern2);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	private List<ParamNode> parseNodes(Node fileNode, boolean isHideName, Set<String> names, String fileName, List<ParamNode> parentNodes) throws Exception
	{
		final Map<String, Integer> defsCounter = new HashMap<>();
		final List<ParamNode> nodes = new LinkedList<>();
		for (Node node = fileNode.getFirstChild(); node != null; node = node.getNextSibling())
		{
			final String nodeName = node.getNodeName();
			if (!nodeName.equals("#text"))
			{
				final boolean isHide = isHideName || parseBoolNode(node, "hidden", true);
				if (nodeName.equalsIgnoreCase("else"))
				{
					final ParamNode prevNode = nodes.isEmpty() ? null : nodes.get(nodes.size() - 1);
					if (prevNode == null)
					{
						DebugUtil.debug("Not found previous IF for [else] data!");
					}
					else
					{
						final ParamNode beginNode = new ParamNode(prevNode.getName(), ParamNodeType.ELSE, null);
						beginNode.setParamIf(prevNode.getParamIf());
						beginNode.setValIf(prevNode.getValIf());
						beginNode.addSubNodes(parseNodes(node, false, names, fileName, nodes));
						nodes.add(beginNode);
						DebugUtil.debug("Found [else] data: " + prevNode.getName());
					}
				}
				else if (node.getAttributes().getNamedItem("name") == null)
				{
					LOGGER.log(Level.WARNING, ("Node name == null, fileName: " + fileName));
				}
				else
				{
					final String entityName = node.getAttributes().getNamedItem("name").getNodeValue();
					if (nodeName.equalsIgnoreCase("node"))
					{
						final String type = node.getAttributes().getNamedItem("reader").getNodeValue();
						if (_definitions.containsKey(type))
						{
							if (!defsCounter.containsKey(type))
							{
								defsCounter.put(type, 1);
							}
							else
							{
								defsCounter.put(type, defsCounter.get(type) + 1);
							}
							final List<ParamNode> defNodes = _definitions.get(type);
							for (ParamNode defNode : defNodes)
							{
								final ParamNode copied = defNode.copy();
								copied.setName(entityName);
								if (isHide)
								{
									copied.setHidden();
								}
								nodes.add(copied);
							}
						}
						else
						{
							final ParamNode dataNode = new ParamNode(entityName, ParamNodeType.VARIABLE, ParamType.valueOf(type));
							if (isHide)
							{
								dataNode.setHidden();
							}
							nodes.add(dataNode);
							DebugUtil.debug("Found node: " + dataNode.getName());
						}
						if (names.contains(entityName))
						{
							LOGGER.log(Level.WARNING, ("Node name duplicated [" + entityName + "]\tfileName: " + fileName));
						}
						names.add(entityName);
					}
					else if (nodeName.equalsIgnoreCase("for"))
					{
						String iteratorName = entityName;
						final boolean skipWriteSize = parseBoolNode(node, "skipWriteSize", false);
						int size = -1;
						if (node.getAttributes().getNamedItem("size") != null)
						{
							final String sizeStr = node.getAttributes().getNamedItem("size").getNodeValue();
							if (sizeStr.startsWith("#"))
							{
								iteratorName = sizeStr.substring(1);
							}
							else
							{
								size = Integer.parseInt(sizeStr);
							}
						}
						
						if (size == 0)
						{
							LOGGER.log(Level.WARNING, ("Size of cycle [" + iteratorName + "] was set to zero. Deprecated cycle?"));
						}
						
						DebugUtil.debug("Found cycle for variable: " + entityName);
						final ParamNode beginNode2 = new ParamNode(entityName, ParamNodeType.FOR, null);
						if (size >= 0)
						{
							beginNode2.setSize(size);
						}
						if (isHide)
						{
							beginNode2.setHidden();
						}
						beginNode2.setSkipWriteSize(skipWriteSize);
						beginNode2.addSubNodes(parseNodes(node, false, names, fileName, nodes));
						beginNode2.setCycleName(iteratorName);
						nodes.add(beginNode2);
						
						boolean iteratorFound = false;
						for (ParamNode n : nodes)
						{
							if (iteratorName.equals(n.getName()))
							{
								n.setIterator();
								iteratorFound = true;
								break;
							}
						}
						
						if (!iteratorFound)
						{
							for (ParamNode n : parentNodes)
							{
								if (iteratorName.equals(n.getName()))
								{
									n.setIterator();
									iteratorFound = true;
									break;
								}
							}
						}
						
						if (!iteratorFound && (size < 0))
						{
							throw new CycleArgumentException("Invalid argument [" + iteratorName + "] for [cycle]");
						}
					}
					else if (nodeName.equalsIgnoreCase("wrapper"))
					{
						final ParamNode beginNode = new ParamNode(entityName, ParamNodeType.WRAPPER, null);
						beginNode.addSubNodes(parseNodes(node, true, names, fileName, nodes));
						nodes.add(beginNode);
						DebugUtil.debug("Found [wrapper] data " + entityName);
					}
					else if (nodeName.equalsIgnoreCase("write"))
					{
						final ParamNode beginNode = new ParamNode(entityName, ParamNodeType.CONSTANT, ParamType.STRING);
						beginNode.setHidden();
						nodes.add(beginNode);
						DebugUtil.debug("Found [constant] data: " + entityName);
					}
					else if (nodeName.equalsIgnoreCase("if"))
					{
						String paramName = node.getAttributes().getNamedItem("param").getNodeValue();
						if (!paramName.startsWith("#"))
						{
							throw new Exception("Invalid argument [" + entityName + "] for [if]");
						}
						
						final String vsl = node.getAttributes().getNamedItem("val").getNodeValue();
						paramName = paramName.substring(1);
						final ParamNode beginNode3 = new ParamNode(entityName, ParamNodeType.IF, null);
						beginNode3.setParamIf(paramName);
						beginNode3.setValIf(vsl);
						beginNode3.addSubNodes(parseNodes(node, false, names, fileName, nodes));
						nodes.add(beginNode3);
						DebugUtil.debug("Found [if] data: " + entityName);
					}
					else if (nodeName.equalsIgnoreCase("mask"))
					{
						String paramName = node.getAttributes().getNamedItem("param").getNodeValue();
						if (!paramName.startsWith("#"))
						{
							throw new Exception("Invalid argument [" + entityName + "] for [mask]");
						}
						
						final int value = Integer.parseInt(node.getAttributes().getNamedItem("val").getNodeValue());
						paramName = paramName.substring(1);
						final ParamNode beginNode3 = new ParamNode(entityName, ParamNodeType.MASK, null);
						beginNode3.setParamMask(paramName);
						beginNode3.setValMask(value);
						beginNode3.addSubNodes(parseNodes(node, false, names, fileName, nodes));
						nodes.add(beginNode3);
						DebugUtil.debug("Found [mask] data: " + entityName);
					}
				}
			}
		}
		return nodes;
	}
	
	public Descriptor findDescriptorForFile(String dir, String fileName)
	{
		if (L2ClientDat.DEV_MODE)
		{
			reload();
		}
		
		final List<DescriptorLink> listDes = _links.get(dir);
		if (listDes == null)
		{
			return null;
		}
		
		for (DescriptorLink desc : listDes)
		{
			if (fileName.toLowerCase().matches(desc.getNamePattern().toLowerCase()) && _descriptors.containsKey(desc.getLinkFile()))
			{
				final Map<String, Descriptor> versions = _descriptors.get(desc.getLinkFile());
				if (versions.containsKey(desc.getLinkVersion()))
				{
					return versions.get(desc.getLinkVersion());
				}
				continue;
			}
		}
		
		return null;
	}
	
	private boolean parseBoolNode(Node node, String name, boolean def)
	{
		if ((node.getAttributes() == null) || (node.getAttributes().getNamedItem(name) == null))
		{
			return def;
		}
		
		return node.getAttributes().getNamedItem(name).getNodeValue().equalsIgnoreCase("true");
	}
	
	private String parseStringNode(Node node, String name, String def)
	{
		if ((node.getAttributes() == null) || (node.getAttributes().getNamedItem(name) == null))
		{
			return def;
		}
		
		return node.getAttributes().getNamedItem(name).getNodeValue();
	}
	
	public Set<String> getChronicleNames()
	{
		if (L2ClientDat.DEV_MODE)
		{
			reload();
		}
		
		return _links.keySet();
	}
	
	private void reload()
	{
		_descriptors.clear();
		_definitions.clear();
		_links.clear();
		parse();
	}
	
	public static DescriptorParser getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DescriptorParser INSTANCE = new DescriptorParser();
	}
}
