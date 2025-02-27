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
package org.l2jmobius.gameserver.network.serverpackets.relics;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.PlayerRelicData;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExRelicsExchangeList extends ServerPacket
{
	private final Player _player;
	
	public ExRelicsExchangeList(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_EXCHANGE_LIST.writeId(this, buffer);
		
		final int attemptsRemainingCountBgrade = _player.getAccountVariables().getInt(AccountVariables.B_GRADE_RELIC_ATEMPTS, Config.RELIC_REPLACE_ATTEMPTS_B_GRADE) > 0 ? _player.getAccountVariables().getInt(AccountVariables.B_GRADE_RELIC_ATEMPTS, Config.RELIC_REPLACE_ATTEMPTS_B_GRADE) : 0;
		final int attemptsRemainingCountAgrade = _player.getAccountVariables().getInt(AccountVariables.A_GRADE_RELIC_ATEMPTS, Config.RELIC_REPLACE_ATTEMPTS_A_GRADE) > 0 ? _player.getAccountVariables().getInt(AccountVariables.A_GRADE_RELIC_ATEMPTS, Config.RELIC_REPLACE_ATTEMPTS_A_GRADE) : 0;
		final int maxRelicsOnConfirmationList = Config.RELIC_UNCONFIRMED_LIST_LIMIT;
		final long defaultReplacementTime = Config.RELIC_UNCONFIRMED_TIME_LIMIT * 86400000; // 86400000 = 1 day in milliseconds
		
		long relicSummonTime = 0;
		final List<Integer> unconfirmedRelics = new ArrayList<>();
		for (PlayerRelicData relic : _player.getRelics())
		{
			if ((relic.getRelicIndex() >= 300) && (relic.getRelicCount() == 1)) // Unconfirmed relics are set on summon to index 300 and up.
			{
				unconfirmedRelics.add(relic.getRelicId());
				relicSummonTime = relic.getRelicSummonTime();
			}
		}
		
		buffer.writeInt(maxRelicsOnConfirmationList);
		buffer.writeInt(unconfirmedRelics.size()); // Confirmation relics array size.
		for (int i = 0; i < unconfirmedRelics.size(); i++)
		{
			final int unconfirmedRelicGrade = RelicData.getInstance().getRelic(unconfirmedRelics.get(i)).getGrade();
			buffer.writeInt(i); // List position.
			buffer.writeInt(unconfirmedRelics.get(i)); // Relic Id.
			buffer.writeInt(unconfirmedRelicGrade == 4 ? attemptsRemainingCountBgrade : attemptsRemainingCountAgrade); // Exchange attempts remaining based on relic grade.
			buffer.writeInt(5); // Exchange attempts max.
			buffer.writeInt((int) ((relicSummonTime + defaultReplacementTime) / 1000)); // Replacement Time.
		}
	}
}
