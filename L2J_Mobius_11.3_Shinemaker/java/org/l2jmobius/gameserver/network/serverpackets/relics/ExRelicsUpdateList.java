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

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExRelicsUpdateList extends ServerPacket
{
	private final int _relicListSize;
	private final int _relicId;
	private final int _relicLevel;
	private final int _relicCount;
	
	public ExRelicsUpdateList(int relicListSize, int relicId, int relicLevel, int relicCount)
	{
		_relicListSize = relicListSize;
		_relicId = relicId;
		_relicLevel = relicLevel;
		_relicCount = relicCount;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_UPDATE_LIST.writeId(this, buffer);
		
		// This is send to show new relics added to the list (Relic/Enhancement/Compounding).
		buffer.writeInt(_relicListSize);
		for (int i = 0; i < _relicListSize; i++)
		{
			buffer.writeInt(_relicId);
			buffer.writeInt(_relicLevel);
			buffer.writeInt(_relicCount);
		}
	}
}