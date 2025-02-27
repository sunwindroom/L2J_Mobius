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
package events.ChuseokHarvestFestival;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.quest.LongTimeEvent;

/**
 * Full Moon Festival<br>
 * @URL https://l2central.info/main/events_and_promos/2090.html?lang=en
 * @author CostyKiller, Tanatos
 */
public class ChuseokHarvestFestival extends LongTimeEvent
{
	// NPCs
	private static final int MOON_CHILD = 34604;
	private static final int FULL_MOON = 34605;
	// Skills
	private static final SkillHolder MOON_BLESSING = new SkillHolder(34400, 1);
	private static final SkillHolder WISHING_BLESSING = new SkillHolder(39817, 1);
	// Items
	private static final int VITALITY_RUNE = 81206;
	// Misc
	private static final String VITALITY_RUNE_VAR = "VITALITY_RUNE_RECEIVED";
	private static final Location GIRAN_LOC = new Location(81115, 148769, -3464);
	private static final Location RUNE_LOC = new Location(46000, -47746, -792);
	
	private ChuseokHarvestFestival()
	{
		addStartNpc(MOON_CHILD, FULL_MOON);
		addFirstTalkId(MOON_CHILD, FULL_MOON);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "34604.htm":
			case "34604-01.htm":
			case "34605.htm":
			case "34605-01.htm":
			case "34605-02.htm":
			{
				htmltext = event;
				break;
			}
			case "toGiran":
			{
				if (npc.getId() != MOON_CHILD)
				{
					break;
				}
				player.teleToLocation(GIRAN_LOC);
				break;
			}
			case "toRune":
			{
				if (npc.getId() != MOON_CHILD)
				{
					break;
				}
				player.teleToLocation(RUNE_LOC);
				break;
			}
			case "makeWish":
			{
				if (npc.getId() != FULL_MOON)
				{
					break;
				}
				
				if (player.getAccountVariables().getBoolean(VITALITY_RUNE_VAR, false))
				{
					player.sendMessage("This account has already received a gift. The gift can only be given once per account");
					htmltext = "34605-02.htm";
					break;
				}
				
				player.getAccountVariables().set(VITALITY_RUNE_VAR, true);
				player.getAccountVariables().storeMe();
				giveItems(player, VITALITY_RUNE, 1);
				htmltext = "34605-02.htm";
				break;
			}
			// TODO: Craft
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!isEventPeriod())
		{
			if (player.isAffectedBySkill(WISHING_BLESSING))
			{
				player.stopSkillEffects(WISHING_BLESSING.getSkill());
			}
			if (player.isAffectedBySkill(MOON_BLESSING))
			{
				player.stopSkillEffects(MOON_BLESSING.getSkill());
			}
			return;
		}
		
		MOON_BLESSING.getSkill().applyEffects(player, player);
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogout(OnPlayerLogout event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isAffectedBySkill(MOON_BLESSING))
		{
			player.stopSkillEffects(MOON_BLESSING.getSkill());
		}
	}
	
	public static void main(String[] args)
	{
		new ChuseokHarvestFestival();
	}
}
