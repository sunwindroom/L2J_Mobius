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
package org.l2jmobius.gameserver.network.serverpackets.dethroneability;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExAbilityOfFireInit extends ServerPacket
{
	private final Player _player;
	private final int _type;
	private int _success = 0;
	
	public ExAbilityOfFireInit(Player player, int type)
	{
		_player = player;
		_type = type;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_ENHANCED_ABILITY_OF_FIRE_INIT.writeId(this, buffer);
		buffer.writeByte(_type);
		
		if (_type == 0) // Fire Source
		{
			// No reset button here
		}
		else if (_type == 1) // Life Source
		{
			if (checkItems(_player))
			{
				for (ItemHolder requiredItems : Config.CONQUEST_ABILITY_UPGRADES_RESET_REQUIRED_ITEMS)
				{
					_player.destroyItemByItemId(getClass().getSimpleName(), requiredItems.getId(), requiredItems.getCount(), _player, true);
					_success = 1;
				}
				buffer.writeByte(_success); // int Result
				if (_success == 1)
				{
					buffer.writeInt(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_UPGRADES, 500)); // int ExpUpCount
					_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_UPGRADES, 500);
					_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_RESET, 0);
					_player.getVariables().storeMe();
				}
				else
				{
					buffer.writeInt(0);
				}
			}
		}
		else if (_type == 2) // Flame Spark
		{
			if (checkItems(_player))
			{
				for (ItemHolder requiredItems : Config.CONQUEST_ABILITY_UPGRADES_RESET_REQUIRED_ITEMS)
				{
					_player.destroyItemByItemId(getClass().getSimpleName(), requiredItems.getId(), requiredItems.getCount(), _player, true);
					_success = 1;
				}
				buffer.writeByte(_success); // int Result
				if (_success == 1)
				{
					buffer.writeInt(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_UPGRADES, 60)); // int ExpUpCount
					_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_UPGRADES, 60);
					_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_RESET, 0);
					_player.getVariables().storeMe();
				}
				else
				{
					buffer.writeInt(0);
				}
			}
		}
		else if (_type == 3) // Fire Totem
		{
			if (checkItems(_player))
			{
				for (ItemHolder requiredItems : Config.CONQUEST_ABILITY_UPGRADES_RESET_REQUIRED_ITEMS)
				{
					_player.destroyItemByItemId(getClass().getSimpleName(), requiredItems.getId(), requiredItems.getCount(), _player, true);
					_success = 1;
				}
				buffer.writeByte(_success); // int Result
				if (_success == 1)
				{
					buffer.writeInt(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_UPGRADES, 100)); // int ExpUpCount
					_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_UPGRADES, 100);
					_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_RESET, 0);
					_player.getVariables().storeMe();
				}
				else
				{
					buffer.writeInt(0);
				}
			}
		}
		else if (_type == 4) // Battle Soul
		{
			if (checkItems(_player))
			{
				for (ItemHolder requiredItems : Config.CONQUEST_ABILITY_UPGRADES_RESET_REQUIRED_ITEMS)
				{
					_player.destroyItemByItemId(getClass().getSimpleName(), requiredItems.getId(), requiredItems.getCount(), _player, true);
					_success = 1;
				}
				buffer.writeByte(_success); // int Result
				if (_success == 1)
				{
					buffer.writeInt(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_UPGRADES, 100)); // int ExpUpCount
					_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_UPGRADES, 100);
					_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_RESET, 0);
					_player.getVariables().storeMe();
				}
				else
				{
					buffer.writeInt(0);
				}
			}
		}
	}
	
	public boolean checkItems(Player player)
	{
		for (ItemHolder requiredItems : Config.CONQUEST_ABILITY_UPGRADES_RESET_REQUIRED_ITEMS)
		{
			if (player.getInventory().getItemByItemId(requiredItems.getId()).getCount() >= requiredItems.getCount())
			{
				return true;
			}
		}
		return false;
	}
}
