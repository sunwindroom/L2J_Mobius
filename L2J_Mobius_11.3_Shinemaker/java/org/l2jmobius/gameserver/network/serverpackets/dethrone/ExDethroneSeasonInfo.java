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

import java.util.Calendar;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Lefteris
 */
public class ExDethroneSeasonInfo extends ServerPacket
{
	private final int _seasonYear;
	private final int _seasonMonth;
	private final boolean _open;
	
	public ExDethroneSeasonInfo(boolean open)
	{
		_seasonYear = Calendar.getInstance().get(Calendar.YEAR);
		_seasonMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
		_open = open;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_DETHRONE_SEASON_INFO.writeId(this, buffer);
		buffer.writeInt(_seasonYear);
		buffer.writeInt(_seasonMonth);
		buffer.writeByte(_open);
	}
}
