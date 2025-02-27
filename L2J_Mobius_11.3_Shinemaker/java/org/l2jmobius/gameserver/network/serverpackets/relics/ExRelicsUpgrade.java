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
package org.l2jmobius.gameserver.network.serverpackets.relics;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExRelicsUpgrade extends ServerPacket
{
	private final Player _player;
	private final boolean _success;
	private final int _relicId;
	private final int _relicLevel;
	
	public ExRelicsUpgrade(Player player, boolean success, int relicId, int relicLevel)
	{
		_player = player;
		_success = success;
		_relicId = relicId;
		_relicLevel = relicLevel;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_UPGRADE.writeId(this, buffer);
		
		// Take item fee based on relic grade.
		int itemFeeId = 0;
		long itemFeeCount = 0;
		switch (RelicData.getInstance().getRelic(_relicId).getGrade())
		{
			case 1:
			{
				itemFeeId = Config.RELIC_ENHANCEMENT_FEE_NO_GRADE.get(0).getId();
				itemFeeCount = Config.RELIC_ENHANCEMENT_FEE_NO_GRADE.get(0).getCount();
				break;
			}
			case 2:
			{
				itemFeeId = Config.RELIC_ENHANCEMENT_FEE_D_GRADE.get(0).getId();
				itemFeeCount = Config.RELIC_ENHANCEMENT_FEE_D_GRADE.get(0).getCount();
				break;
			}
			case 3:
			{
				itemFeeId = Config.RELIC_ENHANCEMENT_FEE_C_GRADE.get(0).getId();
				itemFeeCount = Config.RELIC_ENHANCEMENT_FEE_C_GRADE.get(0).getCount();
				break;
			}
			case 4:
			{
				itemFeeId = Config.RELIC_ENHANCEMENT_FEE_B_GRADE.get(0).getId();
				itemFeeCount = Config.RELIC_ENHANCEMENT_FEE_B_GRADE.get(0).getCount();
				break;
			}
			case 5:
			{
				itemFeeId = Config.RELIC_ENHANCEMENT_FEE_A_GRADE.get(0).getId();
				itemFeeCount = Config.RELIC_ENHANCEMENT_FEE_A_GRADE.get(0).getCount();
				break;
			}
		}
		
		_player.destroyItemByItemId("RelicsUpgrade", itemFeeId, itemFeeCount, _player, true);
		
		buffer.writeByte(_success);
		buffer.writeInt(_relicId);
		buffer.writeInt(_relicLevel);
	}
}
