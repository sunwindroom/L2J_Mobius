/*
 * This file is part of the L2J Mobius project.
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
package org.l2jmobius.gameserver.network.serverpackets.dethroneability;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExHolyFireOpenUI extends ServerPacket
{
	private final Player _player;
	
	public ExHolyFireOpenUI(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_HOLY_FIRE_OPEN_UI.writeId(this, buffer);
		buffer.writeInt(Config.CONQUEST_MAX_SACRED_FIRE_SLOTS_COUNT); // 0 - slots are locked / 1-4 - number of open slots (max ui limit is 4)
		
		for (int i = 1; i <= Config.CONQUEST_MAX_SACRED_FIRE_SLOTS_COUNT; i++)
		{
			
			final int currentState = _player.getVariables().getInt("SACRED_FIRE_SLOT_" + i, 0);
			final long summonTime = _player.getVariables().getLong("SACRED_FIRE_SLOT_" + i + "_SUMMON_TIME", 0);
			final int currentLifetime = (int) ((System.currentTimeMillis() - summonTime) / 1000);
			final boolean rewardState = _player.getVariables().getInt("SACRED_FIRE_SLOT_" + i, 0) == 2;
			
			buffer.writeByte(currentState); // int cState; 0 - you can summon the flame / 1 - summoning the flame / 2- summoning flame is finished get reward / 3 - flame is extinguished
			buffer.writeInt((currentState == 1) ? currentLifetime : (currentState == 2) ? 1500 : 0); // int nElapsedTime; current lifetime value
			buffer.writeInt((currentState == 1) ? 1500 : (currentState == 2) ? 1500 : 0); // int nLifespan; max lifetime value 1500 (25 min in seconds)
			// Rewards
			if (rewardState)
			{
				buffer.writeInt(Config.CONQUEST_SACRED_FIRE_REWARD_PERSONAL_POINTS); // int nRewardPersonalPoint; 0 - reward disabled
				buffer.writeInt(Config.CONQUEST_SACRED_FIRE_REWARD_SERVER_POINTS); // int nRewardServerPoint; 0 - reward disabled
				buffer.writeInt(Config.CONQUEST_SACRED_FIRE_REWARD_FIRE_SOURCE_POINTS); // int nRewardPrimalFirePoint; 0 - reward disabled
			}
			else
			{
				buffer.writeInt(0); // int nRewardPersonalPoint; 0 - reward disabled
				buffer.writeInt(0); // int nRewardServerPoint; 0 - reward disabled
				buffer.writeInt(0); // int nRewardPrimalFirePoint; 0 - reward disabled
			}
			// Common Item Rewards
			if (rewardState)
			{
				buffer.writeInt(Config.CONQUEST_SACRED_FIRE_REWARDS.size()); // rewards array size 0 - rewards disabled
				for (ItemHolder reward : Config.CONQUEST_SACRED_FIRE_REWARDS)
				{
					buffer.writeInt(reward.getId());
					buffer.writeLong(reward.getCount());
				}
			}
			else
			{
				buffer.writeInt(0); // rewards array size 0 - rewards disabled
			}
			buffer.writeByte(1); // int cRewardState; 1 - ??
		}
	}
}
