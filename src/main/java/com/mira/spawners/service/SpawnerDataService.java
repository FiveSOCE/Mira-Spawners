package com.mira.spawners.service;

import com.mira.spawners.MiraSpawnersPlugin;
import com.mira.spawners.api.event.SpawnerStackChangeEvent;
import com.mira.spawners.util.StackMath;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class SpawnerDataService {
    private final MiraSpawnersPlugin plugin;
    private final NamespacedKey stackSizeKey;
    private final NamespacedKey managedKey;

    public SpawnerDataService(MiraSpawnersPlugin plugin) {
        this.plugin = plugin;
        this.stackSizeKey = new NamespacedKey(plugin, "spawner_stack_size");
        this.managedKey = new NamespacedKey(plugin, "managed_spawner");
    }

    public int stackSize(CreatureSpawner spawner) {
        Integer stored = spawner.getPersistentDataContainer().get(stackSizeKey, PersistentDataType.INTEGER);
        return StackMath.clamp(stored == null ? 1 : stored, 1, plugin.maxSpawnerStack());
    }

    public boolean isManaged(CreatureSpawner spawner) {
        Byte stored = spawner.getPersistentDataContainer().get(managedKey, PersistentDataType.BYTE);
        return stored != null && stored == (byte) 1;
    }

    public void setStackSize(CreatureSpawner spawner, int stackSize) {
        setStackSize(spawner, stackSize, null);
    }

    public void setStackSize(CreatureSpawner spawner, int stackSize, UUID actor) {
        write(spawner, stackSize, true, SpawnerStackChangeEvent.Cause.STACK, actor);
    }

    public void write(CreatureSpawner spawner, int stackSize, boolean managed) {
        write(spawner, stackSize, managed, SpawnerStackChangeEvent.Cause.API, null);
    }

    public void write(CreatureSpawner spawner, int stackSize, boolean managed, SpawnerStackChangeEvent.Cause cause, UUID actor) {
        int oldAmount = stackSize(spawner);
        int newAmount = StackMath.clamp(stackSize, 1, plugin.maxSpawnerStack());
        var type = spawner.getSpawnedType();
        PersistentDataContainer pdc = spawner.getPersistentDataContainer();
        pdc.set(stackSizeKey, PersistentDataType.INTEGER, newAmount);
        pdc.set(managedKey, PersistentDataType.BYTE, managed ? (byte) 1 : (byte) 0);
        spawner.update(true, false);
        // Placement is emitted explicitly by the placement listener as 0 -> full placed stack.
        // This avoids reporting a newly-created block's implicit Bukkit size of 1 as real pre-existing value.
        if (cause != SpawnerStackChangeEvent.Cause.PLACE && oldAmount != newAmount && type != null) {
            plugin.getServer().getPluginManager().callEvent(new SpawnerStackChangeEvent(
                    spawner.getLocation(), type, oldAmount, newAmount, cause, actor));
        }
    }
}
