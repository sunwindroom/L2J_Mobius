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
package instances.AshenShadowRevolutionaries;

import java.util.List;

import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.type.ScriptZone;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

import instances.AbstractInstance;

/**
 * @author Mobius, Liamxroy
 * @URL https://l2wiki.com/Ashen_Shadow_Revolutionaries
 * @VIDEO https://www.youtube.com/watch?v=ohkxylKJAtQ
 */
public class AshenShadowRevolutionaries extends AbstractInstance
{
	// NPCs
	private static final int BENUSTA = 34542;
	private static final int TREASURE_CHEST = 34101;
	private static final int[] QUEST_GIVERS =
	{
		34096,
		34097,
		34098,
		34099,
		34100
	};
	// Monsters
	private static final int SPY_DWARF = 23650;
	private static final int SIGNALMAN = 23651;
	private static final int SIGNALMAN_110 = 24811;
	private static final int[] COMMANDERS =
	{
		23653, // Unit Commander 1
		23654, // Unit Commander 2
		23655, // Unit Commander 2
		23656, // Unit Commander 2
		23657, // Unit Commander 3
		23658, // Unit Commander 4
		23659, // Unit Commander 4
		23660, // Unit Commander 5
		23661, // Unit Commander 6
		23662, // Unit Commander 7
		23663, // Unit Commander 8
		23664, // Unit Commander 8
	};
	private static final int[] COMMANDERS_110 =
	{
		24813, // Knight Agar
		24814, // Warrior Ule
		24815, // Warrior Ule
		24816, // Warrior Ule
		24817, // Rogue Fiord
		24818, // Archer Torn
		24819, // Archer Torn
		24820, // Mage Kenaz
		24821, // Enchanter Nied
		24822, // Summoner Inke
		24823, // Healer Zera
		24824, // Healer Zera
	};
	private static final int[] REVOLUTIONARIES =
	{
		23616, // Unit 1 Elite Soldier
		23617, // Unit 2 Elite Soldier
		23618, // Unit 3 Elite Soldier
		23619, // Unit 4 Elite Soldier
		23620, // Unit 5 Elite Soldier
		23621, // Unit 6 Elite Soldier
		23622, // Unit 7 Elite Soldier
		23623, // Unit 8 Elite Soldier
		23624, // Unit 1 Elite Soldier
		23625, // Unit 2 Elite Soldier
		23626, // Unit 3 Elite Soldier
		23627, // Unit 4 Elite Soldier
		23628, // Unit 5 Elite Soldier
		23629, // Unit 6 Elite Soldier
		23630, // Unit 7 Elite Soldier
		23631, // Unit 8 Elite Soldier
		23632, // Unit 1 Elite Soldier
		23633, // Unit 2 Elite Soldier
		23634, // Unit 3 Elite Soldier
		23635, // Unit 4 Elite Soldier
		23636, // Unit 5 Elite Soldier
		23637, // Unit 6 Elite Soldier
		23638, // Unit 7 Elite Soldier
		23639, // Unit 8 Elite Soldier
		23640, // Unit 1 Elite Soldier
		23641, // Unit 2 Elite Soldier
		23642, // Unit 3 Elite Soldier
		23643, // Unit 4 Elite Soldier
		23644, // Unit 5 Elite Soldier
		23645, // Unit 6 Elite Soldier
		23646, // Unit 7 Elite Soldier
		23647, // Unit 8 Elite Soldier
		23648, // Dark Crusader (summon)
		23649, // Banshee Queen (summon)
		SIGNALMAN, // Unit Signalman
		23652, // Unit Guard
		34103, // Revolutionaries Altar
	};
	private static final int[] REVOLUTIONARIES_110 =
	{
		24800, // Knight Agar
		24801, // Warrior Ule
		24802, // Rogue Fiord
		24803, // Archer Torn
		24804, // Mage Kenaz
		24805, // Enchanter Nied
		24806, // Summoner Inke
		24807, // Healer Zera
		24808, // Soul Specter (summon)
		24809, // Banshee Queen (summon)
		SIGNALMAN_110, // Unit Signalman
		23652, // Unit Guard
	};
	// Items
	private static final ItemHolder BENUSTAS_REWARD_BOX = new ItemHolder(81151, 1);
	private static final ItemHolder BENUSTAS_SHINING_REWARD_BOX = new ItemHolder(81452, 1);
	private static final ItemHolder BENUSTAS_REWARD_BOX_110 = new ItemHolder(81741, 1);
	// Locations
	private static final Location QUEST_GIVER_LOCATION = new Location(-77648, 155665, -3190, 21220);
	private static final Location COMMANDER_LOCATION_1 = new Location(-81911, 154244, -3177);
	private static final Location COMMANDER_LOCATION_2 = new Location(-83028, 150866, -3128);
	private static final Location[] SPY_DWARF_LOCATION =
	{
		new Location(-81313, 152102, -3124, 21220), // Magic Shop
		new Location(-83168, 155408, -3175, 64238), // Blacksmith Shop
		new Location(-80000, 153379, -3160, 55621), // Grocery Store
	};
	// Misc
	private static final NpcStringId[] DWARF_SPY_TEXT =
	{
		NpcStringId.HOW_DID_YOU_KNOW_I_WAS_HERE,
		NpcStringId.WHY_ARE_YOU_SO_LATE_HUH_YOU_ARE_NOT_PART_OF_THE_ASHEN_SHADOW_REVOLUTIONARIES,
		NpcStringId.I_LL_HAVE_TO_SILENCE_YOU_IN_ORDER_TO_HIDE_THE_FACT_I_M_A_SPY,
		NpcStringId.YOU_THINK_YOU_CAN_LEAVE_THIS_PLACE_ALIVE_AFTER_SEEING_ME,
		NpcStringId.WAIT_WAIT_IT_WILL_BE_BETTER_FOR_YOU_IF_YOU_LET_ME_LIVE,
		NpcStringId.STOP_I_ONLY_HELPED_THE_ASHEN_SHADOW_REVOLUTIONARIES_FOR_A_LITTLE,
	};
	private static final ScriptZone TOWN_ZONE = ZoneManager.getInstance().getZoneById(60200, ScriptZone.class);
	private static final int[] TEMPLATE_IDS =
	{
		260, // lv. 105
		311, // lv. 110
	};
	
