/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.network.clientpackets.limitshop;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.LimitShopClanData;
import org.l2jmobius.gameserver.data.xml.LimitShopCraftData;
import org.l2jmobius.gameserver.data.xml.LimitShopData;
import org.l2jmobius.gameserver.enums.ExBrProductReplyType;
import org.l2jmobius.gameserver.enums.SpecialItemType;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.PrimeShopRequest;
import org.l2jmobius.gameserver.model.holders.LimitShopProductHolder;
import org.l2jmobius.gameserver.model.holders.LimitShopRandomCraftReward;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ExItemAnnounce;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.limitshop.ExPurchaseLimitShopItemResult;
import org.l2jmobius.gameserver.network.serverpackets.primeshop.ExBRBuyProduct;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Mobius
 */
public class RequestPurchaseLimitShopItemBuy extends ClientPacket
{
	private int _shopIndex;
	private int _productId;
	private int _amount;
	private LimitShopProductHolder _product;
	
	@Override
	protected void readImpl()
	{
		_shopIndex = readByte(); // 3 Lcoin Store, 4 Special Craft, 100 Clan Shop
		_productId = readInt();
		_amount = readInt();
		
		switch (_shopIndex)
		{
			case 3: // Normal Lcoin Shop
			{
				_product = LimitShopData.getInstance().getProduct(_productId);
				break;
			}
			case 4: // Lcoin Special Craft
			{
				_product = LimitShopCraftData.getInstance().getProduct(_productId);
				break;
			}
			case 100: // Clan Shop
			{
				_product = LimitShopClanData.getInstance().getProduct(_productId);
				break;
			}
			default:
			{
				_product = null;
			}
		}
		
		readInt(); // SuccessionItemSID
		readInt(); // MaterialItemSID
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_product == null)
		{
			return;
		}
		
