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
package org.l2jmobius.gameserver.model.holders;

/**
 * @author CostyKiller
 */
public class RelicDataHolder
{
	private final int _relicId;
	private final int _grade;
	private final int _skillId;
	private final int _enchantLevel;
	private final int _skillLevel;
	
	public RelicDataHolder(int relicId, int grade, int skillId, int enchantLevel, int skillLevel)
	{
		_relicId = relicId;
		_grade = grade;
		_skillId = skillId;
		_enchantLevel = enchantLevel;
		_skillLevel = skillLevel;
	}
	
	public int getRelicId()
	{
		return _relicId;
	}
	
	public int getGrade()
	{
		return _grade;
	}
	
	public int getSkillId()
	{
		return _skillId;
	}
	
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}
	
	public int getSkillLevel()
	{
		return _skillLevel;
	}
}
