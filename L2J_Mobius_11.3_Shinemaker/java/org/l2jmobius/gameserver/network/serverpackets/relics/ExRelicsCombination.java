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
package org.l2jmobius.gameserver.network.serverpackets.relics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.PlayerRelicData;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExRelicsCombination extends ServerPacket
{
	private final Player _player;
	private final int _relicsUsedGrade;
	private final int _relicsUsedCount;
	
	public ExRelicsCombination(Player player, int relicsUsedGrade, int relicsUsedCount)
	{
		_player = player;
		_relicsUsedGrade = relicsUsedGrade;
		_relicsUsedCount = relicsUsedCount;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_COMBINATION.writeId(this, buffer);
		int chance = 0;
		switch (_relicsUsedGrade)
		{
			case 1: // No Grade Ingredients Compound.
			{
				chance = (Config.RELIC_COMPOUND_NO_GRADE_INGREDIENTS_CHANCE_D_GRADE + Config.RELIC_COMPOUND_NO_GRADE_INGREDIENTS_CHANCE_SHINING_D_GRADE);
				break;
			}
			case 2: // D Grade Ingredients Compound.
			{
				chance = (Config.RELIC_COMPOUND_D_GRADE_INGREDIENTS_CHANCE_C_GRADE + Config.RELIC_COMPOUND_D_GRADE_INGREDIENTS_CHANCE_SHINING_C_GRADE);
				break;
			}
			case 3: // C Grade Ingredients Compound.
			{
				chance = (Config.RELIC_COMPOUND_C_GRADE_INGREDIENTS_CHANCE_B_GRADE + Config.RELIC_COMPOUND_C_GRADE_INGREDIENTS_CHANCE_SHINING_B_GRADE);
				break;
			}
			case 4: // B Grade Ingredients Compound.
			{
				chance = Config.RELIC_COMPOUND_B_GRADE_INGREDIENTS_CHANCE_A_GRADE;
				break;
			}
		}
		
		final int arraySize = _relicsUsedCount / 4;
		buffer.writeByte(true); // If not true the compound result page is not showing up.
		buffer.writeInt(arraySize); // Obtained relics count array size.
		
		int feeItemId = 0;
		long feeItemCount = 0;
		int itemsOnFailureCount = 0;
		int shardId = 0;
		long shardCount = 0;
		int obtainedRelicId = 0;
		int relicsFailedCount = 0;
		int relicsSuccededCount = 0;
		// Loop each obtained relic.
		for (int i = 0; i < arraySize; i++)
		{
			// Set fees and obtained relic based on ingredients used grade.
			switch (_relicsUsedGrade)
			{
				case 1: // No Grade Ingredients Compound.
				{
					feeItemId = Config.RELIC_COMPOUND_FEE_NO_GRADE.get(0).getId();
					feeItemCount = Config.RELIC_COMPOUND_FEE_NO_GRADE.get(0).getCount();
					if (Rnd.get(100) < chance)
					{
						if (Rnd.get(100) < Config.RELIC_COMPOUND_NO_GRADE_INGREDIENTS_CHANCE_SHINING_D_GRADE)
						{
							obtainedRelicId = Config.D_GRADE_SHINING_RELICS.get(Rnd.get(Config.D_GRADE_SHINING_RELICS.size())); // Random D Grade Shining relic id.
						}
						else
						{
							obtainedRelicId = Config.D_GRADE_COMMON_RELICS.get(Rnd.get(Config.D_GRADE_COMMON_RELICS.size())); // Random D Grade relic id.
						}
						relicsSuccededCount++;
					}
					else
					{
						obtainedRelicId = Config.NO_GRADE_COMMON_RELICS.get(Rnd.get(Config.NO_GRADE_COMMON_RELICS.size())); // Random No Grade relic id.
						relicsFailedCount++;
					}
					buffer.writeInt(obtainedRelicId);
					break;
				}
				case 2: // D Grade Ingredients Compound.
				{
					feeItemId = Config.RELIC_COMPOUND_FEE_D_GRADE.get(0).getId();
					feeItemCount = Config.RELIC_COMPOUND_FEE_D_GRADE.get(0).getCount();
					if (Rnd.get(100) < chance)
					{
						if (Rnd.get(100) < Config.RELIC_COMPOUND_D_GRADE_INGREDIENTS_CHANCE_SHINING_C_GRADE)
						{
							obtainedRelicId = Config.C_GRADE_SHINING_RELICS.get(Rnd.get(Config.C_GRADE_SHINING_RELICS.size())); // Random C Grade Shining relic id.
						}
						else
						{
							obtainedRelicId = Config.C_GRADE_COMMON_RELICS.get(Rnd.get(Config.C_GRADE_COMMON_RELICS.size())); // Random C Grade relic id.
						}
						relicsSuccededCount++;
					}
					else
					{
						if (Rnd.get(100) < Config.RELIC_COMPOUND_D_GRADE_INGREDIENTS_CHANCE_SHINING_D_GRADE)
						{
							obtainedRelicId = Config.D_GRADE_SHINING_RELICS.get(Rnd.get(Config.D_GRADE_SHINING_RELICS.size())); // Random D Grade Shining relic id.
						}
						else
						{
							obtainedRelicId = Config.D_GRADE_COMMON_RELICS.get(Rnd.get(Config.D_GRADE_COMMON_RELICS.size())); // Random D Grade relic id.
						}
						relicsFailedCount++;
					}
					buffer.writeInt(obtainedRelicId);
					break;
				}
				case 3: // C Grade Ingredients Compound.
				{
					feeItemId = Config.RELIC_COMPOUND_FEE_C_GRADE.get(0).getId();
					feeItemCount = Config.RELIC_COMPOUND_FEE_C_GRADE.get(0).getCount();
					itemsOnFailureCount = Config.RELIC_COMPOUND_FAILURE_ITEM_C_GRADE.size();
					shardId = Config.RELIC_COMPOUND_FAILURE_ITEM_C_GRADE.get(0).getId();
					shardCount = Config.RELIC_COMPOUND_FAILURE_ITEM_C_GRADE.get(0).getCount();
					if (Rnd.get(100) < chance)
					{
						if (Rnd.get(100) < Config.RELIC_COMPOUND_C_GRADE_INGREDIENTS_CHANCE_SHINING_B_GRADE)
						{
							obtainedRelicId = Config.B_GRADE_SHINING_RELICS.get(Rnd.get(Config.B_GRADE_SHINING_RELICS.size())); // Random B Grade Shining relic id.
						}
						else
						{
							obtainedRelicId = Config.B_GRADE_COMMON_RELICS.get(Rnd.get(Config.B_GRADE_COMMON_RELICS.size())); // Random B Grade relic id.
						}
						relicsSuccededCount++;
					}
					else
					{
						if (Rnd.get(100) < Config.RELIC_COMPOUND_C_GRADE_INGREDIENTS_CHANCE_SHINING_C_GRADE)
						{
							obtainedRelicId = Config.C_GRADE_SHINING_RELICS.get(Rnd.get(Config.C_GRADE_SHINING_RELICS.size())); // Random C Grade Shining relic id.
						}
						else
						{
							obtainedRelicId = Config.C_GRADE_COMMON_RELICS.get(Rnd.get(Config.C_GRADE_COMMON_RELICS.size())); // Random C Grade relic id.
						}
						relicsFailedCount++;
					}
					buffer.writeInt(obtainedRelicId);
					break;
				}
				case 4: // B Grade Ingredients Compound.
				{
					feeItemId = Config.RELIC_COMPOUND_FEE_B_GRADE.get(0).getId();
					feeItemCount = Config.RELIC_COMPOUND_FEE_B_GRADE.get(0).getCount();
					itemsOnFailureCount = Config.RELIC_COMPOUND_FAILURE_ITEM_B_GRADE.size();
					shardId = Config.RELIC_COMPOUND_FAILURE_ITEM_B_GRADE.get(0).getId();
					shardCount = Config.RELIC_COMPOUND_FAILURE_ITEM_B_GRADE.get(0).getCount();
					if (Rnd.get(100) < chance)
					{
						obtainedRelicId = Config.A_GRADE_COMMON_RELICS.get(Rnd.get(Config.A_GRADE_COMMON_RELICS.size())); // Random A Grade relic id.
						relicsSuccededCount++;
					}
					else
					{
						if (Rnd.get(100) < Config.RELIC_COMPOUND_B_GRADE_INGREDIENTS_CHANCE_SHINING_B_GRADE)
						{
							obtainedRelicId = Config.B_GRADE_SHINING_RELICS.get(Rnd.get(Config.B_GRADE_SHINING_RELICS.size())); // Random B Grade Shining relic id.
						}
						else
						{
							obtainedRelicId = Config.B_GRADE_COMMON_RELICS.get(Rnd.get(Config.B_GRADE_COMMON_RELICS.size())); // Random B Grade relic id.
						}
						relicsFailedCount++;
					}
					buffer.writeInt(obtainedRelicId);
					break;
				}
			}
			
			// Add to DB Table the obtained relics.
			final Collection<PlayerRelicData> storedRelics = _player.getRelics();
			
			// Check if the relic with the same ID exists.
			PlayerRelicData existingRelic = null;
			for (PlayerRelicData relic : storedRelics)
			{
				if (relic.getRelicId() == obtainedRelicId)
				{
					existingRelic = relic;
					break;
				}
			}
			
			final int obtainedRelicGrade = RelicData.getInstance().getRelic(obtainedRelicId).getGrade();
			final PlayerRelicData newRelic = new PlayerRelicData(obtainedRelicId, 0, 0, 0, 0);
			// Increment the count of the existing relic.
			if (existingRelic != null)
			{
				// A/B Grade relics need to be confirmed before add them to relics list.
				if ((obtainedRelicGrade == 4) || (obtainedRelicGrade == 5))
				{
					// Check indexes of relics with same id to avoid duplicate 300+ index.
					final List<Integer> unconfirmedRelics = new ArrayList<>();
					final Collection<PlayerRelicData> storedRelics2 = _player.getRelics();
					for (PlayerRelicData relic2 : storedRelics2)
					{
						if ((relic2.getRelicIndex() >= 300) && (relic2.getRelicId() == existingRelic.getRelicId())) // Unconfirmed relics are set on summon to index 300.
						{
							unconfirmedRelics.add(relic2.getRelicIndex());
						}
					}
					if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
					{
						_player.sendMessage("0.Duplicate relic indexes list: " + unconfirmedRelics);
					}
					newRelic.setRelicCount(1);
					newRelic.setRelicIndex(300 + unconfirmedRelics.size()); // Need to update this to increase last 300+ index.
					newRelic.setRelicSummonTime(System.currentTimeMillis());
					storedRelics.add(newRelic);
					_player.sendPacket(new ExRelicsExchangeList(_player)); // Update relic exchange/confirm list.
					if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
					{
						_player.sendMessage("Existing relic id: " + obtainedRelicId + " was added to confirmation list.");
					}
				}
				else
				{
					// Increment the count of the existing relic if not A/B Grade relics.
					existingRelic.setRelicCount(existingRelic.getRelicCount() + 1);
					_player.sendPacket(new ExRelicsUpdateList(1, existingRelic.getRelicId(), 0, existingRelic.getRelicCount() + 1)); // Update confirmed relic list with new relic.
					if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
					{
						_player.sendMessage("Existing relic id: " + obtainedRelicId + " count increased.");
					}
					if (existingRelic.getRelicIndex() == 0)
					{
						if (!_player.isRelicRegistered(existingRelic.getRelicId(), existingRelic.getRelicLevel()))
						{
							// Auto-Add to relic collections on summon.
							_player.sendPacket(new ExRelicsCollectionUpdate(_player, existingRelic.getRelicId(), existingRelic.getRelicLevel())); // Update collection list.
						}
					}
				}
			}
			// Add the new relic if it doesn't exist.
			else
			{
				// A/B Grade relics need to be confirmed before add them to relics list.
				if ((obtainedRelicGrade == 4) || (obtainedRelicGrade == 5))
				{
					// Set Relic Index to 300 to be able to get the list of confirmation relics later.
					newRelic.setRelicCount(1);
					newRelic.setRelicIndex(300);
					newRelic.setRelicSummonTime(System.currentTimeMillis());
					storedRelics.add(newRelic);
					_player.getAccountVariables().set(AccountVariables.UNCONFIRMED_RELICS_COUNT, _player.getAccountVariables().getInt(AccountVariables.UNCONFIRMED_RELICS_COUNT, 0) + 1);
					_player.getAccountVariables().storeMe();
					_player.sendPacket(new ExRelicsExchangeList(_player)); // Update relic exchange/confirm list.
					if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
					{
						_player.sendMessage("New relic id: " + obtainedRelicId + " was added to confirmation list.");
					}
				}
				// Add new relic if not A/B Grade relics.
				else
				{
					newRelic.setRelicIndex(0);
					storedRelics.add(newRelic);
					_player.sendPacket(new ExRelicsUpdateList(1, newRelic.getRelicId(), 0, 0)); // Update confirmed relic list with new relic.
					if (newRelic.getRelicIndex() == 0)
					{
						if (!_player.isRelicRegistered(newRelic.getRelicId(), newRelic.getRelicLevel()))
						{
							// Auto-Add to relic collections on summon.
							_player.sendPacket(new ExRelicsCollectionUpdate(_player, newRelic.getRelicId(), newRelic.getRelicLevel())); // Update collection list.
						}
					}
				}
			}
		}
		// Show obtained items when failed.
		buffer.writeInt(itemsOnFailureCount); // Obtained items when failed array size.
		buffer.writeInt(shardId); // Item 1 id.
		buffer.writeLong(shardCount); // Item 1 count.
		
		_player.sendPacket(new ExRelicsList(_player)); // Update confirmed relic list relics count.
		_player.sendMessage("You obtained through compounding: " + arraySize + " relics.");
		_player.destroyItemByItemId("RelicsCompound", feeItemId, feeItemCount * arraySize, _player, true);
		// Send summary of compounds
		_player.sendMessage("Relics compound summary: " + relicsSuccededCount + " succeded and " + relicsFailedCount + " failed.");
		if (relicsFailedCount > relicsSuccededCount)
		{
			_player.sendPacket(new ExShowScreenMessage("Relics compound has failed.", ExShowScreenMessage.TOP_CENTER, 5000, 0, true, false));
			if (_relicsUsedGrade >= 3)
			{
				// Add failure items obtained here
				_player.addItem("RelicsCompoundFail", shardId, shardCount * relicsFailedCount, _player, true);
			}
		}
		
	}
}
