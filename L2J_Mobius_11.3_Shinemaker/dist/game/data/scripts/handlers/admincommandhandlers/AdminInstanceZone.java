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

import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.BuilderUtil;
import org.l2jmobius.gameserver.util.GMAudit;

public class AdminInstanceZone implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_instancezone",
		"admin_instancezone_clear"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final String target = (activeChar.getTarget() != null) ? activeChar.getTarget().getName() : "no-target";
		GMAudit.auditGMAction(activeChar.getName(), command, target, "");
		if (command.startsWith("admin_instancezone_clear"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final Player player = World.getInstance().getPlayer(st.nextToken());
				final int instanceId = Integer.parseInt(st.nextToken());
				final String name = InstanceManager.getInstance().getInstanceName(instanceId);
				InstanceManager.getInstance().deleteInstanceTime(player, instanceId);
				BuilderUtil.sendSysMessage(activeChar, "Instance zone " + name + " cleared for player " + player.getName());
				player.sendMessage("Admin cleared instance zone " + name + " for you");
				display(activeChar, activeChar); // for refreshing instance window
				return true;
			}
			catch (Exception e)
			{
				BuilderUtil.sendSysMessage(activeChar, "Failed clearing instance time: " + e.getMessage());
				BuilderUtil.sendSysMessage(activeChar, "Usage: //instancezone_clear <playername> [instanceId]");
				return false;
			}
		}
		else if (command.startsWith("admin_instancezone"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (st.hasMoreTokens())
			{
				Player player = null;
				final String playername = st.nextToken();
				
				try
				{
					player = World.getInstance().getPlayer(playername);
				}
				catch (Exception e)
				{
				}
				
				if (player != null)
				{
					display(player, activeChar);
				}
				else
				{
					BuilderUtil.sendSysMessage(activeChar, "The player " + playername + " is not online");
					BuilderUtil.sendSysMessage(activeChar, "Usage: //instancezone [playername]");
					return false;
				}
			}
			else if (activeChar.getTarget() != null)
			{
				if (activeChar.getTarget().isPlayer())
				{
					display(activeChar.getTarget().asPlayer(), activeChar);
				}
			}
			else
			{
				display(activeChar, activeChar);
			}
		}
		return true;
	}
	
	private void display(Player player, Player activeChar)
	{
		final Map<Integer, Long> instanceTimes = InstanceManager.getInstance().getAllInstanceTimes(player);
		final StringBuilder html = new StringBuilder(500 + (instanceTimes.size() * 200));
		html.append("<html><center><table width=260><tr><td width=40><button value=\"Main\" action=\"bypass admin_admin\" width=40 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td width=180><center>Character Instances</center></td><td width=40><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table><br><font color=\"LEVEL\">Instances for " + player.getName() + "</font><center><br><table><tr><td width=150>Name</td><td width=50>Time</td><td width=70>Action</td></tr>");
		for (Entry<Integer, Long> entry : instanceTimes.entrySet())
		{
			int hours = 0;
			int minutes = 0;
			final int id = entry.getKey();
			final long remainingTime = (entry.getValue() - System.currentTimeMillis()) / 1000;
			if (remainingTime > 0)
			{
				hours = (int) (remainingTime / 3600);
				minutes = (int) ((remainingTime % 3600) / 60);
			}
			
			html.append("<tr><td>" + InstanceManager.getInstance().getInstanceName(id) + "</td><td>" + hours + ":" + minutes + "</td><td><button value=\"Clear\" action=\"bypass -h admin_instancezone_clear " + player.getName() + " " + id + "\" width=60 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		}
		
		html.append("</table></html>");
		
		final NpcHtmlMessage ms = new NpcHtmlMessage(0, 1);
		ms.setHtml(html.toString());
		activeChar.sendPacket(ms);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}