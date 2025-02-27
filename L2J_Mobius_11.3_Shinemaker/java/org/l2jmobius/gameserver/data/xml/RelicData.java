/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.holders.RelicDataHolder;

/**
 * @author CostyKiller
 */
public class RelicData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(RelicData.class.getName());
	
	private static final Map<Integer, RelicDataHolder> RELICS = new HashMap<>();
	
	protected RelicData()
	{
		if (Config.RELIC_SYSTEM_ENABLED)
		{
			load();
		}
	}
	
	@Override
	public void load()
	{
		RELICS.clear();
		
		if (Config.RELIC_SYSTEM_ENABLED)
		{
			parseDatapackFile("data/RelicData.xml");
		}
		
		if (!RELICS.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + RELICS.size() + " relics.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": System is disabled.");
		}
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("relic".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
						Node att;
						final StatSet set = new StatSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						final int relicId = parseInteger(attrs, "id");
						final int grade = parseInteger(attrs, "grade");
						final int skillId = parseInteger(attrs, "skillId");
						int enchantLevel = 0;
						int skillLevel = 0;
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							attrs = b.getAttributes();
							if ("relicStat".equalsIgnoreCase(b.getNodeName()))
							{
								enchantLevel = parseInteger(attrs, "enchantLevel");
								skillLevel = parseInteger(attrs, "skillLevel");
							}
						}
						final RelicDataHolder template = new RelicDataHolder(relicId, grade, skillId, enchantLevel, skillLevel);
						RELICS.put(relicId, template);
					}
				}
			}
		}
	}
	
	public RelicDataHolder getRelic(int id)
	{
		return RELICS.get(id);
	}
	
	public int getRelicSkillId(int id)
	{
		return RELICS.get(id).getSkillId();
	}
	
	public int getRelicSkillLevel(int id)
	{
		return RELICS.get(id).getSkillLevel();
	}
	
	public Collection<RelicDataHolder> getRelics()
	{
		return RELICS.values();
	}
	
	public static RelicData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RelicData INSTANCE = new RelicData();
	}
}
