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
package org.l2jmobius.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.model.PetData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.olympiad.Hero;

/**
 * @author NviX
 */
public class RankManager
{
	private static final Logger LOGGER = Logger.getLogger(RankManager.class.getName());
	
	public static final Long TIME_LIMIT = 2592000000L; // 30 days in milliseconds
	public static final long CURRENT_TIME = System.currentTimeMillis();
	public static final int PLAYER_LIMIT = 500;
	
	private static final String SELECT_CHARACTERS = "SELECT charId,char_name,level,race,base_class, clanid FROM characters WHERE (" + CURRENT_TIME + " - cast(lastAccess as signed) < " + TIME_LIMIT + ") AND accesslevel = 0 AND level > 39 ORDER BY exp DESC, onlinetime DESC LIMIT " + PLAYER_LIMIT;
	private static final String SELECT_CHARACTERS_PVP = "SELECT charId,char_name,level,race,base_class, clanid, deaths, kills, pvpkills FROM characters WHERE (" + CURRENT_TIME + " - cast(lastAccess as signed) < " + TIME_LIMIT + ") AND accesslevel = 0 AND level > 39 ORDER BY kills DESC, onlinetime DESC LIMIT " + PLAYER_LIMIT;
	private static final String SELECT_CHARACTERS_BY_RACE = "SELECT charId FROM characters WHERE (" + CURRENT_TIME + " - cast(lastAccess as signed) < " + TIME_LIMIT + ") AND accesslevel = 0 AND level > 39 AND race = ? ORDER BY exp DESC, onlinetime DESC LIMIT " + PLAYER_LIMIT;
	private static final String SELECT_PETS = "SELECT characters.charId, pets.exp, characters.char_name, pets.level as petLevel, characters.race as char_race, characters.level as char_level, characters.clanId, pet_evolves.index, pet_evolves.level as evolveLevel, pets.item_obj_id, item_id FROM characters, items, pets, pet_evolves WHERE pets.ownerId = characters.charId AND pet_evolves.itemObjId = items.object_id AND pet_evolves.itemObjId = pets.item_obj_id AND (" + CURRENT_TIME + " - cast(characters.lastAccess as signed) < " + TIME_LIMIT + ") AND characters.accesslevel = 0 AND pets.level > 39 ORDER BY pets.exp DESC, characters.onlinetime DESC LIMIT " + PLAYER_LIMIT;
	private static final String SELECT_CLANS = "SELECT characters.level, characters.char_name, clan_data.clan_id, clan_data.clan_level, clan_data.clan_name, clan_data.reputation_score, clan_data.exp FROM characters, clan_data WHERE characters.charId = clan_data.leader_id AND characters.clanid = clan_data.clan_id AND dissolving_expiry_time = 0 ORDER BY exp DESC LIMIT " + PLAYER_LIMIT;
	
	private static final String GET_CURRENT_CYCLE_DATA = "SELECT characters.char_name, characters.level, characters.base_class, characters.clanid, olympiad_nobles.charId, olympiad_nobles.olympiad_points, olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost FROM characters, olympiad_nobles WHERE characters.charId = olympiad_nobles.charId ORDER BY olympiad_nobles.olympiad_points DESC LIMIT " + PLAYER_LIMIT;
	private static final String GET_HEROES = "SELECT characters.charId, characters.char_name, characters.race, characters.sex, characters.base_class, characters.level, characters.clanid, olympiad_nobles_eom.competitions_won, olympiad_nobles_eom.competitions_lost, olympiad_nobles_eom.olympiad_points, heroes.legend_count, heroes.count FROM heroes, characters, olympiad_nobles_eom WHERE characters.charId = heroes.charId AND characters.charId = olympiad_nobles_eom.charId AND heroes.played = 1 ORDER BY olympiad_nobles_eom.olympiad_points DESC, characters.base_class ASC LIMIT " + RankManager.PLAYER_LIMIT;
	private static final String GET_CHARACTERS_BY_CLASS = "SELECT charId FROM characters WHERE (" + CURRENT_TIME + " - cast(lastAccess as signed) < " + TIME_LIMIT + ") AND accesslevel = 0 AND level > 39 AND characters.base_class = ? ORDER BY exp DESC, onlinetime DESC LIMIT " + PLAYER_LIMIT;
	
