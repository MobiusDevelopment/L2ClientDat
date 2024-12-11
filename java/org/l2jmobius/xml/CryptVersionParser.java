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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.clientcryptor.crypt.BlowFishDatCrypter;
import org.l2jmobius.clientcryptor.crypt.DESDatCrypter;
import org.l2jmobius.clientcryptor.crypt.DatCrypter;
import org.l2jmobius.clientcryptor.crypt.RSADatCrypter;
import org.l2jmobius.clientcryptor.crypt.XorDatCrypter;

public class CryptVersionParser
{
	private static final Logger LOGGER = Logger.getLogger(CryptVersionParser.class.getName());
	
	private final Map<String, DatCrypter> _encryptKeys = new LinkedHashMap<>();
	private final Map<String, DatCrypter> _decryptKeys = new LinkedHashMap<>();
	
	public CryptVersionParser()
	{
	}
	
	public void parse()
	{
		final File def = new File("./config/cryptVersion.xml");
		if (def.exists())
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringElementContentWhitespace(true);
			factory.setIgnoringComments(true);
			try
			{
				final Document document = factory.newDocumentBuilder().parse(def);
				for (Node defsNode = document.getFirstChild(); defsNode != null; defsNode = document.getNextSibling())
				{
					if (defsNode.getNodeName().equals("keys"))
					{
						for (Node defNode = defsNode.getFirstChild(); defNode != null; defNode = defNode.getNextSibling())
						{
							if (defNode.getNodeName().equals("key"))
							{
								final String name = defNode.getAttributes().getNamedItem("name").getNodeValue();
								final String type = defNode.getAttributes().getNamedItem("type").getNodeValue().toLowerCase();
								final int code = Integer.parseInt(defNode.getAttributes().getNamedItem("code").getNodeValue().toLowerCase());
								final boolean isDecrypt = Boolean.parseBoolean(defNode.getAttributes().getNamedItem("decrypt").getNodeValue());
								final boolean useStructure = Boolean.parseBoolean(defNode.getAttributes().getNamedItem("useStructure").getNodeValue());
								final String extension = defNode.getAttributes().getNamedItem("extension").getNodeValue();
								DatCrypter dat = null;
								final String s = type;
								switch (s)
								{
									case "rsa":
									{
										final String modulus = defNode.getAttributes().getNamedItem("modulus").getNodeValue();
										final String exp = defNode.getAttributes().getNamedItem("exp").getNodeValue();
										dat = new RSADatCrypter(name, code, modulus, exp, isDecrypt);
										break;
									}
									case "xor":
									{
										dat = new XorDatCrypter(name, code, Integer.parseInt(defNode.getAttributes().getNamedItem("key").getNodeValue()), isDecrypt);
										break;
									}
									case "blowfish":
									{
										dat = new BlowFishDatCrypter(name, code, defNode.getAttributes().getNamedItem("key").getNodeValue(), isDecrypt);
										break;
									}
									case "des":
									{
										dat = new DESDatCrypter(name, code, defNode.getAttributes().getNamedItem("key").getNodeValue(), isDecrypt);
										break;
									}
								}
								if (dat != null)
								{
									dat.addFileExtension(extension);
									dat.setUseStructure(useStructure);
									if (isDecrypt)
									{
										_decryptKeys.put(name, dat);
									}
									else
									{
										_encryptKeys.put(name, dat);
									}
								}
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
	
	public Map<String, DatCrypter> getEncryptKey()
	{
		return _encryptKeys;
	}
	
	public Map<String, DatCrypter> getDecryptKeys()
	{
		return _decryptKeys;
	}
	
	public DatCrypter getEncryptKey(String s)
	{
		return _encryptKeys.get(s);
	}
	
	public DatCrypter getDecryptKey(String s)
	{
		return _decryptKeys.get(s);
	}
	
	public static CryptVersionParser getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CryptVersionParser INSTANCE = new CryptVersionParser();
	}
}
