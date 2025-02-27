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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.dethrone.ExDethroneCheckName;
import org.l2jmobius.gameserver.util.Util;

/**
 * @author Negrito8
 */
public class RequestExDethroneCheckName extends ClientPacket
{
	private static final int CHANGE_NAME = 1; // A fee charged with your name. (ok message ?)
	private static final int ALREADY_EXISTS = -1; // The name already exists
	private static final int INVALID_NAME = -2; // Incorrect name. please try again
	private static final int MAXIMUM_NAME_LENGTH = -3; // Enter character title (Up to 16 characters)
	
	private String _conquestName;
	
	@Override
	protected void readImpl()
	{
		_conquestName = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		int result;
		// For some reason, name is starting with a char non-alphanumeric. (SizedString?)
		if (!Util.isAlphaNumeric(_conquestName.substring(1)) || checkRestrictedNames(_conquestName.substring(1)))
		{
			result = INVALID_NAME;
		}
		
		else if (getIdByName(_conquestName) > 0)
		{
			result = ALREADY_EXISTS;
		}
		else if (_conquestName.length() > 16)
		{
			result = MAXIMUM_NAME_LENGTH;
		}
		else
		{
			result = CHANGE_NAME;
		}
		player.sendPacket(new ExDethroneCheckName(result));
	}
	
	private int getIdByName(String name)
	{
		int id = -1;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement pst = con.prepareStatement("SELECT charId FROM character_variables WHERE var = ? and val = ?"))
		{
			pst.setString(1, PlayerVariables.CONQUEST_NAME);
			pst.setString(2, name);
			try (ResultSet resultSet = pst.executeQuery())
			{
				while (resultSet.next())
				{
					id = resultSet.getInt("charId");
				}
			}
		}
		catch (Exception e)
		{
			PacketLogger.warning(getClass().getSimpleName() + ": Could not check existing name " + e.getMessage());
		}
		
		if (id > 0)
		{
			return id;
		}
		
		// Value not found.
		return -1;
	}
	
	private boolean checkRestrictedNames(String name)
	{
		if (Config.FORBIDDEN_NAMES.length > 0)
		{
			for (String forbidden : Config.FORBIDDEN_NAMES)
			{
				if (name.toLowerCase().contains(forbidden.toLowerCase()))
				{
					return true;
				}
			}
		}
		return false;
	}
}
