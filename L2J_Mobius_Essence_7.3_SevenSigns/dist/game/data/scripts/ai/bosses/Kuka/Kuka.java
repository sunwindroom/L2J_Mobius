package ai.bosses.Kuka;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.enums.RaidBossStatus;
import org.l2jmobius.gameserver.instancemanager.DBSpawnManager;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.type.NoRestartZone;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import ai.AbstractNpcAI;

/**
 * @author Symb1OS
 */
public class Kuka extends AbstractNpcAI
{
	// NPCs
	private static final int KHAMIN = 34173;
	private static final int JISRA = 25925;
	private static final int KUKA = 25926;
	// Item
	private static final int PROOF_OF_COURAGE = 48185;
	// Locations
	private static final Location KUKA_LOC = new Location(-58974, 135294, -2392);
	private static final Location JISRA_LOC = new Location(-60496, 137347, -2392);
	private static final Location TELEPORT_START_LOC = new Location(-57406, 136429, -2396);
	private static final Location TELEPORT_OUT_LOC = new Location(-48363, 140230, -2947);
	// Zone
	private static final NoRestartZone ZONE = ZoneManager.getInstance().getZoneByName("kuka_no_restart", NoRestartZone.class);
	// Misc
	private static final String ENTER_ZONE_PATTERN = "30-50 */2 * * *";
	private static final String KUKA_RESPAWN_PATTERN = "50 */2 * * *";
	private static final String KUKA_DESPAWN_PATTERN = "0 */1 * * *";
	private static final String JISRA_DESPAWN_PATTERN = "30 */1 * * *";
	private final SchedulingPattern _enterZonePattern;
	private final SchedulingPattern _respawnKukaPattern;
	private final SchedulingPattern _despawnKukaPattern;
	private final SchedulingPattern _despawnJisraPattern;
	
	private Kuka()
	{
		addFirstTalkId(KHAMIN);
		addEnterZoneId(ZONE.getId());
		// addAttackId(JISRA);
		addKillId(KUKA, JISRA);
		
		_enterZonePattern = new SchedulingPattern(ENTER_ZONE_PATTERN);
		_respawnKukaPattern = new SchedulingPattern(KUKA_RESPAWN_PATTERN);
		_despawnKukaPattern = new SchedulingPattern(KUKA_DESPAWN_PATTERN);
		_despawnJisraPattern = new SchedulingPattern(JISRA_DESPAWN_PATTERN);
		
		ThreadPool.scheduleAtFixedRate(() -> onSpawn(KUKA, KUKA_LOC, _respawnKukaPattern), _respawnKukaPattern.getDelayToNextFromNow(), 1000 * 60 * 60 * 2);
		ThreadPool.scheduleAtFixedRate(() -> onDespawn(KUKA), _despawnKukaPattern.getDelayToNextFromNow(), 1000 * 60 * 60 * 2);
		ThreadPool.scheduleAtFixedRate(() -> onDespawn(JISRA), _despawnJisraPattern.getDelayToNextFromNow(), 1000 * 60 * 60 * 2);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ("teleport".equals(event))
		{
			if (canMoveToZone(player))
			{
				player.teleToLocation(TELEPORT_START_LOC);
				return null;
			}
			
			return "34173-2.html";
		}
		
		return event;
	}
	
	// @Override
	// public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	// {
	// // TODO: Jisra casting self buff.
	// return super.onAttack(npc, attacker, damage, isSummon);
	// }
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final int npcId = npc.getTemplate().getId();
		if (npcId == KUKA)
		{
			npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.S1_ONLY_YOU_CAN_CHALLENGE_JISRA, 2, 5000, true, killer.getName()));
			killer.getEffectList().add(new BuffInfo(npc, killer, SkillData.getInstance().getSkill(PROOF_OF_COURAGE, 1), false, null, null));
			
			for (Creature creature : ZONE.getCharactersInside())
			{
				if (creature.isPlayer() && !creature.getEffectList().isAffectedBySkill(PROOF_OF_COURAGE))
				{
					creature.teleToLocation(TELEPORT_OUT_LOC);
				}
			}
			
			final Npc jisra = onSpawn(JISRA, JISRA_LOC, null);
			addAttackPlayerDesire(jisra, killer);
			
		}
		else if (npcId == JISRA)
		{
			final Npc jisra = DBSpawnManager.getInstance().getNpc(JISRA);
			DBSpawnManager.getInstance().deleteSpawn(jisra.getSpawn(), true);
			jisra.deleteMe();
		}
		
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayer() && !canMoveToZone((Player) creature))
		{
			creature.teleToLocation(TELEPORT_OUT_LOC);
			creature.sendMessage("Nobody can go through the secret pathway now.");
		}
		
		return super.onEnterZone(creature, zone);
	}
	
	private void onDespawn(int bossId)
	{
		for (Spawn spawn : SpawnTable.getInstance().getSpawns(bossId))
		{
			for (Npc monster : spawn.getSpawnedNpcs())
			{
				if (!monster.isDead())
				{
					DBSpawnManager.getInstance().deleteSpawn(spawn, true);
					monster.deleteMe();
				}
			}
		}
	}
	
	private Npc onSpawn(int id, Location location, SchedulingPattern respawnPattern)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(id);
		try
		{
			if (template != null)
			{
				final Spawn spawn = new Spawn(template);
				spawn.setXYZ(location);
				spawn.setRespawnPattern(respawnPattern);
				return DBSpawnManager.getInstance().addNewSpawn(spawn, true);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Caused an exception " + e.getMessage());
		}
		
		return null;
	}
	
	private boolean isAlive(int npc)
	{
		final RaidBossStatus status = DBSpawnManager.getInstance().getStatus(npc);
		return (status == RaidBossStatus.ALIVE) || (status == RaidBossStatus.COMBAT);
	}
	
	private boolean canMoveToZone(Player player)
	{
		return _enterZonePattern.match(System.currentTimeMillis()) || isAlive(KUKA) || player.getEffectList().isAffectedBySkill(PROOF_OF_COURAGE);
	}
	
	public static void main(String[] args)
	{
		new Kuka();
	}
}
