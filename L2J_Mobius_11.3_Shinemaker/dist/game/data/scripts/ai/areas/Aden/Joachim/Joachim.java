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
package ai.areas.Aden.Joachim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.enums.CategoryType;
import org.l2jmobius.gameserver.enums.ClassId;
import org.l2jmobius.gameserver.enums.SubclassInfoType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.Id;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.impl.creature.npc.OnNpcMenuSelect;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.serverpackets.AcquireSkillList;
import org.l2jmobius.gameserver.network.serverpackets.ExSubjobInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

import ai.AbstractNpcAI;
import quests.Q10107_SplitDestiny.Q10107_SplitDestiny;

/**
 * @author Mobius
 */
public class Joachim extends AbstractNpcAI
{
	// NPC
	private static final int JOACHIM = 34513;
	// Items
	private static final int CHAOS_POMANDER_DUAL_CLASS = 37375;
	private static final int PAULINAS_RGRADE_EQUIPMENT_SET = 46919;
	private static final Map<CategoryType, Integer> POWER_ITEMS = new EnumMap<>(CategoryType.class);
	static
	{
		POWER_ITEMS.put(CategoryType.SIXTH_SIGEL_GROUP, 32264); // Abelius Power
		POWER_ITEMS.put(CategoryType.SIXTH_TIR_GROUP, 32265); // Sapyros Power
		POWER_ITEMS.put(CategoryType.SIXTH_OTHEL_GROUP, 32266); // Ashagen Power
		POWER_ITEMS.put(CategoryType.SIXTH_YR_GROUP, 32267); // Cranigg Power
		POWER_ITEMS.put(CategoryType.SIXTH_FEOH_GROUP, 32268); // Soltkreig Power
		POWER_ITEMS.put(CategoryType.SIXTH_WYNN_GROUP, 32269); // Naviarope Power
		POWER_ITEMS.put(CategoryType.SIXTH_IS_GROUP, 32270); // Leister Power
		POWER_ITEMS.put(CategoryType.SIXTH_EOLH_GROUP, 32271); // Laksis Power
	}
	// Misc
	private static final List<ClassId> DUAL_CLASS_LIST = new ArrayList<>();
	static
	{
		DUAL_CLASS_LIST.addAll(Arrays.asList(ClassId.SIGEL_PHOENIX_KNIGHT, ClassId.SIGEL_HELL_KNIGHT, ClassId.SIGEL_EVA_TEMPLAR, ClassId.SIGEL_SHILLIEN_TEMPLAR));
		DUAL_CLASS_LIST.addAll(Arrays.asList(ClassId.TYRR_DUELIST, ClassId.TYRR_DREADNOUGHT, ClassId.TYRR_TITAN, ClassId.TYRR_GRAND_KHAVATARI, ClassId.TYRR_DOOMBRINGER));
		DUAL_CLASS_LIST.addAll(Arrays.asList(ClassId.OTHELL_ADVENTURER, ClassId.OTHELL_WIND_RIDER, ClassId.OTHELL_GHOST_HUNTER, ClassId.OTHELL_FORTUNE_SEEKER));
		DUAL_CLASS_LIST.addAll(Arrays.asList(ClassId.YUL_SAGITTARIUS, ClassId.YUL_MOONLIGHT_SENTINEL, ClassId.YUL_GHOST_SENTINEL, ClassId.YUL_TRICKSTER));
		DUAL_CLASS_LIST.addAll(Arrays.asList(ClassId.FEOH_ARCHMAGE, ClassId.FEOH_SOULTAKER, ClassId.FEOH_MYSTIC_MUSE, ClassId.FEOH_STORM_SCREAMER, ClassId.FEOH_SOUL_HOUND));
		DUAL_CLASS_LIST.addAll(Arrays.asList(ClassId.ISS_HIEROPHANT, ClassId.ISS_SWORD_MUSE, ClassId.ISS_SPECTRAL_DANCER, ClassId.ISS_DOOMCRYER));
		DUAL_CLASS_LIST.addAll(Arrays.asList(ClassId.WYNN_ARCANA_LORD, ClassId.WYNN_ELEMENTAL_MASTER, ClassId.WYNN_SPECTRAL_MASTER));
		DUAL_CLASS_LIST.addAll(Arrays.asList(ClassId.AEORE_CARDINAL, ClassId.AEORE_EVA_SAINT, ClassId.AEORE_SHILLIEN_SAINT));
	}
	private static final int REAWAKEN_PRICE = 300000000;
	
