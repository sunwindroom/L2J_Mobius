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
import java.util.Collection;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.PlayerRelicData;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExRelicsList extends ServerPacket
{
	private final Player _player;
	private final Collection<PlayerRelicData> _relics;
	private final List<Integer> _confirmedRelics;
	
	public ExRelicsList(Player player)
	{
		_player = player;
		_relics = _player.getRelics();
		
		final List<Integer> confirmedRelics = new ArrayList<>();
		for (PlayerRelicData relic : _relics)
		{
			if (relic.getRelicIndex() < 300) // Unconfirmed relics are set on summon to index 300 and up.
			{
				confirmedRelics.add(relic.getRelicId());
			}
		}
		_confirmedRelics = confirmedRelics;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_LIST.writeId(this, buffer);
		
		buffer.writeInt(1); // _index guessed (min relic id).
		buffer.writeInt(140); // _indexMax guessed (max relic id).
		buffer.writeInt(_confirmedRelics.size()); // Confirmed relics array size.
		
		for (PlayerRelicData relic : _relics)
		{
			if (relic.getRelicIndex() < 300) // Unconfirmed relics are set on summon to index 300 and should not be shown until confirmation.
			{
				buffer.writeInt(relic.getRelicId());
				buffer.writeInt(relic.getRelicLevel());
				buffer.writeInt(relic.getRelicCount());
			}
		}
	}
}
