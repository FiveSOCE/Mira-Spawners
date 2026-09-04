package com.mira.spawners;

import com.mira.spawners.api.MiraSpawnersApi;
import com.mira.spawners.service.MobStackService;
import com.mira.spawners.service.SpawnerItemService;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

final class MiraSpawnersApiImpl implements MiraSpawnersApi {
    private final MiraSpawnersPlugin plugin;
    private final SpawnerItemService items;
    private final MobStackService mobs;

    MiraSpawnersApiImpl(MiraSpawnersPlugin plugin, SpawnerItemService items, MobStackService mobs) {
        this.plugin = plugin;
        this.items = items;
        this.mobs = mobs;
    }

    @Override
    public ItemStack createSpawner(EntityType type, int stackSize) {
        return items.create(type, stackSize);
    }

    @Override
    public int maxSpawnerStack() {
        return plugin.maxSpawnerStack();
    }

    @Override
    public int mobStackSize(LivingEntity entity) {
        return mobs.stackSize(entity);
    }

    @Override
    public boolean isManagedMob(LivingEntity entity) {
        return mobs.isManaged(entity);
    }

    @Override
    public double effectiveSpawnerRateMultiplier(Location location) {
        return plugin.multipliers().effective(location);
    }

    @Override
    public double serverUnitsPerHour() {
        return plugin.analytics().serverUnitsPerHour();
    }

    @Override
    public double factionUnitsPerHour(String factionName) {
        return plugin.analytics().factionStats(factionName).unitsPerHour();
    }
}

