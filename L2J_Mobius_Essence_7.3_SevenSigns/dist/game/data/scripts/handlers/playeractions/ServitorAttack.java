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
package handlers.playeractions;

import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.IPlayerActionHandler;
import org.l2jmobius.gameserver.model.ActionDataHolder;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Servitor Attack player action handler.
 * @author Mobius
 */
public class ServitorAttack implements IPlayerActionHandler
{
	@Override
	public void useAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		if (!player.hasServitors())
		{
			player.sendPacket(SystemMessageId.YOU_DON_T_HAVE_A_SERVITOR);
			return;
		}
		
		final WorldObject target = player.getTarget();
		if (target == null)
		{
			return;
		}
		
		final boolean targetOutOfRange = (player.calculateDistance3D(target) > 3000) || !GeoEngine.getInstance().canMoveToTarget(player, target);
		for (Summon summon : player.getServitors().values())
		{
			if (targetOutOfRange)
			{
				summon.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, player);
			}
			else if (summon.canAttack(target, ctrlPressed))
			{
				summon.doAttack(target);
			}
		}
	}
	
	@Override
	public boolean isPetAction()
	{
		return true;
	}
}