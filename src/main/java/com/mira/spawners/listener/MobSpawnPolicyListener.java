package com.mira.spawners.listener;

import com.mira.spawners.service.MobSpawnPolicyService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public final class MobSpawnPolicyListener implements Listener {
    private final MobSpawnPolicyService policy;

    public MobSpawnPolicyListener(MobSpawnPolicyService policy) {
        this.policy = policy;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (policy.shouldCancelSpawn(event.getEntity(), event.getSpawnReason())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof LivingEntity living && policy.shouldRemoveLoaded(living)) {
                entity.remove();
            }
        }
    }
}