	public AshenShadowRevolutionaries()
	{
		super(TEMPLATE_IDS);
		addStartNpc(BENUSTA, TREASURE_CHEST);
		addFirstTalkId(TREASURE_CHEST, 34151, 34152, 34153, 34154, 34155);
		addFirstTalkId(QUEST_GIVERS);
		addTalkId(BENUSTA, TREASURE_CHEST);
		addSpawnId(REVOLUTIONARIES);
		addSpawnId(REVOLUTIONARIES_110);
		addSpawnId(SPY_DWARF);
		addSpawnId(COMMANDERS);
		addSpawnId(COMMANDERS_110);
		addAttackId(SPY_DWARF);
		addKillId(SIGNALMAN);
		addKillId(SIGNALMAN_110);
		addKillId(COMMANDERS);
		addKillId(COMMANDERS_110);
		addExitZoneId(TOWN_ZONE.getId());
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.contains("enterInstance"))
		{
			final int templateId = event.contains("110") ? TEMPLATE_IDS[1] : TEMPLATE_IDS[0];
			if (player.isInParty())
			{
				final Party party = player.getParty();
				if (!party.isLeader(player))
				{
					player.sendPacket(SystemMessageId.ONLY_A_PARTY_LEADER_CAN_MAKE_THE_REQUEST_TO_ENTER);
					return null;
				}
				
				if (player.isInCommandChannel())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_AS_YOU_DON_T_MEET_THE_REQUIREMENTS);
					return null;
				}
				
				final long currentTime = System.currentTimeMillis();
				final List<Player> members = party.getMembers();
				for (Player member : members)
				{
					if (!member.isInsideRadius3D(npc, 1000))
					{
						player.sendMessage("Player " + member.getName() + " must come closer.");
						return null;
					}
					
					for (int id : TEMPLATE_IDS)
					{
						if (currentTime < InstanceManager.getInstance().getInstanceTime(member, id))
						{
							final SystemMessage msg = new SystemMessage(SystemMessageId.YOU_CANNOT_ENTER_AS_C1_IS_IN_ANOTHER_INSTANCE_ZONE);
							msg.addString(member.getName());
							party.broadcastToPartyMembers(member, msg);
							return null;
						}
					}
				}
				
