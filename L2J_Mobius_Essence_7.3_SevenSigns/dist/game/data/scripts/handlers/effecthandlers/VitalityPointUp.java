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
package handlers.effecthandlers;

import java.util.LinkedList;
import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.enums.UserInfoType;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;

/**
 * Vitality Point Up effect implementation.
 * @author Adry_85
 */
public class VitalityPointUp extends AbstractEffect
{
	private final int _value;
	
	public VitalityPointUp(StatSet params)
	{
		_value = params.getInt("value", 0);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.VITALITY_POINT_UP;
	}
	
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effected != null) && effected.isPlayer();
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		
		player.updateVitalityPoints(_value, false, false);
		
		final UserInfo ui = new UserInfo(player);
		ui.addComponentType(UserInfoType.VITA_FAME);
		player.sendPacket(ui);
		
		// Send item list to update vitality items with red icons in inventory.
		ThreadPool.schedule(() ->
		{
			final List<Item> items = new LinkedList<>();
			ITEMS: for (Item i : player.getInventory().getItems())
			{
				if (i.getTemplate().hasSkills())
				{
					for (ItemSkillHolder s : i.getTemplate().getAllSkills())
					{
						if (s.getSkill().hasEffectType(EffectType.VITALITY_POINT_UP))
						{
							items.add(i);
							continue ITEMS;
						}
					}
				}
			}
			
			if (!items.isEmpty())
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addItems(items);
				player.sendInventoryUpdate(iu);
			}
		}, 1000);
	}
}
