package com.mira.spawners.service;

import com.mira.spawners.MiraSpawnersPlugin;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public final class MobSpawnPolicyService {
    private final MiraSpawnersPlugin plugin;
    private volatile boolean blockNonSpawnerHostiles;
    private volatile Set<EntityType> fullyBlockedTypes = Set.of();

    public MobSpawnPolicyService(MiraSpawnersPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        blockNonSpawnerHostiles = plugin.getConfig().getBoolean("mobs.block-non-spawner-hostiles", true);

        EnumSet<EntityType> blocked = EnumSet.noneOf(EntityType.class);
        for (String configured : plugin.getConfig().getStringList("mobs.fully-blocked-types")) {
            if (configured == null || configured.isBlank()) continue;
            String normalized = configured.trim().toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            try {
                blocked.add(EntityType.valueOf(normalized));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Unknown fully blocked mob type in config: " + configured);
            }
        }
        fullyBlockedTypes = Set.copyOf(blocked);
    }

    public boolean blockNonSpawnerHostiles() {
        return blockNonSpawnerHostiles;
    }

    public boolean isFullyBlocked(EntityType type) {
        return type != null && fullyBlockedTypes.contains(type);
    }

    public Set<EntityType> fullyBlockedTypes() {
        return fullyBlockedTypes;
    }

    public boolean shouldCancelSpawn(LivingEntity entity, CreatureSpawnEvent.SpawnReason reason) {
        if (entity == null) return false;
        if (isFullyBlocked(entity.getType())) return true;
        return blockNonSpawnerHostiles
                && entity instanceof Monster
                && reason != CreatureSpawnEvent.SpawnReason.SPAWNER;
    }

    public boolean shouldRemoveLoaded(LivingEntity entity) {
        if (entity == null) return false;
        if (isFullyBlocked(entity.getType())) return true;
        return blockNonSpawnerHostiles
                && entity instanceof Monster
                && entity.getEntitySpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER;
    }
}
