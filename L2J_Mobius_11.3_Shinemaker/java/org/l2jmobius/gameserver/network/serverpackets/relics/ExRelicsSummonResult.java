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
import org.l2jmobius.gameserver.data.xml.RelicCouponData;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.PlayerRelicData;
import org.l2jmobius.gameserver.model.holders.RelicCouponHolder;
import org.l2jmobius.gameserver.model.holders.RelicDataHolder;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author CostyKiller, Mobius
 */
public class ExRelicsSummonResult extends ServerPacket
{
	private final Player _player;
	private final int _relicCouponItemId;
	private final int _relicSummonCount;
	
	public ExRelicsSummonResult(Player player, int relicCouponItemId, int relicSummonCount)
	{
		_player = player;
		_relicCouponItemId = relicCouponItemId;
		_relicSummonCount = player.getInventory().getItemByItemId(_relicCouponItemId) == null ? 0 : relicSummonCount;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_SUMMON_RESULT.writeId(this, buffer);
		
		buffer.writeByte(true); // Only works with true.
		buffer.writeInt(_relicCouponItemId); // Summon item id.
		buffer.writeInt(_relicSummonCount); // Array size of obtained relics.
		
		final RelicCouponHolder relicCouponHolder = RelicCouponData.getInstance().getRelicIdFromCouponId(_relicCouponItemId);
		if (relicCouponHolder != null)
		{
			final int obtainedRelicId = relicCouponHolder.getRelicId();
			final RelicDataHolder obtainedRelicTemplate = RelicData.getInstance().getRelic(obtainedRelicId);
			if (obtainedRelicTemplate != null)
			{
				buffer.writeInt(obtainedRelicId);
				
				// Add to database table the obtained relics.
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
				
				final PlayerRelicData newRelic = new PlayerRelicData(obtainedRelicId, 0, 0, 0, 0);
				newRelic.setRelicCount(1);
				newRelic.setRelicSummonTime(System.currentTimeMillis());
				if (existingRelic != null)
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
					
					newRelic.setRelicIndex(300 + unconfirmedRelics.size());
					
					// Increase the unconfirmed relics variable count.
					_player.getAccountVariables().set(AccountVariables.UNCONFIRMED_RELICS_COUNT, _player.getAccountVariables().getInt(AccountVariables.UNCONFIRMED_RELICS_COUNT, 0) + 1);
					_player.getAccountVariables().storeMe();
					
					if (Config.RELIC_SUMMON_ANNOUNCE)
					{
						Broadcast.toAllOnlinePlayers(new ExRelicsAnnounce(_player, newRelic.getRelicId()));
					}
				}
				storedRelics.add(newRelic);
				
				_player.storeRelics();
				_player.sendPacket(new ExRelicsList(_player)); // Update confirmed relic list relics count.
				_player.sendPacket(new ExRelicsExchangeList(_player)); // Update relic exchange/confirm list.
				_player.giveRelicSkill(obtainedRelicTemplate);
				return;
			}
		}
		
		// Obtained relics by scroll type.
		int obtainedRelicId = 0;
		for (int i = 1; i <= _relicSummonCount; i++)
		{
			if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				_player.sendMessage("I = " + i);
			}
			
