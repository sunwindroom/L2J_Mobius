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
package events.RedLibra;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.enums.CategoryType;
import org.l2jmobius.gameserver.enums.ClassId;
import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.enums.SubclassInfoType;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.Id;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.impl.creature.npc.OnNpcMenuSelect;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.quest.LongTimeEvent;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AcquireSkillList;
import org.l2jmobius.gameserver.network.serverpackets.ExSubjobInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Red Libra<br>
 * @URL https://4ga.me/3OsQCDf
 * @author Mobius, Tanatos
 */
public class RedLibra extends LongTimeEvent
{
	// NPCs
	private static final int RED = 34210;
	private static final int GREEN = 34211;
	private static final int BLACK = 34212;
	private static final int PINK = 34213;
	private static final int BLUE = 34214;
	// Items
	private static final int STONE_OF_DESTINY = 17722;
	private static final int CHAOS_POMANDER = 37375;
	// Skills
	private static final SkillHolder GREEN_BUFF = new SkillHolder(32266, 1);
	private static final SkillHolder RED_BUFF = new SkillHolder(32264, 1);
	// Misc
	private static final String STONE_OF_DESTINY_VAR = "STONE_OF_DESTINY_RECEIVED";
	private static final String GREEN_BUFF_VAR = "GREEN_BUFF_RECEIVED";
	private static final int PLAYER_MIN_LEVEL = 105;
	private static final List<ClassId> dualClassList = new ArrayList<>();
	static
	{
		dualClassList.addAll(Arrays.asList(ClassId.AEORE_CARDINAL, ClassId.AEORE_EVA_SAINT, ClassId.AEORE_SHILLIEN_SAINT));
		dualClassList.addAll(Arrays.asList(ClassId.FEOH_ARCHMAGE, ClassId.FEOH_SOULTAKER, ClassId.FEOH_MYSTIC_MUSE, ClassId.FEOH_STORM_SCREAMER, ClassId.FEOH_SOUL_HOUND));
		dualClassList.addAll(Arrays.asList(ClassId.ISS_HIEROPHANT, ClassId.ISS_SWORD_MUSE, ClassId.ISS_SPECTRAL_DANCER, ClassId.ISS_DOOMCRYER));
		dualClassList.addAll(Arrays.asList(ClassId.OTHELL_ADVENTURER, ClassId.OTHELL_WIND_RIDER, ClassId.OTHELL_GHOST_HUNTER, ClassId.OTHELL_FORTUNE_SEEKER));
		dualClassList.addAll(Arrays.asList(ClassId.SIGEL_PHOENIX_KNIGHT, ClassId.SIGEL_HELL_KNIGHT, ClassId.SIGEL_EVA_TEMPLAR, ClassId.SIGEL_SHILLIEN_TEMPLAR));
		dualClassList.addAll(Arrays.asList(ClassId.TYRR_DUELIST, ClassId.TYRR_DREADNOUGHT, ClassId.TYRR_TITAN, ClassId.TYRR_GRAND_KHAVATARI, ClassId.TYRR_DOOMBRINGER));
		dualClassList.addAll(Arrays.asList(ClassId.WYNN_ARCANA_LORD, ClassId.WYNN_ELEMENTAL_MASTER, ClassId.WYNN_SPECTRAL_MASTER));
		dualClassList.addAll(Arrays.asList(ClassId.YUL_SAGITTARIUS, ClassId.YUL_MOONLIGHT_SENTINEL, ClassId.YUL_GHOST_SENTINEL, ClassId.YUL_TRICKSTER));
	}
	private static final int REAWAKEN_PRICE = 300000000;
	private static final int RED_BUFF_PRICE = 100000000;
	private static final Location ERATON_LOC = new Location(147342, 23750, -1984);
	
