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

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.RelicCollectionData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.PlayerRelicCollectionData;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExRelicsCollectionInfo extends ServerPacket
{
	private final Player _player;
	private final List<Integer> _relicCollectionIds = new ArrayList<>();
	
	public ExRelicsCollectionInfo(Player player)
	{
		_player = player;
		_relicCollectionIds.clear();
		for (PlayerRelicCollectionData relicCollection : player.getRelicCollections())
		{
			if (!_relicCollectionIds.contains(relicCollection.getRelicCollectionId()))
			{
				_relicCollectionIds.add(relicCollection.getRelicCollectionId());
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_COLLECTION_INFO.writeId(this, buffer);
		
		buffer.writeInt(1); // Page index.
		buffer.writeInt(82); // Max page.
		buffer.writeInt(_relicCollectionIds.size());
		
		for (int i = 1; i <= (_relicCollectionIds.size()); i++)
		{
			buffer.writeInt(_relicCollectionIds.get(i - 1)); // Collection array position.
			
			int completeCount = 0;
			for (PlayerRelicCollectionData coll : _player.getRelicCollections())
			{
				if (coll.getRelicCollectionId() == _relicCollectionIds.get(i - 1))
				{
					completeCount++;
				}
			}
			
			final int completedRelicsCount = RelicCollectionData.getInstance().getRelicCollection(_relicCollectionIds.get(i - 1)).getCompleteCount();
			buffer.writeByte(completeCount == completedRelicsCount);
			buffer.writeInt(completeCount);
			
			for (PlayerRelicCollectionData collection : _player.getRelicCollections())
			{
				if (collection.getRelicCollectionId() == _relicCollectionIds.get(i - 1))
				{
					buffer.writeInt(collection.getRelicId()); // Relic id.
					buffer.writeInt(collection.getRelicLevel()); // Relic level.
				}
			}
		}
	}
}
