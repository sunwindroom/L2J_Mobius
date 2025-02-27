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
package org.l2jmobius.gameserver.network.serverpackets.relics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.PlayerRelicData;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExRelicsExchangeConfirm extends ServerPacket
{
	private final Player _player;
	private final int _index;
	private final int _relicId;
	
	public ExRelicsExchangeConfirm(Player player, int index, int relicId)
	{
		_player = player;
		_index = index;
		_relicId = relicId;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_EXCHANGE_CONFIRM.writeId(this, buffer);
		
		buffer.writeInt(_index);
		buffer.writeByte(1);
		buffer.writeInt(_relicId);
		
		final List<Integer> unconfirmedRelics = new ArrayList<>();
		final Collection<PlayerRelicData> storedRelics = _player.getRelics();
		for (PlayerRelicData relic : storedRelics)
		{
			if ((relic.getRelicIndex() >= 300) && (relic.getRelicCount() == 1)) // Unconfirmed relics are set on summon to index 300.
			{
				unconfirmedRelics.add(relic.getRelicId());
			}
		}
		
		// Remove relic from confirmation list.
		unconfirmedRelics.remove(_index);
		
		PlayerRelicData newRelic = (new PlayerRelicData(_relicId, 0, 0, 0, 0));
		
		// ADD to DB Table the confirmed relics.
		PlayerRelicData existingRelic = null;
		PlayerRelicData existingUnconfirmedRelic = null;
		
		// Check if the relic with the same ID exists.
		for (PlayerRelicData relic : storedRelics)
		{
			if ((relic.getRelicId() == _relicId))
			{
				if ((relic.getRelicIndex() >= 300) && (relic.getRelicCount() > 0))
				{
					existingUnconfirmedRelic = relic;
				}
				else if (relic.getRelicIndex() == 0)
				{
					existingRelic = relic;
				}
			}
		}
		if ((existingUnconfirmedRelic != null) && (existingRelic != null))
		{
			// Decrease the count of the existing relic with index >= 300.
			if (existingUnconfirmedRelic.getRelicIndex() >= 300)
			{
				existingUnconfirmedRelic.setRelicCount(existingUnconfirmedRelic.getRelicCount() - 1);
				_player.getAccountVariables().set(AccountVariables.UNCONFIRMED_RELICS_COUNT, _player.getAccountVariables().getInt(AccountVariables.UNCONFIRMED_RELICS_COUNT, 0) - 1);
				_player.getAccountVariables().storeMe();
				_player.storeRelics();
				if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
				{
					_player.sendMessage("1.Existing unconfirmed Relic Id: " + existingUnconfirmedRelic.getRelicId() + " Index: " + existingUnconfirmedRelic.getRelicIndex() + " count decreased.");
				}
			}
			// Increase the count of the existing relic with index == 0.
			if (existingRelic.getRelicIndex() == 0)
			{
				existingRelic.setRelicCount(existingRelic.getRelicCount() + 1);
				_player.storeRelics();
				_player.sendPacket(new ExRelicsList(_player)); // Update confirmed relic list relics count.
				_player.sendPacket(new ExRelicsUpdateList(1, existingRelic.getRelicId(), 0, existingRelic.getRelicCount() + 1)); // Update confirmed relic list with new relic.
				if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
				{
					_player.sendMessage("2.Existing Relic Id: " + existingRelic.getRelicId() + " Index: " + existingRelic.getRelicIndex() + " count increased.");
				}
				if (!_player.isRelicRegistered(existingRelic.getRelicId(), existingRelic.getRelicLevel()))
				{
					// Auto-Add to relic collections on summon.
					_player.sendPacket(new ExRelicsCollectionUpdate(_player, existingRelic.getRelicId(), existingRelic.getRelicLevel())); // Update collection list.
				}
			}
		}
		else if ((existingUnconfirmedRelic != null) && (existingRelic == null))
		{
			// Decrease the count of the existing relic with index >= 300.
			if (existingUnconfirmedRelic.getRelicIndex() >= 300)
			{
				existingUnconfirmedRelic.setRelicCount(existingUnconfirmedRelic.getRelicCount() - 1);
				_player.getAccountVariables().set(AccountVariables.UNCONFIRMED_RELICS_COUNT, _player.getAccountVariables().getInt(AccountVariables.UNCONFIRMED_RELICS_COUNT, 0) - 1);
				_player.getAccountVariables().storeMe();
				if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
				{
					_player.sendMessage("1.Existing unconfirmed Relic Id: " + existingUnconfirmedRelic.getRelicId() + " Index: " + existingUnconfirmedRelic.getRelicIndex() + " count decreased.");
				}
			}
			// Add the new relic with index == 0 if it doesn't exist.
			storedRelics.add(newRelic);
			_player.storeRelics();
			_player.sendPacket(new ExRelicsUpdateList(1, newRelic.getRelicId(), 0, 0)); // Update confirmed relic list with new relic.
			if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				_player.sendMessage("3.New Relic Id: " + newRelic.getRelicId() + " Index: " + newRelic.getRelicIndex() + " was added.");
			}
			if (!_player.isRelicRegistered(newRelic.getRelicId(), newRelic.getRelicLevel()))
			{
				// Auto-Add to relic collections on summon.
				_player.sendPacket(new ExRelicsCollectionUpdate(_player, newRelic.getRelicId(), newRelic.getRelicLevel())); // Update collection list.
			}
			
		}
		_player.sendPacket(new ExRelicsList(_player));// Update confirmed relic list.
		_player.sendPacket(new ExRelicsExchangeList(_player)); // Update relic exchange/confirm list.
		
		// Cleanup (remove confirmed relics with index 300+ and count 0 from db).
		if ((existingUnconfirmedRelic != null) && (existingUnconfirmedRelic.getRelicCount() == 0))
		{
			_player.deleteRelics(existingUnconfirmedRelic.getRelicId(), existingUnconfirmedRelic.getRelicLevel(), existingUnconfirmedRelic.getRelicCount(), existingUnconfirmedRelic.getRelicIndex(), existingUnconfirmedRelic.getRelicSummonTime());
			_player.getRelics().remove(existingUnconfirmedRelic);
			if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				_player.sendMessage("4.Cleanup Relic Id: " + existingUnconfirmedRelic.getRelicId() + " Index: " + existingUnconfirmedRelic.getRelicIndex() + " was removed from db.");
			}
		}
	}
}