	private final Map<Integer, StatSet> _mainList = new ConcurrentHashMap<>();
	private Map<Integer, StatSet> _snapshotList = new ConcurrentHashMap<>();
	private final Map<Integer, StatSet> _mainOlyList = new ConcurrentHashMap<>();
	private Map<Integer, StatSet> _snapshotOlyList = new ConcurrentHashMap<>();
	private final List<HeroInfo> _mainHeroList = new LinkedList<>();
	private List<HeroInfo> _snapshotHeroList = new LinkedList<>();
	private final Map<Integer, StatSet> _mainPvpList = new ConcurrentHashMap<>();
	private Map<Integer, StatSet> _snapshotPvpList = new ConcurrentHashMap<>();
	private final Map<Integer, StatSet> _mainPetList = new ConcurrentHashMap<>();
	private Map<Integer, StatSet> _snapshotPetList = new ConcurrentHashMap<>();
	private final Map<Integer, StatSet> _mainClanList = new ConcurrentHashMap<>();
	private Map<Integer, StatSet> _snapshotClanList = new ConcurrentHashMap<>();
	
	public class HeroInfo
	{
		public String charName;
		public String clanName;
		public int serverId;
		public int race;
		public boolean isMale;
		public int baseClass;
		public int level;
		public int legendCount;
		public int competitionsWon;
		public int competitionsLost;
		public int olympiadPoints;
		public int clanLevel;
		public boolean isTopHero;
		
		HeroInfo(String charName, String clanName, int serverId, int race, boolean isMale, int baseClass, int level, int legendCount, int competitionsWon, int competitionsLost, int olympiadPoints, int clanLevel, boolean isTopHero)
		{
			this.charName = charName;
			this.clanName = clanName;
			this.serverId = serverId;
			this.race = race;
			this.isMale = isMale;
			this.baseClass = baseClass;
			this.level = level;
			this.legendCount = legendCount;
			this.competitionsWon = competitionsWon;
			this.competitionsLost = competitionsLost;
			this.olympiadPoints = olympiadPoints;
			this.clanLevel = clanLevel;
			this.isTopHero = isTopHero;
		}
	}
	
	protected RankManager()
	{
		ThreadPool.scheduleAtFixedRate(this::update, 0, 1800000);
	}
	
