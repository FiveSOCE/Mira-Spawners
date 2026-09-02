package com.mira.spawners.service;

import com.mira.spawners.MiraSpawnersPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public final class MobSpawnPolicyService {
    private final MiraSpawnersPlugin plugin;
    private final MobStackService mobStacks;
    private final NamespacedKey policyExemptKey;
    private volatile boolean blockNonSpawnerHostiles;
    private volatile Set<EntityType> fullyBlockedTypes = Set.of();

    public MobSpawnPolicyService(MiraSpawnersPlugin plugin, MobStackService mobStacks) {
        this.plugin = plugin;
        this.mobStacks = mobStacks;
        this.policyExemptKey = new NamespacedKey(plugin, "spawn_policy_exempt");
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

    public boolean isHostile(LivingEntity entity) {
        return entity instanceof Enemy;
    }

    public boolean shouldCancelSpawn(LivingEntity entity, CreatureSpawnEvent.SpawnReason reason) {
        if (entity == null) return false;

        // Explicit command and plugin-created entities are intentional special/event spawns.
        // MiraSpawners must not interfere with them, even when their type is otherwise blocked.
        if (isIntentionalSpawn(reason)) {
            markPolicyExempt(entity);
            return false;
        }

        if (isFullyBlocked(entity.getType())) return true;
        return blockNonSpawnerHostiles
                && isHostile(entity)
                && reason != CreatureSpawnEvent.SpawnReason.SPAWNER;
    }

    public boolean shouldCancelSpawnerSpawn(LivingEntity entity, CreatureSpawner source) {
        if (entity == null) return false;
        if (isPolicyExempt(entity)) return false;
        if (isFullyBlocked(entity.getType())) return true;
        return blockNonSpawnerHostiles
                && isHostile(entity)
                && (source == null || !plugin.spawnerData().isManaged(source));
    }

    public boolean shouldRemoveLoaded(LivingEntity entity) {
        if (entity == null) return false;
        if (isPolicyExempt(entity)) return false;
        if (isFullyBlocked(entity.getType())) return true;
        return blockNonSpawnerHostiles
                && isHostile(entity)
                && !mobStacks.isManaged(entity);
    }

    private boolean isIntentionalSpawn(CreatureSpawnEvent.SpawnReason reason) {
        return reason == CreatureSpawnEvent.SpawnReason.CUSTOM
                || reason == CreatureSpawnEvent.SpawnReason.COMMAND;
    }

    private void markPolicyExempt(LivingEntity entity) {
        entity.getPersistentDataContainer().set(policyExemptKey, PersistentDataType.BYTE, (byte) 1);
    }

    private boolean isPolicyExempt(LivingEntity entity) {
        Byte value = entity.getPersistentDataContainer().get(policyExemptKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }
}
