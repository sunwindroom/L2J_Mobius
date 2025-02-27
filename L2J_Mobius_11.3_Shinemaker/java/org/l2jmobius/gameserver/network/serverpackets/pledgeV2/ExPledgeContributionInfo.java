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
package org.l2jmobius.gameserver.network.serverpackets.pledgeV2;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class ExPledgeContributionInfo extends ServerPacket
{
	private final Player _player;
	private final boolean _cycle;
	private final int _previousClaims;
	private final int[] _reputationRequiredTiers =
	{
		2000,
		7000,
		15000,
		30000,
		45000
	};
	private final int[] _fameRewardTiers =
	{
		1300,
		3300,
		5400,
		10300,
		10600
	};
	
	public ExPledgeContributionInfo(Player player, boolean cycle)
	{
		_player = player;
		_cycle = cycle;
		_previousClaims = player.getVariables().getInt(PlayerVariables.CLAN_CONTRIBUTION_REWARDED_COUNT, 0);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		if (_player.getClan() == null)
		{
			return;
		}
		
		ServerPackets.EX_PLEDGE_CONTRIBUTION_INFO.writeId(this, buffer);
		if (_cycle)
		{
			buffer.writeInt(_player.getClanContribution());
		}
		else
		{
			buffer.writeInt(_player.getVariables().getInt(PlayerVariables.CLAN_CONTRIBUTION_PREVIOUS, 0));
		}
		buffer.writeInt(_player.getClanContributionTotal());
		if (_previousClaims < 4)
		{
			buffer.writeInt(_reputationRequiredTiers[_previousClaims]);
			buffer.writeInt(-1);
			buffer.writeInt(0);
			buffer.writeInt(_fameRewardTiers[_previousClaims]);
		}
		else
		{
			int reputationRequired = 15000 * (_previousClaims);
			int fameReward = 10600 + ((_previousClaims - 4) * 200);
			buffer.writeInt(reputationRequired);
			buffer.writeInt(-1);
			buffer.writeInt(0);
			buffer.writeInt(fameReward);
		}
	}
}
