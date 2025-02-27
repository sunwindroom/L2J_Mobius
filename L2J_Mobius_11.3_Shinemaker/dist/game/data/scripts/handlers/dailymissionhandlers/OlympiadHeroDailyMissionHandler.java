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
package handlers.dailymissionhandlers;

import org.l2jmobius.gameserver.enums.DailyMissionStatus;
import org.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import org.l2jmobius.gameserver.model.DailyMissionDataHolder;
import org.l2jmobius.gameserver.model.DailyMissionPlayerEntry;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerTakeHero;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;

/**
 * @author CostyKiller
 */
public class OlympiadHeroDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _minLevel;
	private final int _maxLevel;
	private final int _minClanLevel;
	private final int _minClanMasteryLevel;
	private final int _amount;
	private final int _requiredMissionCompleteId;
	
	public OlympiadHeroDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_minLevel = holder.getParams().getInt("minLevel", 0);
		_maxLevel = holder.getParams().getInt("maxLevel", Integer.MAX_VALUE);
		_minClanLevel = holder.getParams().getInt("minClanLevel", 0);
		_minClanMasteryLevel = holder.getParams().getInt("minClanMasteryLevel", 0);
		_requiredMissionCompleteId = holder.getRequiredMissionCompleteId();
		_amount = holder.getRequiredCompletions();
	}
	
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(this, EventType.ON_PLAYER_TAKE_HERO, (OnPlayerTakeHero event) -> onPlayerTakeHero(event), this));
	}
	
	@Override
	public boolean isAvailable(Player player)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), false);
		if (entry != null)
		{
			switch (entry.getStatus())
			{
				case NOT_AVAILABLE: // Initial state
				{
					if (entry.getProgress() >= _amount)
					{
						entry.setStatus(DailyMissionStatus.AVAILABLE);
						storePlayerEntry(entry);
					}
					break;
				}
				case AVAILABLE:
				{
					return true;
				}
			}
		}
		return false;
	}
	
	private void onPlayerTakeHero(OnPlayerTakeHero event)
	{
		Player player = event.getPlayer();
		if (event.getPlayer() != null)
		{
			if ((player.getLevel() < _minLevel) || (player.getLevel() > _maxLevel) || !checkClan(player))
			{
				return;
			}
			final DailyMissionPlayerEntry heroEntry = getPlayerEntry(player.getObjectId(), true);
			if ((heroEntry.getStatus() == DailyMissionStatus.NOT_AVAILABLE) && (((_requiredMissionCompleteId != 0) && checkRequiredMission(player)) || (_requiredMissionCompleteId == 0)))
			{
				if (heroEntry.increaseProgress() >= _amount)
				{
					heroEntry.setStatus(DailyMissionStatus.AVAILABLE);
				}
				storePlayerEntry(heroEntry);
			}
		}
	}
	
	private boolean checkClan(Player player)
	{
		if (player == null)
		{
			return false;
		}
		
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return false;
		}
		
		final int clanMastery = clan.hasMastery(14) ? 14 : clan.hasMastery(15) ? 15 : clan.hasMastery(16) ? 16 : 0;
		return ((clan.getLevel() >= _minClanLevel) && (clanMastery >= _minClanMasteryLevel));
	}
	
	private boolean checkRequiredMission(Player player)
	{
		if (player == null)
		{
			return false;
		}
		
		final DailyMissionPlayerEntry missionEntry = getPlayerEntry(player.getObjectId(), false);
		return (missionEntry != null) && (_requiredMissionCompleteId != 0) && (missionEntry.getRewardId() == _requiredMissionCompleteId) && (getStatus(player) == DailyMissionStatus.COMPLETED.getClientId());
	}
}