	private synchronized void update()
	{
		// Load charIds All
		_snapshotList = _mainList;
		_mainList.clear();
		_snapshotOlyList = _mainOlyList;
		_mainOlyList.clear();
		_snapshotHeroList = _mainHeroList;
		_mainHeroList.clear();
		_snapshotPvpList = _mainPvpList;
		_mainPvpList.clear();
		_snapshotPetList = _mainPetList;
		_mainPetList.clear();
		_snapshotClanList = _mainClanList;
		_mainClanList.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_CHARACTERS))
		{
			try (ResultSet rset = statement.executeQuery())
			{
				int i = 1;
				while (rset.next())
				{
					final StatSet player = new StatSet();
					final int charId = rset.getInt("charId");
					final int classId = rset.getInt("base_class");
					player.set("charId", charId);
					player.set("name", rset.getString("char_name"));
					player.set("level", rset.getInt("level"));
					player.set("classId", rset.getInt("base_class"));
					final int race = rset.getInt("race");
					player.set("race", race);
					loadRaceRank(charId, race, player);
					loadClassRank(charId, classId, player);
					
					final Clan clan = ClanTable.getInstance().getClan(rset.getInt("clanid"));
					if (clan != null)
					{
						player.set("clanName", clan.getName());
					}
					else
					{
						player.set("clanName", "");
					}
					
					_mainList.put(i, player);
					i++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load chars total rank data: " + this + " - " + e.getMessage(), e);
		}
		
		// load olympiad data.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(GET_CURRENT_CYCLE_DATA))
		{
			try (ResultSet rset = statement.executeQuery())
			{
				int i = 1;
				while (rset.next())
				{
					final StatSet player = new StatSet();
					final int charId = rset.getInt("charId");
					player.set("charId", charId);
					player.set("name", rset.getString("char_name"));
					
					final Clan clan = ClanTable.getInstance().getClan(rset.getInt("clanid"));
					if (clan != null)
					{
						player.set("clanName", clan.getName());
						player.set("clanLevel", clan.getLevel());
					}
					else
					{
						player.set("clanName", "");
						player.set("clanLevel", 0);
					}
					
					player.set("level", rset.getInt("level"));
					final int classId = rset.getInt("base_class");
					player.set("classId", classId);
					player.set("competitions_won", rset.getInt("competitions_won"));
					player.set("competitions_lost", rset.getInt("competitions_lost"));
					player.set("olympiad_points", rset.getInt("olympiad_points"));
					
					if (Hero.getInstance().getCompleteHeroes().containsKey(charId))
					{
						final StatSet hero = Hero.getInstance().getCompleteHeroes().get(charId);
						player.set("count", hero.getInt("count", 0));
						player.set("legend_count", hero.getInt("legend_count", 0));
					}
					else
					{
						player.set("count", 0);
						player.set("legend_count", 0);
					}
					
					loadClassRank(charId, classId, player);
					
					_mainOlyList.put(i, player);
					i++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load olympiad total rank data: " + this + " - " + e.getMessage(), e);
		}
		
		if (!Hero.getInstance().getHeroes().isEmpty())
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement(GET_HEROES);
				ResultSet rset = statement.executeQuery())
			{
				boolean isFirstHero = true;
				while (rset.next())
				{
					final String charName = rset.getString("char_name");
					final int clanId = rset.getInt("clanid");
					final String clanName = (clanId > 0) ? ClanTable.getInstance().getClan(clanId).getName() : "";
					final int race = rset.getInt("race");
					final boolean isMale = rset.getInt("sex") != 1;
					final int baseClass = rset.getInt("base_class");
					final int level = rset.getInt("level");
					final int legendCount = rset.getInt("legend_count");
					final int competitionsWon = rset.getInt("competitions_won");
					final int competitionsLost = rset.getInt("competitions_lost");
					final int olympiadPoints = rset.getInt("olympiad_points");
					final int clanLevel = (clanId > 0) ? ClanTable.getInstance().getClan(clanId).getLevel() : 0;
					final boolean isTopHero = isFirstHero;
					_mainHeroList.add(new HeroInfo(charName, clanName, Config.SERVER_ID, race, isMale, baseClass, level, legendCount, competitionsWon, competitionsLost, olympiadPoints, clanLevel, isTopHero));
					isFirstHero = false;
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Could not load Hero and Legend Info rank data: " + this + " - " + e.getMessage(), e);
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_CHARACTERS_PVP))
		{
			try (ResultSet rset = statement.executeQuery())
			{
				int i = 1;
				while (rset.next())
				{
					final StatSet player = new StatSet();
					final int charId = rset.getInt("charId");
					player.set("charId", charId);
					player.set("name", rset.getString("char_name"));
					player.set("level", rset.getInt("level"));
					player.set("classId", rset.getInt("base_class"));
					final int race = rset.getInt("race");
					player.set("race", race);
					player.set("kills", rset.getInt("kills"));
					player.set("deaths", rset.getInt("deaths"));
					player.set("points", rset.getInt("pvpkills"));
					loadRaceRank(charId, race, player);
					
					final Clan clan = ClanTable.getInstance().getClan(rset.getInt("clanid"));
					if (clan != null)
					{
						player.set("clanName", clan.getName());
					}
					else
					{
						player.set("clanName", "");
					}
					
					_mainPvpList.put(i, player);
					i++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load pvp total rank data: " + this + " - " + e.getMessage(), e);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_PETS))
		{
			try (ResultSet rset = statement.executeQuery())
			{
				int i = 1;
				while (rset.next())
				{
					final StatSet pet = new StatSet();
					final int controlledItemObjId = rset.getInt("item_obj_id");
					pet.set("controlledItemObjId", controlledItemObjId);
					pet.set("name", PetDataTable.getInstance().getNameByItemObjectId(controlledItemObjId));
					pet.set("ownerId", rset.getInt("charId"));
					pet.set("owner_name", rset.getString("char_name"));
					pet.set("owner_race", rset.getString("char_race"));
					pet.set("owner_level", rset.getInt("char_level"));
					pet.set("level", rset.getInt("petLevel"));
					pet.set("evolve_level", rset.getInt("evolveLevel"));
					pet.set("exp", rset.getLong("exp"));
					
					final Clan clan = ClanTable.getInstance().getClan(rset.getInt("clanid"));
					if (clan != null)
					{
						pet.set("clanName", clan.getName());
					}
					else
					{
						pet.set("clanName", "");
					}
					
					final PetData petData = PetDataTable.getInstance().getPetDataByItemId(rset.getInt("item_id"));
					pet.set("petType", petData.getType());
					pet.set("npcId", petData.getNpcId());
					
					_mainPetList.put(i++, pet);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load pet total rank data: " + this + " - " + e.getMessage(), e);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_CLANS))
		{
			try (ResultSet rset = statement.executeQuery())
			{
				int i = 1;
				while (rset.next())
				{
					final StatSet player = new StatSet();
					player.set("char_name", rset.getString("char_name"));
					player.set("level", rset.getInt("level"));
					player.set("clan_level", rset.getInt("clan_level"));
					player.set("clan_name", rset.getString("clan_name"));
					player.set("reputation_score", rset.getInt("reputation_score"));
					player.set("exp", rset.getLong("exp"));
					player.set("clan_id", rset.getInt("clan_id"));
					_mainClanList.put(i, player);
					i++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load clan total rank data: " + this + " - " + e.getMessage(), e);
		}
	}
	
	private void loadClassRank(int charId, int classId, StatSet player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(GET_CHARACTERS_BY_CLASS))
		{
			ps.setInt(1, classId);
			try (ResultSet rset = ps.executeQuery())
			{
				int i = 0;
				while (rset.next())
				{
					if (rset.getInt("charId") == charId)
					{
						player.set("classRank", i + 1);
					}
					i++;
				}
				if (i == 0)
				{
					player.set("classRank", 0);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load chars classId olympiad rank data: " + this + " - " + e.getMessage(), e);
		}
	}
	
	private void loadRaceRank(int charId, int race, StatSet player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_CHARACTERS_BY_RACE))
		{
			ps.setInt(1, race);
			try (ResultSet rset = ps.executeQuery())
			{
				int i = 0;
				while (rset.next())
				{
					if (rset.getInt("charId") == charId)
					{
						player.set("raceRank", i + 1);
					}
					i++;
				}
				if (i == 0)
				{
					player.set("raceRank", 0);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load chars race rank data: " + this + " - " + e.getMessage(), e);
		}
	}
	
	public Map<Integer, StatSet> getRankList()
	{
		return _mainList;
	}
	
	public Map<Integer, StatSet> getSnapshotList()
	{
		return _snapshotList;
	}
	
	public Map<Integer, StatSet> getOlyRankList()
	{
		return _mainOlyList;
	}
	
	public Map<Integer, StatSet> getSnapshotOlyList()
	{
		return _snapshotOlyList;
	}
	
	public Collection<HeroInfo> getSnapshotHeroList()
	{
		return _snapshotHeroList;
	}
	
	public Map<Integer, StatSet> getPvpRankList()
	{
		return _mainPvpList;
	}
	
	public Map<Integer, StatSet> getSnapshotPvpRankList()
	{
		return _snapshotPvpList;
	}
	
	public Map<Integer, StatSet> getPetRankList()
	{
		return _mainPetList;
	}
	
	public Map<Integer, StatSet> getSnapshotPetRankList()
	{
		return _snapshotPetList;
	}
	
	public Map<Integer, StatSet> getClanRankList()
	{
		return _mainClanList;
	}
	
	public Map<Integer, StatSet> getSnapshotClanRankList()
	{
		return _snapshotClanList;
	}
	
	public int getPlayerGlobalRank(Player player)
	{
		final int playerOid = player.getObjectId();
		for (Entry<Integer, StatSet> entry : _mainList.entrySet())
		{
			final StatSet stats = entry.getValue();
			if (stats.getInt("charId") != playerOid)
			{
				continue;
			}
			return entry.getKey();
		}
		return 0;
	}
	
	public int getPlayerRaceRank(Player player)
	{
		final int playerOid = player.getObjectId();
		for (StatSet stats : _mainList.values())
		{
			if (stats.getInt("charId") != playerOid)
			{
				continue;
			}
			return stats.getInt("raceRank");
		}
		return 0;
	}
	
	public int getPlayerClassRank(Player player)
	{
		final int playerOid = player.getObjectId();
		for (StatSet stats : _mainList.values())
		{
			if (stats.getInt("charId") != playerOid)
			{
				continue;
			}
			return stats.getInt("classRank");
		}
		return 0;
	}
	
	public Collection<Integer> getTop50()
	{
		final List<Integer> result = new LinkedList<>();
		for (int i = 1; i <= 50; i++)
		{
			final StatSet rank = _mainList.get(i);
			if (rank == null)
			{
				break;
			}
			result.add(rank.getInt("charId"));
		}
		return result;
	}
	
	public static RankManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RankManager INSTANCE = new RankManager();
	}
}