	private RedLibra()
	{
		addStartNpc(RED, GREEN, BLACK, PINK, BLUE);
		addFirstTalkId(RED, GREEN, BLACK, PINK, BLUE);
		addTalkId(RED, GREEN, BLACK, PINK, BLUE);
		
		startQuestTimer("schedule", 1000, null, null);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "34210-1.htm":
			case "34210-2.htm":
			case "34210-3.htm":
			case "34210-4.htm":
			case "34210-5.htm":
			case "34211-1.htm":
			case "34211-2.htm":
			case "34211-3.htm":
			case "34211-4.htm":
			case "34211-5.htm":
			case "34212-1.htm":
			case "34212-2.htm":
			case "34212-3.htm":
			case "34212-4.htm":
			case "34212-5.htm":
			case "34212-6.htm":
			case "34212-7.htm":
			case "34212-8.htm":
			case "34212-9.htm":
			case "34212-10.htm":
			case "34214-1.htm":
			case "34214-2.htm":
			case "34214-3.htm":
			case "34214-4.htm":
			case "34214-5.htm":
			case "34214-6.htm":
			{
				htmltext = event;
				break;
			}
			case "freeStone":
			{
				if (npc.getId() != RED)
				{
					break;
				}
				if (player.getAccountVariables().getBoolean(STONE_OF_DESTINY_VAR, false))
				{
					player.sendMessage("This account has already received a gift. The gift can only be given once per account");
					htmltext = "34210-stoneGiven.htm";
					break;
				}
				
				player.getAccountVariables().set(STONE_OF_DESTINY_VAR, true);
				player.getAccountVariables().storeMe();
				giveItems(player, STONE_OF_DESTINY, 1);
				htmltext = "34210-stoneGiven.htm";
				break;
			}
			case "teleport":
			{
				if (npc.getId() != RED)
				{
					break;
				}
				player.teleToLocation(ERATON_LOC);
				break;
			}
			case "redBuff":
			{
				if (player.getAdena() < RED_BUFF_PRICE)
				{
					htmltext = "34210-noAdena.htm";
					break;
				}
				SkillCaster.triggerCast(player, player, RED_BUFF.getSkill());
				player.reduceAdena((getClass().getSimpleName() + "_redBuff"), RED_BUFF_PRICE, npc, true);
				htmltext = "34210-buffGiven.htm";
				break;
			}
			case "reawakenDualclass":
			{
				if (!player.hasDualClass() || (player.getLevel() < 85) || !player.isDualClassActive() || !player.isAwakenedClass())
				{
					htmltext = "34211-noDual.htm";
				}
				else if (player.getAdena() < REAWAKEN_PRICE)
				{
					htmltext = "34211-noAdena.htm";
				}
				else if (player.isTransformed() || player.hasSummon())
				{
					htmltext = "34211-noTransform.htm";
				}
				else if (!player.isInventoryUnder80(true) || (player.getWeightPenalty() >= 2))
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_SPACE_IN_THE_INVENTORY);
					player.sendMessage("Not enough space in the inventory. Unable to process this request until your inventory's weight and slot count are less than 80 percent of capacity.");
				}
				else
				{
					htmltext = "34211-reawaken.htm";
				}
				break;
			}
			case "reawakenConfirm":
			{
				if (!player.hasDualClass() || (player.getLevel() < 85) || !player.isDualClassActive() || !player.isAwakenedClass())
				{
					htmltext = "34211-noDual.htm";
				}
				else if (player.getAdena() < REAWAKEN_PRICE)
				{
					htmltext = "34211-noAdena.htm";
				}
				else if (player.isTransformed() || player.hasSummon())
				{
					htmltext = "34211-noTransform.htm";
				}
				else if (!player.isInventoryUnder80(true) || (player.getWeightPenalty() >= 2))
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_SPACE_IN_THE_INVENTORY);
					player.sendMessage("Not enough space in the inventory. Unable to process this request until your inventory's weight and slot count are less than 80 percent of capacity.");
				}
				else
				{
					htmltext = "34211-reawakenList.htm";
				}
				break;
			}
			case "reawaken_SIXTH_SIGEL_GROUP":
			case "reawaken_SIXTH_TIR_GROUP":
			case "reawaken_SIXTH_OTHEL_GROUP":
			case "reawaken_SIXTH_YR_GROUP":
			case "reawaken_SIXTH_FEOH_GROUP":
			case "reawaken_SIXTH_IS_GROUP":
			case "reawaken_SIXTH_WYNN_GROUP":
			case "reawaken_SIXTH_EOLH_GROUP":
			{
				final CategoryType cType = CategoryType.valueOf(event.replace("reawaken_", ""));
				if (cType == null)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Cannot parse CategoryType, event: " + event);
				}
				
				final StringBuilder sb = new StringBuilder();
				final NpcHtmlMessage html = getNpcHtmlMessage(player, npc, "34211-reawakenClassList.htm");
				for (ClassId dualClasses : getDualClasses(player, cType))
				{
					if (dualClasses != null)
					{
						sb.append("<button value=\"" + ClassListData.getInstance().getClass(dualClasses.getId()).getClassName() + "\" action=\"bypass -h menu_select?ask=1&reply=" + dualClasses.getId() + "\" width=\"200\" height=\"31\" back=\"L2UI_CT1.HtmlWnd_DF_Awake_Down\" fore=\"L2UI_CT1.HtmlWnd_DF_Awake\"><br>");
					}
				}
				html.replace("%dualclassList%", sb.toString());
				player.sendPacket(html);
				break;
			}
			case "greenBuff":
			{
				if (npc.getId() != GREEN)
				{
					break;
				}
				if (player.getLevel() < PLAYER_MIN_LEVEL)
				{
					htmltext = "34211-noLevel.htm";
					break;
				}
				if (player.getAccountVariables().getBoolean(GREEN_BUFF_VAR, false))
				{
					player.sendMessage("This account has already received a gift. The gift can only be given once per account");
					htmltext = "34211-buffGiven.htm";
					break;
				}
				
				player.getAccountVariables().set(GREEN_BUFF_VAR, true);
				player.getAccountVariables().storeMe();
				SkillCaster.triggerCast(player, player, GREEN_BUFF.getSkill());
				htmltext = "34211-buffGiven.htm";
				break;
			}
			case "schedule":
			{
				final long currentTime = System.currentTimeMillis();
				final Calendar calendar = Calendar.getInstance();
				calendar.set(Calendar.HOUR_OF_DAY, 6);
				calendar.set(Calendar.MINUTE, 30);
				if (calendar.getTimeInMillis() < currentTime)
				{
					calendar.add(Calendar.DAY_OF_YEAR, 1);
				}
				cancelQuestTimers("reset");
				startQuestTimer("reset", calendar.getTimeInMillis() - currentTime, null, null);
				break;
			}
			case "reset":
			{
				if (isEventPeriod())
				{
					// Update data for offline players.
					try (Connection con = DatabaseFactory.getConnection();
						PreparedStatement ps = con.prepareStatement("DELETE FROM account_gsdata WHERE var=?"))
					{
						ps.setString(1, GREEN_BUFF_VAR);
						ps.executeUpdate();
					}
					catch (Exception e)
					{
						LOGGER.log(Level.SEVERE, "Could not reset Red Libra Guild Event var: ", e);
					}
					
					// Update data for online players.
					for (Player plr : World.getInstance().getPlayers())
					{
						plr.getAccountVariables().remove(GREEN_BUFF_VAR);
						plr.getAccountVariables().storeMe();
					}
				}
				cancelQuestTimers("schedule");
				startQuestTimer("schedule", 1000, null, null);
				break;
			}
		}
		return htmltext;
	}
	
	@RegisterEvent(EventType.ON_NPC_MENU_SELECT)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(GREEN)
	public void onNpcMenuSelect(OnNpcMenuSelect event)
	{
		final Player player = event.getTalker();
		final Npc npc = event.getNpc();
		final int ask = event.getAsk();
		
		switch (ask)
		{
			case 1:
			{
				final int classId = event.getReply();
				if (!player.hasDualClass() || (player.getLevel() < 85) || !player.isDualClassActive() || !player.isAwakenedClass() || (player.getAdena() < REAWAKEN_PRICE) || player.isTransformed() || player.hasSummon() || !player.isInventoryUnder80(true) || (player.getWeightPenalty() >= 2))
				{
					break;
				}
				
				if (!getDualClasses(player, null).contains(ClassId.getClassId(classId)))
				{
					break;
				}
				
				player.reduceAdena((getClass().getSimpleName() + "_Reawaken"), REAWAKEN_PRICE, npc, true);
				final int level = player.getLevel();
				
				final int classIndex = player.getClassIndex();
				if (player.modifySubClass(classIndex, classId, true))
				{
					player.abortCast();
					player.stopAllEffectsExceptThoseThatLastThroughDeath();
					player.stopAllEffects();
					player.stopCubics();
					player.setActiveClass(classIndex);
					player.sendPacket(new ExSubjobInfo(player, SubclassInfoType.CLASS_CHANGED));
					player.sendPacket(getNpcHtmlMessage(player, npc, "34211-reawakenSuccess.htm"));
					SkillTreeData.getInstance().cleanSkillUponChangeClass(player);
					player.restoreDualSkills();
					player.sendPacket(new AcquireSkillList(player));
					player.sendSkillList();
					takeItems(player, CHAOS_POMANDER, -1);
					giveItems(player, CHAOS_POMANDER, 2);
				}
				
				addExpAndSp(player, (ExperienceData.getInstance().getExpForLevel(level) + 1) - player.getExp(), 0);
				break;
			}
		}
	}
	
	public List<ClassId> getAvailableDualclasses(Player player)
	{
		final List<ClassId> dualClasses = new ArrayList<>();
		for (ClassId ClassId : ClassId.values())
		{
			if ((ClassId.getRace() != Race.ERTHEIA) && CategoryData.getInstance().isInCategory(CategoryType.SIXTH_CLASS_GROUP, ClassId.getId()) && (ClassId.getId() != player.getClassId().getId()))
			{
				dualClasses.add(ClassId);
			}
		}
		return dualClasses;
	}
	
	private List<ClassId> getDualClasses(Player player, CategoryType cType)
	{
		final List<ClassId> tempList = new ArrayList<>();
		final int baseClassId = player.getBaseClass();
		final int dualClassId = player.getClassId().getId();
		for (ClassId temp : dualClassList)
		{
			if ((temp.getId() != baseClassId) && (temp.getId() != dualClassId) && ((cType == null) || CategoryData.getInstance().isInCategory(cType, temp.getId())))
			{
				tempList.add(temp);
			}
		}
		return tempList;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + "-1.htm";
	}
	
	private NpcHtmlMessage getNpcHtmlMessage(Player player, Npc npc, String fileName)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		final String text = getHtm(player, fileName);
		if (text == null)
		{
			LOGGER.info("Cannot find HTML file for " + RedLibra.class.getSimpleName() + " AI: " + fileName);
			return null;
		}
		html.setHtml(text);
		return html;
	}
	
	public static void main(String[] args)
	{
		new RedLibra();
	}
}
