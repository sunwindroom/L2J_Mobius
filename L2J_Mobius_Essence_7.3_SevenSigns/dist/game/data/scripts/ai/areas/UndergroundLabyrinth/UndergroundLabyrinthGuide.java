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
package ai.areas.UndergroundLabyrinth;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;

import ai.AbstractNpcAI;

/**
 * @author Liamxroy
 */
public class UndergroundLabyrinthGuide extends AbstractNpcAI
{
	// NPCs
	private static final int ROBIN = 34360;
	// Items
	private static final int MARKS_OF_REPENTANCE = 97783;
	private static final int LCOIN = 91663;
	// Locations
	private static final Location ADEN = new Location(147433, 26627, -2205);
	
	private UndergroundLabyrinthGuide()
	{
		addStartNpc(ROBIN);
		addFirstTalkId(ROBIN);
		addTalkId(ROBIN);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "teleport_out":
			{
				if (!player.isPrisoner())
				{
					player.teleToLocation(ADEN);
				}
				else
				{
					player.sendMessage("You cannot teleport while you are a prisoner.");
				}
				break;
			}
			case "pay_marks":
			{
				if ((player.getInventory().getItemByItemId(MARKS_OF_REPENTANCE) == null) || !(player.getInventory().getItemByItemId(MARKS_OF_REPENTANCE).getCount() < Config.MARK_RELEASE_AMOUNT))
				{
					player.sendMessage("You don't have enough Mark of Repentance. Collect 100 and come back.");
					break;
				}
				
				if (player.destroyItemByItemId("Prison", MARKS_OF_REPENTANCE, Config.MARK_RELEASE_AMOUNT, player, true))
				{
					player.setReputation(-135000);
					player.getPrisonerInfo().processFreedom(false);
				}
				break;
			}
			case "pay_lcoin":
			{
				final long count = Config.LCOIN_RELEASE_AMOUNT;
				if ((player.getInventory().getItemByItemId(LCOIN) == null) || (player.getInventory().getItemByItemId(LCOIN).getCount() < count))
				{
					player.sendMessage("You don't have enough L-Coins for this transaction.");
					break;
				}
				
				if (player.destroyItemByItemId("Prison", LCOIN, count, player, true))
				{
					player.getPrisonerInfo().processFreedom(false);
				}
				break;
			}
		}
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new UndergroundLabyrinthGuide();
	}
}
