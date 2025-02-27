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
package org.l2jmobius.gameserver.network.clientpackets.homunculus;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.homunculus.Homunculus;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExActivateHomunculusResult;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExShowHomunculusList;

/**
 * @author Mobius
 */
public class RequestExActivateHomunculus extends ClientPacket
{
	private int _slot;
	private boolean _activate;
	
	@Override
	protected void readImpl()
	{
		_slot = readInt();
		_activate = readByte() == 1; // enabled?
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final int size = player.getHomunculusList().size();
		if (size == 0)
		{
			return;
		}
		
		final Homunculus homunculus = player.getHomunculusList().get(_slot);
		if (homunculus == null)
		{
			return;
		}
		
		for (int i = 0; i < Config.MAX_HOMUNCULUS_COUNT; i++)
		{
			if (size <= i)
			{
				break;
			}
			
			final Homunculus homu = player.getHomunculusList().get(i);
			if (homu == null)
			{
				continue;
			}
			
			if (homu.isActive())
			{
				homu.setActive(false);
				player.getHomunculusList().update(homu);
				player.getHomunculusList().refreshStats(true);
				player.sendPacket(new ExShowHomunculusList(player));
				player.sendPacket(new ExActivateHomunculusResult(false));
			}
		}
		
		if (_activate)
		{
			if (!homunculus.isActive())
			{
				
				homunculus.setActive(true);
				player.getHomunculusList().update(homunculus);
				player.getHomunculusList().refreshStats(true);
				player.sendPacket(new ExShowHomunculusList(player));
				player.sendPacket(new ExActivateHomunculusResult(true));
			}
		}
		else
		{
			if (homunculus.isActive())
			{
				homunculus.setActive(false);
				player.getHomunculusList().update(homunculus);
				player.getHomunculusList().refreshStats(true);
				player.sendPacket(new ExShowHomunculusList(player));
				player.sendPacket(new ExActivateHomunculusResult(false));
			}
		}
	}
}
