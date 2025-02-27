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
package org.l2jmobius.gameserver.model.quest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NewQuestData;
import org.l2jmobius.gameserver.enums.AcquireSkillType;
import org.l2jmobius.gameserver.enums.CategoryType;
import org.l2jmobius.gameserver.enums.ClassId;
import org.l2jmobius.gameserver.enums.Faction;
import org.l2jmobius.gameserver.enums.QuestType;
import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.enums.TrapAction;
import org.l2jmobius.gameserver.instancemanager.QuestManager;
import org.l2jmobius.gameserver.model.KeyValuePair;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.Trap;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.events.AbstractScript;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.holders.NpcLogListHolder;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.interfaces.IIdentifiable;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.olympiad.CompetitionType;
import org.l2jmobius.gameserver.model.olympiad.Participant;
import org.l2jmobius.gameserver.model.quest.newquestdata.NewQuest;
import org.l2jmobius.gameserver.model.quest.newquestdata.NewQuestCondition;
import org.l2jmobius.gameserver.model.quest.newquestdata.NewQuestReward;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExQuestNpcLogList;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcQuestHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestDialog;
import org.l2jmobius.gameserver.util.Util;

/**
 * Quest main class.
 * @author Luis Arias
 */
public class Quest extends AbstractScript implements IIdentifiable
{
	public static final Logger LOGGER = Logger.getLogger(Quest.class.getName());
	
	/** Map containing lists of timers from the name of the timer. */
	private final Map<String, List<QuestTimer>> _questTimers = new HashMap<>();
	/** Map containing all the start conditions. */
	private final Set<QuestCondition> _startCondition = ConcurrentHashMap.newKeySet(1);
	
	private final int _questId;
	private final byte _initialState = State.CREATED;
	private boolean _isCustom = false;
	private NpcStringId _questNameNpcStringId;
	
	private int[] _questItemIds = null;
	
	private final NewQuest _questData;
	
	private static final String DEFAULT_NO_QUEST_MSG = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
	
	private static final String QUEST_DELETE_FROM_CHAR_QUERY = "DELETE FROM character_quests WHERE charId=? AND name=?";
	private static final String QUEST_DELETE_FROM_CHAR_QUERY_NON_REPEATABLE_QUERY = "DELETE FROM character_quests WHERE charId=? AND name=? AND var!=?";
	
	private static final int RESET_HOUR = 6;
	private static final int RESET_MINUTES = 30;
	
	private static final SkillHolder[] STORY_QUEST_BUFFS =
	{
		new SkillHolder(32840, 1), // Fantasia Harmony - Adventurer
		new SkillHolder(34243, 1), // Musician's Melody - Adventurer
		new SkillHolder(34254, 1), // Sonata Performance - Adventurer
	};
	
	/**
	 * @return the reset hour for a daily quest, could be overridden on a script.
	 */
	public int getResetHour()
	{
		return RESET_HOUR;
	}
	
	/**
	 * @return the reset minutes for a daily quest, could be overridden on a script.
	 */
	public int getResetMinutes()
	{
		return RESET_MINUTES;
	}
	
	/**
	 * The Quest object constructor.<br>
	 * Constructing a quest also calls the {@code init_LoadGlobalData} convenience method.
	 * @param questId ID of the quest
	 */
	public Quest(int questId)
	{
		_questId = questId;
		if (questId > 0)
		{
			QuestManager.getInstance().addQuest(this);
		}
		else
		{
			QuestManager.getInstance().addScript(this);
		}
		
		_questData = NewQuestData.getInstance().getQuestById(questId);
		if (_questData != null)
		{
			addNewQuestConditions(_questData.getConditions(), null);
			
			if (_questData.getQuestType() == 1)
			{
				if (_questData.getStartNpcId() > 0)
				{
					addFirstTalkId(_questData.getStartNpcId());
				}
				
				if ((_questData.getEndNpcId() > 0) && (_questData.getEndNpcId() != _questData.getStartNpcId()))
				{
					addFirstTalkId(_questData.getEndNpcId());
				}
			}
			else if (_questData.getQuestType() == 4)
			{
				if (_questData.getStartItemId() > 0)
				{
					addItemTalkId(_questData.getStartItemId());
				}
			}
			
			if (_questData.getGoal().getItemId() > 0)
			{
				registerQuestItems(_questData.getGoal().getItemId());
			}
		}
		
		onLoad();
	}
	
	/**
	 * This method is, by default, called by the constructor of all scripts.<br>
	 * Children of this class can implement this function in order to define what variables to load and what structures to save them in.<br>
	 * By default, nothing is loaded.
	 */
	protected void onLoad()
	{
	}
	
	/**
	 * The function onSave is, by default, called at shutdown, for all quests, by the QuestManager.<br>
	 * Children of this class can implement this function in order to convert their structures<br>
	 * into <var, value> tuples and make calls to save them to the database, if needed.<br>
	 * By default, nothing is saved.
	 */
	public void onSave()
	{
	}
	
	/**
	 * Gets the quest ID.
	 * @return the quest ID
	 */
	@Override
	public int getId()
	{
		return _questId;
	}
	
	/**
	 * @return the NpcStringId of the current quest, used in Quest link bypass
	 */
	public int getNpcStringId()
	{
		return _questNameNpcStringId != null ? _questNameNpcStringId.getId() / 100 : (_questId > 10000 ? _questId - 5000 : _questId);
	}
	
	public NpcStringId getQuestNameNpcStringId()
	{
		return _questNameNpcStringId;
	}
	
	public void setQuestNameNpcStringId(NpcStringId npcStringId)
	{
		_questNameNpcStringId = npcStringId;
	}
	
	/**
	 * Add a new quest state of this quest to the database.
	 * @param player the owner of the newly created quest state
	 * @return the newly created {@link QuestState} object
	 */
	public QuestState newQuestState(Player player)
	{
		return new QuestState(this, player, _initialState);
	}
	
	/**
	 * Get the specified player's {@link QuestState} object for this quest.<br>
	 * If the player does not have it and initIfNode is {@code true},<br>
	 * create a new QuestState object and return it, otherwise return {@code null}.
	 * @param player the player whose QuestState to get
	 * @param initIfNone if true and the player does not have a QuestState for this quest,<br>
	 *            create a new QuestState
	 * @return the QuestState object for this quest or null if it doesn't exist
	 */
	public QuestState getQuestState(Player player, boolean initIfNone)
	{
		final QuestState qs = player.getQuestState(getName());
		if ((qs != null) || !initIfNone)
		{
			return qs;
		}
		return newQuestState(player);
	}
	
	/**
	 * @return the initial state of the quest
	 */
	public byte getInitialState()
	{
		return _initialState;
	}
	
	/**
	 * @return the name of the quest
	 */
	public String getName()
	{
		return getClass().getSimpleName();
	}
	
	/**
	 * @return the path of the quest script
	 */
	public String getPath()
	{
		final String path = getClass().getName().replace('.', '/');
		return path.substring(0, path.lastIndexOf('/' + getClass().getSimpleName()));
	}
	
	/**
	 * Add a timer to the quest (if it doesn't exist already) and start it.
	 * @param name the name of the timer (also passed back as "event" in {@link #onEvent(String, Npc, Player)})
	 * @param time time in ms for when to fire the timer
	 * @param npc the NPC associated with this timer (can be null)
	 * @param player the player associated with this timer (can be null)
	 * @see #startQuestTimer(String, long, Npc, Player, boolean)
	 */
	public void startQuestTimer(String name, long time, Npc npc, Player player)
	{
		startQuestTimer(name, time, npc, player, false);
	}
	
	/**
	 * Gets the quest timers.
	 * @return the quest timers
	 */
	public Map<String, List<QuestTimer>> getQuestTimers()
	{
		return _questTimers;
	}
	
	/**
	 * Add a timer to the quest (if it doesn't exist already) and start it.
	 * @param name the name of the timer (also passed back as "event" in {@link #onEvent(String, Npc, Player)})
	 * @param time time in ms for when to fire the timer
	 * @param npc the NPC associated with this timer (can be null)
	 * @param player the player associated with this timer (can be null)
	 * @param repeating indicates whether the timer is repeatable or one-time.<br>
	 *            If {@code true}, the task is repeated every {@code time} milliseconds until explicitly stopped.
	 */
	public void startQuestTimer(String name, long time, Npc npc, Player player, boolean repeating)
	{
		if (name == null)
		{
			return;
		}
		
		synchronized (_questTimers)
		{
			if (!_questTimers.containsKey(name))
			{
				_questTimers.put(name, new CopyOnWriteArrayList<>());
			}
			
			// If there exists a timer with this name, allow the timer only if the [npc, player] set is unique nulls act as wildcards.
			if (getQuestTimer(name, npc, player) == null)
			{
				_questTimers.get(name).add(new QuestTimer(this, name, time, npc, player, repeating));
			}
		}
	}
	
	/**
	 * Get a quest timer that matches the provided name and parameters.
	 * @param name the name of the quest timer to get
	 * @param npc the NPC associated with the quest timer to get
	 * @param player the player associated with the quest timer to get
	 * @return the quest timer that matches the specified parameters or {@code null} if nothing was found
	 */
	public QuestTimer getQuestTimer(String name, Npc npc, Player player)
	{
		if (name == null)
		{
			return null;
		}
		
		final List<QuestTimer> timers = _questTimers.get(name);
		if ((timers == null) || timers.isEmpty())
		{
			return null;
		}
		
		for (QuestTimer timer : timers)
		{
			if ((timer != null) && timer.equals(this, name, npc, player))
			{
				return timer;
			}
		}
		
		return null;
	}
	
	/**
	 * Cancel all quest timers with the specified name.
	 * @param name the name of the quest timers to cancel
	 */
	public void cancelQuestTimers(String name)
	{
		if (name == null)
		{
			return;
		}
		
		final List<QuestTimer> timers = _questTimers.get(name);
		if ((timers == null) || timers.isEmpty())
		{
			return;
		}
		
		for (QuestTimer timer : timers)
		{
			if (timer != null)
			{
				timer.cancel();
			}
		}
		
		timers.clear();
	}
	
	/**
	 * Cancel the quest timer that matches the specified name and parameters.
	 * @param name the name of the quest timer to cancel
	 * @param npc the NPC associated with the quest timer to cancel
	 * @param player the player associated with the quest timer to cancel
	 */
	public void cancelQuestTimer(String name, Npc npc, Player player)
	{
		if (name == null)
		{
			return;
		}
		
		final List<QuestTimer> timers = _questTimers.get(name);
		if ((timers == null) || timers.isEmpty())
		{
			return;
		}
		
		for (QuestTimer timer : timers)
		{
			if ((timer != null) && timer.equals(this, name, npc, player))
			{
				timer.cancel();
			}
		}
	}
	
	/**
	 * Remove a quest timer from the list of all timers.<br>
	 * Note: does not stop the timer itself!
	 * @param timer the {@link QuestState} object to remove
	 */
	public void removeQuestTimer(QuestTimer timer)
	{
		if (timer == null)
		{
			return;
		}
		
		final List<QuestTimer> timers = _questTimers.get(timer.toString());
		if (timers != null)
		{
			timers.remove(timer);
		}
	}
	
