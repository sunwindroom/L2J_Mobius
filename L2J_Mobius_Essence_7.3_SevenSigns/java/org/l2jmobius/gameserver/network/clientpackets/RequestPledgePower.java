/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanPrivilege;
import org.l2jmobius.gameserver.network.serverpackets.ManagePledgePower;

public class RequestPledgePower extends ClientPacket
{
	private int _rank;
	private int _action;
	private int _privs;
	
	@Override
	protected void readImpl()
	{
		_rank = readInt();
		_action = readInt();
		if (_action == 2)
		{
			_privs = readInt();
		}
		else
		{
			_privs = 0;
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Clan clan = player.getClan();
		player.sendPacket(new ManagePledgePower(clan, _action, _rank));
		if ((_action == 2) && player.isClanLeader())
		{
			if (_rank == 9)
			{
				// The rights below cannot be bestowed upon Academy members:
				// Join a clan or be dismissed
				// Title management, crest management, master management, level management,
				// bulletin board administration
				// Clan war, right to dismiss, set functions
				// Auction, manage taxes, attack/defend registration, mercenary management
				// => Leaves only CP_CL_VIEW_WAREHOUSE, CP_CH_OPEN_DOOR, CP_CS_OPEN_DOOR?
				_privs &= ClanPrivilege.CL_VIEW_WAREHOUSE.getBitmask() | ClanPrivilege.CH_OPEN_DOOR.getBitmask() | ClanPrivilege.CS_OPEN_DOOR.getBitmask();
			}
			clan.setRankPrivs(_rank, _privs);
		}
	}
}