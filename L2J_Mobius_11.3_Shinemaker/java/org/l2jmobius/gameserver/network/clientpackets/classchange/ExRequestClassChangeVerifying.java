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
package org.l2jmobius.gameserver.network.clientpackets.classchange;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.enums.CategoryType;
import org.l2jmobius.gameserver.instancemanager.QuestManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.classchange.ExClassChangeSetAlarm;

/**
 * @author Mobius
 */
public class ExRequestClassChangeVerifying extends ClientPacket
{
	private int _classId;
	
	@Override
	protected void readImpl()
	{
		_classId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_classId != player.getClassId().getId())
		{
			return;
		}
		
		if (player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
		{
			return;
		}
		
		if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
		{
			if (!fourthClassCheck(player))
			{
				return;
			}
		}
		else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP))
		{
			if (!thirdClassCheck(player))
			{
				return;
			}
		}
		else if (player.isInCategory(CategoryType.SECOND_CLASS_GROUP))
		{
			if (!secondClassCheck(player))
			{
				return;
			}
		}
		else if (player.isInCategory(CategoryType.FIRST_CLASS_GROUP))
		{
			if (!firstClassCheck(player))
			{
				return;
			}
		}
		
		player.sendPacket(ExClassChangeSetAlarm.STATIC_PACKET);
	}
	
	private boolean firstClassCheck(Player player)
	{
		if (Config.DISABLE_TUTORIAL)
		{
			return true;
		}
		
		Quest quest = QuestManager.getInstance().getQuest(10009); // Path of Destiny - Beginning
		if (quest != null)
		{
			final QuestState qs = player.getQuestState(quest.getName());
			if ((qs != null) && qs.isStarted())
			{
				return true;
			}
		}
		quest = QuestManager.getInstance().getQuest(10207); // Path of Destiny - Beginning
		if (quest != null)
		{
			final QuestState qs = player.getQuestState(quest.getName());
			if ((qs != null) && qs.isStarted())
			{
				return true;
			}
		}
		quest = QuestManager.getInstance().getQuest(10307); // Path of Destiny - Beginning
		if (quest != null)
		{
			final QuestState qs = player.getQuestState(quest.getName());
			if ((qs != null) && qs.isStarted())
			{
				return true;
			}
		}
		
		return false;
	}
	
	private boolean secondClassCheck(Player player)
	{
		if (Config.DISABLE_TUTORIAL)
		{
			return true;
		}
		
		Quest quest = QuestManager.getInstance().getQuest(10015); // Path of Destiny - Proving
		if (quest != null)
		{
			final QuestState qs = player.getQuestState(quest.getName());
			if ((qs != null) && qs.isStarted())
			{
				return true;
			}
		}
		quest = QuestManager.getInstance().getQuest(20015); // Path of Destiny - Proving
		if (quest != null)
		{
			final QuestState qs = player.getQuestState(quest.getName());
			if ((qs != null) && qs.isStarted())
			{
				return true;
			}
		}
		quest = QuestManager.getInstance().getQuest(30015); // Path of Destiny - Proving
		if (quest != null)
		{
			final QuestState qs = player.getQuestState(quest.getName());
			if ((qs != null) && qs.isStarted())
			{
				return true;
			}
		}
		
		return false;
	}
	
	private boolean thirdClassCheck(Player player)
	{
		if (Config.DISABLE_TUTORIAL)
		{
			return true;
		}
		
		Quest quest = QuestManager.getInstance().getQuest(10024); // Path of Destiny - Conviction
		if (quest != null)
		{
			final QuestState qs = player.getQuestState(quest.getName());
			if ((qs != null) && qs.isStarted())
			{
				return true;
			}
		}
		quest = QuestManager.getInstance().getQuest(10224); // Path of Destiny - Conviction
		if (quest != null)
		{
			final QuestState qs = player.getQuestState(quest.getName());
			if ((qs != null) && qs.isStarted())
			{
				return true;
			}
		}
		quest = QuestManager.getInstance().getQuest(10324); // Path of Destiny - Conviction
		if (quest != null)
		{
			final QuestState qs = player.getQuestState(quest.getName());
			if ((qs != null) && qs.isStarted())
			{
				return true;
			}
		}
		
		return false;
	}
	
	private boolean fourthClassCheck(Player player)
	{
		if (Config.DISABLE_TUTORIAL)
		{
			return true;
		}
		
		Quest quest = QuestManager.getInstance().getQuest(10036); // Path of Destiny - Overcome
		if (quest != null)
		{
			final QuestState qs = player.getQuestState(quest.getName());
			if ((qs != null) && qs.isStarted())
			{
				return true;
			}
		}
		quest = QuestManager.getInstance().getQuest(10236); // Path of Destiny - Overcome
		if (quest != null)
		{
			final QuestState qs = player.getQuestState(quest.getName());
			if ((qs != null) && qs.isStarted())
			{
				return true;
			}
		}
		quest = QuestManager.getInstance().getQuest(10336); // Path of Destiny - Overcome
		if (quest != null)
		{
			final QuestState qs = player.getQuestState(quest.getName());
			if ((qs != null) && qs.isStarted())
			{
				return true;
			}
		}
		
		return false;
	}
}
