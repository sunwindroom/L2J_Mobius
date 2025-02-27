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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.enums.PrivateStoreType;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.RecipeBookItemList;

public class RequestRecipeBookOpen extends ClientPacket
{
	private boolean _isDwarvenCraft;
	
	@Override
	protected void readImpl()
	{
		_isDwarvenCraft = (readInt() == 0);
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isCastingNow())
		{
			player.sendPacket(SystemMessageId.YOUR_RECIPE_BOOK_MAY_NOT_BE_ACCESSED_WHILE_USING_A_SKILL);
			return;
		}
		
		if (player.getPrivateStoreType() == PrivateStoreType.MANUFACTURE)
		{
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_ALTER_YOUR_RECIPE_BOOK_WHILE_ENGAGED_IN_MANUFACTURING);
			return;
		}
		
		if (player.isProcessingTransaction())
		{
			player.sendPacket(SystemMessageId.ITEM_CREATION_IS_NOT_POSSIBLE_WHILE_ENGAGED_IN_A_TRADE);
			return;
		}
		
		player.sendPacket(new RecipeBookItemList(player, _isDwarvenCraft));
	}
}