		if ((_amount < 1) || (_amount > 10000))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVENTORY_OVERFLOW));
			player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, 0, Collections.emptyList()));
			return;
		}
		
		if (!player.isInventoryUnder80(false))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVENTORY_OVERFLOW));
			player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, 0, Collections.emptyList()));
			return;
		}
		
		if ((player.getLevel() < _product.getMinLevel()) || (player.getLevel() > _product.getMaxLevel()))
		{
			player.sendPacket(SystemMessageId.YOUR_LEVEL_CANNOT_PURCHASE_THIS_ITEM);
			player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, 0, Collections.emptyList()));
			return;
		}
		
		if (player.hasItemRequest() || player.hasRequest(PrimeShopRequest.class))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER_STATE));
			player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, 0, Collections.emptyList()));
			return;
		}
		
		// Add request.
		player.addRequest(new PrimeShopRequest(player));
		
		// Check limits.
		if (_product.getAccountDailyLimit() > 0) // Sale period.
		{
			final long amount = _product.getAccountDailyLimit() * _amount;
			if (amount < 1)
			{
				player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
				player.removeRequest(PrimeShopRequest.class);
				player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, 0, Collections.emptyList()));
				return;
			}
			
			final int currentPurchaseCount = player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + _product.getProductionId(), 0);
			if ((currentPurchaseCount + _amount) > _product.getAccountDailyLimit())
			{
				player.sendMessage("You have reached your daily limit."); // TODO: Retail system message?
				player.removeRequest(PrimeShopRequest.class);
				player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, 0, Collections.emptyList()));
				return;
			}
		}
		else if (_product.getAccountWeeklyLimit() > 0)
		{
			final long amount = _product.getAccountWeeklyLimit() * _amount;
			if (amount < 1)
			{
				player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
				player.removeRequest(PrimeShopRequest.class);
				player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, 0, Collections.emptyList()));
				return;
			}
			
			final int currentPurchaseCount = player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + _product.getProductionId(), 0);
			if ((currentPurchaseCount + _amount) > _product.getAccountWeeklyLimit())
			{
				player.sendMessage("You have reached your weekly limit."); // TODO: Retail system message?
				player.removeRequest(PrimeShopRequest.class);
				player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, 0, Collections.emptyList()));
				return;
			}
		}
		else if (_product.getAccountMonthlyLimit() > 0)
		{
			final long amount = _product.getAccountMonthlyLimit() * _amount;
			if (amount < 1)
			{
				player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
				player.removeRequest(PrimeShopRequest.class);
				player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, 0, Collections.emptyList()));
				return;
			}
			
			final int currentPurchaseCount = player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + _product.getProductionId(), 0);
			if ((currentPurchaseCount + _amount) > _product.getAccountMonthlyLimit())
			{
				player.sendMessage("You have reached your monthly limit."); // TODO: Retail system message?
				player.removeRequest(PrimeShopRequest.class);
				player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, 0, Collections.emptyList()));
				return;
			}
		}
		else if (_product.getAccountBuyLimit() > 0) // Count limit.
		{
			final long amount = _product.getAccountBuyLimit() * _amount;
			if (amount < 1)
			{
				player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
				player.removeRequest(PrimeShopRequest.class);
				player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, 0, Collections.emptyList()));
				return;
			}
			
			final int currentPurchaseCount = player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_COUNT + _product.getProductionId(), 0);
			if ((currentPurchaseCount + _amount) > _product.getAccountBuyLimit())
			{
				player.sendMessage("You cannot buy any more of this item."); // TODO: Retail system message?
				player.removeRequest(PrimeShopRequest.class);
				player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, 0, Collections.emptyList()));
				return;
			}
		}
		
		// Check existing items.
		final int remainingInfo = Math.max(0, Math.max(_product.getAccountBuyLimit(), Math.max(_product.getAccountDailyLimit(), _product.getAccountMonthlyLimit())));
		for (int i = 0; i < _product.getIngredientIds().length; i++)
		{
			if (_product.getIngredientIds()[i] == 0)
			{
				continue;
			}
			
			if (_product.getIngredientIds()[i] == Inventory.ADENA_ID)
			{
				final long amount = _product.getIngredientQuantities()[i] * _amount;
				if (amount < 1)
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, remainingInfo, Collections.emptyList()));
					return;
				}
				
				if (player.getAdena() < amount)
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, remainingInfo, Collections.emptyList()));
					return;
				}
			}
			else if (_product.getIngredientIds()[i] == SpecialItemType.HONOR_COINS.getClientId())
			{
				final long amount = _product.getIngredientQuantities()[i] * _amount;
				if (amount < 1)
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, remainingInfo, Collections.emptyList()));
					return;
				}
				
				if (player.getHonorCoins() < amount)
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, remainingInfo, Collections.emptyList()));
					return;
				}
			}
			else if (_product.getIngredientIds()[i] == SpecialItemType.PC_CAFE_POINTS.getClientId())
			{
				final long amount = _product.getIngredientQuantities()[i] * _amount;
				if (amount < 1)
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, remainingInfo, Collections.emptyList()));
					return;
				}
				
				if (player.getPcCafePoints() < amount)
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, remainingInfo, Collections.emptyList()));
					return;
				}
			}
			else
			{
				final long amount = _product.getIngredientQuantities()[i] * _amount;
				if (amount < 1)
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, remainingInfo, Collections.emptyList()));
					return;
				}
				
				if (player.getInventory().getInventoryItemCount(_product.getIngredientIds()[i], _product.getIngredientEnchants()[i] == 0 ? -1 : _product.getIngredientEnchants()[i], true) < amount)
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
					player.removeRequest(PrimeShopRequest.class);
					player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, remainingInfo, Collections.emptyList()));
					return;
				}
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
				player.reduceAdena("LCoinShop", _product.getIngredientQuantities()[i] * _amount, player, true);
			}
			else if (_product.getIngredientIds()[i] == SpecialItemType.HONOR_COINS.getClientId())
			{
				player.setHonorCoins(player.getHonorCoins() - (_product.getIngredientQuantities()[i] * _amount));
			}
			else if (_product.getIngredientIds()[i] == SpecialItemType.PC_CAFE_POINTS.getClientId())
			{
				final int newPoints = (int) (player.getPcCafePoints() - (_product.getIngredientQuantities()[i] * _amount));
				player.setPcCafePoints(newPoints);
				player.sendPacket(new ExPCCafePointInfo(player.getPcCafePoints(), (int) (-(_product.getIngredientQuantities()[i] * _amount)), 1));
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
						player.destroyItem("LCoinShop", item, player, true);
					}
				}
				else
				{
					final long amount = _product.getIngredientQuantities()[i] * _amount;
					if (amount < 1)
					{
						player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
						player.removeRequest(PrimeShopRequest.class);
						player.sendPacket(new ExPurchaseLimitShopItemResult(false, _shopIndex, _productId, remainingInfo, Collections.emptyList()));
						return;
					}
					
					player.destroyItemByItemId("LCoinShop", _product.getIngredientIds()[i], amount, player, true);
				}
			}
			if (Config.VIP_SYSTEM_L_SHOP_AFFECT)
			{
				player.updateVipPoints(_amount);
			}
		}
		
		// Reward.
		final Map<Integer, LimitShopRandomCraftReward> rewards = new HashMap<>();
		if (_product.getProductionId2() > 0)
		{
			for (int i = 0; i < _amount; i++)
			{
				if (Rnd.get(100) < _product.getChance())
				{
					rewards.computeIfAbsent(0, k -> new LimitShopRandomCraftReward(_product.getProductionId(), 0, 0)).getCount().addAndGet((int) _product.getCount());
					final Item item = player.addItem("LCoinShop", _product.getProductionId(), _product.getCount(), _product.getEnchant(), player, true);
					if (_product.isAnnounce())
					{
						Broadcast.toAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.SPECIAL_CREATION));
					}
				}
				else if ((Rnd.get(100) < _product.getChance2()) || (_product.getProductionId3() == 0))
				{
					rewards.computeIfAbsent(1, k -> new LimitShopRandomCraftReward(_product.getProductionId2(), 0, 1)).getCount().addAndGet((int) _product.getCount2());
					final Item item = player.addItem("LCoinShop", _product.getProductionId2(), _product.getCount2(), player, true);
					if (_product.isAnnounce2())
					{
						Broadcast.toAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.SPECIAL_CREATION));
					}
				}
				else if ((Rnd.get(100) < _product.getChance3()) || (_product.getProductionId4() == 0))
				{
					rewards.computeIfAbsent(2, k -> new LimitShopRandomCraftReward(_product.getProductionId3(), 0, 2)).getCount().addAndGet((int) _product.getCount3());
					final Item item = player.addItem("LCoinShop", _product.getProductionId3(), _product.getCount3(), player, true);
					if (_product.isAnnounce3())
					{
						Broadcast.toAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.SPECIAL_CREATION));
					}
				}
				else if ((Rnd.get(100) < _product.getChance4()) || (_product.getProductionId5() == 0))
				{
					rewards.computeIfAbsent(3, k -> new LimitShopRandomCraftReward(_product.getProductionId4(), 0, 3)).getCount().addAndGet((int) _product.getCount4());
					final Item item = player.addItem("LCoinShop", _product.getProductionId4(), _product.getCount4(), player, true);
					if (_product.isAnnounce4())
					{
						Broadcast.toAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.SPECIAL_CREATION));
					}
				}
				else if (_product.getProductionId5() > 0)
				{
					rewards.computeIfAbsent(4, k -> new LimitShopRandomCraftReward(_product.getProductionId5(), 0, 4)).getCount().addAndGet((int) _product.getCount5());
					final Item item = player.addItem("LCoinShop", _product.getProductionId5(), _product.getCount5(), player, true);
					if (_product.isAnnounce5())
					{
						Broadcast.toAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.SPECIAL_CREATION));
					}
				}
			}
		}
		else if (Rnd.get(100) < _product.getChance())
		{
			rewards.put(0, new LimitShopRandomCraftReward(_product.getProductionId(), (int) (_product.getCount() * _amount), 0));
			final Item item = player.addItem("LCoinShop", _product.getProductionId(), _product.getCount() * _amount, _product.getEnchant(), player, true);
			if (_product.isAnnounce())
			{
				Broadcast.toAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.SPECIAL_CREATION));
			}
		}
		
		// Update account variables.
		if (_product.getAccountDailyLimit() > 0)
		{
			player.getAccountVariables().set(AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + _product.getProductionId(), player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + _product.getProductionId(), 0) + _amount);
		}
		else if (_product.getAccountWeeklyLimit() > 0)
		{
			player.getAccountVariables().set(AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + _product.getProductionId(), player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + _product.getProductionId(), 0) + _amount);
		}
		else if (_product.getAccountMonthlyLimit() > 0)
		{
			player.getAccountVariables().set(AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + _product.getProductionId(), player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + _product.getProductionId(), 0) + _amount);
		}
		else if (_product.getAccountBuyLimit() > 0)
		{
			player.getAccountVariables().set(AccountVariables.LCOIN_SHOP_PRODUCT_COUNT + _product.getProductionId(), player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_COUNT + _product.getProductionId(), 0) + _amount);
		}
		
		player.sendPacket(new ExPurchaseLimitShopItemResult(true, _shopIndex, _productId, Math.max(remainingInfo - _amount, 0), rewards.values()));
		player.sendItemList();
		
		// Remove request.
		ThreadPool.schedule(() -> player.removeRequest(PrimeShopRequest.class), 1000);
	}
}
