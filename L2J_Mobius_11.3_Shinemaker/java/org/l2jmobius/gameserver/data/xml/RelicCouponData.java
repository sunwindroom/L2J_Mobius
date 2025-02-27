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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.holders.RelicCouponHolder;

/**
 * @author Liamxroy
 */
public class RelicCouponData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(RelicCouponData.class.getName());
	private static final Map<Integer, RelicCouponHolder> RELIC_COUPONS = new HashMap<>();
	
	protected RelicCouponData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		RELIC_COUPONS.clear();
		parseDatapackFile("data/RelicCouponData.xml");
		
		if (!RELIC_COUPONS.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + RELIC_COUPONS.size() + " relic coupon data.");
		}
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "coupon", couponNode ->
		{
			final StatSet set = new StatSet(parseAttributes(couponNode));
			final int itemId = set.getInt("itemId");
			if (ItemData.getInstance().getTemplate(itemId) == null)
			{
				LOGGER.info(getClass().getSimpleName() + ": Could not find coupon with item id " + itemId + ".");
				return;
			}
			
			final int relicId = set.getInt("relicId", 0);
			RELIC_COUPONS.put(itemId, new RelicCouponHolder(itemId, relicId));
		}));
	}
	
	public RelicCouponHolder getRelicIdFromCouponId(int itemId)
	{
		return RELIC_COUPONS.get(itemId);
	}
	
	public static RelicCouponData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RelicCouponData INSTANCE = new RelicCouponData();
	}
}