	// These are methods to call within the core to call the quest events.
	
	/**
	 * @param npc the NPC that was attacked
	 * @param attacker the attacking player
	 * @param damage the damage dealt to the NPC by the player
	 * @param isSummon if {@code true}, the attack was actually made by the player's summon
	 * @param skill the skill used to attack the NPC (can be null)
	 */
	public void notifyAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		String res = null;
		try
		{
			res = onAttack(npc, attacker, damage, isSummon, skill);
		}
		catch (Exception e)
		{
			showError(attacker, e);
			return;
		}
		showResult(attacker, res);
	}
	
	/**
	 * @param killer the character that killed the {@code victim}
	 * @param victim the character that was killed by the {@code killer}
	 * @param qs the quest state object of the player to be notified of this event
	 */
	public void notifyDeath(Creature killer, Creature victim, QuestState qs)
	{
		String res = null;
		try
		{
			res = onDeath(killer, victim, qs);
		}
		catch (Exception e)
		{
			showError(qs.getPlayer(), e);
			return;
		}
		showResult(qs.getPlayer(), res);
	}
	
	/**
	 * @param item
	 * @param player
	 */
	public void notifyItemUse(ItemTemplate item, Player player)
	{
		String res = null;
		try
		{
			res = onItemUse(item, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	/**
	 * @param instance
	 * @param player
	 * @param skill
	 */
	public void notifySpellFinished(Npc instance, Player player, Skill skill)
	{
		String res = null;
		try
		{
			res = onSpellFinished(instance, player, skill);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	/**
	 * Notify quest script when something happens with a trap.
	 * @param trap the trap instance which triggers the notification
	 * @param trigger the character which makes effect on the trap
	 * @param action 0: trap casting its skill. 1: trigger detects the trap. 2: trigger removes the trap
	 */
	public void notifyTrapAction(Trap trap, Creature trigger, TrapAction action)
	{
		final Player player = trigger.asPlayer();
		String res = null;
		
		try
		{
			res = onTrapAction(trap, trigger, action);
		}
		catch (Exception e)
		{
			if (player != null)
			{
				showError(player, e);
			}
			LOGGER.log(Level.WARNING, "Exception on onTrapAction() in notifyTrapAction(): " + e.getMessage(), e);
			return;
		}
		
		if (player != null)
		{
			showResult(player, res);
		}
	}
	
	/**
	 * @param npc the spawned NPC
	 */
	public void notifySpawn(Npc npc)
	{
		try
		{
			onSpawn(npc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onSpawn() in notifySpawn(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param npc the teleport NPC
	 */
	public void notifyTeleport(Npc npc)
	{
		try
		{
			onTeleport(npc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onTeleport() in notifyTeleport(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param event
	 * @param npc
	 * @param player
	 */
	public void notifyEvent(String event, Npc npc, Player player)
	{
		String res = null;
		try
		{
			// Simulated talk should not exist when event runs.
			if (player != null)
			{
				player.setSimulatedTalking(false);
			}
			res = onEvent(event, npc, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res, npc);
	}
	
	/**
	 * @param player the player entering the world
	 */
	public void notifyEnterWorld(Player player)
	{
		String res = null;
		try
		{
			res = onEnterWorld(player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	/**
	 * @param npc
	 * @param killer
	 * @param isSummon
	 */
	public void notifyKill(Npc npc, Player killer, boolean isSummon)
	{
		String res = null;
		try
		{
			// Simulated talk should not exist when killing.
			if (killer != null)
			{
				killer.setSimulatedTalking(false);
				
				final QuestState qs = getQuestState(killer, false);
				if (qs != null)
				{
					qs.setSimulated(false);
				}
			}
			
			res = onKill(npc, killer, isSummon);
		}
		catch (Exception e)
		{
			showError(killer, e);
			return;
		}
		showResult(killer, res);
	}
	
	/**
	 * @param npc
	 * @param player
	 */
	public void notifyTalk(Npc npc, Player player)
	{
		String res = null;
		try
		{
			final Set<Quest> startingQuests = new HashSet<>();
			for (AbstractEventListener listener : npc.getListeners(EventType.ON_NPC_QUEST_START))
			{
				final Object owner = listener.getOwner();
				if (owner instanceof Quest)
				{
					startingQuests.add((Quest) owner);
				}
			}
			
			final String startConditionHtml = getStartConditionHtml(player, npc);
			if (startingQuests.contains(this) && (startConditionHtml != null))
			{
				res = startConditionHtml;
			}
			else
			{
				res = onTalk(npc, player, false);
			}
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		player.setLastQuestNpcObject(npc.getObjectId());
		showResult(player, res, npc);
	}
	
	/**
	 * Override the default NPC dialogs when a quest defines this for the given NPC.<br>
	 * Note: If the default html for this npc needs to be shown, onFirstTalk should call npc.showChatWindow(player) and then return null.
	 * @param npc the NPC whose dialogs to override
	 * @param player the player talking to the NPC
	 */
	public void notifyFirstTalk(Npc npc, Player player)
	{
		String res = null;
		try
		{
			res = onFirstTalk(npc, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res, npc);
	}
	
	/**
	 * Notify the quest engine that an skill has been acquired.
	 * @param npc the NPC
	 * @param player the player
	 * @param skill the skill
	 * @param type the skill learn type
	 */
	public void notifyAcquireSkill(Npc npc, Player player, Skill skill, AcquireSkillType type)
	{
		String res = null;
		try
		{
			res = onAcquireSkill(npc, player, skill, type);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	/**
	 * @param item
	 * @param player
	 */
	public void notifyItemTalk(Item item, Player player)
	{
		String res = null;
		try
		{
			res = onItemTalk(item, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	/**
	 * @param item
	 * @param player
	 * @return
	 */
	public String onItemTalk(Item item, Player player)
	{
		return null;
	}
	
	/**
	 * @param item
	 * @param player
	 * @param event
	 */
	public void notifyItemEvent(Item item, Player player, String event)
	{
		String res = null;
		try
		{
			res = onItemEvent(item, player, event);
			if ((res != null) && (res.equalsIgnoreCase("true") || res.equalsIgnoreCase("false")))
			{
				return;
			}
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	/**
	 * @param npc
	 * @param caster
	 * @param skill
	 * @param targets
	 * @param isSummon
	 */
	public void notifySkillSee(Npc npc, Player caster, Skill skill, WorldObject[] targets, boolean isSummon)
	{
		String res = null;
		try
		{
			res = onSkillSee(npc, caster, skill, targets, isSummon);
		}
		catch (Exception e)
		{
			showError(caster, e);
			return;
		}
		showResult(caster, res);
	}
	
	/**
	 * @param npc
	 * @param caller
	 * @param attacker
	 * @param isSummon
	 */
	public void notifyFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
		String res = null;
		try
		{
			res = onFactionCall(npc, caller, attacker, isSummon);
		}
		catch (Exception e)
		{
			showError(attacker, e);
			return;
		}
		showResult(attacker, res);
	}
	
	/**
	 * @param npc
	 * @param player
	 * @param isSummon
	 */
	public void notifyAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		String res = null;
		try
		{
			res = onAggroRangeEnter(npc, player, isSummon);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	/**
	 * @param npc the NPC that sees the creature
	 * @param creature the creature seen by the NPC
	 */
	public void notifyCreatureSee(Npc npc, Creature creature)
	{
		Player player = null;
		if (creature.isPlayer())
		{
			player = creature.asPlayer();
		}
		String res = null;
		try
		{
			res = onCreatureSee(npc, creature);
		}
		catch (Exception e)
		{
			if (player != null)
			{
				showError(player, e);
			}
			return;
		}
		if (player != null)
		{
			showResult(player, res);
		}
	}
	
	/**
	 * @param eventName - name of event
	 * @param sender - NPC, who sent event
	 * @param receiver - NPC, who received event
	 * @param reference - WorldObject to pass, if needed
	 */
	public void notifyEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		try
		{
			onEventReceived(eventName, sender, receiver, reference);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onEventReceived() in notifyEventReceived(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param creature
	 * @param zone
	 */
	public void notifyEnterZone(Creature creature, ZoneType zone)
	{
		final Player player = creature.asPlayer();
		String res = null;
		try
		{
			res = onEnterZone(creature, zone);
		}
		catch (Exception e)
		{
			if (player != null)
			{
				showError(player, e);
			}
			return;
		}
		if (player != null)
		{
			showResult(player, res);
		}
	}
	
	/**
	 * @param creature
	 * @param zone
	 */
	public void notifyExitZone(Creature creature, ZoneType zone)
	{
		final Player player = creature.asPlayer();
		String res = null;
		try
		{
			res = onExitZone(creature, zone);
		}
		catch (Exception e)
		{
			if (player != null)
			{
				showError(player, e);
			}
			return;
		}
		if (player != null)
		{
			showResult(player, res);
		}
	}
	
	/**
	 * @param winner
	 * @param looser
	 * @param type
	 */
	public void notifyOlympiadMatch(Participant winner, Participant looser, CompetitionType type)
	{
		try
		{
			onOlympiadMatchFinish(winner, looser, type);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Execution on onOlympiadMatchFinish() in notifyOlympiadMatch(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param npc
	 */
	public void notifyMoveFinished(Npc npc)
	{
		try
		{
			onMoveFinished(npc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onMoveFinished() in notifyMoveFinished(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param npc
	 */
	public void notifyNodeArrived(Npc npc)
	{
		try
		{
			onNodeArrived(npc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onNodeArrived() in notifyNodeArrived(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param npc
	 */
	public void notifyRouteFinished(Npc npc)
	{
		try
		{
			onRouteFinished(npc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onRouteFinished() in notifyRouteFinished(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param npc
	 * @param player
	 * @return {@code true} if player can see this npc, {@code false} otherwise.
	 */
	public boolean notifyOnCanSeeMe(Npc npc, Player player)
	{
		try
		{
			return onCanSeeMe(npc, player);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onCanSeeMe() in notifyOnCanSeeMe(): " + e.getMessage(), e);
		}
		return false;
	}
	
	// These are methods that java calls to invoke scripts.
	
	/**
	 * This function is called in place of {@link #onAttack(Npc, Player, int, boolean, Skill)} if the former is not implemented.<br>
	 * If a script contains both onAttack(..) implementations, then this method will never be called unless the script's {@link #onAttack(Npc, Player, int, boolean, Skill)} explicitly calls this method.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that got attacked the NPC.
	 * @param attacker this parameter contains a reference to the exact instance of the player who attacked.
	 * @param damage this parameter represents the total damage that this attack has inflicted to the NPC.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the damage was actually dealt by the player's pet.
	 * @return
	 */
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player attacks an NPC that is registered for the quest.<br>
	 * If is not overridden by a subclass, then default to the returned value of the simpler (and older) {@link #onAttack(Npc, Player, int, boolean)} override.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that got attacked.
	 * @param attacker this parameter contains a reference to the exact instance of the player who attacked the NPC.
	 * @param damage this parameter represents the total damage that this attack has inflicted to the NPC.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the damage was actually dealt by the player's summon
	 * @param skill parameter is the skill that player used to attack NPC.
	 * @return
	 */
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		return onAttack(npc, attacker, damage, isSummon);
	}
	
	/**
	 * This function is called whenever an <b>exact instance</b> of a character who was previously registered for this event dies.<br>
	 * The registration for {@link #onDeath(Creature, Creature, QuestState)} events <b>is not</b> done via the quest itself, but it is instead handled by the QuestState of a particular player.
	 * @param killer this parameter contains a reference to the exact instance of the NPC that <b>killed</b> the character.
	 * @param victim this parameter contains a reference to the exact instance of the character that got killed.
	 * @param qs this parameter contains a reference to the QuestState of whomever was interested (waiting) for this kill.
	 * @return
	 */
	public String onDeath(Creature killer, Creature victim, QuestState qs)
	{
		return onEvent("", (killer instanceof Npc) ? killer.asNpc() : null, qs.getPlayer());
	}
	
	/**
	 * This function is called whenever a player clicks on a link in a quest dialog and whenever a timer fires.<br>
	 * If the player has a quest state, use it as parameter in the next call, otherwise return null.
	 * @param event this parameter contains a string identifier for the event.<br>
	 *            Generally, this string is passed directly via the link.<br>
	 *            For example:<br>
	 *            <code>
	 *            &lt;a action="bypass -h Quest 626_ADarkTwilight 31517-01.htm"&gt;hello&lt;/a&gt;
	 *            </code><br>
	 *            The above link sets the event variable to "31517-01.htm" for the quest 626_ADarkTwilight.<br>
	 *            In the case of timers, this will be the name of the timer.<br>
	 *            This parameter serves as a sort of identifier.
	 * @param npc this parameter contains a reference to the instance of NPC associated with this event.<br>
	 *            This may be the NPC registered in a timer, or the NPC with whom a player is speaking, etc.<br>
	 *            This parameter may be {@code null} in certain circumstances.
	 * @param player this parameter contains a reference to the player participating in this function.<br>
	 *            It may be the player speaking to the NPC, or the player who caused a timer to start (and owns that timer).<br>
	 *            This parameter may be {@code null} in certain circumstances.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onEvent(String event, Npc npc, Player player)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player kills a NPC that is registered for the quest.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that got killed.
	 * @param killer this parameter contains a reference to the exact instance of the player who killed the NPC.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the killer was the player's pet.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (!getNpcLogList(killer).isEmpty())
		{
			sendNpcLogList(killer);
		}
		return null;
	}
	
	/**
	 * This function is called whenever a player clicks to the "Quest" link of an NPC that is registered for the quest.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player is talking with.
	 * @param talker this parameter contains a reference to the exact instance of the player who is talking to the NPC.
	 * @param simulated Used by QuestLink to determine state of quest.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onTalk(Npc npc, Player talker, boolean simulated)
	{
		final QuestState qs = talker.getQuestState(getName());
		if (qs != null)
		{
			qs.setSimulated(simulated);
		}
		talker.setSimulatedTalking(simulated);
		return onTalk(npc, talker);
	}
	
	/**
	 * This function is called whenever a player clicks to the "Quest" link of an NPC that is registered for the quest.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player is talking with.
	 * @param talker this parameter contains a reference to the exact instance of the player who is talking to the NPC.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onTalk(Npc npc, Player talker)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player talks to an NPC that is registered for the quest.<br>
	 * That is, it is triggered from the very first click on the NPC, not via another dialog.<br>
	 * <b>Note 1:</b><br>
	 * Each NPC can be registered to at most one quest for triggering this function.<br>
	 * In other words, the same one NPC cannot respond to an "onFirstTalk" request from two different quests.<br>
	 * Attempting to register an NPC in two different quests for this function will result in one of the two registration being ignored.<br>
	 * <b>Note 2:</b><br>
	 * Since a Quest link isn't clicked in order to reach this, a quest state can be invalid within this function.<br>
	 * The coder of the script may need to create a new quest state (if necessary).<br>
	 * <b>Note 3:</b><br>
	 * The returned value of onFirstTalk replaces the default HTML that would have otherwise been loaded from a sub-folder of DatapackRoot/game/data/html/.<br>
	 * If you wish to show the default HTML, within onFirstTalk do npc.showChatWindow(player) and then return ""
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player is talking with.
	 * @param player this parameter contains a reference to the exact instance of the player who is talking to the NPC.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onFirstTalk(Npc npc, Player player)
	{
		return null;
	}
	
	/**
	 * @param item
	 * @param player
	 * @param event
	 * @return
	 */
	public String onItemEvent(Item item, Player player, String event)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player request a skill list.<br>
	 * TODO: Re-implement, since Skill Trees rework it's support was removed.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player requested the skill list.
	 * @param player this parameter contains a reference to the exact instance of the player who requested the skill list.
	 * @return
	 */
	public String onAcquireSkillList(Npc npc, Player player)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player request a skill info.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player requested the skill info.
	 * @param player this parameter contains a reference to the exact instance of the player who requested the skill info.
	 * @param skill this parameter contains a reference to the skill that the player requested its info.
	 * @return
	 */
	public String onAcquireSkillInfo(Npc npc, Player player, Skill skill)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player acquire a skill.<br>
	 * TODO: Re-implement, since Skill Trees rework it's support was removed.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player requested the skill.
	 * @param player this parameter contains a reference to the exact instance of the player who requested the skill.
	 * @param skill this parameter contains a reference to the skill that the player requested.
	 * @param type the skill learn type
	 * @return
	 */
	public String onAcquireSkill(Npc npc, Player player, Skill skill, AcquireSkillType type)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player uses a quest item that has a quest events list.<br>
	 * TODO: complete this documentation and unhardcode it to work with all item uses not with those listed.
	 * @param item the quest item that the player used
	 * @param player the player who used the item
	 * @return
	 */
	public String onItemUse(ItemTemplate item, Player player)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player casts a skill near a registered NPC (1000 distance).<br>
	 * <b>Note:</b><br>
	 * If a skill does damage, both onSkillSee(..) and onAttack(..) will be triggered for the damaged NPC!<br>
	 * However, only onSkillSee(..) will be triggered if the skill does no damage,<br>
	 * or if it damages an NPC who has no onAttack(..) registration while near another NPC who has an onSkillSee registration.<br>
	 * TODO: confirm if the distance is 1000 and unhardcode.
	 * @param npc the NPC that saw the skill
	 * @param caster the player who cast the skill
	 * @param skill the actual skill that was used
	 * @param targets an array of all objects (can be any type of object, including mobs and players) that were affected by the skill
	 * @param isSummon if {@code true}, the skill was actually cast by the player's summon, not the player himself
	 * @return
	 */
	public String onSkillSee(Npc npc, Player caster, Skill skill, WorldObject[] targets, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever an NPC finishes casting a skill.
	 * @param npc the NPC that casted the skill.
	 * @param player the player who is the target of the skill. Can be {@code null}.
	 * @param skill the actual skill that was used by the NPC.
	 * @return
	 */
	public String onSpellFinished(Npc npc, Player player, Skill skill)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a trap action is performed.
	 * @param trap this parameter contains a reference to the exact instance of the trap that was activated.
	 * @param trigger this parameter contains a reference to the exact instance of the character that triggered the action.
	 * @param action this parameter contains a reference to the action that was triggered.
	 * @return
	 */
	public String onTrapAction(Trap trap, Creature trigger, TrapAction action)
	{
		return null;
	}
	
	/**
	 * This function is called whenever an NPC spawns or re-spawns and passes a reference to the newly (re)spawned NPC.<br>
	 * Currently the only function that has no reference to a player.<br>
	 * It is useful for initializations, starting quest timers, displaying chat (NpcSay), and more.
	 * @param npc this parameter contains a reference to the exact instance of the NPC who just (re)spawned.
	 * @return
	 */
	public String onSpawn(Npc npc)
	{
		return null;
	}
	
	/**
	 * This function is called whenever an NPC is teleport.
	 * @param npc this parameter contains a reference to the exact instance of the NPC who just teleport.
	 */
	protected void onTeleport(Npc npc)
	{
	}
	
	/**
	 * This function is called whenever an NPC is called by another NPC in the same faction.
	 * @param npc this parameter contains a reference to the exact instance of the NPC who is being asked for help.
	 * @param caller this parameter contains a reference to the exact instance of the NPC who is asking for help.
	 * @param attacker this parameter contains a reference to the exact instance of the player who attacked.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the attacker was the player's summon.
	 * @return
	 */
	public String onFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player enters an NPC aggression range.
	 * @param npc this parameter contains a reference to the exact instance of the NPC whose aggression range is being transgressed.
	 * @param player this parameter contains a reference to the exact instance of the player who is entering the NPC's aggression range.
	 * @param isSummon this parameter if it's {@code false} it denotes that the character that entered the aggression range was indeed the player, else it specifies that the character was the player's summon.
	 * @return
	 */
	public String onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever an NPC "sees" a creature.
	 * @param npc the NPC who sees the creature
	 * @param creature the creature seen by the NPC
	 * @return
	 */
	public String onCreatureSee(Npc npc, Creature creature)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player enters the game.
	 * @param player this parameter contains a reference to the exact instance of the player who is entering to the world.
	 * @return
	 */
	public String onEnterWorld(Player player)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a character enters a registered zone.
	 * @param creature this parameter contains a reference to the exact instance of the character who is entering the zone.
	 * @param zone this parameter contains a reference to the zone.
	 * @return
	 */
	public String onEnterZone(Creature creature, ZoneType zone)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a character exits a registered zone.
	 * @param creature this parameter contains a reference to the exact instance of the character who is exiting the zone.
	 * @param zone this parameter contains a reference to the zone.
	 * @return
	 */
	public String onExitZone(Creature creature, ZoneType zone)
	{
		return null;
	}
	
	/**
	 * @param eventName - name of event
	 * @param sender - NPC, who sent event
	 * @param receiver - NPC, who received event
	 * @param reference - WorldObject to pass, if needed
	 * @return
	 */
	public String onEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player wins an Olympiad Game.
	 * @param winner in this match.
	 * @param looser in this match.
	 * @param type the competition type.
	 */
	public void onOlympiadMatchFinish(Participant winner, Participant looser, CompetitionType type)
	{
	}
	
	/**
	 * This function is called whenever a player looses an Olympiad Game.
	 * @param loser this parameter contains a reference to the exact instance of the player who lose the competition.
	 * @param type this parameter contains a reference to the competition type.
	 */
	public void onOlympiadLose(Player loser, CompetitionType type)
	{
	}
	
	/**
	 * This function is called whenever a NPC finishes moving
	 * @param npc registered NPC
	 */
	public void onMoveFinished(Npc npc)
	{
	}
	
	/**
	 * This function is called whenever a walker NPC (controlled by WalkingManager) arrive a walking node
	 * @param npc registered NPC
	 */
	public void onNodeArrived(Npc npc)
	{
	}
	
	/**
	 * This function is called whenever a walker NPC (controlled by WalkingManager) arrive to last node
	 * @param npc registered NPC
	 */
	public void onRouteFinished(Npc npc)
	{
	}
	
	/**
	 * @param mob
	 * @param player
	 * @param isSummon
	 * @return {@code true} if npc can hate the playable, {@code false} otherwise.
	 */
	public boolean onNpcHate(Attackable mob, Player player, boolean isSummon)
	{
		return true;
	}
	
	/**
	 * @param summon
	 */
	public void onSummonSpawn(Summon summon)
	{
	}
	
	/**
	 * @param summon
	 */
	public void onSummonTalk(Summon summon)
	{
	}
	
	/**
	 * This listener is called when instance world is created.
	 * @param instance created instance world
	 * @param player player who create instance world
	 */
	public void onInstanceCreated(Instance instance, Player player)
	{
	}
	
	/**
	 * This listener is called when instance being destroyed.
	 * @param instance instance world which will be destroyed
	 */
	public void onInstanceDestroy(Instance instance)
	{
	}
	
	/**
	 * This listener is called when player enter into instance.
	 * @param player player who enter
	 * @param instance instance where player enter
	 */
	public void onInstanceEnter(Player player, Instance instance)
	{
	}
	
	/**
	 * This listener is called when player leave instance.
	 * @param player player who leaved
	 * @param instance instance which player leaved
	 */
	public void onInstanceLeave(Player player, Instance instance)
	{
	}
	
	/**
	 * This listener is called when NPC {@code npc} being despawned.
	 * @param npc NPC which will be despawned
	 */
	public void onNpcDespawn(Npc npc)
	{
	}
	
	/**
	 * @param npc
	 * @param player
	 * @return {@code true} if player can see this npc, {@code false} otherwise.
	 */
	public boolean onCanSeeMe(Npc npc, Player player)
	{
		return false;
	}
	
	/**
	 * Show an error message to the specified player.
	 * @param player the player to whom to send the error (must be a GM)
	 * @param t the {@link Throwable} to get the message/stacktrace from
	 * @return {@code false}
	 */
	public boolean showError(Player player, Throwable t)
	{
		LOGGER.log(Level.WARNING, getScriptFile().toAbsolutePath().toString(), t);
		if (t.getMessage() == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": " + t.getMessage());
		}
		if ((player != null) && player.getAccessLevel().isGm())
		{
			final String res = "<html><body><title>Script error</title>" + CommonUtil.getStackTrace(t) + "</body></html>";
			return showResult(player, res);
		}
		return false;
	}
	
	/**
	 * @param player the player to whom to show the result
	 * @param res the message to show to the player
	 * @return {@code false} if the message was sent, {@code true} otherwise
	 * @see #showResult(Player, String, Npc)
	 */
	public boolean showResult(Player player, String res)
	{
		return showResult(player, res, null);
	}
	
	/**
	 * Show a message to the specified player.<br>
	 * <u><i>Concept:</i></u><br>
	 * Three cases are managed according to the value of the {@code res} parameter:<br>
	 * <ul>
	 * <li><u>{@code res} ends with ".htm" or ".html":</u> the contents of the specified HTML file are shown in a dialog window</li>
	 * <li><u>{@code res} starts with "&lt;html&gt;":</u> the contents of the parameter are shown in a dialog window</li>
	 * <li><u>all other cases :</u> the text contained in the parameter is shown in chat</li>
	 * </ul>
	 * @param player the player to whom to show the result
	 * @param npc npc to show the result for
	 * @param res the message to show to the player
	 * @return {@code false} if the message was sent, {@code true} otherwise
	 */
	public boolean showResult(Player player, String res, Npc npc)
	{
		if ((res == null) || res.isEmpty() || (player == null))
		{
			return true;
		}
		
		if (res.endsWith(".htm") || res.endsWith(".html"))
		{
			showHtmlFile(player, res, npc);
		}
		else if (res.startsWith("<html>"))
		{
			final NpcHtmlMessage npcReply = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0, res);
			if (npc != null)
			{
				npcReply.replace("%objectId%", npc.getObjectId());
			}
			npcReply.replace("%playername%", player.getName());
			player.sendPacket(npcReply);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			player.sendMessage(res);
		}
		return false;
	}
	
	/**
	 * Loads all quest states and variables for the specified player.
	 * @param player the player who is entering the world
	 */
	public static void playerEnter(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement invalidQuestData = con.prepareStatement("DELETE FROM character_quests WHERE charId = ? AND name = ?");
			PreparedStatement invalidQuestDataVar = con.prepareStatement("DELETE FROM character_quests WHERE charId = ? AND name = ? AND var = ?");
			PreparedStatement ps1 = con.prepareStatement("SELECT name, value FROM character_quests WHERE charId = ? AND var = ?"))
		{
			// Get list of quests owned by the player from database
			ps1.setInt(1, player.getObjectId());
			ps1.setString(2, "<state>");
			try (ResultSet rs = ps1.executeQuery())
			{
				while (rs.next())
				{
					// Get the ID of the quest and its state
					final String questId = rs.getString("name");
					final String statename = rs.getString("value");
					
					// Search quest associated with the ID
					final Quest q = QuestManager.getInstance().getQuest(questId);
					if (q == null)
					{
						LOGGER.finer("Unknown quest " + questId + " for " + player);
						if (Config.AUTODELETE_INVALID_QUEST_DATA)
						{
							invalidQuestData.setInt(1, player.getObjectId());
							invalidQuestData.setString(2, questId);
							invalidQuestData.executeUpdate();
						}
						continue;
					}
					
					// Create a new QuestState for the player that will be added to the player's list of quests
					new QuestState(q, player, State.getStateId(statename));
				}
			}
			
			// Get list of quests owned by the player from the DB in order to add variables used in the quest.
			try (PreparedStatement ps2 = con.prepareStatement("SELECT name, var, value FROM character_quests WHERE charId = ? AND var <> ?"))
			{
				ps2.setInt(1, player.getObjectId());
				ps2.setString(2, "<state>");
				try (ResultSet rs = ps2.executeQuery())
				{
					while (rs.next())
					{
						final String questId = rs.getString("name");
						final String var = rs.getString("var");
						final String value = rs.getString("value");
						// Get the QuestState saved in the loop before
						final QuestState qs = player.getQuestState(questId);
						if (qs == null)
						{
							LOGGER.finer("Lost variable " + var + " in quest " + questId + " for " + player);
							if (Config.AUTODELETE_INVALID_QUEST_DATA)
							{
								invalidQuestDataVar.setInt(1, player.getObjectId());
								invalidQuestDataVar.setString(2, questId);
								invalidQuestDataVar.setString(3, var);
								invalidQuestDataVar.executeUpdate();
							}
							continue;
						}
						// Add parameter to the quest
						qs.setInternal(var, value);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not insert char quest:", e);
		}
	}
	
	/**
	 * Insert in the database the quest for the player.
	 * @param qs the {@link QuestState} object whose variable to insert
	 * @param var the name of the variable
	 * @param value the value of the variable
	 */
	public static void createQuestVarInDb(QuestState qs, String var, String value)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_quests (charId,name,var,value) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE value=?"))
		{
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.setString(4, value);
			statement.setString(5, value);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not insert char quest:", e);
		}
	}
	
	/**
	 * Update the value of the variable "var" for the specified quest in database
	 * @param qs the {@link QuestState} object whose variable to update
	 * @param var the name of the variable
	 * @param value the value of the variable
	 */
	public static void updateQuestVarInDb(QuestState qs, String var, String value)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE character_quests SET value=? WHERE charId=? AND name=? AND var = ?"))
		{
			statement.setString(1, value);
			statement.setInt(2, qs.getPlayer().getObjectId());
			statement.setString(3, qs.getQuestName());
			statement.setString(4, var);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not update char quest:", e);
		}
	}
	
	/**
	 * Delete a variable of player's quest from the database.
	 * @param qs the {@link QuestState} object whose variable to delete
	 * @param var the name of the variable to delete
	 */
	public static void deleteQuestVarInDb(QuestState qs, String var)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_quests WHERE charId=? AND name=? AND var=?"))
		{
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Unable to delete char quest!", e);
		}
	}
	
	/**
	 * Delete from the database all variables and states of the specified quest state.
	 * @param qs the {@link QuestState} object whose variables to delete
	 * @param repeatable if {@code false}, the state variable will be preserved, otherwise it will be deleted as well
	 */
	public static void deleteQuestInDb(QuestState qs, boolean repeatable)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(repeatable ? QUEST_DELETE_FROM_CHAR_QUERY : QUEST_DELETE_FROM_CHAR_QUERY_NON_REPEATABLE_QUERY))
		{
			ps.setInt(1, qs.getPlayer().getObjectId());
			ps.setString(2, qs.getQuestName());
			if (!repeatable)
			{
				ps.setString(3, "<state>");
			}
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not delete char quest:", e);
		}
	}
	
	/**
	 * Create a database record for the specified quest state.
	 * @param qs the {@link QuestState} object whose data to write in the database
	 */
	public static void createQuestInDb(QuestState qs)
	{
		createQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
	}
	
	/**
	 * Update a quest state record of the specified quest state in database.
	 * @param qs the {@link QuestState} object whose data to update in the database
	 */
	public static void updateQuestInDb(QuestState qs)
	{
		updateQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
	}
	
	/**
	 * @param player the player whose language settings to use in finding the html of the right language
	 * @return the default html for when no quest is available: "You are either not on a quest that involves this NPC.."
	 */
	public static String getNoQuestMsg(Player player)
	{
		final String result = HtmCache.getInstance().getHtm(player, "data/html/noquest.htm");
		if ((result != null) && (result.length() > 0))
		{
			return result;
		}
		return DEFAULT_NO_QUEST_MSG;
	}
	
	/**
	 * @param player the player whose language settings to use in finding the html of the right language
	 * @return the default html for when player don't have minimum level for reward: "You cannot receive quest rewards as your character.."
	 */
	public static String getNoQuestLevelRewardMsg(Player player)
	{
		return HtmCache.getInstance().getHtm(player, "data/html/noquestlevelreward.html");
	}
	
	/**
	 * @param player the player whose language settings to use in finding the html of the right language
	 * @return the default html for when quest is already completed
	 */
	public static String getAlreadyCompletedMsg(Player player)
	{
		return getAlreadyCompletedMsg(player, QuestType.ONE_TIME);
	}
	
	/**
	 * @param player the player whose language settings to use in finding the html of the right language
	 * @param type the Quest type
	 * @return the default html for when quest is already completed
	 */
	public static String getAlreadyCompletedMsg(Player player, QuestType type)
	{
		return HtmCache.getInstance().getHtm(player, (type == QuestType.ONE_TIME ? "data/html/alreadyCompleted.html" : "data/html/alreadyCompletedDaily.html"));
	}
	
	// TODO: Clean up these methods
	public void addStartNpc(int npcId)
	{
		setNpcQuestStartId(npcId);
	}
	
	public void addFirstTalkId(int npcId)
	{
		setNpcFirstTalkId(event -> notifyFirstTalk(event.getNpc(), event.getPlayer()), npcId);
	}
	
	public void addTalkId(int npcId)
	{
		setNpcTalkId(npcId);
	}
	
	public void addKillId(int npcId)
	{
		setAttackableKillId(kill -> notifyKill(kill.getTarget(), kill.getAttacker(), kill.isSummon()), npcId);
	}
	
	public void addAttackId(int npcId)
	{
		setAttackableAttackId(attack -> notifyAttack(attack.getTarget(), attack.getAttacker(), attack.getDamage(), attack.isSummon(), attack.getSkill()), npcId);
	}
	
	/**
	 * Add the quest to the NPC's startQuest
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addStartNpc(int... npcIds)
	{
		setNpcQuestStartId(npcIds);
	}
	
	/**
	 * Add the quest to the NPC's startQuest
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addStartNpc(Collection<Integer> npcIds)
	{
		setNpcQuestStartId(npcIds);
	}
	
	/**
	 * Add the quest to the NPC's first-talk (default action dialog).
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addFirstTalkId(int... npcIds)
	{
		setNpcFirstTalkId(event -> notifyFirstTalk(event.getNpc(), event.getPlayer()), npcIds);
	}
	
	/**
	 * Add the quest to the NPC's first-talk (default action dialog).
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addFirstTalkId(Collection<Integer> npcIds)
	{
		setNpcFirstTalkId(event -> notifyFirstTalk(event.getNpc(), event.getPlayer()), npcIds);
	}
	
	/**
	 * Add the NPC to the AcquireSkill dialog.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAcquireSkillId(int... npcIds)
	{
		setPlayerSkillLearnId(event -> notifyAcquireSkill(event.getTrainer(), event.getPlayer(), event.getSkill(), event.getAcquireType()), npcIds);
	}
	
	/**
	 * Add the NPC to the AcquireSkill dialog.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAcquireSkillId(Collection<Integer> npcIds)
	{
		setPlayerSkillLearnId(event -> notifyAcquireSkill(event.getTrainer(), event.getPlayer(), event.getSkill(), event.getAcquireType()), npcIds);
	}
	
	/**
	 * Add the Item to the notify when player speaks with it.
	 * @param itemIds the IDs of the Item to register
	 */
	public void addItemBypassEventId(int... itemIds)
	{
		setItemBypassEvenId(event -> notifyItemEvent(event.getItem(), event.getPlayer(), event.getEvent()), itemIds);
	}
	
	/**
	 * Add the Item to the notify when player speaks with it.
	 * @param itemIds the IDs of the Item to register
	 */
	public void addItemBypassEventId(Collection<Integer> itemIds)
	{
		setItemBypassEvenId(event -> notifyItemEvent(event.getItem(), event.getPlayer(), event.getEvent()), itemIds);
	}
	
	/**
	 * Add the Item to the notify when player speaks with it.
	 * @param itemIds the IDs of the Item to register
	 */
	public void addItemTalkId(int... itemIds)
	{
		setItemTalkId(event -> notifyItemTalk(event.getItem(), event.getPlayer()), itemIds);
	}
	
	/**
	 * Add the Item to the notify when player speaks with it.
	 * @param itemIds the IDs of the Item to register
	 */
	public void addItemTalkId(Collection<Integer> itemIds)
	{
		setItemTalkId(event -> notifyItemTalk(event.getItem(), event.getPlayer()), itemIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for attack events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAttackId(int... npcIds)
	{
		setAttackableAttackId(attack -> notifyAttack(attack.getTarget(), attack.getAttacker(), attack.getDamage(), attack.isSummon(), attack.getSkill()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for attack events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAttackId(Collection<Integer> npcIds)
	{
		setAttackableAttackId(attack -> notifyAttack(attack.getTarget(), attack.getAttacker(), attack.getDamage(), attack.isSummon(), attack.getSkill()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for kill events.
	 * @param npcIds
	 */
	public void addKillId(int... npcIds)
	{
		setAttackableKillId(kill -> notifyKill(kill.getTarget(), kill.getAttacker(), kill.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest event to the collection of NPC IDs that will respond to for on kill events.
	 * @param npcIds the collection of NPC IDs
	 */
	public void addKillId(Collection<Integer> npcIds)
	{
		setAttackableKillId(kill -> notifyKill(kill.getTarget(), kill.getAttacker(), kill.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Talk Events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addTalkId(int... npcIds)
	{
		setNpcTalkId(npcIds);
	}
	
	public void addTalkId(Collection<Integer> npcIds)
	{
		setNpcTalkId(npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Teleport Events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addTeleportId(int... npcIds)
	{
		setNpcTeleportId(event -> notifyTeleport(event.getNpc()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Teleport Events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addTeleportId(Collection<Integer> npcIds)
	{
		setNpcTeleportId(event -> notifyTeleport(event.getNpc()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for spawn events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSpawnId(int... npcIds)
	{
		setNpcSpawnId(event -> notifySpawn(event.getNpc()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for spawn events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSpawnId(Collection<Integer> npcIds)
	{
		setNpcSpawnId(event -> notifySpawn(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onNpcDespawn to NPCs.
	 * @param npcIds
	 */
	public void addDespawnId(int... npcIds)
	{
		setNpcDespawnId(event -> onNpcDespawn(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onNpcDespawn to NPCs.
	 * @param npcIds
	 */
	public void addDespawnId(Collection<Integer> npcIds)
	{
		setNpcDespawnId(event -> onNpcDespawn(event.getNpc()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for skill see events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSkillSeeId(int... npcIds)
	{
		setNpcSkillSeeId(event -> notifySkillSee(event.getTarget(), event.getCaster(), event.getSkill(), event.getTargets(), event.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for skill see events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSkillSeeId(Collection<Integer> npcIds)
	{
		setNpcSkillSeeId(event -> notifySkillSee(event.getTarget(), event.getCaster(), event.getSkill(), event.getTargets(), event.isSummon()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSpellFinishedId(int... npcIds)
	{
		setNpcSkillFinishedId(event -> notifySpellFinished(event.getCaster(), event.getTarget(), event.getSkill()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSpellFinishedId(Collection<Integer> npcIds)
	{
		setNpcSkillFinishedId(event -> notifySpellFinished(event.getCaster(), event.getTarget(), event.getSkill()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addTrapActionId(int... npcIds)
	{
		setTrapActionId(event -> notifyTrapAction(event.getTrap(), event.getTrigger(), event.getAction()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addTrapActionId(Collection<Integer> npcIds)
	{
		setTrapActionId(event -> notifyTrapAction(event.getTrap(), event.getTrigger(), event.getAction()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for faction call events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addFactionCallId(int... npcIds)
	{
		setAttackableFactionIdId(event -> notifyFactionCall(event.getNpc(), event.getCaller(), event.getAttacker(), event.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for faction call events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addFactionCallId(Collection<Integer> npcIds)
	{
		setAttackableFactionIdId(event -> notifyFactionCall(event.getNpc(), event.getCaller(), event.getAttacker(), event.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for character see events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAggroRangeEnterId(int... npcIds)
	{
		setAttackableAggroRangeEnterId(event -> notifyAggroRangeEnter(event.getNpc(), event.getPlayer(), event.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for character see events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAggroRangeEnterId(Collection<Integer> npcIds)
	{
		setAttackableAggroRangeEnterId(event -> notifyAggroRangeEnter(event.getNpc(), event.getPlayer(), event.isSummon()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addCreatureSeeId(int... npcIds)
	{
		setCreatureSeeId(event -> notifyCreatureSee(event.getCreature().asNpc(), event.getSeen()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addCreatureSeeId(Collection<Integer> npcIds)
	{
		setCreatureSeeId(event -> notifyCreatureSee(event.getCreature().asNpc(), event.getSeen()), npcIds);
	}
	
	/**
	 * Register onEnterZone trigger for zone
	 * @param zoneId the ID of the zone to register
	 */
	public void addEnterZoneId(int zoneId)
	{
		setCreatureZoneEnterId(event -> notifyEnterZone(event.getCreature(), event.getZone()), zoneId);
	}
	
	/**
	 * Register onEnterZone trigger for zones
	 * @param zoneIds the IDs of the zones to register
	 */
	public void addEnterZoneId(int... zoneIds)
	{
		setCreatureZoneEnterId(event -> notifyEnterZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Register onEnterZone trigger for zones
	 * @param zoneIds the IDs of the zones to register
	 */
	public void addEnterZoneId(Collection<Integer> zoneIds)
	{
		setCreatureZoneEnterId(event -> notifyEnterZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Register onExitZone trigger for zone
	 * @param zoneId the ID of the zone to register
	 */
	public void addExitZoneId(int zoneId)
	{
		setCreatureZoneExitId(event -> notifyExitZone(event.getCreature(), event.getZone()), zoneId);
	}
	
	/**
	 * Register onExitZone trigger for zones
	 * @param zoneIds the IDs of the zones to register
	 */
	public void addExitZoneId(int... zoneIds)
	{
		setCreatureZoneExitId(event -> notifyExitZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Register onExitZone trigger for zones
	 * @param zoneIds the IDs of the zones to register
	 */
	public void addExitZoneId(Collection<Integer> zoneIds)
	{
		setCreatureZoneExitId(event -> notifyExitZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Register onEventReceived trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addEventReceivedId(int... npcIds)
	{
		setNpcEventReceivedId(event -> notifyEventReceived(event.getEventName(), event.getSender(), event.getReceiver(), event.getReference()), npcIds);
	}
	
	/**
	 * Register onEventReceived trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addEventReceivedId(Collection<Integer> npcIds)
	{
		setNpcEventReceivedId(event -> notifyEventReceived(event.getEventName(), event.getSender(), event.getReceiver(), event.getReference()), npcIds);
	}
	
	/**
	 * Register onMoveFinished trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addMoveFinishedId(int... npcIds)
	{
		setNpcMoveFinishedId(event -> notifyMoveFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onMoveFinished trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addMoveFinishedId(Collection<Integer> npcIds)
	{
		setNpcMoveFinishedId(event -> notifyMoveFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onNodeArrived trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addNodeArrivedId(int... npcIds)
	{
		setNpcMoveNodeArrivedId(event -> notifyNodeArrived(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onNodeArrived trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addNodeArrivedId(Collection<Integer> npcIds)
	{
		setNpcMoveNodeArrivedId(event -> notifyNodeArrived(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onRouteFinished trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addRouteFinishedId(int... npcIds)
	{
		setNpcMoveRouteFinishedId(event -> notifyRouteFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onRouteFinished trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addRouteFinishedId(Collection<Integer> npcIds)
	{
		setNpcMoveRouteFinishedId(event -> notifyRouteFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onNpcHate trigger for NPC
	 * @param npcIds
	 */
	public void addNpcHateId(int... npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!onNpcHate(event.getNpc(), event.getPlayer(), event.isSummon()), false, false), npcIds);
	}
	
	/**
	 * Register onNpcHate trigger for NPC
	 * @param npcIds
	 */
	public void addNpcHateId(Collection<Integer> npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!onNpcHate(event.getNpc(), event.getPlayer(), event.isSummon()), false, false), npcIds);
	}
	
	/**
	 * Register onSummonSpawn trigger when summon is spawned.
	 * @param npcIds
	 */
	public void addSummonSpawnId(int... npcIds)
	{
		setPlayerSummonSpawnId(event -> onSummonSpawn(event.getSummon()), npcIds);
	}
	
	/**
	 * Register onSummonSpawn trigger when summon is spawned.
	 * @param npcIds
	 */
	public void addSummonSpawnId(Collection<Integer> npcIds)
	{
		setPlayerSummonSpawnId(event -> onSummonSpawn(event.getSummon()), npcIds);
	}
	
	/**
	 * Register onSummonTalk trigger when master talked to summon.
	 * @param npcIds
	 */
	public void addSummonTalkId(int... npcIds)
	{
		setPlayerSummonTalkId(event -> onSummonTalk(event.getSummon()), npcIds);
	}
	
	/**
	 * Register onSummonTalk trigger when summon is spawned.
	 * @param npcIds
	 */
	public void addSummonTalkId(Collection<Integer> npcIds)
	{
		setPlayerSummonTalkId(event -> onSummonTalk(event.getSummon()), npcIds);
	}
	
	/**
	 * Registers onCanSeeMe trigger whenever an npc info must be sent to player.
	 * @param npcIds
	 */
	public void addCanSeeMeId(int... npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!notifyOnCanSeeMe(event.getNpc(), event.getPlayer()), false, false), npcIds);
	}
	
	/**
	 * Registers onCanSeeMe trigger whenever an npc info must be sent to player.
	 * @param npcIds
	 */
	public void addCanSeeMeId(Collection<Integer> npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!notifyOnCanSeeMe(event.getNpc(), event.getPlayer()), false, false), npcIds);
	}
	
	public void addOlympiadMatchFinishId()
	{
		setOlympiadMatchResult(event -> notifyOlympiadMatch(event.getWinner(), event.getLoser(), event.getCompetitionType()));
	}
	
	/**
	 * Register onInstanceCreated trigger when instance is created.
	 * @param templateIds
	 */
	public void addInstanceCreatedId(int... templateIds)
	{
		setInstanceCreatedId(event -> onInstanceCreated(event.getInstanceWorld(), event.getCreator()), templateIds);
	}
	
	/**
	 * Register onInstanceCreated trigger when instance is created.
	 * @param templateIds
	 */
	public void addInstanceCreatedId(Collection<Integer> templateIds)
	{
		setInstanceCreatedId(event -> onInstanceCreated(event.getInstanceWorld(), event.getCreator()), templateIds);
	}
	
	/**
	 * Register onInstanceDestroy trigger when instance is destroyed.
	 * @param templateIds
	 */
	public void addInstanceDestroyId(int... templateIds)
	{
		setInstanceDestroyId(event -> onInstanceDestroy(event.getInstanceWorld()), templateIds);
	}
	
	/**
	 * Register onInstanceCreate trigger when instance is destroyed.
	 * @param templateIds
	 */
	public void addInstanceDestroyId(Collection<Integer> templateIds)
	{
		setInstanceDestroyId(event -> onInstanceDestroy(event.getInstanceWorld()), templateIds);
	}
	
	/**
	 * Register onInstanceEnter trigger when player enter into instance.
	 * @param templateIds
	 */
	public void addInstanceEnterId(int... templateIds)
	{
		setInstanceEnterId(event -> onInstanceEnter(event.getPlayer(), event.getInstanceWorld()), templateIds);
	}
	
	/**
	 * Register onInstanceEnter trigger when player enter into instance.
	 * @param templateIds
	 */
	public void addInstanceEnterId(Collection<Integer> templateIds)
	{
		setInstanceEnterId(event -> onInstanceEnter(event.getPlayer(), event.getInstanceWorld()), templateIds);
	}
	
	/**
	 * Register onInstanceEnter trigger when player leave from instance.
	 * @param templateIds
	 */
	public void addInstanceLeaveId(int... templateIds)
	{
		setInstanceLeaveId(event -> onInstanceLeave(event.getPlayer(), event.getInstanceWorld()), templateIds);
	}
	
	/**
	 * Register onInstanceEnter trigger when player leave from instance.
	 * @param templateIds
	 */
	public void addInstanceLeaveId(Collection<Integer> templateIds)
	{
		setInstanceLeaveId(event -> onInstanceLeave(event.getPlayer(), event.getInstanceWorld()), templateIds);
	}
	
	/**
	 * Use this method to get a random party member from a player's party.<br>
	 * Useful when distributing rewards after killing an NPC.
	 * @param player this parameter represents the player whom the party will taken.
	 * @return {@code null} if {@code player} is {@code null}, {@code player} itself if the player does not have a party, and a random party member in all other cases
	 */
	public Player getRandomPartyMember(Player player)
	{
		if (player == null)
		{
			return null;
		}
		final Party party = player.getParty();
		if ((party == null) || (party.getMembers().isEmpty()))
		{
			return player;
		}
		return party.getMembers().get(Rnd.get(party.getMembers().size()));
	}
	
	/**
	 * Get a random party member with required cond value.
	 * @param player the instance of a player whose party is to be searched
	 * @param cond the value of the "cond" variable that must be matched
	 * @return a random party member that matches the specified condition, or {@code null} if no match was found
	 */
	public Player getRandomPartyMember(Player player, int cond)
	{
		return getRandomPartyMember(player, "cond", String.valueOf(cond));
	}
	
	/**
	 * Auxiliary function for party quests.<br>
	 * Note: This function is only here because of how commonly it may be used by quest developers.<br>
	 * For any variations on this function, the quest script can always handle things on its own.
	 * @param player the instance of a player whose party is to be searched
	 * @param var the quest variable to look for in party members. If {@code null}, it simply unconditionally returns a random party member
	 * @param value the value of the specified quest variable the random party member must have
	 * @return a random party member that matches the specified conditions or {@code null} if no match was found.<br>
	 *         If the {@code var} parameter is {@code null}, a random party member is selected without any conditions.<br>
	 *         The party member must be within a range of 1500 ingame units of the target of the reference player, or, if no target exists, within the same range of the player itself
	 */
	public Player getRandomPartyMember(Player player, String var, String value)
	{
		// if no valid player instance is passed, there is nothing to check...
		if (player == null)
		{
			return null;
		}
		
		// for null var condition, return any random party member.
		if (var == null)
		{
			return getRandomPartyMember(player);
		}
		
		// normal cases...if the player is not in a party, check the player's state
		QuestState temp = null;
		final Party party = player.getParty();
		// if this player is not in a party, just check if this player instance matches the conditions itself
		if ((party == null) || (party.getMembers().isEmpty()))
		{
			temp = player.getQuestState(getName());
			if ((temp != null) && temp.isSet(var) && temp.get(var).equalsIgnoreCase(value))
			{
				return player; // match
			}
			return null; // no match
		}
		
		// if the player is in a party, gather a list of all matching party members (possibly including this player)
		final List<Player> candidates = new ArrayList<>();
		// get the target for enforcing distance limitations.
		WorldObject target = player.getTarget();
		if (target == null)
		{
			target = player;
		}
		
		for (Player partyMember : party.getMembers())
		{
			if (partyMember == null)
			{
				continue;
			}
			temp = partyMember.getQuestState(getName());
			if ((temp != null) && (temp.get(var) != null) && (temp.get(var)).equalsIgnoreCase(value) && partyMember.isInsideRadius3D(target, Config.ALT_PARTY_RANGE))
			{
				candidates.add(partyMember);
			}
		}
		// if there was no match, return null...
		if (candidates.isEmpty())
		{
			return null;
		}
		// if a match was found from the party, return one of them at random.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	/**
	 * Auxiliary function for party quests.<br>
	 * Note: This function is only here because of how commonly it may be used by quest developers.<br>
	 * For any variations on this function, the quest script can always handle things on its own.
	 * @param player the player whose random party member is to be selected
	 * @param state the quest state required of the random party member
	 * @return {@code null} if nothing was selected or a random party member that has the specified quest state
	 */
	public Player getRandomPartyMemberState(Player player, byte state)
	{
		// if no valid player instance is passed, there is nothing to check...
		if (player == null)
		{
			return null;
		}
		
		// normal cases...if the player is not in a party check the player's state
		QuestState temp = null;
		final Party party = player.getParty();
		// if this player is not in a party, just check if this player instance matches the conditions itself
		if ((party == null) || (party.getMembers().isEmpty()))
		{
			temp = player.getQuestState(getName());
			if ((temp != null) && (temp.getState() == state))
			{
				return player; // match
			}
			
			return null; // no match
		}
		
		// if the player is in a party, gather a list of all matching party members (possibly
		// including this player)
		final List<Player> candidates = new ArrayList<>();
		
		// get the target for enforcing distance limitations.
		WorldObject target = player.getTarget();
		if (target == null)
		{
			target = player;
		}
		
		for (Player partyMember : party.getMembers())
		{
			if (partyMember == null)
			{
				continue;
			}
			temp = partyMember.getQuestState(getName());
			if ((temp != null) && (temp.getState() == state) && partyMember.isInsideRadius3D(target, Config.ALT_PARTY_RANGE))
			{
				candidates.add(partyMember);
			}
		}
		// if there was no match, return null...
		if (candidates.isEmpty())
		{
			return null;
		}
		
		// if a match was found from the party, return one of them at random.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	/**
	 * Get a random party member from the specified player's party.<br>
	 * If the player is not in a party, only the player himself is checked.<br>
	 * The lucky member is chosen by standard loot roll rules -<br>
	 * each member rolls a random number, the one with the highest roll wins.
	 * @param player the player whose party to check
	 * @param npc the NPC used for distance and other checks (if {@link #checkPartyMember(Player, Npc)} is overriden)
	 * @return the random party member or {@code null}
	 */
	public Player getRandomPartyMember(Player player, Npc npc)
	{
		if ((player == null) || !checkDistanceToTarget(player, npc))
		{
			return null;
		}
		final Party party = player.getParty();
		Player luckyPlayer = null;
		if (party == null)
		{
			if (checkPartyMember(player, npc))
			{
				luckyPlayer = player;
			}
		}
		else
		{
			int highestRoll = 0;
			for (Player member : party.getMembers())
			{
				final int rnd = getRandom(1000);
				if ((rnd > highestRoll) && checkPartyMember(member, npc))
				{
					highestRoll = rnd;
					luckyPlayer = member;
				}
			}
		}
		return (luckyPlayer != null) && checkDistanceToTarget(luckyPlayer, npc) ? luckyPlayer : null;
	}
	
	/**
	 * This method is called for every party member in {@link #getRandomPartyMember(Player, Npc)}.<br>
	 * It is intended to be overriden by the specific quest implementations.
	 * @param player the player to check
	 * @param npc the NPC that was passed to {@link #getRandomPartyMember(Player, Npc)}
	 * @return {@code true} if this party member passes the check, {@code false} otherwise
	 */
	public boolean checkPartyMember(Player player, Npc npc)
	{
		return true;
	}
	
	/**
	 * Get a random party member from the player's party who has this quest at the specified quest progress.<br>
	 * If the player is not in a party, only the player himself is checked.
	 * @param player the player whose random party member state to get
	 * @param condition the quest progress step the random member should be at (-1 = check only if quest is started)
	 * @param playerChance how many times more chance does the player get compared to other party members (3 - 3x more chance).<br>
	 *            On retail servers, the killer usually gets 2-3x more chance than other party members
	 * @param target the NPC to use for the distance check (can be null)
	 * @return the {@link QuestState} object of the random party member or {@code null} if none matched the condition
	 */
	public QuestState getRandomPartyMemberState(Player player, int condition, int playerChance, Npc target)
	{
		if ((player == null) || (playerChance < 1))
		{
			return null;
		}
		
		QuestState qs = player.getQuestState(getName());
		if (!player.isInParty())
		{
			return !checkPartyMemberConditions(qs, condition, target) || !checkDistanceToTarget(player, target) ? null : qs;
		}
		
		final List<QuestState> candidates = new ArrayList<>();
		if (checkPartyMemberConditions(qs, condition, target) && (playerChance > 0))
		{
			for (int i = 0; i < playerChance; i++)
			{
				candidates.add(qs);
			}
		}
		
		for (Player member : player.getParty().getMembers())
		{
			if (member == player)
			{
				continue;
			}
			
			qs = member.getQuestState(getName());
			if (checkPartyMemberConditions(qs, condition, target))
			{
				candidates.add(qs);
			}
		}
		
		if (candidates.isEmpty())
		{
			return null;
		}
		
		qs = candidates.get(getRandom(candidates.size()));
		return !checkDistanceToTarget(qs.getPlayer(), target) ? null : qs;
	}
	
	private boolean checkPartyMemberConditions(QuestState qs, int condition, Npc npc)
	{
		return (qs != null) && ((condition == -1) ? qs.isStarted() : qs.isCond(condition)) && checkPartyMember(qs, npc);
	}
	
	private static boolean checkDistanceToTarget(Player player, Npc target)
	{
		return (target == null) || Util.checkIfInRange(Config.ALT_PARTY_RANGE, player, target, true);
	}
	
	/**
	 * This method is called for every party member in {@link #getRandomPartyMemberState(Player, int, int, Npc)} if/after all the standard checks are passed.<br>
	 * It is intended to be overriden by the specific quest implementations.<br>
	 * It can be used in cases when there are more checks performed than simply a quest condition check,<br>
	 * for example, if an item is required in the player's inventory.
	 * @param qs the {@link QuestState} object of the party member
	 * @param npc the NPC that was passed as the last parameter to {@link #getRandomPartyMemberState(Player, int, int, Npc)}
	 * @return {@code true} if this party member passes the check, {@code false} otherwise
	 */
	public boolean checkPartyMember(QuestState qs, Npc npc)
	{
		return true;
	}
	
	/**
	 * Send an HTML file to the specified player.
	 * @param player the player to send the HTML to
	 * @param filename the name of the HTML file to show
	 * @return the contents of the HTML file that was sent to the player
	 * @see #showHtmlFile(Player, String, Npc)
	 */
	public String showHtmlFile(Player player, String filename)
	{
		return showHtmlFile(player, filename, null);
	}
	
	/**
	 * Send an HTML file to the specified player.
	 * @param player the player to send the HTML file to
	 * @param filename the name of the HTML file to show
	 * @param npc the NPC that is showing the HTML file
	 * @return the contents of the HTML file that was sent to the player
	 * @see #showHtmlFile(Player, String, Npc)
	 */
	public String showHtmlFile(Player player, String filename, Npc npc)
	{
		final boolean questwindow = !filename.endsWith(".html");
		
		// Create handler to file linked to the quest
		String content = getHtm(player, filename);
		
		// Send message to client if message not empty
		if (content != null)
		{
			if (npc != null)
			{
				content = content.replace("%objectId%", String.valueOf(npc.getObjectId()));
			}
			
			if (questwindow && (_questId > 0) && (_questId < 20000) && (_questId != 999))
			{
				final NpcQuestHtmlMessage npcReply = new NpcQuestHtmlMessage(npc != null ? npc.getObjectId() : 0, _questId);
				npcReply.setHtml(content);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
			}
			else
			{
				final NpcHtmlMessage npcReply = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0, content);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
			}
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		
		return content;
	}
	
	/**
	 * @param player for language prefix.
	 * @param fileName the html file to be get.
	 * @return the HTML file contents
	 */
	public String getHtm(Player player, String fileName)
	{
		final HtmCache hc = HtmCache.getInstance();
		String content = hc.getHtm(player, fileName.startsWith("data/") ? fileName : "data/scripts/" + getPath() + "/" + fileName);
		if (content == null)
		{
			content = hc.getHtm(player, "data/scripts/" + getPath() + "/" + fileName);
			if (content == null)
			{
				content = hc.getHtm(player, "data/scripts/quests/" + getName() + "/" + fileName);
			}
		}
		return content;
	}
	
	/**
	 * @return the registered quest items IDs.
	 */
	public int[] getRegisteredItemIds()
	{
		return _questItemIds;
	}
	
	/**
	 * Registers all items that have to be destroyed in case player abort the quest or finish it.
	 * @param items
	 */
	public void registerQuestItems(int... items)
	{
		for (int id : items)
		{
			if ((id != 0) && (ItemData.getInstance().getTemplate(id) == null))
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found registerQuestItems for non existing item: " + id + "!");
			}
		}
		_questItemIds = items;
	}
	
	/**
	 * Remove all quest items associated with this quest from the specified player's inventory.
	 * @param player the player whose quest items to remove
	 */
	public void removeRegisteredQuestItems(Player player)
	{
		takeItems(player, -1, _questItemIds);
	}
	
	@Override
	public String getScriptName()
	{
		return getName();
	}
	
	@Override
	public void setActive(boolean status)
	{
		// TODO: Implement me.
	}
	
	@Override
	public boolean reload()
	{
		unload();
		return super.reload();
	}
	
	@Override
	public boolean unload()
	{
		return unload(true);
	}
	
	/**
	 * @param removeFromList
	 * @return
	 */
	public boolean unload(boolean removeFromList)
	{
		onSave();
		
		// Cancel all pending timers before reloading.
		// If timers ought to be restarted, the quest can take care of it with its code (example: save global data indicating what timer must be restarted).
		for (List<QuestTimer> timers : _questTimers.values())
		{
			for (QuestTimer timer : timers)
			{
				timer.cancel();
			}
			timers.clear();
		}
		_questTimers.clear();
		
		if (removeFromList)
		{
			return QuestManager.getInstance().removeScript(this) && super.unload();
		}
		return super.unload();
	}
	
	public void setOnEnterWorld(boolean value)
	{
		if (value)
		{
			setPlayerLoginId(event -> notifyEnterWorld(event.getPlayer()));
		}
		else
		{
			for (AbstractEventListener listener : getListeners())
			{
				if (listener.getType() == EventType.ON_PLAYER_LOGIN)
				{
					listener.unregisterMe();
				}
			}
		}
	}
	
	/**
	 * If a quest is set as custom, it will display it's name in the NPC Quest List.<br>
	 * Retail quests are unhardcoded to display the name using a client string.
	 * @param value if {@code true} the quest script will be set as custom quest.
	 */
	public void setCustom(boolean value)
	{
		_isCustom = value;
	}
	
	/**
	 * Verifies if this is a custom quest.
	 * @return {@code true} if the quest script is a custom quest, {@code false} otherwise.
	 */
	public boolean isCustomQuest()
	{
		return _isCustom;
	}
	
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		return Collections.emptySet();
	}
	
	public <T> boolean isTarget(int[] ids, WorldObject target, Class<T> clazz)
	{
		if ((target != null) && clazz.isInstance(target))
		{
			return CommonUtil.contains(ids, target.getId());
		}
		return false;
	}
	
	public void sendNpcLogList(Player player)
	{
		if (player.getQuestState(getName()) != null)
		{
			final ExQuestNpcLogList packet = new ExQuestNpcLogList(_questId);
			getNpcLogList(player).forEach(packet::add);
			player.sendPacket(packet);
		}
	}
	
	/**
	 * Gets the start conditions.
	 * @return the start conditions
	 */
	private Set<QuestCondition> getStartConditions()
	{
		return _startCondition;
	}
	
	/**
	 * Verifies if the player meets all the start conditions.
	 * @param player the player
	 * @return {@code true} if all conditions are met
	 */
	public boolean canStartQuest(Player player)
	{
		for (QuestCondition cond : _startCondition)
		{
			if (!cond.test(player))
			{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Gets the HTML for the first starting condition not met.
	 * @param player the player
	 * @param npc
	 * @return the HTML
	 */
	public String getStartConditionHtml(Player player, Npc npc)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && !qs.isCreated())
		{
			return null;
		}
		
		for (QuestCondition cond : _startCondition)
		{
			if (!cond.test(player))
			{
				return cond.getHtml(npc);
			}
		}
		
		return null;
	}
	
	/**
	 * Adds a predicate to the start conditions.
	 * @param questStartRequirement the predicate condition
	 * @param html the HTML to display if that condition is not met
	 */
	public void addCondStart(Predicate<Player> questStartRequirement, String html)
	{
		getStartConditions().add(new QuestCondition(questStartRequirement, html));
	}
	
	/**
	 * Adds a predicate to the start conditions.
	 * @param questStartRequirement the predicate condition
	 * @param pairs the HTML to display if the condition is not met per each npc
	 */
	@SafeVarargs
	public final void addCondStart(Predicate<Player> questStartRequirement, KeyValuePair<Integer, String>... pairs)
	{
		getStartConditions().add(new QuestCondition(questStartRequirement, pairs));
	}
	
	/**
	 * Adds a minimum/maximum level start condition to the quest.
	 * @param minLevel the minimum player's level to start the quest
	 * @param maxLevel the maximum player's level to start the quest
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondLevel(int minLevel, int maxLevel, String html)
	{
		addCondStart(p -> (p.getLevel() >= minLevel) && (p.getLevel() <= maxLevel), html);
	}
	
	/**
	 * Adds a minimum/maximum level start condition to the quest.
	 * @param minLevel the minimum player's level to start the quest
	 * @param maxLevel the maximum player's level to start the quest
	 * @param pairs the HTML to display if the condition is not met per each npc
	 */
	@SafeVarargs
	public final void addCondMinLevel(int minLevel, int maxLevel, KeyValuePair<Integer, String>... pairs)
	{
		addCondStart(p -> (p.getLevel() >= minLevel) && (p.getLevel() <= maxLevel), pairs);
	}
	
	/**
	 * Adds a minimum level start condition to the quest.
	 * @param minLevel the minimum player's level to start the quest
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondMinLevel(int minLevel, String html)
	{
		addCondStart(p -> p.getLevel() >= minLevel, html);
	}
	
	/**
	 * Adds a minimum level start condition to the quest.
	 * @param minLevel the minimum player's level to start the quest
	 * @param pairs the HTML to display if the condition is not met per each npc
	 */
	@SafeVarargs
	public final void addCondMinLevel(int minLevel, KeyValuePair<Integer, String>... pairs)
	{
		addCondStart(p -> p.getLevel() >= minLevel, pairs);
	}
	
	/**
	 * Adds a minimum/maximum level start condition to the quest.
	 * @param maxLevel the maximum player's level to start the quest
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondMaxLevel(int maxLevel, String html)
	{
		addCondStart(p -> p.getLevel() <= maxLevel, html);
	}
	
	/**
	 * Adds a minimum/maximum level start condition to the quest.
	 * @param maxLevel the maximum player's level to start the quest
	 * @param pairs the HTML to display if the condition is not met per each npc
	 */
	@SafeVarargs
	public final void addCondMaxLevel(int maxLevel, KeyValuePair<Integer, String>... pairs)
	{
		addCondStart(p -> p.getLevel() <= maxLevel, pairs);
	}
	
	/**
	 * Adds a race start condition to the quest.
	 * @param race the race
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondRace(Race race, String html)
	{
		addCondStart(p -> p.getRace() == race, html);
	}
	
	/**
	 * Adds a race start condition to the quest.
	 * @param race the race
	 * @param pairs the HTML to display if the condition is not met per each npc
	 */
	@SafeVarargs
	public final void addCondRace(Race race, KeyValuePair<Integer, String>... pairs)
	{
		addCondStart(p -> p.getRace() == race, pairs);
	}
	
	/**
	 * Adds a not-race start condition to the quest.
	 * @param race the race
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondNotRace(Race race, String html)
	{
		addCondStart(p -> p.getRace() != race, html);
	}
	
	/**
	 * Adds a not-race start condition to the quest.
	 * @param race the race
	 * @param pairs the HTML to display if the condition is not met per each npc
	 */
	@SafeVarargs
	public final void addCondNotRace(Race race, KeyValuePair<Integer, String>... pairs)
	{
		addCondStart(p -> p.getRace() != race, pairs);
	}
	
	/**
	 * Adds a quest completed start condition to the quest.
	 * @param name the quest name
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondCompletedQuest(String name, String html)
	{
		addCondStart(p -> p.hasQuestState(name) && p.getQuestState(name).isCompleted(), html);
	}
	
	/**
	 * Adds a quest completed start condition to the quest.
	 * @param name the quest name
	 * @param pairs the HTML to display if the condition is not met per each npc
	 */
	@SafeVarargs
	public final void addCondCompletedQuest(String name, KeyValuePair<Integer, String>... pairs)
	{
		addCondStart(p -> p.hasQuestState(name) && p.getQuestState(name).isCompleted(), pairs);
	}
	
	/**
	 * Adds a quest started start condition to the quest.
	 * @param name the quest name
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondStartedQuest(String name, String html)
	{
		addCondStart(p -> p.hasQuestState(name) && p.getQuestState(name).isStarted(), html);
	}
	
	/**
	 * Adds a quest started start condition to the quest.
	 * @param name the quest name
	 * @param pairs the HTML to display if the condition is not met per each npc
	 */
	@SafeVarargs
	public final void addCondStartedQuest(String name, KeyValuePair<Integer, String>... pairs)
	{
		addCondStart(p -> p.hasQuestState(name) && p.getQuestState(name).isStarted(), pairs);
	}
	
	/**
	 * Adds a class ID start condition to the quest.
	 * @param classId the class ID
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondClassId(ClassId classId, String html)
	{
		addCondStart(p -> p.getClassId() == classId, html);
	}
	
	/**
	 * Adds a class ID start condition to the quest.
	 * @param classId the class ID
	 * @param pairs the HTML to display if the condition is not met per each npc
	 */
	@SafeVarargs
	public final void addCondClassId(ClassId classId, KeyValuePair<Integer, String>... pairs)
	{
		addCondStart(p -> p.getClassId() == classId, pairs);
	}
	
	/**
	 * Adds a class IDs start condition to the quest.
	 * @param classIds the class ID
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondClassIds(List<ClassId> classIds, String html)
	{
		addCondStart(p -> classIds.contains(p.getClassId()), html);
	}
	
	/**
	 * Adds an item ID start condition to the quest.
	 * @param itemId the item ID
	 * @param pairs the HTML to display if the condition is not met per each npc
	 */
	@SafeVarargs
	public final void addCondItemId(int itemId, KeyValuePair<Integer, String>... pairs)
	{
		addCondStart(p -> !p.getInventory().getAllItemsByItemId(itemId).isEmpty(), pairs);
	}
	
	public void addCondPlayerKarma()
	{
		addCondStart(p -> p.getReputation() < 0);
	}
	
	public void addNewQuestConditions(NewQuestCondition condition, String html)
	{
		if (!condition.getAllowedClassIds().isEmpty())
		{
			addCondStart(p -> condition.getAllowedClassIds().contains(p.getClassId()), html);
		}
		
		if (!condition.getPreviousQuestIds().isEmpty())
		{
			for (Integer questId : condition.getPreviousQuestIds())
			{
				final Quest quest = QuestManager.getInstance().getQuest(questId);
				if (quest != null)
				{
					if (!condition.getOneOfPreQuests())
					{
						addCondStart(p -> p.hasQuestState(quest.getName()) && p.getQuestState(quest.getName()).isCompleted(), html);
					}
					else
					{
						addCondStart(p -> p.hasAnyCompletedQuestStates(condition.getPreviousQuestIds()), html);
					}
				}
			}
			
			addCondMinLevel(condition.getMinLevel(), html);
			addCondMaxLevel(condition.getMaxLevel(), html);
		}
	}
	
	/**
	 * Adds a not-class ID start condition to the quest.
	 * @param classId the class ID
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondNotClassId(ClassId classId, String html)
	{
		addCondStart(p -> p.getClassId() != classId, html);
	}
	
	/**
	 * Adds a not-class ID start condition to the quest.
	 * @param classId the class ID
	 * @param pairs the HTML to display if the condition is not met per each npc
	 */
	@SafeVarargs
	public final void addCondNotClassId(ClassId classId, KeyValuePair<Integer, String>... pairs)
	{
		addCondStart(p -> p.getClassId() != classId, pairs);
	}
	
	/**
	 * Adds a subclass active start condition to the quest.
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondIsSubClassActive(String html)
	{
		addCondStart(p -> p.isSubClassActive(), html);
	}
	
	/**
	 * Adds a subclass active start condition to the quest.
	 * @param pairs the HTML to display if the condition is not met per each npc
	 */
	@SafeVarargs
	public final void addCondIsSubClassActive(KeyValuePair<Integer, String>... pairs)
	{
		addCondStart(p -> p.isSubClassActive(), pairs);
	}
	
	/**
	 * Adds a not-subclass active start condition to the quest.
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondIsNotSubClassActive(String html)
	{
		addCondStart(p -> !p.isSubClassActive() && !p.isDualClassActive(), html);
	}
	
	/**
	 * Adds a not-subclass active start condition to the quest.
	 * @param pairs the HTML to display if the condition is not met per each npc
	 */
	@SafeVarargs
	public final void addCondIsNotSubClassActive(KeyValuePair<Integer, String>... pairs)
	{
		addCondStart(p -> !p.isSubClassActive() && !p.isDualClassActive(), pairs);
	}
	
	/**
	 * Adds a category start condition to the quest.
	 * @param categoryType the category type
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondInCategory(CategoryType categoryType, String html)
	{
		addCondStart(p -> p.isInCategory(categoryType), html);
	}
	
	/**
	 * Adds a category start condition to the quest.
	 * @param categoryType the category type
	 * @param pairs the HTML to display if the condition is not met per each npc
	 */
	@SafeVarargs
	public final void addCondInCategory(CategoryType categoryType, KeyValuePair<Integer, String>... pairs)
	{
		addCondStart(p -> p.isInCategory(categoryType), pairs);
	}
	
	/**
	 * Adds a clan level start condition to the quest.
	 * @param clanLevel the clan level
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondClanLevel(int clanLevel, String html)
	{
		addCondStart(p -> (p.getClan() != null) && (p.getClan().getLevel() > clanLevel), html);
	}
	
	/**
	 * Adds a category start condition to the quest.
	 * @param clanLevel the clan level
	 * @param pairs the HTML to display if the condition is not met per each npc
	 */
	@SafeVarargs
	public final void addCondClanLevel(int clanLevel, KeyValuePair<Integer, String>... pairs)
	{
		addCondStart(p -> (p.getClan() != null) && (p.getClan().getLevel() > clanLevel), pairs);
	}
	
	/**
	 * Adds a faction level start condition to the quest.
	 * @param faction the faction
	 * @param factionLevel the faction level
	 * @param html the HTML to display if the condition is not met
	 */
	public void addFactionLevel(Faction faction, int factionLevel, String html)
	{
		addCondStart(p -> p.getFactionLevel(faction) >= factionLevel, html);
	}
	
	public void onQuestAborted(Player player)
	{
	}
	
	public void giveStoryBuffReward(Player player)
	{
		if (Config.ENABLE_STORY_QUEST_BUFF_REWARD)
		{
			for (SkillHolder holder : STORY_QUEST_BUFFS)
			{
				SkillCaster.triggerCast(player, player, holder.getSkill());
			}
		}
	}
	
	public NewQuest getQuestData()
	{
		return _questData;
	}
	
	public void rewardPlayer(Player player)
	{
		final NewQuestReward reward = _questData.getRewards();
		final List<ItemHolder> rewardItems = reward.getItems();
		if ((rewardItems != null) && !rewardItems.isEmpty())
		{
			for (ItemHolder item : rewardItems)
			{
				rewardItems(player, item);
			}
		}
		
		if (reward.getLevel() > 0)
		{
			final long playerExp = player.getExp();
			final long targetExp = ExperienceData.getInstance().getExpForLevel(reward.getLevel());
			if (playerExp < targetExp)
			{
				player.addExpAndSp(targetExp - playerExp, 0);
				player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
				player.broadcastUserInfo();
			}
		}
		
		if (reward.getExp() > 0)
		{
			player.getStat().addExp(reward.getExp());
			player.broadcastUserInfo();
		}
		
		if (reward.getSp() > 0)
		{
			player.getStat().addSp(reward.getSp());
			player.broadcastUserInfo();
		}
	}
	
	public boolean teleportToQuestLocation(Player player, ILocational loc)
	{
		if (loc == null)
		{
			return false;
		}
		
		if (player.isDead())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_WHILE_DEAD);
			return false;
		}
		
		// Players should not be able to teleport if in a special location.
		if ((player.getMovieHolder() != null) || player.isFishing() || player.isInInstance() || player.isOnEvent() || player.isInOlympiadMode() || player.inObserverMode() || player.isInTraingCamp() || player.isInTimedHuntingZone())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_RIGHT_NOW);
			return false;
		}
		
		// Teleport in combat configuration.
		if (!Config.TELEPORT_WHILE_PLAYER_IN_COMBAT && (player.isInCombat() || player.isCastingNow()))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_WHILE_IN_COMBAT);
			return false;
		}
		
		// Karma related configurations.
		if ((!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT || !Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK) && (player.getReputation() < 0))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_RIGHT_NOW);
			return false;
		}
		
		// Cannot escape effect.
		if (player.isAffected(EffectFlag.CANNOT_ESCAPE))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_RIGHT_NOW);
			return false;
		}
		
		player.abortCast();
		player.stopMove(null);
		player.teleToLocation(loc);
		return true;
	}
	
	public void setType(int type)
	{
		_questData.setType(type);
	}
	
	public void sendAcceptDialog(Player player)
	{
		ThreadPool.schedule(() -> player.sendPacket(new ExQuestDialog(getId(), QuestDialogType.ACCEPT)), 2000);
	}
	
	public void sendEndDialog(Player player)
	{
		ThreadPool.schedule(() -> player.sendPacket(new ExQuestDialog(getId(), QuestDialogType.END)), 2000);
	}
}
