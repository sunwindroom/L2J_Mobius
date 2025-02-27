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
package org.l2jmobius.gameserver.network.clientpackets.relics;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.RelicCouponRequest;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsSummonResult;

/**
 * @author CostyKiller
 */
public class RequestRelicsSummon extends ClientPacket
{
	private int _couponId;
	
	@Override
	protected void readImpl()
	{
		_couponId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Item couponItem = player.getInventory().getItemByItemId(_couponId);
		if (couponItem == null)
		{
			player.sendPacket(SystemMessageId.FAILURE_ALL_MATERIALS_ARE_LOST);
			return;
		}
		
		if (player.getAccountVariables().getInt(AccountVariables.UNCONFIRMED_RELICS_COUNT, 0) == Config.RELIC_UNCONFIRMED_LIST_LIMIT)
		{
			player.sendPacket(SystemMessageId.SUMMON_COMPOUND_IS_UNAVAILABLE_AS_YOU_HAVE_MORE_THAN_100_UNCONFIRMED_RELICS);
			return;
		}
		
		player.addRequest(new RelicCouponRequest(player));
		
		final int relicSummonCount = Config.ELEVEN_SUMMON_COUNT_COUPONS.contains(_couponId) ? 11 : 1;
		player.sendPacket(new ExRelicsSummonResult(player, _couponId, relicSummonCount));
		player.destroyItem("RelicSummon", couponItem, 1, player, true);
	}
}