package com.mira.spawners.listener;

import com.mira.spawners.api.event.SpawnerStackChangeEvent;
import com.mira.spawners.service.SpawnerAnalyticsService;
import com.mira.spawners.service.SpawnerDataService;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;

public final class SpawnerAnalyticsListener implements Listener {
    private final SpawnerAnalyticsService analytics;
    private final SpawnerDataService data;

    public SpawnerAnalyticsListener(SpawnerAnalyticsService analytics, SpawnerDataService data) {
        this.analytics = analytics;
        this.data = data;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(SpawnerSpawnEvent event) {
        CreatureSpawner source = event.getSpawner();
        if (source != null) {
            int produced = (int) Math.round(data.stackSize(source) * analytics.rateMultiplier(source.getLocation()));
            analytics.recordSpawn(source, Math.max(1, produced));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStackChange(SpawnerStackChangeEvent event) {
        analytics.recordStack(event.location(), event.entityType(), event.newAmount());
    }
}
