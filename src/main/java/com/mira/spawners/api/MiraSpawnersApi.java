package com.mira.spawners.api;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public interface MiraSpawnersApi {
    ItemStack createSpawner(EntityType type, int stackSize);

    int maxSpawnerStack();

    int mobStackSize(LivingEntity entity);

    boolean isManagedMob(LivingEntity entity);
}
