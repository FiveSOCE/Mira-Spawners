package com.mira.spawners.service;

import com.mira.spawners.MiraSpawnersPlugin;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;

/**
 * Keeps physical and Mira-managed spawners in a sane native state.
 *
 * MiraSpawners used to set managed spawners to requiredPlayerRange=0 so a
 * loaded chunk acted as the activation boundary. That conflicts with the
 * current MiraLoaders contract and can also leave persisted spawners unable to
 * run normally after loaders are removed or expire.
 *
 * Baseline rule now is always vanilla-shaped:
 * - no active loader: a nearby real player is required;
 * - active fueled loader: MiraLoaders supplements the off-player spawn clock.
 */
public final class SpawnerActivationService {
    private final MiraSpawnersPlugin plugin;

    public SpawnerActivationService(MiraSpawnersPlugin plugin, SpawnerDataService ignored) {
        this.plugin = plugin;
    }

    public void normalize(CreatureSpawner spawner) {
        boolean changed = false;

        // Migration repair for old MiraSpawners / MiraLoaders builds which
        // persisted zero as an activation/suppression value.
        if (spawner.getRequiredPlayerRange() <= 0) {
            spawner.setRequiredPlayerRange(plugin.spawnerRequiredPlayerRange());
            changed = true;
        }

        if (spawner.getMaxNearbyEntities() <= 0) {
            spawner.setMaxNearbyEntities(plugin.spawnerMaxNearbyEntities());
            changed = true;
        }

        if (changed) spawner.update(true, false);
    }

    public void normalizeChunk(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities(false)) {
            if (state instanceof CreatureSpawner spawner) normalize(spawner);
        }
    }

    public void normalizeLoadedChunks() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) normalizeChunk(chunk);
        }
    }
}
