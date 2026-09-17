package com.mira.spawners.listener;

import com.mira.spawners.MiraSpawnersPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.UUID;

/** Bridges MiraLoaders' playerless physical-spawner clock into MiraSpawners. */
public final class LoaderDrivenSpawnerListener implements Listener {
    private static final NamespacedKey MARKER = NamespacedKey.fromString("miraloaders:loader_spawner_spawn");
    private static final NamespacedKey WORLD = NamespacedKey.fromString("miraloaders:loader_spawner_world");
    private static final NamespacedKey X = NamespacedKey.fromString("miraloaders:loader_spawner_x");
    private static final NamespacedKey Y = NamespacedKey.fromString("miraloaders:loader_spawner_y");
    private static final NamespacedKey Z = NamespacedKey.fromString("miraloaders:loader_spawner_z");

    private final MiraSpawnersPlugin plugin;

    public LoaderDrivenSpawnerListener(MiraSpawnersPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLoaderSpawnerSpawn(CreatureSpawnEvent event) {
        LivingEntity living = event.getEntity();
        Byte marker = living.getPersistentDataContainer().get(MARKER, PersistentDataType.BYTE);
        if (marker == null || marker != (byte) 1) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) return;

        CreatureSpawner source = sourceSpawner(living);
        if (source == null) {
            event.setCancelled(true);
            return;
        }

        if (plugin.mobSpawnPolicy().shouldCancelSpawnerSpawn(living, source)) {
            event.setCancelled(true);
            return;
        }

        if (plugin.spawnerData().isManaged(source)) {
            if (isBaby(living)) {
                event.setCancelled(true);
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> validateManagedSpawn(living));
        }

        plugin.mobStacks().handleLoaderSpawnerSpawn(event, source);
    }

    private CreatureSpawner sourceSpawner(LivingEntity living) {
        String worldId = living.getPersistentDataContainer().get(WORLD, PersistentDataType.STRING);
        Integer x = living.getPersistentDataContainer().get(X, PersistentDataType.INTEGER);
        Integer y = living.getPersistentDataContainer().get(Y, PersistentDataType.INTEGER);
        Integer z = living.getPersistentDataContainer().get(Z, PersistentDataType.INTEGER);
        if (worldId == null || x == null || y == null || z == null) return null;

        try {
            World world = plugin.getServer().getWorld(UUID.fromString(worldId));
            if (world == null) return null;
            BlockState state = world.getBlockAt(x, y, z).getState();
            return state instanceof CreatureSpawner spawner ? spawner : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void validateManagedSpawn(LivingEntity entity) {
        if (entity == null || !entity.isValid()) return;
        if (isBaby(entity)) {
            removeJockeyPair(entity);
            return;
        }

        Entity vehicle = entity.getVehicle();
        if (vehicle instanceof Chicken chicken && entity instanceof Zombie) {
            entity.leaveVehicle();
            chicken.remove();
            entity.remove();
            return;
        }

        if (entity instanceof Chicken chicken) {
            for (Entity passenger : new ArrayList<>(chicken.getPassengers())) {
                if (passenger instanceof Zombie zombie) {
                    chicken.eject();
                    zombie.remove();
                    chicken.remove();
                    return;
                }
            }
        }
    }

    private void removeJockeyPair(LivingEntity entity) {
        Entity vehicle = entity.getVehicle();
        if (vehicle instanceof Chicken chicken) {
            entity.leaveVehicle();
            chicken.remove();
        }

        if (entity instanceof Chicken chicken) {
            for (Entity passenger : new ArrayList<>(chicken.getPassengers())) {
                if (passenger instanceof Zombie zombie) zombie.remove();
            }
            chicken.eject();
        }
        entity.remove();
    }

    private boolean isBaby(LivingEntity entity) {
        if (entity instanceof Zombie zombie) return zombie.isBaby();
        if (entity instanceof Ageable ageable) return !ageable.isAdult();
        try {
            var method = entity.getClass().getMethod("isBaby");
            Object value = method.invoke(entity);
            return value instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
