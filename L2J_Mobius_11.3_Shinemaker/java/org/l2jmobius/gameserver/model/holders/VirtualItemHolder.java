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

public class VirtualItemHolder
{
	private final int _indexMain;
	private final int _indexSub;
	private final int _slot;
	private final int _costVISPoint;
	private final int _itemClass;
	private final int _enchant;
	
	public VirtualItemHolder(int indexMain, int indexSub, int slot, int costVISPoint, int itemClass, int enchant)
	{
		_indexMain = indexMain;
		_indexSub = indexSub;
		_slot = slot;
		_costVISPoint = costVISPoint;
		_itemClass = itemClass;
		_enchant = enchant;
	}
	
	public int getIndexMain()
	{
		return _indexMain;
	}
	
	public int getIndexSub()
	{
		return _indexSub;
	}
	
	public int getSlot()
	{
		return _slot;
	}
	
	public int getCostVISPoint()
	{
		return _costVISPoint;
	}
	
	public int getItemClass()
	{
		return _itemClass;
	}
	
	public int getEnchant()
	{
		return _enchant;
	}
}