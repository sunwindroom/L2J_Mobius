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
package org.l2jmobius.gameserver.network.serverpackets.gacha;

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.holders.GachaItemTimeStampHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public class UniqueGachaHistory extends ServerPacket
{
	private static final int SHORT_PACKET_SIZE = 2 + 1 + 4 + 8;
	
	private final List<GachaItemTimeStampHolder> _items;
	
	public UniqueGachaHistory(List<GachaItemTimeStampHolder> items)
	{
		_items = items;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_UNIQUE_GACHA_HISTORY.writeId(this, buffer);
		buffer.writeInt(_items.size());
		for (GachaItemTimeStampHolder item : _items)
		{
			buffer.writeShort(SHORT_PACKET_SIZE);
			buffer.writeByte(item.getRank().getClientId());
			buffer.writeInt(item.getId());
			buffer.writeLong(item.getCount());
			buffer.writeInt(item.getTimeStampFromNow());
		}
	}
}
