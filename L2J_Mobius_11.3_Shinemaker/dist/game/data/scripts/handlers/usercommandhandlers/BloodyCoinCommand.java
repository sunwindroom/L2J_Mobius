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
package handlers.usercommandhandlers;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.handler.IUserCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author CostyKiller
 */
public class BloodyCoinCommand implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		180
	};
	
	@Override
	public boolean useUserCommand(int id, Player player)
	{
		if (id != COMMAND_IDS[0])
		{
			return false;
		}
		
		if (player.getLevel() < Config.CONQUEST_TELEPORT_REQUIRED_LEVEL) // Retail level is 110.
		{
			player.sendPacket(SystemMessageId.THE_BLOODY_COIN_SYSTEM_IS_AVAILABLE_TO_CHARACTERS_OF_LV_110);
		}
		else
		{
			final int attackPoints = player.getVariables().getInt("CONQUEST_ATTACK_POINTS", Config.CONQUEST_ATTACK_POINTS);
			final int lifePoints = player.getVariables().getInt("CONQUEST_LIFE_POINTS", Config.CONQUEST_LIFE_POINTS);
			
			player.sendPacket(SystemMessageId.BLOODY_COINS_INFO);
			
			SystemMessage sm1 = new SystemMessage(SystemMessageId.ATTACK_POINTS_S1);
			sm1.addString(Integer.toString(attackPoints));
			player.sendPacket(sm1);
			
			SystemMessage sm2 = new SystemMessage(SystemMessageId.VITALITY_S1);
			sm2.addString(Integer.toString(lifePoints));
			player.sendPacket(sm2);
			
			return true;
		}
		return false;
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}