			// Relic Summon Coupon (relic of No-grade / D-grade / C-grade).
			if (Config.RELIC_SUMMON_COUPONS.contains(_relicCouponItemId))
			{
				final int relicChance = Rnd.get(100);
				final int shiningRelicChance = Rnd.get(100);
				if (relicChance <= Config.RELIC_SUMMON_COMMON_COUPON_CHANCE_C_GRADE)
				{
					if (shiningRelicChance <= Config.RELIC_SUMMON_CHANCE_SHINING_C_GRADE)
					{
						obtainedRelicId = Config.C_GRADE_SHINING_RELICS.get(Rnd.get(Config.C_GRADE_SHINING_RELICS.size()));
					}
					else
					{
						obtainedRelicId = Config.C_GRADE_COMMON_RELICS.get(Rnd.get(Config.C_GRADE_COMMON_RELICS.size()));
					}
				}
				else if (relicChance <= Config.RELIC_SUMMON_COMMON_COUPON_CHANCE_D_GRADE)
				{
					if (shiningRelicChance <= Config.RELIC_SUMMON_CHANCE_SHINING_D_GRADE)
					{
						obtainedRelicId = Config.D_GRADE_SHINING_RELICS.get(Rnd.get(Config.D_GRADE_SHINING_RELICS.size()));
					}
					else
					{
						obtainedRelicId = Config.D_GRADE_COMMON_RELICS.get(Rnd.get(Config.D_GRADE_COMMON_RELICS.size()));
					}
				}
				else
				{
					obtainedRelicId = Config.NO_GRADE_COMMON_RELICS.get(Rnd.get(Config.NO_GRADE_COMMON_RELICS.size()));
				}
			}
			// Shining Relic Summon Coupon (relics of No-grade / D-grade / C-grade / B-grade).
			else if (Config.SHINING_RELIC_SUMMON_COUPONS.contains(_relicCouponItemId))
			{
				final int relicChance = Rnd.get(100);
				final int shiningRelicChance = Rnd.get(100);
				if (relicChance <= Config.RELIC_SUMMON_SHINING_COUPON_CHANCE_B_GRADE)
				{
					if (shiningRelicChance <= Config.RELIC_SUMMON_CHANCE_SHINING_B_GRADE)
					{
						obtainedRelicId = Config.B_GRADE_SHINING_RELICS.get(Rnd.get(Config.B_GRADE_SHINING_RELICS.size()));
					}
					else
					{
						obtainedRelicId = Config.B_GRADE_COMMON_RELICS.get(Rnd.get(Config.B_GRADE_COMMON_RELICS.size()));
					}
				}
				else if (relicChance <= Config.RELIC_SUMMON_SHINING_COUPON_CHANCE_C_GRADE)
				{
					if (shiningRelicChance <= Config.RELIC_SUMMON_CHANCE_SHINING_C_GRADE)
					{
						obtainedRelicId = Config.C_GRADE_SHINING_RELICS.get(Rnd.get(Config.C_GRADE_SHINING_RELICS.size()));
					}
					else
					{
						obtainedRelicId = Config.C_GRADE_COMMON_RELICS.get(Rnd.get(Config.C_GRADE_COMMON_RELICS.size()));
					}
				}
				else if (relicChance <= Config.RELIC_SUMMON_SHINING_COUPON_CHANCE_D_GRADE)
				{
					if (shiningRelicChance <= Config.RELIC_SUMMON_CHANCE_SHINING_D_GRADE)
					{
						obtainedRelicId = Config.D_GRADE_SHINING_RELICS.get(Rnd.get(Config.D_GRADE_SHINING_RELICS.size()));
					}
					else
					{
						obtainedRelicId = Config.D_GRADE_COMMON_RELICS.get(Rnd.get(Config.D_GRADE_COMMON_RELICS.size()));
					}
				}
				else
				{
					obtainedRelicId = Config.NO_GRADE_COMMON_RELICS.get(Rnd.get(Config.NO_GRADE_COMMON_RELICS.size()));
				}
			}
			// C-grade Relic Summon Coupon (relics of C-grade).
			else if (Config.C_GRADE_RELIC_SUMMON_COUPONS.contains(_relicCouponItemId))
			{
				final int shiningRelicChance = Rnd.get(100);
				if (shiningRelicChance <= Config.RELIC_SUMMON_CHANCE_SHINING_C_GRADE)
				{
					obtainedRelicId = Config.C_GRADE_SHINING_RELICS.get(Rnd.get(Config.C_GRADE_SHINING_RELICS.size()));
				}
				else
				{
					obtainedRelicId = Config.C_GRADE_COMMON_RELICS.get(Rnd.get(Config.C_GRADE_COMMON_RELICS.size()));
				}
			}
			// B-grade Relic Summon Coupon (relics of B-grade).
			else if (Config.B_GRADE_RELIC_SUMMON_COUPONS.contains(_relicCouponItemId))
			{
				final int shiningRelicChance = Rnd.get(100);
				if (shiningRelicChance <= Config.RELIC_SUMMON_CHANCE_SHINING_B_GRADE)
				{
					obtainedRelicId = Config.B_GRADE_SHINING_RELICS.get(Rnd.get(Config.B_GRADE_SHINING_RELICS.size()));
				}
				else
				{
					obtainedRelicId = Config.B_GRADE_COMMON_RELICS.get(Rnd.get(Config.B_GRADE_COMMON_RELICS.size()));
				}
			}
			// A-grade Relic Summon Coupon (relics of A-grade).
			else if (Config.A_GRADE_RELIC_SUMMON_COUPONS.contains(_relicCouponItemId))
			{
				obtainedRelicId = Config.A_GRADE_COMMON_RELICS.get(Rnd.get(Config.A_GRADE_COMMON_RELICS.size()));
			}
			// C-grade Relic Ticket (relics of D-grade / C-grade).
			else if (Config.C_GRADE_RELIC_TICKETS.contains(_relicCouponItemId))
			{
				final int relicChance = Rnd.get(100);
				final int shiningRelicChance = Rnd.get(100);
				if (relicChance < Config.RELIC_SUMMON_C_TICKET_CHANCE_C_GRADE)
				{
					obtainedRelicId = Config.C_GRADE_COMMON_RELICS.get(Rnd.get(Config.C_GRADE_COMMON_RELICS.size()));
				}
				else
				{
					if (shiningRelicChance <= Config.RELIC_SUMMON_CHANCE_SHINING_D_GRADE)
					{
						obtainedRelicId = Config.D_GRADE_SHINING_RELICS.get(Rnd.get(Config.D_GRADE_SHINING_RELICS.size()));
					}
					else
					{
						obtainedRelicId = Config.D_GRADE_COMMON_RELICS.get(Rnd.get(Config.D_GRADE_COMMON_RELICS.size()));
					}
				}
			}
			// B-grade Relic Ticket (relics of C-grade / B-grade).
			else if (Config.B_GRADE_RELIC_TICKETS.contains(_relicCouponItemId))
			{
				final int relicChance = Rnd.get(100);
				final int shiningRelicChance = Rnd.get(100);
				if (relicChance < Config.RELIC_SUMMON_B_TICKET_CHANCE_B_GRADE)
				{
					if (shiningRelicChance <= Config.RELIC_SUMMON_CHANCE_SHINING_B_GRADE)
					{
						obtainedRelicId = Config.B_GRADE_SHINING_RELICS.get(Rnd.get(Config.B_GRADE_SHINING_RELICS.size()));
					}
					else
					{
						obtainedRelicId = Config.B_GRADE_COMMON_RELICS.get(Rnd.get(Config.B_GRADE_COMMON_RELICS.size()));
					}
				}
				else
				{
					if (shiningRelicChance <= Config.RELIC_SUMMON_CHANCE_SHINING_C_GRADE)
					{
						obtainedRelicId = Config.C_GRADE_SHINING_RELICS.get(Rnd.get(Config.C_GRADE_SHINING_RELICS.size()));
					}
					else
					{
						obtainedRelicId = Config.C_GRADE_COMMON_RELICS.get(Rnd.get(Config.C_GRADE_COMMON_RELICS.size()));
					}
				}
			}
			// A-grade Relic Ticket (relics of B-grade / A-grade).
			else if (Config.A_GRADE_RELIC_TICKETS.contains(_relicCouponItemId))
			{
				final int relicChance = Rnd.get(100);
				final int shiningRelicChance = Rnd.get(100);
				if (relicChance < Config.RELIC_SUMMON_A_TICKET_CHANCE_A_GRADE)
				{
					obtainedRelicId = Config.A_GRADE_COMMON_RELICS.get(Rnd.get(Config.A_GRADE_COMMON_RELICS.size()));
				}
				else
				{
					if (shiningRelicChance < Config.RELIC_SUMMON_CHANCE_SHINING_B_GRADE)
					{
						obtainedRelicId = Config.B_GRADE_SHINING_RELICS.get(Rnd.get(Config.B_GRADE_SHINING_RELICS.size()));
					}
					else
					{
						obtainedRelicId = Config.B_GRADE_COMMON_RELICS.get(Rnd.get(Config.B_GRADE_COMMON_RELICS.size()));
					}
				}
			}
			
