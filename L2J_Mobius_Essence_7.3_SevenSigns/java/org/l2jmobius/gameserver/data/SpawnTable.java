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
package org.l2jmobius.gameserver.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.spawns.NpcSpawnTemplate;
import org.l2jmobius.gameserver.model.spawns.SpawnGroup;
import org.l2jmobius.gameserver.model.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.model.zone.type.SpawnTerritory;

/**
 * Spawn data retriever.
 * @author Zoey76, Mobius
 */
public class SpawnTable
{
	private static final Logger LOGGER = Logger.getLogger(SpawnTable.class.getName());
	private static final Map<Integer, Set<Spawn>> _spawnTable = new ConcurrentHashMap<>();
	private static final String OTHER_XML_FOLDER = "data/spawns/Others";
	
	/**
	 * Gets the spawn data.
	 * @return the spawn data
	 */
	public Map<Integer, Set<Spawn>> getSpawnTable()
	{
		return _spawnTable;
	}
	
	/**
	 * Gets the spawns for the NPC Id.
	 * @param npcId the NPC Id
	 * @return the spawn set for the given npcId
	 */
	public Set<Spawn> getSpawns(int npcId)
	{
		return _spawnTable.getOrDefault(npcId, Collections.emptySet());
	}
	
	/**
	 * Gets the spawn count for the given NPC ID.
	 * @param npcId the NPC Id
	 * @return the spawn count
	 */
	public int getSpawnCount(int npcId)
	{
		return getSpawns(npcId).size();
	}
	
	/**
	 * Gets a spawn for the given NPC ID.
	 * @param npcId the NPC Id
	 * @return a spawn for the given NPC ID or {@code null}
	 */
	public Spawn getAnySpawn(int npcId)
	{
		return getSpawns(npcId).stream().findFirst().orElse(null);
	}
	
