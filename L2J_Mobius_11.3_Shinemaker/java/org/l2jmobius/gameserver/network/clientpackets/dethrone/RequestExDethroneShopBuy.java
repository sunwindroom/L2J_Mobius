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

import java.util.Collection;

import org.l2jmobius.gameserver.data.xml.DethroneShopData;
import org.l2jmobius.gameserver.enums.ExBrProductReplyType;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.PrimeShopRequest;
import org.l2jmobius.gameserver.model.holders.DethroneShopHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.dethrone.ExDethronePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.dethrone.ExDethroneShopBuy;
import org.l2jmobius.gameserver.network.serverpackets.primeshop.ExBRBuyProduct;

/**
 * @author Liamxroy
 */
public class RequestExDethroneShopBuy extends ClientPacket
{
	private int _productId;
	private int _amount;
	private DethroneShopHolder _product;
	
	@Override
	protected void readImpl()
	{
		_productId = readInt();
		_amount = readInt();
		_product = DethroneShopData.getInstance().getProduct(_productId);
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final long personalPoints = player.getVariables().getLong(PlayerVariables.CONQUEST_PERSONAL_POINTS, 0);
		
		if (_product == null)
		{
			return;
		}
		
		if ((_amount < 1) || (_amount > 10000))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVENTORY_OVERFLOW));
			player.sendPacket(new ExDethroneShopBuy(false));
			return;
		}
		
		if (!player.isInventoryUnder80(false))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVENTORY_OVERFLOW));
			player.sendPacket(new ExDethroneShopBuy(false));
			return;
		}
		
		if (player.hasItemRequest() || player.hasRequest(PrimeShopRequest.class))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER_STATE));
			player.sendPacket(new ExDethroneShopBuy(false));
			return;
		}
		
		// Check existing items.
		for (int i = 0; i < _product.getIngredientIds().length; i++)
		{
			if (_product.getIngredientIds()[i] == 0)
			{
				continue;
			}
			if (_product.getIngredientIds()[i] == Inventory.ADENA_ID)
			{
				if (player.getAdena() < (_product.getIngredientQuantities()[i] * _amount))
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.sendPacket(new ExDethroneShopBuy(false));
					return;
				}
			}
			else if (_product.getIngredientIds()[i] == Inventory.CONQUEST_PERSONAL_POINTS)
			{
				if (personalPoints < (_product.getIngredientQuantities()[i] * _amount))
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.sendPacket(new ExDethroneShopBuy(false));
					return;
				}
			}
			
			else if (player.getInventory().getInventoryItemCount(_product.getIngredientIds()[i], _product.getIngredientEnchants()[i] == 0 ? -1 : _product.getIngredientEnchants()[i], true) < (_product.getIngredientQuantities()[i] * _amount))
			{
				player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
				player.sendPacket(new ExDethroneShopBuy(false));
				return;
			}
		}
		
		// Remove items.
		for (int i = 0; i < _product.getIngredientIds().length; i++)
		{
			if (_product.getIngredientIds()[i] == 0)
			{
				continue;
			}
			if (_product.getIngredientIds()[i] == Inventory.ADENA_ID)
			{
				player.reduceAdena("DethroneShop", _product.getIngredientQuantities()[i] * _amount, player, true);
			}
			else if (_product.getIngredientIds()[i] == Inventory.CONQUEST_PERSONAL_POINTS)
			{
				player.getVariables().set(PlayerVariables.CONQUEST_PERSONAL_POINTS, (personalPoints - (_product.getIngredientQuantities()[i] * _amount)));
				// Message
				SystemMessage sm = new SystemMessage(SystemMessageId.PERSONAL_CONQUEST_POINTS_S1);
				sm.addLong(_product.getIngredientQuantities()[i] * _amount);
				player.sendPacket(sm);
			}
			else
			{
				if (_product.getIngredientEnchants()[i] > 0)
				{
					int count = 0;
					final Collection<Item> items = player.getInventory().getAllItemsByItemId(_product.getIngredientIds()[i], _product.getIngredientEnchants()[i]);
					for (Item item : items)
					{
						if (count == _amount)
						{
							break;
						}
						count++;
						player.destroyItem("DethroneShop", item, player, true);
					}
				}
				else
				{
					player.destroyItemByItemId("DethroneShop", _product.getIngredientIds()[i], _product.getIngredientQuantities()[i] * _amount, player, true);
				}
			}
		}
		
		// Reward.
		if (_product.getProductionId() > 0)
		{
			player.addItem("DethroneShop", _product.getProductionId(), _product.getCount() * _amount, player, true);
			
			if (_product.getProductionId2() > 0)
			{
				player.addItem("DethroneShop", _product.getProductionId2(), _product.getCount2() * _amount, player, true);
			}
			else if (_product.getProductionId4() > 0)
			{
				player.addItem("DethroneShop", _product.getProductionId3(), _product.getCount3() * _amount, player, true);
			}
			else if (_product.getProductionId5() > 0)
			{
				player.addItem("DethroneShop", _product.getProductionId4(), _product.getCount4() * _amount, player, true);
			}
			else if (_product.getProductionId5() > 0)
			{
				player.addItem("DethroneShop", _product.getProductionId5(), _product.getCount5() * _amount, player, true);
			}
		}
		
		player.sendPacket(new ExDethroneShopBuy(true));
		player.sendPacket(new ExDethronePointInfo(personalPoints));
		player.sendItemList();
	}
}