	private Joachim()
	{
		addStartNpc(JOACHIM);
		addTalkId(JOACHIM);
		addFirstTalkId(JOACHIM);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "addDualClass":
			{
				final QuestState qs = player.getQuestState(Q10107_SplitDestiny.class.getSimpleName());
				if (player.getLevel() < 105)
				{
					htmltext = "noLevel.html";
				}
				else if (!qs.isCompleted() && !Config.ALT_GAME_DUALCLASS_WITHOUT_QUEST)
				{
					htmltext = "noQuest.html";
				}
				else if (player.hasDualClass())
				{
					htmltext = "hasDual.html";
				}
				else if (player.hasSummon() || player.isTransformed())
				{
					htmltext = "noSummon.html";
				}
				else
				{
					htmltext = "selectDual.html";
				}
				break;
			}
			case "addDualClass_SIXTH_SIGEL_GROUP":
			case "addDualClass_SIXTH_TIR_GROUP":
			case "addDualClass_SIXTH_OTHEL_GROUP":
			case "addDualClass_SIXTH_YR_GROUP":
			case "addDualClass_SIXTH_FEOH_GROUP":
			case "addDualClass_SIXTH_IS_GROUP":
			case "addDualClass_SIXTH_WYNN_GROUP":
			case "addDualClass_SIXTH_EOLH_GROUP":
			{
				final CategoryType cType = CategoryType.valueOf(event.replace("addDualClass_", ""));
				if (cType == null)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Cannot parse CategoryType, event: " + event);
				}
				
				final StringBuilder sb = new StringBuilder();
				final NpcHtmlMessage html = getNpcHtmlMessage(player, npc, "selectClass.html");
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
			case "reawakenDualClass":
			{
				if (!player.isDualClassActive())
				{
					htmltext = "noDualClass.html";
				}
				else if ((player.getAdena() < REAWAKEN_PRICE))
				{
					htmltext = "noAdena.html";
				}
				else if (player.hasSummon() || player.isTransformed())
				{
					htmltext = "noSummon.html";
				}
				else
				{
					htmltext = "reawakenDual.html";
				}
				break;
			}
			case "startReawaken":
			{
				htmltext = "selectReawakenDual.html";
				break;
			}
			case "thinkAgain":
			{
				htmltext = "thinkAgain.html";
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
				final NpcHtmlMessage html = getNpcHtmlMessage(player, npc, "selectReawakenClass.html");
				for (ClassId dualClasses : getDualClasses(player, cType))
				{
					if (dualClasses != null)
					{
						sb.append("<button value=\"" + ClassListData.getInstance().getClass(dualClasses.getId()).getClassName() + "\" action=\"bypass -h menu_select?ask=2&reply=" + dualClasses.getId() + "\" width=\"200\" height=\"31\" back=\"L2UI_CT1.HtmlWnd_DF_Awake_Down\" fore=\"L2UI_CT1.HtmlWnd_DF_Awake\"><br>");
					}
				}
				html.replace("%dualclassList%", sb.toString());
				player.sendPacket(html);
				break;
			}
		}
		return htmltext;
	}
	
	private NpcHtmlMessage getNpcHtmlMessage(Player player, Npc npc, String fileName)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		final String text = getHtm(player, fileName);
		if (text == null)
		{
			LOGGER.info("Cannot find HTML file for " + Joachim.class.getSimpleName() + " AI: " + fileName);
			return null;
		}
		html.setHtml(text);
		return html;
	}
	
	private List<ClassId> getDualClasses(Player player, CategoryType cType)
	{
		final List<ClassId> tempList = new ArrayList<>();
		final int baseClassId = player.getBaseClass();
		final int dualClassId = player.getClassId().getId();
		for (ClassId temp : DUAL_CLASS_LIST)
		{
			if ((temp.getId() != baseClassId) && (temp.getId() != dualClassId) && ((cType == null) || CategoryData.getInstance().isInCategory(cType, temp.getId())))
			{
				tempList.add(temp);
			}
		}
		return tempList;
	}
	
	@RegisterEvent(EventType.ON_NPC_MENU_SELECT)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(JOACHIM)
	public void onNpcMenuSelect(OnNpcMenuSelect event)
	{
		final Player player = event.getTalker();
		final Npc npc = event.getNpc();
		final int ask = event.getAsk();
		
		switch (ask)
		{
			case 1: // Create DualClass
			{
				final int classId = event.getReply();
				if (player.isTransformed() || player.hasSummon() || player.hasDualClass() || !player.isAwakenedClass())
				{
					break;
				}
				
				// Validating classId
				if (!getDualClasses(player, null).contains(ClassId.getClassId(classId)))
				{
					break;
				}
				
				if (player.addSubClass(classId, 1, true))
				{
					player.abortCast();
					player.stopAllEffectsExceptThoseThatLastThroughDeath();
					player.stopAllEffects();
					player.stopCubics();
					player.setActiveClass(1);
					player.sendPacket(new ExSubjobInfo(player, SubclassInfoType.CLASS_CHANGED));
					player.sendPacket(getNpcHtmlMessage(player, npc, "dualClassSuccess.html"));
					SkillTreeData.getInstance().cleanSkillUponChangeClass(player);
					player.restoreDualSkills();
					player.sendPacket(new AcquireSkillList(player));
					player.sendSkillList();
					player.broadcastUserInfo();
					
					// Fix Death Knight model animation.
					if (player.isDeathKnight())
					{
						player.transform(101, false);
						ThreadPool.schedule(() -> player.stopTransformation(false), 50);
					}
					
					// Item rewards
					player.addItem("subclass", CHAOS_POMANDER_DUAL_CLASS, 2, player, true);
					player.addItem("subclass", PAULINAS_RGRADE_EQUIPMENT_SET, 1, player, true);
					giveItems(player, getPowerItemId(player), 1);
				}
				break;
			}
			case 2: // Reawaken (change dual class)
			{
				final int classId = event.getReply();
				if (player.isTransformed() || player.hasSummon() || (!player.hasDualClass() || !player.isDualClassActive() || !player.isAwakenedClass()))
				{
					break;
				}
				
				// Validating classId
				if (!getDualClasses(player, null).contains(ClassId.getClassId(classId)))
				{
					break;
				}
				
				if ((player.getAdena() < REAWAKEN_PRICE))
				{
					final NpcHtmlMessage html = getNpcHtmlMessage(player, npc, "noAdena.html");
					player.sendPacket(html);
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
					player.sendPacket(getNpcHtmlMessage(player, npc, "reawakenSuccess.html"));
					SkillTreeData.getInstance().cleanSkillUponChangeClass(player);
					player.restoreDualSkills();
					player.sendPacket(new AcquireSkillList(player));
					player.sendSkillList();
					takeItems(player, CHAOS_POMANDER_DUAL_CLASS, -1);
					giveItems(player, CHAOS_POMANDER_DUAL_CLASS, 2);
				}
				
				addExpAndSp(player, (ExperienceData.getInstance().getExpForLevel(level) + 1) - player.getExp(), 0);
				break;
			}
		}
	}
	
	private int getPowerItemId(Player player)
	{
		return POWER_ITEMS.entrySet().stream().filter(e -> player.isInCategory(e.getKey())).mapToInt(Entry::getValue).findFirst().orElse(0);
	}
	
	public static void main(String[] args)
	{
		new Joachim();
	}
}
