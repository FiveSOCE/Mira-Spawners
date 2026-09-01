package com.mira.spawners.service;

import com.mira.spawners.MiraSpawnersPlugin;
import com.mira.spawners.util.StackMath;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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
        write(spawner, stackSize, true);
    }

    public void write(CreatureSpawner spawner, int stackSize, boolean managed) {
        PersistentDataContainer pdc = spawner.getPersistentDataContainer();
        pdc.set(stackSizeKey, PersistentDataType.INTEGER,
                StackMath.clamp(stackSize, 1, plugin.maxSpawnerStack()));
        pdc.set(managedKey, PersistentDataType.BYTE, managed ? (byte) 1 : (byte) 0);
        spawner.update(true, false);
    }
}
