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
package ai.areas.Conquest.Flowers;

import org.l2jmobius.gameserver.instancemanager.GlobalVariablesManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.impl.conquest.OnConquestFlowerCollect;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

import ai.AbstractNpcAI;

/**
 * Conquest Flowers AI.<br>
 * Fire Flowers grow only in the central area of Fire Source, while Life Flowers and Power Flowers can be found all over the Fire Area.<br>
 * To collect the Flowers, you will need 1000 HP and 500,000 SP.<br>
 * When you collect the Flowers you might receive the following items with a certain chance: Seed of Fire, Ghost Soul, personal Conquest points, server Conquest points, Fire Source points, Sacred Fire Summon Scroll and Eigis Armor Fragment.<br>
 * When you collect Fire Flowers, you may also receive Divine Flower that is needed to enhance the <Flame Spark> Primordial Fire Source ability.
 * @author CostyKiller
 */
public class Flowers extends AbstractNpcAI
{
	// NPCs
	private static final int FIRE_FLOWER = 34655;
	private static final int LIFE_FLOWER = 34656;
	private static final int POWER_FLOWER = 34657;
	
	// Gather flower requirements
	private static final int REQUIRED_HP = 1000;
	private static final int REQUIRED_SP = 500000;
	
	// Rewards
	private static final int PERSONAL_POINTS_AMOUNT = 267;
	private static final int SERVER_POINTS_AMOUNT = 500;
	private static final int SEED_OF_FIRE = 82616;
	private static final int SEALED_GHOST_SOUL_ORB = 82610;
	private static final int SEALED_FIRE_SOURCE = 82658;
	private static final int SACRED_FIRE_SUMMON_SCROLL = 82614;
	private static final int EIGIS_ARMOR_FRAGMENT = 82083;
	private static final int DIVINE_FIRE = 82615;
	private static final int DIVINE_FIRE_CHANCE = 15; // 15%
	
	private Flowers()
	{
		addStartNpc(FIRE_FLOWER, LIFE_FLOWER, POWER_FLOWER);
		addFirstTalkId(FIRE_FLOWER, LIFE_FLOWER, POWER_FLOWER);
		addTalkId(FIRE_FLOWER, LIFE_FLOWER, POWER_FLOWER);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "34655.html":
			case "34656.html":
			case "34657.html":
			case "34655-01.html":
			case "34656-01.html":
			case "34657-01.html":
			{
				htmltext = event;
				break;
			}
			case "collectFlower":
			{
				if ((player.getCurrentHp() < REQUIRED_HP) && (player.getSp() < REQUIRED_SP))
				{
					player.sendMessage("You can't collect this flower, the requirements are not met.");
				}
				else
				{
					player.setCurrentHp(player.getCurrentHp() - REQUIRED_HP);
					player.setSp(player.getSp() - REQUIRED_SP);
					// Messages
					SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_SP_HAS_DECREASED_BY_S1);
					sm.addLong(REQUIRED_SP);
					SystemMessage sm2 = new SystemMessage(SystemMessageId.C1_HAS_DRAINED_YOU_OF_S2_HP);
					sm2.addString(npc.getName());
					sm2.addLong(REQUIRED_HP);
					player.sendPacket(sm);
					player.sendPacket(sm2);
					npc.onDecay();
					htmltext = npc.getId() + "-01.html";
					// Notify to scripts.
					if (EventDispatcher.getInstance().hasListener(EventType.ON_CONQUEST_FLOWER_COLLECT))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnConquestFlowerCollect(player, npc.getId()));
					}
					
					if (player.isInventoryUnder90(false))
					{
						final int random = getRandom(100);
						// Reward only from Fire Flower
						if ((random < DIVINE_FIRE_CHANCE) && (npc.getId() == FIRE_FLOWER))
						{
							giveItems(player, DIVINE_FIRE, 1);
						}
						// Rewards from all flowers
						if (random < 5) // 5% chance
						{
							giveItems(player, SACRED_FIRE_SUMMON_SCROLL, 1);
						}
						else if (random < 10) // 10% chance
						{
							giveItems(player, SEALED_GHOST_SOUL_ORB, 1);
						}
						else if (random < 15) // 15% chance
						{
							giveItems(player, SEALED_FIRE_SOURCE, 1);
							// player.sendPacket(SystemMessageId.YOU_HAVE_RECEIVED_FIRE_SOURCE_POINTS);
						}
						else if (random < 20) // 20% chance
						{
							giveItems(player, SEED_OF_FIRE, 1);
						}
						else if (random < 25) // 25% chance
						{
							giveItems(player, EIGIS_ARMOR_FRAGMENT, 1);
						}
						else if (random < 30) // 30% chance
						{
							GlobalVariablesManager.getInstance().set("CONQUEST_SERVER_POINTS", GlobalVariablesManager.getInstance().getLong("CONQUEST_SERVER_POINTS", 0) + SERVER_POINTS_AMOUNT);
							player.sendPacket(SystemMessageId.YOU_HAVE_RECEIVED_SERVER_CONQUEST_POINTS);
						}
						else
						{
							player.getVariables().set(PlayerVariables.CONQUEST_PERSONAL_POINTS, player.getVariables().getLong(PlayerVariables.CONQUEST_PERSONAL_POINTS, 0) + PERSONAL_POINTS_AMOUNT);
							player.sendPacket(SystemMessageId.YOU_HAVE_RECEIVED_PERSONAL_CONQUEST_POINTS);
						}
					}
					else
					{
						player.sendPacket(SystemMessageId.YOUR_INVENTORY_S_WEIGHT_SLOT_LIMIT_HAS_BEEN_EXCEEDED_SO_YOU_CAN_T_RECEIVE_THE_REWARD_PLEASE_FREE_UP_SOME_SPACE_AND_TRY_AGAIN);
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	public static void main(String[] args)
	{
		new Flowers();
	}
}
