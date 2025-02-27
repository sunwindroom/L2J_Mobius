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
package org.l2jmobius.gameserver.network.serverpackets.ability;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Atronic
 */
public class ExAcquireAPSkillList extends ServerPacket
{
	private final int _abilityPoints;
	private final int _usedAbilityPoints;
	private final int _remainingAbilityPointsAPresetMain;
	private final int _remainingAbilityPointsBPresetMain;
	private final int _remainingAbilityPointsAPresetDual;
	private final int _remainingAbilityPointsBPresetDual;
	private final int _abilityPreset;
	private final Player _player;
	private final boolean _enable;
	private final List<Skill> _skills = new ArrayList<>();
	
	public ExAcquireAPSkillList(Player player)
	{
		_player = player;
		_abilityPoints = player.getAbilityPoints();
		_usedAbilityPoints = player.getAbilityPointsUsed();
		_remainingAbilityPointsAPresetMain = _abilityPoints - player.getVariables().getInt(PlayerVariables.ABILITY_POINTS_USED_MAIN_CLASS_A, 0);
		_remainingAbilityPointsBPresetMain = _abilityPoints - player.getVariables().getInt(PlayerVariables.ABILITY_POINTS_USED_MAIN_CLASS_B, 0);
		_remainingAbilityPointsAPresetDual = _abilityPoints - player.getVariables().getInt(PlayerVariables.ABILITY_POINTS_USED_DUAL_CLASS_A, 0);
		_remainingAbilityPointsBPresetDual = _abilityPoints - player.getVariables().getInt(PlayerVariables.ABILITY_POINTS_USED_DUAL_CLASS_B, 0);
		_abilityPreset = player.getAbilityPreset();
		
		String learnedSkillsInfo = "";
		if (_abilityPreset == 0)
		{
			if (player.isDualClassActive())
			{
				learnedSkillsInfo = player.getVariables().getString(PlayerVariables.ABILITY_POINTS_DUAL_CLASS_SKILLS_A, "");
			}
			else
			{
				learnedSkillsInfo = player.getVariables().getString(PlayerVariables.ABILITY_POINTS_MAIN_CLASS_SKILLS_A, "");
			}
		}
		else
		{
			if (player.isDualClassActive())
			{
				learnedSkillsInfo = player.getVariables().getString(PlayerVariables.ABILITY_POINTS_DUAL_CLASS_SKILLS_B, "");
			}
			else
			{
				learnedSkillsInfo = player.getVariables().getString(PlayerVariables.ABILITY_POINTS_MAIN_CLASS_SKILLS_B, "");
			}
		}
		
		final String[] learnedSkills = learnedSkillsInfo.split(",");
		for (SkillLearn sk : SkillTreeData.getInstance().getAbilitySkillTree().values())
		{
			final Skill knownSkill = player.getKnownSkill(sk.getSkillId());
			if ((knownSkill != null) && (knownSkill.getLevel() == sk.getSkillLevel()) && CommonUtil.contains(learnedSkills, sk.getSkillId() + "-" + sk.getSkillLevel()))
			{
				_skills.add(knownSkill);
			}
		}
		
		_enable = (player.getLevel() >= 85) && (!player.isSubClassActive() || player.isDualClassActive());
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_ACQUIRE_AP_SKILL_LIST.writeId(this, buffer);
		buffer.writeByte(_enable); // 430 Success changed to Result.
		if (_player.isDualClassActive())
		{
			buffer.writeInt(_remainingAbilityPointsAPresetDual); // APresetRemainAP
			buffer.writeInt(_remainingAbilityPointsBPresetDual); // BPresetRemainAP
		}
		else
		{
			buffer.writeInt(_remainingAbilityPointsAPresetMain); // APresetRemainAP
			buffer.writeInt(_remainingAbilityPointsBPresetMain); // BPresetRemainAP
		}
		buffer.writeByte(_abilityPreset); // CurrentPreset
		buffer.writeLong(Config.ABILITY_POINTS_RESET_SP); // Changed to from Adena to SP on Grand Crusade.
		buffer.writeInt(_abilityPoints);
		buffer.writeInt(_usedAbilityPoints);
		buffer.writeInt(_skills.size());
		for (Skill skill : _skills)
		{
			buffer.writeInt(skill.getId());
			buffer.writeInt(skill.getLevel());
		}
	}
}
