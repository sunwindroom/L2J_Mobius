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

import java.util.Collections;
import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExAbilityOfFireLevelUp extends ServerPacket
{
	private static final int PRIMORDIAL_FIRE_SOURCE_SKILL_ID = 34495;
	private static final int FIRE_SOURCE_SKILL_ID = 34498;
	private static final int LIFE_SOURCE_SKILL_ID = 34499;
	private static final int BATTLE_SOUL_SKILL_ID = 34500;
	private static final int FIRE_TOTEM_SKILL_ID = 34501;
	private static final int FLAME_SPARK_SKILL_ID = 34502;
	
	private final Player _player;
	private final int _type;
	private int _success = 0;
	private int _nextAbilityLevel = 0;
	
	public ExAbilityOfFireLevelUp(Player player, int type)
	{
		_player = player;
		_type = type;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_ENHANCED_ABILITY_OF_FIRE_LEVEL_UP.writeId(this, buffer);
		
		buffer.writeByte(_type);
		if (_type == 0) // Fire Source
		{
			_nextAbilityLevel = _player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_LEVEL, 0) + 1;
			if (checkItems(_player))
			{
				for (ItemHolder requiredItems : getNextAbilityLevelRequirements())
				{
					_player.destroyItemByItemId(getClass().getSimpleName(), requiredItems.getId(), requiredItems.getCount(), _player, true);
					_success = 1;
				}
				buffer.writeByte(_success); // int Result
				buffer.writeInt(_nextAbilityLevel); // int Level
				_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_EXP, 0);
				_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_LEVEL, _nextAbilityLevel);
				// Add ability skill
				_player.addSkill(SkillData.getInstance().getSkill(FIRE_SOURCE_SKILL_ID, _nextAbilityLevel), true);
				// Check ability set levels to add set skill
				if (checkAbilitySetLevels() != 0)
				{
					_player.addSkill(SkillData.getInstance().getSkill(PRIMORDIAL_FIRE_SOURCE_SKILL_ID, checkAbilitySetLevels()), true);
				}
			}
		}
		else if (_type == 1) // Life Source
		{
			_nextAbilityLevel = _player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_LEVEL, 0) + 1;
			if (checkItems(_player))
			{
				for (ItemHolder requiredItems : getNextAbilityLevelRequirements())
				{
					_player.destroyItemByItemId(getClass().getSimpleName(), requiredItems.getId(), requiredItems.getCount(), _player, true);
					_success = 1;
				}
				buffer.writeByte(_success); // int Result
				buffer.writeInt(_nextAbilityLevel); // int Level
				_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_EXP, 0);
				_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_LEVEL, _nextAbilityLevel);
				// Add ability skill
				_player.addSkill(SkillData.getInstance().getSkill(LIFE_SOURCE_SKILL_ID, _nextAbilityLevel), true);
				// Check ability set levels to add set skill
				if (checkAbilitySetLevels() != 0)
				{
					_player.addSkill(SkillData.getInstance().getSkill(PRIMORDIAL_FIRE_SOURCE_SKILL_ID, checkAbilitySetLevels()), true);
				}
			}
		}
		else if (_type == 2) // Flame Spark
		{
			_nextAbilityLevel = _player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_LEVEL, 0) + 1;
			if (checkItems(_player))
			{
				for (ItemHolder requiredItems : getNextAbilityLevelRequirements())
				{
					_player.destroyItemByItemId(getClass().getSimpleName(), requiredItems.getId(), requiredItems.getCount(), _player, true);
					_success = 1;
				}
				buffer.writeByte(_success); // int Result
				buffer.writeInt(_nextAbilityLevel); // int Level
				_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_EXP, 0);
				_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_LEVEL, _nextAbilityLevel);
				// Add ability skill
				_player.addSkill(SkillData.getInstance().getSkill(FLAME_SPARK_SKILL_ID, _nextAbilityLevel), true);
				// Check ability set levels to add set skill
				if (checkAbilitySetLevels() != 0)
				{
					_player.addSkill(SkillData.getInstance().getSkill(PRIMORDIAL_FIRE_SOURCE_SKILL_ID, checkAbilitySetLevels()), true);
				}
			}
		}
		else if (_type == 3) // Fire Totem
		{
			_nextAbilityLevel = _player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_LEVEL, 0) + 1;
			if (checkItems(_player))
			{
				for (ItemHolder requiredItems : getNextAbilityLevelRequirements())
				{
					_player.destroyItemByItemId(getClass().getSimpleName(), requiredItems.getId(), requiredItems.getCount(), _player, true);
					_success = 1;
				}
				buffer.writeByte(_success); // int Result
				buffer.writeInt(_nextAbilityLevel); // int Level
				_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_EXP, 0);
				_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_LEVEL, _nextAbilityLevel);
				// Add ability skill
				_player.addSkill(SkillData.getInstance().getSkill(FIRE_TOTEM_SKILL_ID, _nextAbilityLevel), true);
				// Check ability set levels to add set skill
				if (checkAbilitySetLevels() != 0)
				{
					_player.addSkill(SkillData.getInstance().getSkill(PRIMORDIAL_FIRE_SOURCE_SKILL_ID, checkAbilitySetLevels()), true);
				}
			}
		}
		else if (_type == 4) // Battle Soul
		{
			_nextAbilityLevel = _player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_LEVEL, 0) + 1;
			if (checkItems(_player))
			{
				for (ItemHolder requiredItems : getNextAbilityLevelRequirements())
				{
					_player.destroyItemByItemId(getClass().getSimpleName(), requiredItems.getId(), requiredItems.getCount(), _player, true);
					_success = 1;
				}
				buffer.writeByte(_success); // int Result
				buffer.writeInt(_nextAbilityLevel); // int Level
				_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_EXP, 0);
				_player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_LEVEL, _nextAbilityLevel);
				// Add ability skill
				_player.addSkill(SkillData.getInstance().getSkill(BATTLE_SOUL_SKILL_ID, _nextAbilityLevel), true);
				// Check ability set levels to add set skill
				if (checkAbilitySetLevels() != 0)
				{
					_player.addSkill(SkillData.getInstance().getSkill(PRIMORDIAL_FIRE_SOURCE_SKILL_ID, checkAbilitySetLevels()), true);
				}
			}
		}
	}
	
	public boolean checkItems(Player player)
	{
		for (ItemHolder requiredItems : getNextAbilityLevelRequirements())
		{
			if (player.getInventory().getItemByItemId(requiredItems.getId()).getCount() >= requiredItems.getCount())
			{
				return true;
			}
		}
		return false;
	}
	
	public List<ItemHolder> getNextAbilityLevelRequirements()
	{
		final List<ItemHolder> requiredItems;
		switch (_nextAbilityLevel)
		{
			case 1:
			{
				requiredItems = Config.CONQUEST_ABILITY_UPGRADE_LEVEL_1_REQUIRED_ITEMS;
				break;
			}
			case 2:
			{
				requiredItems = Config.CONQUEST_ABILITY_UPGRADE_LEVEL_2_REQUIRED_ITEMS;
				break;
			}
			case 3:
			{
				requiredItems = Config.CONQUEST_ABILITY_UPGRADE_LEVEL_3_REQUIRED_ITEMS;
				break;
			}
			case 4:
			{
				requiredItems = Config.CONQUEST_ABILITY_UPGRADE_LEVEL_4_REQUIRED_ITEMS;
				break;
			}
			case 5:
			{
				requiredItems = Config.CONQUEST_ABILITY_UPGRADE_LEVEL_5_REQUIRED_ITEMS;
				break;
			}
			case 6:
			{
				requiredItems = Config.CONQUEST_ABILITY_UPGRADE_LEVEL_6_REQUIRED_ITEMS;
				break;
			}
			case 7:
			{
				requiredItems = Config.CONQUEST_ABILITY_UPGRADE_LEVEL_7_REQUIRED_ITEMS;
				break;
			}
			case 8:
			{
				requiredItems = Config.CONQUEST_ABILITY_UPGRADE_LEVEL_8_REQUIRED_ITEMS;
				break;
			}
			case 9:
			{
				requiredItems = Config.CONQUEST_ABILITY_UPGRADE_LEVEL_9_REQUIRED_ITEMS;
				break;
			}
			case 10:
			{
				requiredItems = Config.CONQUEST_ABILITY_UPGRADE_LEVEL_10_REQUIRED_ITEMS;
				break;
			}
			default:
			{
				requiredItems = Collections.emptyList();
				break;
			}
		}
		return requiredItems;
	}
	
	public int checkAbilitySetLevels()
	{
		int setSkillLevel = 0;
		if ((_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_LEVEL, 0) == 10) && //
			(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_LEVEL, 0) == 10) && //
			(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_LEVEL, 0) == 10) && //
			(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_LEVEL, 0) == 10) && //
			(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_LEVEL, 0) == 10))
		{
			setSkillLevel = 3;
		}
		else if ((_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_LEVEL, 0) >= 6) && //
			(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_LEVEL, 0) >= 6) && //
			(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_LEVEL, 0) >= 6) && //
			(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_LEVEL, 0) >= 6) && //
			(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_LEVEL, 0) >= 6))
		{
			setSkillLevel = 2;
		}
		else if ((_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_LEVEL, 0) >= 3) && //
			(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_LEVEL, 0) >= 3) && //
			(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_LEVEL, 0) >= 3) && //
			(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_LEVEL, 0) >= 3) && //
			(_player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_LEVEL, 0) >= 3))
		{
			setSkillLevel = 1;
		}
		return setSkillLevel;
	}
}
