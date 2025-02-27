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
package org.l2jmobius.gameserver.network.clientpackets.dethrone;

import org.l2jmobius.gameserver.instancemanager.GlobalVariablesManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.dethrone.ExDethroneDisconnectCastle;
import org.l2jmobius.gameserver.network.serverpackets.dethrone.ExDethroneServerInfo;

/**
 * @author CostyKiller
 */
public class RequestExDethroneDisconnectCastle extends ClientPacket
{
	@Override
	protected void readImpl()
	{
		// _connected = readBoolean(); // cDummy
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!GlobalVariablesManager.getInstance().getBoolean("CONQUEST_CONNECTED", false))
		{
			player.sendMessage("Conquest connection is already inactive.");
		}
		else // Disconnect.
		{
			player.sendMessage("Conquest connection is inactive.");
			
			final long serverPoints = GlobalVariablesManager.getInstance().getLong("CONQUEST_SERVER_POINTS", 0);
			final long serverSoulOrbs = GlobalVariablesManager.getInstance().getLong("CONQUEST_SERVER_SOUL_ORBS", 0L);
			final boolean adenCastleOwner = (player.getClan() != null) && (player.getObjectId() == player.getClan().getLeaderId()) && (player.getClan().getCastleId() == 5); // Aden Castle Id is 5 _bConnected = true
			GlobalVariablesManager.getInstance().set("CONQUEST_CONNECTED", false);
			player.sendPacket(new ExDethroneDisconnectCastle(false));
			player.sendPacket(new ExDethroneServerInfo(serverPoints, serverSoulOrbs, adenCastleOwner, false));
			player.sendPacket(SystemMessageId.THE_CONNECTION_TO_THE_CONQUEST_WORLD_IS_LOST);
		}
	}
}