			buffer.writeInt(obtainedRelicId);
			
			// Add to database table the obtained relics.
			Collection<PlayerRelicData> storedRelics = _player.getRelics();
			
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
			
			final RelicDataHolder obtainedRelicTemplate = RelicData.getInstance().getRelic(obtainedRelicId);
			if (obtainedRelicTemplate != null)
			{
				final PlayerRelicData newRelic = new PlayerRelicData(obtainedRelicId, 0, 0, 0, 0);
				final int obtainedRelicGrade = obtainedRelicTemplate.getGrade();
				if (existingRelic != null)
				{
					// A/B Grade relics need to be added to confirmation list first.
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
						newRelic.setRelicIndex(300 + unconfirmedRelics.size());
						newRelic.setRelicSummonTime(System.currentTimeMillis());
						storedRelics.add(newRelic);
						// Increase the unconfirmed relics variable count.
						_player.getAccountVariables().set(AccountVariables.UNCONFIRMED_RELICS_COUNT, _player.getAccountVariables().getInt(AccountVariables.UNCONFIRMED_RELICS_COUNT, 0) + 1);
						_player.getAccountVariables().storeMe();
						_player.storeRelics();
						_player.sendPacket(new ExRelicsExchangeList(_player)); // Update relic exchange/confirm list.
						if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
						{
							_player.sendMessage("1.Duplicate relic id: " + newRelic.getRelicId() + " was added to confirmation list.");
						}
						if (Config.RELIC_SUMMON_ANNOUNCE)
						{
							// Announce new the obtained relic.
							Broadcast.toAllOnlinePlayers(new ExRelicsAnnounce(_player, newRelic.getRelicId()));
						}
					}
					// Update existing relics if not A/B Grade relics.
					else if (!((obtainedRelicGrade == 4) || (obtainedRelicGrade == 5)))
					{
						existingRelic.setRelicCount(existingRelic.getRelicCount() + 1);
						_player.storeRelics();
						_player.sendPacket(new ExRelicsUpdateList(1, existingRelic.getRelicId(), 0, 1)); // Update confirmed relic list with new relic.
						if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
						{
							_player.sendMessage("2.Existing relic id: " + existingRelic.getRelicId() + " count was updated.");
						}
						// Announce the existing obtained relic.
						if (Config.RELIC_SUMMON_ANNOUNCE && !Config.RELIC_ANNOUNCE_ONLY_A_B_GRADE)
						{
							Broadcast.toAllOnlinePlayers(new ExRelicsAnnounce(_player, existingRelic.getRelicId()));
						}
						// Check if relic is already registered in some collection.
						if (!_player.isRelicRegistered(existingRelic.getRelicId(), existingRelic.getRelicLevel()))
						{
							// Auto-Add to relic collections on summon.
							_player.sendPacket(new ExRelicsCollectionUpdate(_player, existingRelic.getRelicId(), existingRelic.getRelicLevel())); // Update collection list.
						}
					}
				}
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
						_player.storeRelics();
						_player.getAccountVariables().set(AccountVariables.UNCONFIRMED_RELICS_COUNT, _player.getAccountVariables().getInt(AccountVariables.UNCONFIRMED_RELICS_COUNT, 0) + 1);
						_player.getAccountVariables().storeMe();
						_player.sendPacket(new ExRelicsExchangeList(_player)); // Update relic exchange/confirm list.
						if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
						{
							_player.sendMessage("1.New relic id: " + newRelic.getRelicId() + " was added to confirmation list.");
						}
						if (Config.RELIC_SUMMON_ANNOUNCE)
						{
							// Announce the new obtained relic.
							Broadcast.toAllOnlinePlayers(new ExRelicsAnnounce(_player, newRelic.getRelicId()));
						}
					}
					else // Add new relics if not A/B Grade relics.
					{
						storedRelics.add(newRelic);
						_player.storeRelics();
						_player.sendPacket(new ExRelicsUpdateList(1, newRelic.getRelicId(), 0, 0)); // Update confirmed relic list with new relic.
						if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
						{
							_player.sendMessage("2.New relic id: " + newRelic.getRelicId() + " was added to relic list.");
						}
						if (Config.RELIC_SUMMON_ANNOUNCE && !Config.RELIC_ANNOUNCE_ONLY_A_B_GRADE)
						{
							// Announce the new obtained relic
							Broadcast.toAllOnlinePlayers(new ExRelicsAnnounce(_player, newRelic.getRelicId()));
						}
						if (!_player.isRelicRegistered(newRelic.getRelicId(), newRelic.getRelicLevel()))
						{
							// Auto-Add to relic collections on summon.
							_player.sendPacket(new ExRelicsCollectionUpdate(_player, newRelic.getRelicId(), newRelic.getRelicLevel())); // Update collection list.
						}
					}
				}
				_player.storeRelics();
				_player.giveRelicSkill(obtainedRelicTemplate);
			}
			else
			{
				PacketLogger.warning("ExRelicsSummonResult: Relic coupon " + _relicCouponItemId + " is probably not registred in configs.");
			}
			_player.sendPacket(new ExRelicsList(_player)); // Update confirmed relic list relics count.
			_player.sendPacket(new ExRelicsExchangeList(_player)); // Update relic exchange/confirm list.
		}
	}
}
