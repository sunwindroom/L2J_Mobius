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
package org.l2jmobius.gameserver.network.serverpackets.dethrone;

import java.util.Collection;
import java.util.Collections;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.DailyMissionDataConquest;
import org.l2jmobius.gameserver.model.DailyMissionDataHolder;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExDethroneDailyMissionInfo extends ServerPacket
{
	private final Player _player;
	private final Collection<DailyMissionDataHolder> _rewards;
	
	public ExDethroneDailyMissionInfo(Player player)
	{
		_player = player;
		_rewards = DailyMissionDataConquest.getInstance().getDailyMissionData(player);
	}
	
	public ExDethroneDailyMissionInfo(Player player, DailyMissionDataHolder holder)
	{
		_player = player;
		_rewards = Collections.singletonList(holder);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		if (!DailyMissionDataConquest.getInstance().isAvailable() || (_player.getClan() == null))
		{
			return;
		}
		
		ServerPackets.EX_DETHRONE_DAILY_MISSION_INFO.writeId(this, buffer);
		buffer.writeInt(_rewards.size()); // how many missions have
		for (DailyMissionDataHolder reward : _rewards)
		{
			int progress = reward.getProgress(_player);
			int status = reward.getStatus(_player);
			if (status == 2) // daily mission status
			{
				status = 0; // in progress status
			}
			else if (status == 1) // daily mission status
			{
				status = 2; // mission complete status
			}
			buffer.writeInt(reward.getId()); // mission name (only can use is 1)
			buffer.writeInt(progress); // current item count/300
			buffer.writeByte(status); // 1 - receive, 0 - no receive
		}
	}
}
