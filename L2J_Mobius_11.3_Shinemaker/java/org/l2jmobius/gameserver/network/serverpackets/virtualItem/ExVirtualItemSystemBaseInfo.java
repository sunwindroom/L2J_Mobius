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
package org.l2jmobius.gameserver.network.serverpackets.virtualItem;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public class ExVirtualItemSystemBaseInfo extends ServerPacket
{
	private final Player _player;
	
	public ExVirtualItemSystemBaseInfo(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_VIRTUALITEM_SYSTEM_BASE_INFO.writeId(this, buffer);
		buffer.writeInt(2592000); // Event ending time in seconds 3600 = 1 hour
		buffer.writeInt(_player.getVariables().getInt(PlayerVariables.ILLUSORY_POINTS_ACQUIRED, 0)); // Total Illusory Points acquired
		buffer.writeInt(_player.getVariables().getInt(PlayerVariables.ILLUSORY_POINTS_USED, 0)); // Total Illusory Points used
	}
}

/*
 * struct _S_EX_VIRTUALITEM_SYSTEM_BASE_INFO { var int nEndTime; var int nTotalGetVISPoint; var int nTotalUsedVISPoint; };
 */