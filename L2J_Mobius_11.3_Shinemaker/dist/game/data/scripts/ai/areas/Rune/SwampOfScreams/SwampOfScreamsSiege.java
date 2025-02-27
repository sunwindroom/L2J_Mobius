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
package ai.areas.Rune.SwampOfScreams;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicReference;

import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.spawns.SpawnGroup;
import org.l2jmobius.gameserver.model.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.network.NpcStringId;

import ai.AbstractNpcAI;

/**
 * @author Tanatos
 */
public class SwampOfScreamsSiege extends AbstractNpcAI
{
	// NPCs
	private static final int[] SWAMP_MONSTERS =
	{
		24570,
		24571,
		24572,
		24573
	};
	private static final int SWAMP_PETRA = 24574;
	private static final AtomicReference<SpawnTemplate> SPAWN_SWAMP_MONSTERS = new AtomicReference<>();
	// Misc
	private static final int[] DAYS_OF_WEEK =
	{
		Calendar.MONDAY,
		Calendar.TUESDAY,
		Calendar.WEDNESDAY,
		Calendar.THURSDAY,
		Calendar.FRIDAY,
		Calendar.SATURDAY,
		Calendar.SUNDAY
	};
	// Schedule: 12-13 & 19-20
	private static final int[] DAY_TIME =
	{
		12,
		00
	};
	private static final int[] NIGHT_TIME =
	{
		19,
		00
	};
	private static final long DESPAWN_DELAY = 3600000;
	private static boolean _daytime = false;
	
	private SwampOfScreamsSiege()
	{
		addKillId(SWAMP_MONSTERS);
		scheduleDayTime();
		scheduleNightTime();
		LOGGER.info("Swamp of Screams siege starts from 12:00 to 13:00 and from 19:00 to 20:00.");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "day_time_spawn":
			{
				World.getInstance().getPlayers().forEach(p -> showOnScreenMsg(p, NpcStringId.HERALD_S_ANNOUNCEMENT_12_00_13_00_MONSTERS_ARE_SPAWNING_IN_THE_SWAMP_OF_SCREAMS_KILL_THEM_ALL, 2, 10000, true));
				SPAWN_SWAMP_MONSTERS.set(SpawnData.getInstance().getSpawnByName("SwampOfScreamsMonsters"));
				SPAWN_SWAMP_MONSTERS.get().getGroups().forEach(SpawnGroup::spawnAll);
				_daytime = true;
				startQuestTimer("despawn", DESPAWN_DELAY, null, null);
				break;
			}
			case "night_time_spawn":
			{
				World.getInstance().getPlayers().forEach(p -> showOnScreenMsg(p, NpcStringId.HERALD_S_ANNOUNCEMENT_19_00_20_00_MONSTERS_ARE_INVADING_THE_SWAMP_OF_SCREAMS_AT_NIGHT_TIME, 2, 10000, true));
				SPAWN_SWAMP_MONSTERS.set(SpawnData.getInstance().getSpawnByName("SwampOfScreamsMonsters"));
				SPAWN_SWAMP_MONSTERS.get().getGroups().forEach(SpawnGroup::spawnAll);
				_daytime = false;
				startQuestTimer("despawn", DESPAWN_DELAY, null, null);
				break;
			}
			case "despawn":
			{
				if (_daytime)
				{
					World.getInstance().getPlayers().forEach(p -> showOnScreenMsg(p, NpcStringId.HERALD_S_ANNOUNCEMENT_19_00_20_00_MONSTERS_IN_THE_SWAMP_OF_SCREAMS_HAVE_FLED, 2, 10000, true));
				}
				else
				{
					World.getInstance().getPlayers().forEach(p -> showOnScreenMsg(p, NpcStringId.HERALD_S_ANNOUNCEMENT_12_00_13_00_MONSTERS_IN_THE_SWAMP_OF_SCREAMS_ARE_DEFEATED, 2, 10000, true));
				}
				SPAWN_SWAMP_MONSTERS.set(SpawnData.getInstance().getSpawnByName("SwampOfScreamsMonsters"));
				SPAWN_SWAMP_MONSTERS.get().getGroups().forEach(SpawnGroup::despawnAll);
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if ((CommonUtil.contains(SWAMP_MONSTERS, npc.getId())) && (getRandom(100) < 3))
		{
			addSpawn(SWAMP_PETRA, npc.getLocation(), false, 600000, false);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	private void scheduleDayTime()
	{
		long time = Long.MAX_VALUE;
		for (int day : DAYS_OF_WEEK)
		{
			final long nextDateMillis = getNextDateMilis(day, DAY_TIME[0], DAY_TIME[1]);
			if (nextDateMillis < time)
			{
				time = nextDateMillis;
			}
		}
		startQuestTimer("day_time_spawn", time - System.currentTimeMillis(), null, null);
	}
	
	private void scheduleNightTime()
	{
		long time = Long.MAX_VALUE;
		for (int day : DAYS_OF_WEEK)
		{
			final long nextDateMillis = getNextDateMilis(day, NIGHT_TIME[0], NIGHT_TIME[1]);
			if (nextDateMillis < time)
			{
				time = nextDateMillis;
			}
		}
		startQuestTimer("night_time_spawn", time - System.currentTimeMillis(), null, null);
	}
	
	private long getNextDateMilis(int dayOfWeek, int hour, int minute)
	{
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, 0);
		for (int i = 0; i < 7; i++)
		{
			if ((calendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek) && (calendar.getTimeInMillis() > System.currentTimeMillis()))
			{
				return calendar.getTimeInMillis();
			}
			calendar.add(Calendar.DAY_OF_WEEK, 1);
		}
		return calendar.getTimeInMillis();
	}
	
	public static void main(String[] args)
	{
		new SwampOfScreamsSiege();
	}
}
