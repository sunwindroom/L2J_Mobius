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
package ai.others.MysteriousWizard;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;

import ai.AbstractNpcAI;

/**
 * @author Mobius
 */
public class MysteriousWizard extends AbstractNpcAI
{
	private static final int MYSTERIOUS_WIZARD = 31522;
	
	private MysteriousWizard()
	{
		addFirstTalkId(MYSTERIOUS_WIZARD);
		addStartNpc(MYSTERIOUS_WIZARD);
		addTalkId(MYSTERIOUS_WIZARD);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			if (world.getTemplateId() == 254) // Fortress of the Dead
			{
				return "31522-01.html";
			}
			else if (world.getTemplateId() == 255) // Chamber of Prophecies
			{
				return "31522.html";
			}
		}
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new MysteriousWizard();
	}
}
