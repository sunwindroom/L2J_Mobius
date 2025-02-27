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
package org.l2jmobius.gameserver.network.clientpackets.dethrone;

import java.util.Collection;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.DailyMissionDataConquest;
import org.l2jmobius.gameserver.instancemanager.GlobalVariablesManager;
import org.l2jmobius.gameserver.model.DailyMissionDataHolder;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.RewardRequest;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.dethrone.ExDethroneDailyMissionComplete;
import org.l2jmobius.gameserver.network.serverpackets.dethrone.ExDethroneDailyMissionGetReward;
import org.l2jmobius.gameserver.network.serverpackets.dethrone.ExDethroneDailyMissionInfo;

/**
 * @author CostyKiller
 */
public class RequestExDethroneDailyMissionGetReward extends ClientPacket
{
	private int _id;
	
	@Override
	protected void readImpl()
	{
		_id = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		if (!getClient().getFloodProtectors().canPerformPlayerAction())
		{
			return;
		}
		
		final Player player = getPlayer();
		if ((player == null) || (player.getClan() == null))
		{
			return;
		}
		
		if (player.hasRequest(RewardRequest.class))
		{
			return;
		}
		player.addRequest(new RewardRequest(player));
		
		final Collection<DailyMissionDataHolder> reward = DailyMissionDataConquest.getInstance().getDailyMissionData(_id);
		if ((reward != null) && !reward.isEmpty())
		{
			for (DailyMissionDataHolder holder : reward)
			{
				if (holder.isDisplayable(player))
				{
					holder.requestReward(player);
					player.sendPacket(new ExDethroneDailyMissionInfo(player, holder));
				}
				player.sendPacket(new ExDethroneDailyMissionComplete(true));
				
				final int personalDethronePoint = 250; // reward
				final int serverDethronePoint = 500; // reward
				final long currentPersonalDethronePoints = player.getVariables().getLong(PlayerVariables.CONQUEST_PERSONAL_POINTS, 0);
				final long currentServerDethronePoints = GlobalVariablesManager.getInstance().getLong("CONQUEST_SERVER_POINTS", 0L);
				player.sendPacket(new ExDethroneDailyMissionGetReward(_id, true, personalDethronePoint, serverDethronePoint));
				player.getVariables().set(PlayerVariables.CONQUEST_PERSONAL_POINTS, currentPersonalDethronePoints + personalDethronePoint);
				GlobalVariablesManager.getInstance().set("CONQUEST_SERVER_POINTS", currentServerDethronePoints + serverDethronePoint);
			}
		}
		
		ThreadPool.schedule(() -> player.removeRequest(RewardRequest.class), 300);
	}
}
