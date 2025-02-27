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
package ai.others.DarkJudge;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.SystemMessageId;

import ai.AbstractNpcAI;

/**
 * Dark Judge AI.
 * @author St3eT, Mobius
 */
public class DarkJudge extends AbstractNpcAI
{
	// NPC
	private static final int DARK_JUDGE = 30981;
	// Item
	private static final int SCROLL_SIN_EATER = 82965;
	
	private DarkJudge()
	{
		addStartNpc(DARK_JUDGE);
		addTalkId(DARK_JUDGE);
		addFirstTalkId(DARK_JUDGE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "30981.html":
			case "30981-03.html":
			{
				htmltext = event;
				break;
			}
			case "weakenBreath":
			{
				if (player.getShilensBreathDebuffLevel() >= 3)
				{
					player.setShilensBreathDebuffLevel(2);
					htmltext = "30981-01.html";
				}
				else
				{
					htmltext = "30981-02.html";
				}
				break;
			}
			case "trade":
			{
				if ((player.getAdena() < 1000000))
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA_2);
					return null;
				}
				
				if (getQuestItemsCount(player, SCROLL_SIN_EATER) > 0)
				{
					player.sendPacket(SystemMessageId.NO_MORE_ITEMS_CAN_BE_REGISTERED);
					return null;
				}
				
				if (player.getReputation() < 0)
				{
					takeItems(player, Inventory.ADENA_ID, 1000000);
					giveItems(player, SCROLL_SIN_EATER, 1);
					return null;
				}
				
				htmltext = "30981-03.html";
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if ((player.getLevel() < 85) || (player.getReputation() >= 0))
		{
			return "30981-04.html";
		}
		return "30981.html";
	}
	
	public static void main(String[] args)
	{
		new DarkJudge();
	}
}
