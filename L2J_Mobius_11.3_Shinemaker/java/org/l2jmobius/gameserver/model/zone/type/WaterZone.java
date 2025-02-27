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
package org.l2jmobius.gameserver.model.zone.type;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.FakePlayerInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.ServerObjectInfo;

public class WaterZone extends ZoneType
{
	public WaterZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		// Tempfix for remaining in water zones on exit.
		if (!creature.isInsideZone(ZoneId.WATER))
		{
			creature.setInsideZone(ZoneId.WATER, true);
		}
		
		// TODO: update to only send speed status when that packet is known
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			if (player.checkTransformed(transform -> !transform.canSwim()))
			{
				creature.stopTransformation(true);
			}
			else
			{
				player.broadcastUserInfo();
			}
		}
		else if (creature.isNpc())
		{
			World.getInstance().forEachVisibleObject(creature, Player.class, player ->
			{
				if (creature.isFakePlayer())
				{
					player.sendPacket(new FakePlayerInfo(creature.asNpc()));
				}
				else if (creature.getRunSpeed() == 0)
				{
					player.sendPacket(new ServerObjectInfo(creature.asNpc(), player));
				}
				else
				{
					player.sendPacket(new NpcInfo(creature.asNpc()));
				}
			});
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		// Tempfix for remaining in water zones on exit.
		if (creature.isInsideZone(ZoneId.WATER))
		{
			creature.setInsideZone(ZoneId.WATER, false);
		}
		
		// TODO: update to only send speed status when that packet is known
		if (creature.isPlayer())
		{
			// Mobius: Attempt to stop water task.
			final Player player = creature.asPlayer();
			if (!player.isInsideZone(ZoneId.WATER))
			{
				player.stopWaterTask();
			}
			if (!player.isTeleporting())
			{
				player.broadcastUserInfo();
			}
		}
		else if (creature.isNpc())
		{
			World.getInstance().forEachVisibleObject(creature, Player.class, player ->
			{
				if (creature.isFakePlayer())
				{
					player.sendPacket(new FakePlayerInfo(creature.asNpc()));
				}
				else if (creature.getRunSpeed() == 0)
				{
					player.sendPacket(new ServerObjectInfo(creature.asNpc(), player));
				}
				else
				{
					player.sendPacket(new NpcInfo(creature.asNpc()));
				}
			});
		}
	}
	
	public int getWaterZ()
	{
		return getZone().getHighZ();
	}
}
