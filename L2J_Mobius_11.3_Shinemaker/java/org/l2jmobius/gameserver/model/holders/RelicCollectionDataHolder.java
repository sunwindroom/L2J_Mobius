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

import java.util.Collection;
import java.util.List;

/**
 * @author CostyKiller
 */
public class RelicCollectionDataHolder
{
	private final int _relicCollectionId;
	private final int _optionId;
	private final int _category;
	private final int _completeCount;
	private final List<RelicDataHolder> _relics;
	
	public RelicCollectionDataHolder(int relicCollectionId, int optionId, int category, int completeCount, List<RelicDataHolder> relics)
	{
		_relicCollectionId = relicCollectionId;
		_optionId = optionId;
		_category = category;
		_completeCount = completeCount;
		_relics = relics;
	}
	
	public int getCollectionId()
	{
		return _relicCollectionId;
	}
	
	public int getOptionId()
	{
		return _optionId;
	}
	
	public int getCategory()
	{
		return _category;
	}
	
	public int getCompleteCount()
	{
		return _completeCount;
	}
	
	public Collection<RelicDataHolder> getRelics()
	{
		return _relics;
	}
	
	public RelicDataHolder getRelic(int index)
	{
		return _relics.get(index);
	}
}
