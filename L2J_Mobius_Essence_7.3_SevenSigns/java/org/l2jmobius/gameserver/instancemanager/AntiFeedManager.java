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

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.network.GameClient;

public class AntiFeedManager
{
	protected static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
	
	public static final int GAME_ID = 0;
	public static final int OLYMPIAD_ID = 1;
	public static final int TVT_ID = 2;
	public static final int L2EVENT_ID = 3;
	public static final int OFFLINE_PLAY = 4;
	
	private final Map<Integer, Long> _lastDeathTimes = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, AtomicInteger>> _eventIPs = new ConcurrentHashMap<>();
	
	protected AntiFeedManager()
	{
	}
	
	/**
	 * Set time of the last player's death to current
	 * @param objectId Player's objectId
	 */
	public void setLastDeathTime(int objectId)
	{
		_lastDeathTimes.put(objectId, System.currentTimeMillis());
	}
	
	/**
	 * Check if current kill should be counted as non-feeded.
	 * @param attacker Attacker character
	 * @param target Target character
	 * @return True if kill is non-feeded.
	 */
	public boolean check(Creature attacker, Creature target)
	{
		if (!Config.ANTIFEED_ENABLE)
		{
			return true;
		}
		
		if (target == null)
		{
			return false;
		}
		
		final Player targetPlayer = target.asPlayer();
		if (targetPlayer == null)
		{
			return false;
		}
		
		// Players in offline mode should't be valid targets.
		if (targetPlayer.getClient().isDetached())
		{
			return false;
		}
		
		if ((Config.ANTIFEED_INTERVAL > 0) && _lastDeathTimes.containsKey(targetPlayer.getObjectId()) && ((System.currentTimeMillis() - _lastDeathTimes.get(targetPlayer.getObjectId())) < Config.ANTIFEED_INTERVAL))
		{
			return false;
		}
		
		if (Config.ANTIFEED_DUALBOX && (attacker != null))
		{
			final Player attackerPlayer = attacker.asPlayer();
			if (attackerPlayer == null)
			{
				return false;
			}
			
			final GameClient targetClient = targetPlayer.getClient();
			final GameClient attackerClient = attackerPlayer.getClient();
			if ((targetClient == null) || (attackerClient == null) || targetClient.isDetached() || attackerClient.isDetached())
			{
				// unable to check ip address
				return !Config.ANTIFEED_DISCONNECTED_AS_DUALBOX;
			}
			
			return !targetClient.getIp().equals(attackerClient.getIp());
		}
		
		return true;
	}
	
	/**
	 * Clears all timestamps
	 */
	public void clear()
	{
		_lastDeathTimes.clear();
	}
	
	/**
	 * Register new event for dualbox check. Should be called only once.
	 * @param eventId
	 */
	public void registerEvent(int eventId)
	{
		_eventIPs.putIfAbsent(eventId, new ConcurrentHashMap<>());
	}
	
	/**
	 * @param eventId
	 * @param player
	 * @param max
	 * @return If number of all simultaneous connections from player's IP address lower than max then increment connection count and return true.<br>
	 *         False if number of all simultaneous connections from player's IP address higher than max.
	 */
	public boolean tryAddPlayer(int eventId, Player player, int max)
	{
		return tryAddClient(eventId, player.getClient(), max);
	}
	
	/**
	 * @param eventId
	 * @param client
	 * @param max
	 * @return If number of all simultaneous connections from player's IP address lower than max then increment connection count and return true.<br>
	 *         False if number of all simultaneous connections from player's IP address higher than max.
	 */
	public boolean tryAddClient(int eventId, GameClient client, int max)
	{
		if (client == null)
		{
			return false; // unable to determine IP address
		}
		
		final Map<Integer, AtomicInteger> event = _eventIPs.get(eventId);
		if (event == null)
		{
			return false; // no such event registered
		}
		
		final Integer addrHash = client.getIp().hashCode();
		final AtomicInteger connectionCount = event.computeIfAbsent(addrHash, k -> new AtomicInteger());
		if ((connectionCount.get() + 1) <= (max + Config.DUALBOX_CHECK_WHITELIST.getOrDefault(addrHash, 0)))
		{
			connectionCount.incrementAndGet();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Decreasing number of active connection from player's IP address
	 * @param eventId
	 * @param player
	 * @return true if success and false if any problem detected.
	 */
	public boolean removePlayer(int eventId, Player player)
	{
		return removeClient(eventId, player.getClient());
	}
	
	/**
	 * Decreasing number of active connection from player's IP address
	 * @param eventId
	 * @param client
	 * @return true if success and false if any problem detected.
	 */
	public boolean removeClient(int eventId, GameClient client)
	{
		if (client == null)
		{
			return false; // unable to determine IP address
		}
		
		final Map<Integer, AtomicInteger> event = _eventIPs.get(eventId);
		if (event == null)
		{
			return false; // no such event registered
		}
		
		final Integer addrHash = client.getIp().hashCode();
		return event.computeIfPresent(addrHash, (k, v) ->
		{
			if ((v == null) || (v.decrementAndGet() == 0))
			{
				return null;
			}
			return v;
		}) != null;
	}
	
	/**
	 * Remove player connection IP address from all registered events lists.
	 * @param client
	 */
	public void onDisconnect(GameClient client)
	{
		if (client == null)
		{
			return;
		}
		
		final Player player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isInOfflineMode())
		{
			return;
		}
		
		final String clientIp = client.getIp();
		if (clientIp == null)
		{
			return;
		}
		
		for (Entry<Integer, Map<Integer, AtomicInteger>> entry : _eventIPs.entrySet())
		{
			final int eventId = entry.getKey();
			if (eventId == OLYMPIAD_ID)
			{
				final AtomicInteger count = entry.getValue().get(clientIp.hashCode());
				if ((count != null) && (OlympiadManager.getInstance().isRegistered(player) || (player.getOlympiadGameId() != -1)))
				{
					count.decrementAndGet();
				}
			}
			else
			{
				removeClient(eventId, client);
			}
		}
	}
	
	/**
	 * Clear all entries for this eventId.
	 * @param eventId
	 */
	public void clear(int eventId)
	{
		final Map<Integer, AtomicInteger> event = _eventIPs.get(eventId);
		if (event != null)
		{
			event.clear();
		}
	}
	
	/**
	 * @param player
	 * @param max
	 * @return maximum number of allowed connections (whitelist + max)
	 */
	public int getLimit(Player player, int max)
	{
		return getLimit(player.getClient(), max);
	}
	
	/**
	 * @param client
	 * @param max
	 * @return maximum number of allowed connections (whitelist + max)
	 */
	public int getLimit(GameClient client, int max)
	{
		if (client == null)
		{
			return max;
		}
		
		final Integer addrHash = client.getIp().hashCode();
		int limit = max;
		if (Config.DUALBOX_CHECK_WHITELIST.containsKey(addrHash))
		{
			limit += Config.DUALBOX_CHECK_WHITELIST.get(addrHash);
		}
		return limit;
	}
	
	public static AntiFeedManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AntiFeedManager INSTANCE = new AntiFeedManager();
	}
}