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
import org.l2jmobius.gameserver.network.serverpackets.dethrone.ExDethroneConnectCastle;
import org.l2jmobius.gameserver.network.serverpackets.dethrone.ExDethroneServerInfo;

/**
 * @author CostyKiller
 */
public class RequestExDethroneConnectCastle extends ClientPacket
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
			player.sendMessage("Conquest connection is active.");
			
			// Take items and connect to conquest.
			player.destroyItemByItemId("ConquestConnectionFee", 3031, 500000, player, true); // 500k Spirit Ore
			player.destroyItemByItemId("ConquestConnectionFee", 81981, 1, player, true); // 1 Dimensional Chain
			GlobalVariablesManager.getInstance().set("CONQUEST_CONNECTED", true);
			
			final long serverPoints = GlobalVariablesManager.getInstance().getLong("CONQUEST_SERVER_POINTS", 0L);
			final long serverSoulOrbs = GlobalVariablesManager.getInstance().getLong("CONQUEST_SERVER_SOUL_ORBS", 0L);
			final boolean _bAdenCastleOwner = (player.getClan() != null) && (player.getObjectId() == player.getClan().getLeaderId()) && (player.getClan().getCastleId() == 5); // Aden Castle Id is 5 _bConnected = true
			
			player.sendPacket(new ExDethroneConnectCastle(true));
			player.sendPacket(new ExDethroneServerInfo(serverPoints, serverSoulOrbs, _bAdenCastleOwner, true));
			player.sendPacket(SystemMessageId.THE_CONNECTION_TO_THE_CONQUEST_WORLD_WAS_MADE);
		}
		else // Already connected.
		{
			player.sendMessage("Conquest Connection is already active.");
		}
	}
}