	/**
	 * Adds a new spawn to the spawn table.
	 * @param spawn the spawn to add
	 * @param store if {@code true} it'll be saved in the spawn XML files
	 */
	public synchronized void addNewSpawn(Spawn spawn, boolean store)
	{
		addSpawn(spawn);
		
		if (store)
		{
			// Create output directory if it doesn't exist
			final File outputDirectory = new File(OTHER_XML_FOLDER);
			if (!outputDirectory.exists())
			{
				boolean result = false;
				try
				{
					outputDirectory.mkdir();
					result = true;
				}
				catch (SecurityException se)
				{
					// empty
				}
				if (result)
				{
					LOGGER.info(getClass().getSimpleName() + ": Created directory: " + OTHER_XML_FOLDER);
				}
			}
			
			// XML file for spawn
			final int x = ((spawn.getX() - World.WORLD_X_MIN) >> 15) + World.TILE_X_MIN;
			final int y = ((spawn.getY() - World.WORLD_Y_MIN) >> 15) + World.TILE_Y_MIN;
			final File spawnFile = new File(OTHER_XML_FOLDER + "/" + x + "_" + y + ".xml");
			
			// Write info to XML
			final String spawnId = String.valueOf(spawn.getId());
			final String spawnCount = String.valueOf(spawn.getAmount());
			final String spawnX = String.valueOf(spawn.getX());
			final String spawnY = String.valueOf(spawn.getY());
			final String spawnZ = String.valueOf(spawn.getZ());
			final String spawnHeading = String.valueOf(spawn.getHeading());
			final String spawnDelay = String.valueOf(spawn.getRespawnDelay() / 1000);
			if (spawnFile.exists()) // update
			{
				final File tempFile = new File(spawnFile.getAbsolutePath().substring(Config.DATAPACK_ROOT.getAbsolutePath().length() + 1).replace('\\', '/') + ".tmp");
				try
				{
					final BufferedReader reader = new BufferedReader(new FileReader(spawnFile));
					final BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
					String currentLine;
					while ((currentLine = reader.readLine()) != null)
					{
						if (currentLine.contains("</group>"))
						{
							final NpcTemplate template = NpcData.getInstance().getTemplate(spawn.getId());
							final String title = template.getTitle();
							final String name = title.isEmpty() ? template.getName() : template.getName() + " - " + title;
							writer.write("			<npc id=\"" + spawnId + (spawn.getAmount() > 1 ? "\" count=\"" + spawnCount : "") + "\" x=\"" + spawnX + "\" y=\"" + spawnY + "\" z=\"" + spawnZ + (spawn.getHeading() > 0 ? "\" heading=\"" + spawnHeading : "") + "\" respawnTime=\"" + spawnDelay + "sec\" /> <!-- " + name + " -->" + System.lineSeparator());
							writer.write(currentLine + System.lineSeparator());
							continue;
						}
						writer.write(currentLine + System.lineSeparator());
					}
					writer.close();
					reader.close();
					spawnFile.delete();
					tempFile.renameTo(spawnFile);
				}
				catch (Exception e)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Could not store spawn in the spawn XML files: " + e);
				}
			}
			else // new file
			{
				try
				{
					final NpcTemplate template = NpcData.getInstance().getTemplate(spawn.getId());
					final String title = template.getTitle();
					final String name = title.isEmpty() ? template.getName() : template.getName() + " - " + title;
					final BufferedWriter writer = new BufferedWriter(new FileWriter(spawnFile));
					writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
					writer.write("<list xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"../../xsd/spawns.xsd\">" + System.lineSeparator());
					writer.write("	<spawn name=\"" + x + "_" + y + "\">" + System.lineSeparator());
					writer.write("		<group>" + System.lineSeparator());
					writer.write("			<npc id=\"" + spawnId + (spawn.getAmount() > 1 ? "\" count=\"" + spawnCount : "") + "\" x=\"" + spawnX + "\" y=\"" + spawnY + "\" z=\"" + spawnZ + (spawn.getHeading() > 0 ? "\" heading=\"" + spawnHeading : "") + "\" respawnTime=\"" + spawnDelay + "sec\" /> <!-- " + name + " -->" + System.lineSeparator());
					writer.write("		</group>" + System.lineSeparator());
					writer.write("	</spawn>" + System.lineSeparator());
					writer.write("</list>" + System.lineSeparator());
					writer.close();
					LOGGER.info(getClass().getSimpleName() + ": Created file: " + OTHER_XML_FOLDER + "/" + x + "_" + y + ".xml");
				}
				catch (Exception e)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Spawn " + spawn + " could not be added to the spawn XML files: " + e);
				}
			}
		}
	}
	
	/**
	 * Delete an spawn from the spawn table.
	 * @param spawn the spawn to delete
	 * @param update if {@code true} the spawn XML files will be updated
	 */
	public synchronized void deleteSpawn(Spawn spawn, boolean update)
	{
		if (!removeSpawn(spawn) && !update)
		{
			return;
		}
		
		if (update)
		{
			final int x = ((spawn.getX() - World.WORLD_X_MIN) >> 15) + World.TILE_X_MIN;
			final int y = ((spawn.getY() - World.WORLD_Y_MIN) >> 15) + World.TILE_Y_MIN;
			final NpcSpawnTemplate npcSpawnTemplate = spawn.getNpcSpawnTemplate();
			final File spawnFile = npcSpawnTemplate != null ? npcSpawnTemplate.getSpawnTemplate().getFile() : new File(OTHER_XML_FOLDER + "/" + x + "_" + y + ".xml");
			final File tempFile = new File(spawnFile.getAbsolutePath().substring(Config.DATAPACK_ROOT.getAbsolutePath().length() + 1).replace('\\', '/') + ".tmp");
			try
			{
				final BufferedReader reader = new BufferedReader(new FileReader(spawnFile));
				final BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
				
				boolean found = false; // in XML you can have more than one spawn with same coords
				boolean isMultiLine = false; // in case spawn has more stats
				boolean lastLineFound = false; // used to check for empty file
				int lineCount = 0;
				String currentLine;
				
				final SpawnGroup group = npcSpawnTemplate != null ? npcSpawnTemplate.getGroup() : null;
				List<SpawnTerritory> territories = group != null ? group.getTerritories() : Collections.emptyList();
				boolean simpleTerritory = false;
				if (territories.isEmpty())
				{
					final SpawnTemplate spawnTemplate = npcSpawnTemplate != null ? npcSpawnTemplate.getSpawnTemplate() : null;
					if (spawnTemplate != null)
					{
						territories = spawnTemplate.getTerritories();
						simpleTerritory = true;
					}
				}
				
				if (territories.isEmpty())
				{
					final String spawnId = String.valueOf(spawn.getId());
					final String spawnX = String.valueOf(npcSpawnTemplate != null ? npcSpawnTemplate.getSpawnLocation().getX() : spawn.getX());
					final String spawnY = String.valueOf(npcSpawnTemplate != null ? npcSpawnTemplate.getSpawnLocation().getY() : spawn.getY());
					final String spawnZ = String.valueOf(npcSpawnTemplate != null ? npcSpawnTemplate.getSpawnLocation().getZ() : spawn.getZ());
					
					while ((currentLine = reader.readLine()) != null)
					{
						if (!found)
						{
							if (isMultiLine)
							{
								if (currentLine.contains("</npc>"))
								{
									found = true;
								}
								continue;
							}
							if (currentLine.contains(spawnId) && currentLine.contains(spawnX) && currentLine.contains(spawnY) && currentLine.contains(spawnZ))
							{
								if (!currentLine.contains("/>") && !currentLine.contains("</npc>"))
								{
									isMultiLine = true;
								}
								else
								{
									found = true;
								}
								continue;
							}
						}
						writer.write(currentLine + System.lineSeparator());
						if (currentLine.contains("</list>"))
						{
							lastLineFound = true;
						}
						if (!lastLineFound)
						{
							lineCount++;
						}
					}
				}
				else
				{
					SEARCH: while ((currentLine = reader.readLine()) != null)
					{
						if (!found)
						{
							if (isMultiLine)
							{
								if (currentLine.contains("</group>") || (simpleTerritory && currentLine.contains("<territories>")))
								{
									found = true;
								}
								continue;
							}
							for (SpawnTerritory territory : territories)
							{
								if (currentLine.contains('"' + territory.getName() + '"'))
								{
									isMultiLine = true;
									continue SEARCH;
								}
							}
						}
						writer.write(currentLine + System.lineSeparator());
						if (currentLine.contains("</list>"))
						{
							lastLineFound = true;
						}
						if (!lastLineFound)
						{
							lineCount++;
						}
					}
				}
				
				writer.close();
				reader.close();
				spawnFile.delete();
				tempFile.renameTo(spawnFile);
				// Delete empty file
				if (lineCount < 8)
				{
					LOGGER.info(getClass().getSimpleName() + ": Deleted empty file: " + spawnFile.getAbsolutePath().substring(Config.DATAPACK_ROOT.getAbsolutePath().length() + 1).replace('\\', '/'));
					spawnFile.delete();
				}
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Spawn " + spawn + " could not be removed from the spawn XML files: " + e);
			}
		}
	}
	
	/**
	 * Add a spawn to the spawn set if present, otherwise add a spawn set and add the spawn to the newly created spawn set.
	 * @param spawn the NPC spawn to add
	 */
	private void addSpawn(Spawn spawn)
	{
		_spawnTable.computeIfAbsent(spawn.getId(), k -> ConcurrentHashMap.newKeySet(1)).add(spawn);
	}
	
	/**
	 * Remove a spawn from the spawn set, if the spawn set is empty, remove it as well.
	 * @param spawn the NPC spawn to remove
	 * @return {@code true} if the spawn was successfully removed, {@code false} otherwise
	 */
	private boolean removeSpawn(Spawn spawn)
	{
		final Set<Spawn> set = _spawnTable.get(spawn.getId());
		if (set != null)
		{
			final boolean removed = set.remove(spawn);
			if (set.isEmpty())
			{
				_spawnTable.remove(spawn.getId());
			}
			set.forEach(this::notifyRemoved);
			return removed;
		}
		notifyRemoved(spawn);
		return false;
	}
	
	private void notifyRemoved(Spawn spawn)
	{
		if ((spawn != null) && (spawn.getLastSpawn() != null) && (spawn.getNpcSpawnTemplate() != null))
		{
			spawn.getNpcSpawnTemplate().notifyDespawnNpc(spawn.getLastSpawn());
		}
	}
	
	/**
	 * Execute a procedure over all spawns.<br>
	 * <font size="4" color="red">Do not use it!</font>
	 * @param function the function to execute
	 * @return {@code true} if all procedures were executed, {@code false} otherwise
	 */
	public boolean forEachSpawn(Function<Spawn, Boolean> function)
	{
		for (Set<Spawn> set : _spawnTable.values())
		{
			for (Spawn spawn : set)
			{
				if (!function.apply(spawn))
				{
					return false;
				}
			}
		}
		return true;
	}
	
	public static SpawnTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SpawnTable INSTANCE = new SpawnTable();
	}
}
