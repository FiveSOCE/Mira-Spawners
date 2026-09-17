package com.mira.spawners.service;

import com.mira.spawners.MiraSpawnersPlugin;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;

public final class SpawnerActivationService {
    private final MiraSpawnersPlugin plugin;
    private final SpawnerDataService data;

    public SpawnerActivationService(MiraSpawnersPlugin plugin, SpawnerDataService data) {
        this.plugin = plugin;
        this.data = data;
    }

    public void normalize(CreatureSpawner spawner) {
        if (!plugin.activateManagedSpawnersInLoadedChunks() || !data.isManaged(spawner)) return;
        if (spawner.getRequiredPlayerRange() == 0) return;

        // Bukkit/Paper treats a required player range <= 0 as always active while
        // players are online. Because the spawner block entity only ticks while its
        // chunk is ticking, this turns chunk activity into the activation boundary
        // instead of a 3D distance check around the physical spawner.
        spawner.setRequiredPlayerRange(0);
        spawner.update(true, false);
    }

    public void normalizeChunk(Chunk chunk) {
        if (!plugin.activateManagedSpawnersInLoadedChunks()) return;
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof CreatureSpawner spawner) normalize(spawner);
        }
    }

    public void normalizeLoadedChunks() {
        if (!plugin.activateManagedSpawnersInLoadedChunks()) return;
        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) normalizeChunk(chunk);
        }
    }
}