				for (Player member : members)
				{
					enterInstance(member, npc, templateId);
				}
			}
			else if (player.isGM())
			{
				enterInstance(player, npc, templateId);
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_A_PARTY_SO_YOU_CANNOT_ENTER);
			}
			
			if (player.getInstanceWorld() != null)
			{
				startQuestTimer("chest_talk", 1000, player.getInstanceWorld().getNpc(TREASURE_CHEST), null);
			}
			return null;
		}
		else if (event.equals("chest_talk"))
		{
			final Instance world = npc.getInstanceWorld();
			if ((world != null) && world.isStatus(0))
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.OPEN_THIS_BOX);
				startQuestTimer("chest_talk", 10000, npc, null);
			}
			return null;
		}
		else if (event.equals("openBox"))
		{
			final Instance world = npc.getInstanceWorld();
			if ((world != null) && world.isStatus(0))
			{
				world.setStatus(1);
				world.spawnGroup("wave_1");
				final Npc questGiver = addSpawn(getRandomEntry(QUEST_GIVERS), QUEST_GIVER_LOCATION, false, 0, false, world.getId());
				questGiver.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THERE_S_NO_ONE_RIGHT);
				if (questGiver.getId() == 34098) // Blacksmith Kluto
				{
					world.spawnGroup("goods");
				}
				if (questGiver.getId() == 34100) // Yuyuria
				{
					world.spawnGroup("altars");
				}
				if (questGiver.getId() == 34097) // Adonius
				{
					world.setParameter("CAPTIVES", world.spawnGroup("captives"));
					for (Npc captive : world.getParameters().getList("CAPTIVES", Npc.class))
					{
						captive.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.FLESH_STONE);
						captive.setTargetable(false);
						captive.broadcastInfo();
					}
				}
				else if (getRandom(10) < 3)
				{
					addSpawn(SPY_DWARF, getRandomEntry(SPY_DWARF_LOCATION), false, 0, false, world.getId());
				}
				showOnScreenMsg(world, NpcStringId.ASHEN_SHADOW_REVOLUTIONARIES_KEEP_THE_FORMATION, ExShowScreenMessage.TOP_CENTER, 10000, false);
			}
			return null;
		}
		else if (event.equals("exitInstance"))
		{
			final Instance world = npc.getInstanceWorld();
			if (world != null)
			{
				world.ejectPlayer(player);
			}
			return null;
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = npc.getInstanceWorld();
		if (world == null)
		{
			return null;
		}
		
		if ((npc.getId() == TREASURE_CHEST) && (world.getStatus() > 0))
		{
			return "34101-1.html";
		}
		
		return npc.getId() + ".html";
	}
	
	@Override
	public void onInstanceCreated(Instance instance, Player player)
	{
		instance.getParameters().set("INITIAL_PARTY_MEMBERS", player.getParty() != null ? player.getParty().getMemberCount() : 1);
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (world == null)
		{
			return null;
		}
		
		final int id = npc.getId();
		if (id == SIGNALMAN)
		{
			addSpawn(getRandomEntry(COMMANDERS), world.isStatus(1) ? COMMANDER_LOCATION_1 : COMMANDER_LOCATION_2, false, 0, false, world.getId());
		}
		else if (id == SIGNALMAN_110)
		{
			addSpawn(getRandomEntry(COMMANDERS_110), world.isStatus(1) ? COMMANDER_LOCATION_1 : COMMANDER_LOCATION_2, false, 0, false, world.getId());
		}
		else if (CommonUtil.contains(world.getTemplateId() == TEMPLATE_IDS[0] ? COMMANDERS : COMMANDERS_110, id))
		{
			world.incStatus();
			if (world.getStatus() < 3)
			{
				world.spawnGroup("wave_2");
			}
			else
			{
				final List<Npc> captives = world.getParameters().getList("CAPTIVES", Npc.class);
				if (captives != null)
				{
					for (Npc captive : captives)
					{
						captive.setTargetable(true);
						captive.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.FLESH_STONE);
						captive.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.MAGIC_SQUARE);
						captive.broadcastInfo();
					}
				}
				for (Player member : world.getPlayers())
				{
					giveItems(member, world.getTemplateId() == TEMPLATE_IDS[0] ? BENUSTAS_REWARD_BOX : BENUSTAS_REWARD_BOX_110);
				}
				final Party party = world.getFirstPlayer().getParty();
				final Player randomPlayer = party != null ? party.getRandomPlayer() : null;
				if ((randomPlayer != null) && (getRandom(100) < 80) && (world.getPlayersCount() == world.getParameters().getInt("INITIAL_PARTY_MEMBERS", 0)))
				{
					giveItems(randomPlayer, BENUSTAS_SHINING_REWARD_BOX);
				}
				world.spawnGroup("wave_3");
				world.finishInstance();
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (getRandom(10) < 1)
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(DWARF_SPY_TEXT));
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		npc.setRandomWalking(false);
		if (npc.getId() == 34103)
		{
			npc.setImmobilized(true);
			npc.detachAI();
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onExitZone(Creature creature, ZoneType zone)
	{
		final Instance world = creature.getInstanceWorld();
		if (creature.isPlayer() && (world != null))
		{
			creature.asPlayer().teleToLocation(world.getEnterLocation());
		}
		return super.onExitZone(creature, zone);
	}
	
	public static void main(String[] args)
	{
		new AshenShadowRevolutionaries();
	}
}
