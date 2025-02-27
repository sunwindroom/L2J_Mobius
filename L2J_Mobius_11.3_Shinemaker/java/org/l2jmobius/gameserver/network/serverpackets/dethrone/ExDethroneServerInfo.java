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

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExDethroneServerInfo extends ServerPacket
{
	private final long _serverPoints;
	private final long _serverSoulOrbs;
	private final boolean _adenCastleOwner;
	private final boolean _connected;
	
	public ExDethroneServerInfo(long serverPoints, long serverSoulOrbs, boolean adenCastleOwner, boolean connected)
	{
		_serverPoints = serverPoints;
		_serverSoulOrbs = serverSoulOrbs;
		_adenCastleOwner = adenCastleOwner;
		_connected = connected;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_DETHRONE_SERVER_INFO.writeId(this, buffer);
		// Receiving Server Points
		buffer.writeInt(1); // array size (only one server atm)
		
		buffer.writeInt(1); // server rank
		buffer.writeInt(Config.SERVER_ID); // server id (off 15)
		buffer.writeLong(_serverPoints); // server points
		
		// buffer.writeInt(2); // server rank 2
		// buffer.writeInt(2); // server id 2 (off 16)
		// buffer.writeLong(123456789); // server points 2
		
		// Soul Orb Status
		buffer.writeInt(1); // array size (only one server atm)
		
		// buffer.writeInt(1); // server rank 1
		// buffer.writeInt(1); // server id 1
		// buffer.writeLong(1234); // server score 1
		//
		// buffer.writeInt(2); // server rank 2
		// buffer.writeInt(2); // server id 2
		// buffer.writeLong(12345); // server score 2
		//
		// buffer.writeInt(3); // server rank 3
		// buffer.writeInt(3); // server id 3
		// buffer.writeLong(123456); // server score 3
		
		buffer.writeInt(1); // server rank 1
		buffer.writeInt(Config.SERVER_ID); // server id 4
		buffer.writeLong(_serverSoulOrbs); // server score 4
		
		// Connection List Array
		// this number must match the number of server in both arrays
		// number of servers from soul orb status array that can be connected (0 - no connections available, connecting to conquest button is grey (disabled)
		buffer.writeInt(1); // array size
		// buffer.writeInt(_bConnected ? 1 : 0); // connection status on/off for server id 1
		// buffer.writeInt(_bConnected ? 2 : 0); // connection status on/off for server id 2
		// buffer.writeInt(_bConnected ? 3 : 0); // connection status on/off for server id 3
		buffer.writeInt(_connected ? Config.SERVER_ID : 0); // connection status on/off for server id 4
		
		// Connection button status
		buffer.writeInt(_adenCastleOwner ? 1 : 0); // 0 - connecting to conquest button is grey (disabled) / 1 - connecting to conquest button is green (enabled)
		
		buffer.writeInt(Config.SERVER_ID); // nDethroneWorldID
	}
}
