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
package ai.areas.OrbisTemple;

import org.l2jmobius.gameserver.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.skill.SkillCaster;

import ai.AbstractNpcAI;

/**
 * Orbis Temple Statue AI.
 * @author Mobius
 */
public class OrbisTempleStatues extends AbstractNpcAI
{
	// NPCs
	private static final int VICTIM_1 = 22911;
	private static final int VICTIM_2 = 22912;
	private static final int VICTIM_3 = 22913;
	private static final int GUARD_1 = 22914;
	private static final int GUARD_2 = 22915;
	private static final int GUARD_3 = 22916;
	private static final int THROWER_1 = 22917;
	private static final int THROWER_2 = 22918;
	private static final int THROWER_3 = 22919;
	private static final int ANCIENT_HERO = 22925;
	private static final int CURATOR = 22923;
	private static final int CHIEF_CURATOR = 22927;
	// Items
	private static final int SWORD = 15280;
	private static final int SPEAR = 17372;
	// Skill
	private static final SkillHolder SPAWN_SKILL = new SkillHolder(14277, 1); // Orbis Spawn
	private static final SkillHolder DESPAWN_SKILL = new SkillHolder(14278, 1); // Orbis De-spawn
	
	public OrbisTempleStatues()
	{
		addAttackId(VICTIM_1, VICTIM_2, VICTIM_3, GUARD_1, GUARD_2, GUARD_3, THROWER_1, THROWER_2, THROWER_3, CURATOR);
		addMoveFinishedId(VICTIM_1, VICTIM_2, VICTIM_3, GUARD_1, GUARD_2, GUARD_3, THROWER_1, THROWER_2, THROWER_3, CURATOR, ANCIENT_HERO, CHIEF_CURATOR);
		addSpawnId(VICTIM_1, VICTIM_2, VICTIM_3, GUARD_1, GUARD_2, GUARD_3, THROWER_1, THROWER_2, THROWER_3, CURATOR, ANCIENT_HERO, CHIEF_CURATOR);
		addKillId(VICTIM_1, VICTIM_2, VICTIM_3, GUARD_1, GUARD_2, GUARD_3, THROWER_1, THROWER_2, THROWER_3, CURATOR, ANCIENT_HERO, CHIEF_CURATOR);
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isPet)
	{
		switch (npc.getId())
		{
			case VICTIM_1:
			case VICTIM_2:
			case VICTIM_3:
			case GUARD_1:
			case GUARD_2:
			case GUARD_3:
			case CURATOR:
			{
				if (npc.isImmobilized())
				{
					npc.setImmobilized(false);
					npc.setRHandId(SWORD);
				}
				break;
			}
			case THROWER_1:
			case THROWER_2:
			case THROWER_3:
			{
				if (npc.isImmobilized())
				{
					npc.setImmobilized(false);
					npc.setRHandId(SPEAR);
				}
				break;
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public void onMoveFinished(Npc npc)
	{
		if ((npc.getTarget() == null) && (npc.getTemplate().getRHandId() == 0))
		{
			npc.setRHandId(0);
		}
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		npc.setRandomWalking(false);
		if (npc.getId() < ANCIENT_HERO)
		{
			npc.setImmobilized(true);
		}
		if (npc.isMonster())
		{
			npc.stopSkillEffects(SkillFinishType.SILENT, DESPAWN_SKILL.getSkillId());
			SkillCaster.triggerCast(npc, npc, SPAWN_SKILL.getSkill());
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.isMonster())
		{
			SkillCaster.triggerCast(killer, npc, DESPAWN_SKILL.getSkill());
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new OrbisTempleStatues();
	}
}