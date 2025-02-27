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
package instances.KartiasLabyrinth;

import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.FriendlyNpc;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureAttacked;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.events.impl.instance.OnInstanceStatusChange;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skill.SkillCaster;

import ai.AbstractNpcAI;

/**
 * Kartia Helper Eliyah's Guardian Spirit
 * @author flanagak
 */
public class KartiaHelperGuardian extends AbstractNpcAI
{
	// NPCs
	private static final int[] KARTIA_ELIYAH =
	{
		33615, // Eliyah (Kartia 85)
		33626, // Eliyah (Kartia 90)
		33637, // Eliyah (Kartia 95)
	};
	private static final int[] KARTIA_ADOLPH =
	{
		33609, // Adolph (Kartia 85)
		33620, // Adolph (Kartia 90)
		33631, // Adolph (Kartia 95)
	};
	private static final int[] KARTIA_FRIENDS =
	{
		33617, // Elise (Kartia 85)
		33628, // Elise (Kartia 90)
		33639, // Elise (Kartia 95)
		33609, // Adolph (Kartia 85)
		33620, // Adolph (Kartia 90)
		33631, // Adolph (Kartia 95)
		33611, // Barton (Kartia 85)
		33622, // Barton (Kartia 90)
		33633, // Barton (Kartia 95)
		33615, // Eliyah (Kartia 85)
		33626, // Eliyah (Kartia 90)
		33637, // Eliyah (Kartia 95)
		33613, // Hayuk (Kartia 85)
		33624, // Hayuk (Kartia 90)
		33635, // Hayuk (Kartia 95)
		33618, // Eliyah's Guardian Spirit (Kartia 85)
		33629, // Eliyah's Guardian Spirit (Kartia 90)
		33640, // Eliyah's Guardian Spirit (Kartia 95)
	};
	private static final int[] KARTIA_GUARDIANS =
	{
		33618, // Eliyah's Guardian Spirit (Kartia 85)
		33629, // Eliyah's Guardian Spirit (Kartia 90)
		33640, // Eliyah's Guardian Spirit (Kartia 95)
	};
	// Misc
	private static final int[] KARTIA_SOLO_INSTANCES =
	{
		205, // Solo 85
		206, // Solo 90
		207, // Solo 95
	};
	
	private KartiaHelperGuardian()
	{
		addCreatureSeeId(KARTIA_GUARDIANS);
		setCreatureKillId(this::onCreatureKill, KARTIA_GUARDIANS);
		setCreatureAttackedId(this::onCreatureAttacked, KARTIA_GUARDIANS);
		setInstanceStatusChangeId(this::onInstanceStatusChange, KARTIA_SOLO_INSTANCES);
	}
	
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance == null)
		{
			return;
		}
		
		if (event.equals("CHECK_ACTION"))
		{
			final FriendlyNpc eliyah = npc.getVariables().getObject("ELIYAH_OBJECT", FriendlyNpc.class);
			if (eliyah != null)
			{
				final double distance = npc.calculateDistance2D(eliyah);
				if (distance > 300)
				{
					final Location loc = new Location(eliyah.getX(), eliyah.getY(), eliyah.getZ() + 50);
					final Location randLoc = new Location(loc.getX() + getRandom(-50, 50), loc.getY() + getRandom(-50, 50), loc.getZ());
					if (distance > 600)
					{
						npc.teleToLocation(loc);
					}
					else
					{
						npc.setRunning();
					}
					addMoveToDesire(npc, randLoc, 23);
				}
				else if (!npc.isInCombat() || (npc.getTarget() == null))
				{
					final Creature monster = eliyah.getTarget().asCreature();
					if ((monster != null) && !CommonUtil.contains(KARTIA_FRIENDS, monster.getId()))
					{
						addAttackDesire(npc, monster);
					}
				}
			}
		}
		else if (event.equals("USE_SKILL"))
		{
			if ((npc.isInCombat() || npc.isAttackingNow() || (npc.getTarget() != null)) && (npc.getCurrentMpPercent() > 25) && !CommonUtil.contains(KARTIA_FRIENDS, npc.getTargetId()))
			{
				useRandomSkill(npc);
			}
		}
	}
	
	public void onInstanceStatusChange(OnInstanceStatusChange event)
	{
		final Instance instance = event.getWorld();
		final int status = event.getStatus();
		if (status == 1)
		{
			instance.getAliveNpcs(KARTIA_GUARDIANS).forEach(guardian -> getTimers().addRepeatingTimer("CHECK_ACTION", 3000, guardian, null));
			instance.getAliveNpcs(KARTIA_GUARDIANS).forEach(guardian -> getTimers().addRepeatingTimer("USE_SKILL", 6000, guardian, null));
		}
	}
	
	@Override
	public String onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer())
		{
			npc.getVariables().set("PLAYER_OBJECT", creature.asPlayer());
		}
		else if (CommonUtil.contains(KARTIA_ADOLPH, creature.getId()))
		{
			npc.getVariables().set("ADOLPH_OBJECT", creature);
		}
		else if (CommonUtil.contains(KARTIA_ELIYAH, creature.getId()))
		{
			npc.getVariables().set("ELIYAH_OBJECT", creature);
		}
		return super.onCreatureSee(npc, creature);
	}
	
	public void useRandomSkill(Npc npc)
	{
		final Instance instance = npc.getInstanceWorld();
		final WorldObject target = npc.getTarget();
		if (target == null)
		{
			return;
		}
		
		if ((instance != null) && !npc.isCastingNow() && (!CommonUtil.contains(KARTIA_FRIENDS, target.getId())))
		{
			final StatSet instParams = instance.getTemplateParameters();
			final SkillHolder skill01 = instParams.getSkillHolder("guardianSpiritsBlow");
			final SkillHolder skill02 = instParams.getSkillHolder("guardianSpiritsWrath");
			final int numberOfActiveSkills = 2;
			final int randomSkill = getRandom(numberOfActiveSkills + 1);
			
			switch (randomSkill)
			{
				case 0:
				case 1:
				{
					if ((skill01 != null) && SkillCaster.checkUseConditions(npc, skill01.getSkill()))
					{
						npc.doCast(skill01.getSkill(), null, true, false);
					}
					break;
				}
				case 2:
				{
					if ((skill02 != null) && SkillCaster.checkUseConditions(npc, skill02.getSkill()))
					{
						npc.doCast(skill02.getSkill(), null, true, false);
					}
					break;
				}
			}
		}
	}
	
	public void onCreatureAttacked(OnCreatureAttacked event)
	{
		final Npc npc = event.getTarget().asNpc();
		if (npc != null)
		{
			final Instance instance = npc.getInstanceWorld();
			if ((instance != null) && !npc.isInCombat() && !event.getAttacker().isPlayable() && !CommonUtil.contains(KARTIA_FRIENDS, event.getAttacker().getId()))
			{
				npc.setTarget(event.getAttacker());
				addAttackDesire(npc, npc.getTarget().asCreature());
			}
		}
	}
	
	public void onCreatureKill(OnCreatureDeath event)
	{
		final Npc npc = event.getTarget().asNpc();
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			getTimers().cancelTimersOf(npc);
			npc.doDie(event.getAttacker());
		}
	}
	
	public static void main(String[] args)
	{
		new KartiaHelperGuardian();
	}
}
