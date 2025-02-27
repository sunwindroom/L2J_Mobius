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
public class PlayerRelicCollectionData
{
	private final int _relicCollectionId;
	private final int _relicId;
	private final int _relicLevel;
	private final int _index;
	
	public PlayerRelicCollectionData(int relicCollectionId, int relicId, int relicLevel, int index)
	{
		_relicCollectionId = relicCollectionId;
		_relicId = relicId;
		_relicLevel = relicLevel;
		_index = index;
	}
	
	public int getRelicCollectionId()
	{
		return _relicCollectionId;
	}
	
	public int getRelicId()
	{
		return _relicId;
	}
	
	public int getRelicLevel()
	{
		return _relicLevel;
	}
	
	public int getIndex()
	{
		return _index;
	}
}