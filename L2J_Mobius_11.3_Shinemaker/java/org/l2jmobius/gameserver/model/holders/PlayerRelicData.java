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
public class PlayerRelicData
{
	private final int _relicId;
	private int _relicLevel;
	private int _relicCount;
	private int _relicIndex;
	private long _relicSummonTime;
	
	public PlayerRelicData(int relicId, int relicLevel, int relicCount, int relicIndex, long relicSummonTime)
	{
		_relicId = relicId;
		_relicLevel = relicLevel;
		_relicCount = relicCount;
		_relicIndex = relicIndex;
		_relicSummonTime = relicSummonTime;
	}
	
	public int getRelicId()
	{
		return _relicId;
	}
	
	public int getRelicLevel()
	{
		return _relicLevel;
	}
	
	public int getRelicCount()
	{
		return _relicCount;
	}
	
	public int getRelicIndex()
	{
		return _relicIndex;
	}
	
	public long getRelicSummonTime()
	{
		return _relicSummonTime;
	}
	
	public void setRelicLevel(int level)
	{
		_relicLevel = level;
	}
	
	public void setRelicCount(int count)
	{
		_relicCount = count;
	}
	
	public void setRelicIndex(int index)
	{
		_relicIndex = index;
	}
	
	public void setRelicSummonTime(long relicSummonTime)
	{
		_relicSummonTime = relicSummonTime;
	}
}