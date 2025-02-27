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
package org.l2jmobius.gameserver.network.clientpackets.homunculus;

import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.HomunculusSlotData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.homunculus.HomunculusSlotTemplate;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExActivateHomunculusResult;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExHomunculusPointInfo;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExShowHomunculusList;

/**
 * @author Manax
 */
public class RequestExHomunculusActivateSlot extends ClientPacket
{
	private int _slot;
	
	@Override
	protected void readImpl()
	{
		_slot = readInt();
		// _activate = readByte() == 1; // enabled?
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final int size = player.getHomunculusList().size();
		final HomunculusSlotTemplate template = HomunculusSlotData.getInstance().getTemplate(_slot);
		if ((size != 0) && ((player.getHomunculusList().get(_slot) != null) || (_slot == player.getAvailableHomunculusSlotCount())))
		{
			PacketLogger.info(getClass().getSimpleName() + " player " + player.getName() + " " + player.getObjectId() + " trying unlock already unlocked slot!");
			player.sendPacket(new ExActivateHomunculusResult(false));
			return;
		}
		if (!template.getSlotEnabled())
		{
			Logger.getLogger(getClass().getSimpleName() + " player " + player.getName() + " " + player.getObjectId() + " trying unlock disabled slot!");
			player.sendPacket(new ExActivateHomunculusResult(false));
			return;
		}
		
		final List<ItemHolder> fee = template.getPrice();
		for (ItemHolder feeHolder : fee)
		{
			if ((player.getInventory().getItemByItemId(feeHolder.getId()) == null) || ((player.getInventory().getItemByItemId(feeHolder.getId()) != null) && (player.getInventory().getItemByItemId(feeHolder.getId()).getCount() < feeHolder.getCount())))
			{
				player.sendPacket(new ExActivateHomunculusResult(false));
				return;
			}
		}
		for (ItemHolder feeHolder : fee)
		{
			if (!player.destroyItemByItemId("Homunclus slot unlock", feeHolder.getId(), feeHolder.getCount(), player, true))
			{
				Logger.getLogger(getClass().getSimpleName() + " player " + player.getName() + " " + player.getObjectId() + " trying unlock slot without items!");
				player.sendPacket(new ExActivateHomunculusResult(false));
				return;
			}
		}
		
		player.broadcastUserInfo();
		player.getVariables().set(PlayerVariables.HOMUNCULUS_OPENED_SLOT_COUNT, _slot);
		player.sendPacket(new ExHomunculusPointInfo(player));
		player.sendPacket(new ExShowHomunculusList(player));
		player.sendPacket(new ExActivateHomunculusResult(true));
	}
}
