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
package org.l2jmobius.gameserver.network.clientpackets.virtualItem;

import java.util.LinkedList;
import java.util.List;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.VirtualItemHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.virtualItem.ExVirtualItemSystem;

public class RequestExVirtualItemSystem extends ClientPacket
{
	private int _type;
	private int _selectIndexMain;
	private int _selectIndexSub;
	private int _selectSlot;
	private int _nIndexMain;
	private int _nIndexSub;
	private int _nSlot;
	private int _nCostVISPoint;
	private int _nItemClass;
	private int _nEnchant;
	private final List<VirtualItemHolder> _updateVisItemInfo = new LinkedList<>();
	
	@Override
	protected void readImpl()
	{
		_type = readByte();
		_selectIndexMain = readInt();
		_selectIndexSub = readInt();
		_selectSlot = readInt();
		readShort(); // Always 26
		_nIndexMain = readInt();
		_nIndexSub = readInt();
		_nSlot = readInt();
		_nCostVISPoint = readInt();
		_nItemClass = readInt();
		_nEnchant = readInt();
		final VirtualItemHolder virtualItem = new VirtualItemHolder(_nIndexMain, _nIndexSub, _nSlot, _nCostVISPoint, _nItemClass, _nEnchant);
		_updateVisItemInfo.add(virtualItem);
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		// Debug
		player.sendMessage("------------------------------------------------");
		player.sendMessage("cType:" + _type);
		player.sendMessage("nSelectIndexMain:" + _selectIndexMain);
		player.sendMessage("nSelectIndexSub:" + _selectIndexSub);
		player.sendMessage("nIndexMain:" + _nIndexMain);
		player.sendMessage("nIndexSub:" + _nIndexSub);
		player.sendMessage("nSlot:" + _nSlot);
		player.sendMessage("Item/Skill Cost:" + _nCostVISPoint);
		player.sendMessage("Item/Skill Id:" + _nItemClass);
		player.sendMessage("Item/Skill Enchant:" + _nEnchant);
		
		if (player.isInStoreMode() || player.isCrafting() || player.isProcessingRequest() || player.isProcessingTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_SYSTEM_DURING_TRADING_PRIVATE_STORE_AND_WORKSHOP_SETUP);
			return;
		}
		
		if (player.isFishing())
		{
			// You can't mount, dismount, break and drop items while fishing
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_2);
			return;
		}
		
		if (player.isFlying())
		{
			return;
		}
		player.sendPacket(new ExVirtualItemSystem(player, _type, _selectIndexMain, _selectIndexSub, _selectSlot, _updateVisItemInfo));
	}
}

/*
 * struct _C_EX_VIRTUALITEM_SYSTEM { var int cType; var int nSelectIndexMain; var int nSelectIndexSub; var int nSelectSlot; var _VirtualItemInfo updateVisItemInfo; }; struct _VirtualItemInfo { var int nIndexMain; var int nIndexSub; var int nSlot; var int nCostVISPoint; var int nItemClass; var int
 * nEnchant; };
 */