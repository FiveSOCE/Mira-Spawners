package com.mira.spawners.listener;

import com.mira.spawners.MiraSpawnersPlugin;
import com.mira.spawners.service.SpawnerActivationService;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public final class SpawnerActivationListener implements Listener {
    private final MiraSpawnersPlugin plugin;
    private final SpawnerActivationService activation;

    public SpawnerActivationListener(MiraSpawnersPlugin plugin, SpawnerActivationService activation) {
        this.plugin = plugin;
        this.activation = activation;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        activation.normalizeChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawnerPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.SPAWNER) return;

        // The primary placement listener writes MiraSpawners PDC at HIGHEST.
        // Run next tick so we always read the finalized managed state.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!(event.getBlockPlaced().getState() instanceof CreatureSpawner spawner)) return;
            activation.normalize(spawner);
        });
    }
}
