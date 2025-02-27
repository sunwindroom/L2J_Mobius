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
package ai.areas.Heine.LionelHunter;

import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.QuestState;

import ai.AbstractNpcAI;
import quests.Q10514_NobleMaterial4.Q10514_NobleMaterial4;
import quests.Q10515_NobleMaterial4.Q10515_NobleMaterial4;
import quests.Q10516_NobleMaterial4.Q10516_NobleMaterial4;
import quests.Q10517_NobleMaterial4.Q10517_NobleMaterial4;

/**
 * Lionel Hunter AI.
 * @author Stayway / CostyKiller
 */
public class LionelHunter extends AbstractNpcAI
{
	// NPC
	private static final int NPC_LIONEL = 33907;
	// Multisell
	private static final int SHIELD_SIGIL = 3390708;
	
	private LionelHunter()
	{
		addStartNpc(NPC_LIONEL);
		addTalkId(NPC_LIONEL);
		addFirstTalkId(NPC_LIONEL);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "33907.html":
			case "33907-02.html":
			case "33907-01.html":
			case "33907-03.html":
			{
				htmltext = event;
				break;
			}
			case "ExaltedShield":
			{
				if (player.getNobleLevel() > 0)
				{
					final QuestState qs = player.getQuestState(Q10514_NobleMaterial4.class.getSimpleName());
					final QuestState qs1 = player.getQuestState(Q10515_NobleMaterial4.class.getSimpleName());
					final QuestState qs2 = player.getQuestState(Q10516_NobleMaterial4.class.getSimpleName());
					final QuestState qs3 = player.getQuestState(Q10517_NobleMaterial4.class.getSimpleName());
					if (((qs != null) && qs.isCompleted()) || ((qs1 != null) && qs1.isCompleted()) || ((qs2 != null) && qs2.isCompleted()) || ((qs3 != null) && qs3.isCompleted()))
					{
						MultisellData.getInstance().separateAndSend(SHIELD_SIGIL, player, null, false);
						break;
					}
				}
				htmltext = "noreq.html";
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	public static void main(String[] args)
	{
		new LionelHunter();
	}
}
