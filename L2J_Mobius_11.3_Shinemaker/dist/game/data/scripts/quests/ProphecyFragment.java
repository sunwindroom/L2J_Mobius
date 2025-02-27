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
package quests;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.newquestdata.QuestCondType;

import ai.AbstractNpcAI;
import quests.Q10031_ProphecyMachineRestoration.Q10031_ProphecyMachineRestoration;
import quests.Q10032_ToGereth.Q10032_ToGereth;
import quests.Q10033_ProphecyInterpretation.Q10033_ProphecyInterpretation;
import quests.Q10131_ProphecyMachineRestoration.Q10131_ProphecyMachineRestoration;
import quests.Q10132_ToGereth.Q10132_ToGereth;
import quests.Q10133_ProphecyInterpretation.Q10133_ProphecyInterpretation;
import quests.Q10231_ProphecyMachineRestoration.Q10231_ProphecyMachineRestoration;
import quests.Q10232_ToGereth.Q10232_ToGereth;
import quests.Q10233_ProphecyInterpretation.Q10233_ProphecyInterpretation;
import quests.Q10331_ProphecyMachineRestoration.Q10331_ProphecyMachineRestoration;
import quests.Q10332_ToGereth.Q10332_ToGereth;
import quests.Q10333_ProphecyInterpretation.Q10333_ProphecyInterpretation;

/**
 * @author Mobius
 */
public class ProphecyFragment extends AbstractNpcAI
{
	private ProphecyFragment()
	{
		addItemTalkId(39540); // Prophecy Fragment
		addItemTalkId(39539); // Prophecy Fragment
	}
	
	@Override
	public String onItemTalk(Item item, Player player)
	{
		QuestState questState = player.getQuestState(Q10031_ProphecyMachineRestoration.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10032_ToGereth.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10033_ProphecyInterpretation.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10131_ProphecyMachineRestoration.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10132_ToGereth.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10133_ProphecyInterpretation.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10231_ProphecyMachineRestoration.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10232_ToGereth.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10233_ProphecyInterpretation.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10331_ProphecyMachineRestoration.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10332_ToGereth.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10333_ProphecyInterpretation.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10131_ProphecyMachineRestoration.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10132_ToGereth.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10133_ProphecyInterpretation.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10231_ProphecyMachineRestoration.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10232_ToGereth.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10233_ProphecyInterpretation.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10331_ProphecyMachineRestoration.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10332_ToGereth.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		questState = player.getQuestState(Q10333_ProphecyInterpretation.class.getSimpleName());
		if ((questState != null) && questState.isStarted())
		{
			questState.setCond(QuestCondType.DONE);
			sendEndDialog(player);
			return null;
		}
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new ProphecyFragment();
	}
}
