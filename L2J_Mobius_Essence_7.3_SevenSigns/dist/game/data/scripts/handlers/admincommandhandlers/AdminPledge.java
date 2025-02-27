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
package handlers.admincommandhandlers;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.ClanLevelData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.instancemanager.GlobalVariablesManager;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.GMViewPledgeInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.BuilderUtil;

/**
 * <b>Pledge Manipulation:</b><br>
 * <li>With target in a character without clan:<br>
 * //pledge create clanname
 * <li>With target in a clan leader:<br>
 * //pledge info<br>
 * //pledge dismiss<br>
 * //pledge setlevel level<br>
 * //pledge rep reputation_points
 */
public class AdminPledge implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_pledge"
	};
	
	private static final int REP_POINTS_REWARD_LEVEL = 5;
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command);
		final String cmd = st.nextToken();
		if (cmd == null)
		{
			return false;
		}
		
		switch (cmd)
		{
			case "admin_pledge":
			{
				final WorldObject target = activeChar.getTarget();
				Player player = null;
				if (target instanceof Player)
				{
					player = target.asPlayer();
				}
				else
				{
					player = activeChar;
				}
				
				final String name = player.getName();
				String action = null;
				String parameter = null;
				if (st.hasMoreTokens())
				{
					action = st.nextToken(); // create|info|dismiss|setlevel|rep
				}
				
				if (action == null)
				{
					BuilderUtil.sendSysMessage(activeChar, "Not allowed Action on Clan");
					showMainPage(activeChar);
					return false;
				}
				
				if (!action.equals("create") && !player.isClanLeader())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER).addString(name));
					showMainPage(activeChar);
					return false;
				}
				
				if (st.hasMoreTokens())
				{
					parameter = st.nextToken(); // clanname|nothing|nothing|level|rep_points
				}
				
				switch (action)
				{
					case "create":
					{
						if ((parameter == null) || (parameter.length() == 0))
						{
							BuilderUtil.sendSysMessage(activeChar, "Please, enter clan name.");
							showMainPage(activeChar);
							return false;
						}
						
						final long cet = player.getClanCreateExpiryTime();
						player.setClanCreateExpiryTime(0);
						final Clan clan = ClanTable.getInstance().createClan(player, parameter);
						if (clan != null)
						{
							BuilderUtil.sendSysMessage(activeChar, "Clan " + parameter + " created. Leader: " + player.getName());
							return true;
						}
						
						player.setClanCreateExpiryTime(cet);
						BuilderUtil.sendSysMessage(activeChar, "There was a problem while creating the clan.");
						showMainPage(activeChar);
						return false;
					}
					case "dismiss":
					{
						ClanTable.getInstance().destroyClan(player.getClanId());
						final Clan clan = player.getClan();
						if (clan == null)
						{
							BuilderUtil.sendSysMessage(activeChar, "Clan disbanded.");
							return true;
						}
						
						BuilderUtil.sendSysMessage(activeChar, "There was a problem while destroying the clan.");
						showMainPage(activeChar);
						return false;
					}
					case "info":
					{
						final Clan clan;
						if (parameter != null)
						{
							clan = ClanTable.getInstance().getClanByName(parameter);
						}
						else
						{
							clan = player.getClan();
						}
						activeChar.sendPacket(new GMViewPledgeInfo(clan, player));
						return true;
					}
					case "setlevel":
					{
						if (parameter == null)
						{
							BuilderUtil.sendSysMessage(activeChar, "Usage: //pledge <setlevel|rep> <number>");
							showMainPage(activeChar);
							return false;
						}
						
						final Clan clan = player.getClan();
						int level = clan.getLevel();
						try
						{
							level = Integer.parseInt(parameter);
						}
						catch (NumberFormatException nfe)
						{
							BuilderUtil.sendSysMessage(activeChar, "Level incorrect.");
							BuilderUtil.sendSysMessage(activeChar, "Usage: //pledge <setlevel|rep> <number>");
							showMainPage(activeChar);
							return false;
						}
						
						if ((level >= 0) && (level <= ClanLevelData.getInstance().getMaxLevel()))
						{
							clan.changeLevel(level);
							player.getClan().setExp(activeChar.getObjectId(), ClanLevelData.getInstance().getLevelExp(level));
							BuilderUtil.sendSysMessage(activeChar, "You set level " + level + " for clan " + clan.getName());
							return true;
						}
						
						BuilderUtil.sendSysMessage(activeChar, "Level incorrect.");
						BuilderUtil.sendSysMessage(activeChar, "Usage: //pledge <setlevel|rep> <number>");
						showMainPage(activeChar);
						return false;
					}
					case "rep":
					{
						if (parameter == null)
						{
							BuilderUtil.sendSysMessage(activeChar, "Usage: //pledge <setlevel|rep> <number>");
							showMainPage(activeChar);
							return false;
						}
						
						final Clan clan = player.getClan();
						int points = clan.getReputationScore();
						try
						{
							points = Integer.parseInt(parameter);
						}
						catch (NumberFormatException nfe)
						{
							BuilderUtil.sendSysMessage(activeChar, "Points incorrect.");
							BuilderUtil.sendSysMessage(activeChar, "Usage: //pledge <setlevel|rep> <number>");
							showMainPage(activeChar);
							return false;
						}
						
						if (clan.getLevel() < REP_POINTS_REWARD_LEVEL)
						{
							BuilderUtil.sendSysMessage(activeChar, "Only clans of level 5 or above may receive reputation points.");
							showMainPage(activeChar);
							return false;
						}
						
						try
						{
							clan.addReputationScore(points);
							BuilderUtil.sendSysMessage(activeChar, "You " + (points > 0 ? "add " : "remove ") + Math.abs(points) + " points " + (points > 0 ? "to " : "from ") + clan.getName() + "'s reputation. Their current score is " + clan.getReputationScore());
							showMainPage(activeChar);
							return false;
						}
						catch (Exception e)
						{
							BuilderUtil.sendSysMessage(activeChar, "Usage: //pledge <rep> <number>");
						}
						break;
					}
					case "arena":
					{
						final Clan clan = player.getClan();
						if (clan == null)
						{
							BuilderUtil.sendSysMessage(activeChar, "Target player has no clan!");
							break;
						}
						
						try
						{
							final int stage = Integer.parseInt(parameter);
							GlobalVariablesManager.getInstance().set(GlobalVariablesManager.MONSTER_ARENA_VARIABLE + clan.getId(), stage);
							BuilderUtil.sendSysMessage(activeChar, "You set " + stage + " Monster Arena stage for clan " + clan.getName() + "");
						}
						catch (Exception e)
						{
							BuilderUtil.sendSysMessage(activeChar, "Usage: //pledge arena <number>");
						}
						break;
					}
					default:
					{
						BuilderUtil.sendSysMessage(activeChar, "Clan action not allowed.");
						showMainPage(activeChar);
						return false;
					}
				}
			}
			default:
			{
				BuilderUtil.sendSysMessage(activeChar, "Clan command not allowed.");
				showMainPage(activeChar);
			}
		}
		return false;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void showMainPage(Player activeChar)
	{
		AdminHtml.showAdminHtml(activeChar, "game_menu.htm");
	}
}
