package com.mira.spawners.service;

import com.mira.spawners.MiraSpawnersPlugin;
import com.mira.spawners.util.StackMath;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class MobStackService {
    private final MiraSpawnersPlugin plugin;
    private final SpawnerDataService spawnerData;
    private final NamespacedKey managedKey;
    private final NamespacedKey stackSizeKey;
    private final Random random = new Random();

    public MobStackService(MiraSpawnersPlugin plugin, SpawnerDataService spawnerData) {
        this.plugin = plugin;
        this.spawnerData = spawnerData;
        this.managedKey = new NamespacedKey(plugin, "managed_mob");
        this.stackSizeKey = new NamespacedKey(plugin, "mob_stack_size");
    }

    public void handleSpawnerSpawn(SpawnerSpawnEvent event) {
        if (!(event.getEntity() instanceof LivingEntity incomingEntity)) {
            return;
        }

        CreatureSpawner source = event.getSpawner();
        int incomingCount = source == null ? 1 : spawnerData.stackSize(source);
        incomingCount = StackMath.clamp(incomingCount, 1, plugin.maxMobStack());

        LivingEntity target = findMergeTarget(incomingEntity);
        if (target == null) {
            setStackSize(incomingEntity, incomingCount);
            return;
        }

        int current = stackSize(target);
        StackMath.Transfer transfer = StackMath.transfer(current, incomingCount, plugin.maxMobStack());
        if (transfer.accepted() > 0) {
            setStackSize(target, current + transfer.accepted());
        }

        if (transfer.remainder() == 0) {
            event.setCancelled(true);
        } else {
            setStackSize(incomingEntity, transfer.remainder());
        }
    }

    public void handleDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isManaged(entity)) {
            return;
        }

        int count = stackSize(entity);
        if (count <= 1) {
            return;
        }

        EntityDamageEvent lastDamage = entity.getLastDamageCause();
        boolean directLavaDeath = plugin.lavaStackKill()
                && lastDamage != null
                && lastDamage.getCause() == EntityDamageEvent.DamageCause.LAVA;

        if (directLavaDeath) {
            expandLavaDrops(event, count);
            return;
        }

        int remaining = count - 1;
        Location location = entity.getLocation().clone();
        EntityType type = entity.getType();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            World world = location.getWorld();
            if (world == null) {
                return;
            }
            Entity replacement = world.spawnEntity(location, type);
            if (replacement instanceof LivingEntity living) {
                setStackSize(living, remaining);
            }
        });
    }

    public boolean isManaged(LivingEntity entity) {
        Byte value = entity.getPersistentDataContainer().get(managedKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public int stackSize(LivingEntity entity) {
        Integer value = entity.getPersistentDataContainer().get(stackSizeKey, PersistentDataType.INTEGER);
        return StackMath.clamp(value == null ? 1 : value, 1, plugin.maxMobStack());
    }

    public void setStackSize(LivingEntity entity, int count) {
        int safeCount = StackMath.clamp(count, 1, plugin.maxMobStack());
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(managedKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(stackSizeKey, PersistentDataType.INTEGER, safeCount);

        if (plugin.showMobStackName() && safeCount > 1) {
            entity.setCustomName(SpawnerItemService.prettyName(entity.getType()) + " x" + safeCount);
            entity.setCustomNameVisible(true);
        } else if (safeCount <= 1) {
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
        }
    }

    private LivingEntity findMergeTarget(LivingEntity incoming) {
        double radius = plugin.mergeRadius();
        Collection<Entity> nearby = incoming.getWorld().getNearbyEntities(
                incoming.getLocation(), radius, radius, radius,
                candidate -> candidate instanceof LivingEntity
                        && !candidate.getUniqueId().equals(incoming.getUniqueId())
                        && candidate.getType() == incoming.getType()
                        && !candidate.isDead()
                        && isManaged((LivingEntity) candidate)
                        && stackSize((LivingEntity) candidate) < plugin.maxMobStack()
        );

        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity candidate : nearby) {
            LivingEntity living = (LivingEntity) candidate;
            double distance = living.getLocation().distanceSquared(incoming.getLocation());
            if (distance < closestDistance) {
                closest = living;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private void expandLavaDrops(EntityDeathEvent event, int count) {
        List<ItemStack> firstDrops = event.getDrops().stream().map(ItemStack::clone).toList();
        Map<ItemStack, Integer> totals = new HashMap<>();
        addDrops(totals, firstDrops);

        LivingEntity entity = event.getEntity();
        LootTable lootTable = entity instanceof Mob mob ? mob.getLootTable() : null;
        LootContext context = new LootContext.Builder(entity.getLocation())
                .lootedEntity(entity)
                .build();

        for (int i = 1; i < count; i++) {
            if (lootTable != null) {
                try {
                    addDrops(totals, lootTable.populateLoot(random, context));
                    continue;
                } catch (RuntimeException ignored) {
                    // Fall back to the first mob's already-computed drops for this unit.
                }
            }
            addDrops(totals, firstDrops);
        }

        event.getDrops().clear();
        event.getDrops().addAll(splitDrops(totals));
    }

    private static void addDrops(Map<ItemStack, Integer> totals, Collection<ItemStack> drops) {
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().isAir() || drop.getAmount() <= 0) {
                continue;
            }
            ItemStack key = drop.clone();
            int amount = key.getAmount();
            key.setAmount(1);
            totals.merge(key, amount, Integer::sum);
        }
    }

    private static List<ItemStack> splitDrops(Map<ItemStack, Integer> totals) {
        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<ItemStack, Integer> entry : totals.entrySet()) {
            int remaining = entry.getValue();
            int max = Math.max(1, entry.getKey().getMaxStackSize());
            while (remaining > 0) {
                ItemStack stack = entry.getKey().clone();
                int amount = Math.min(max, remaining);
                stack.setAmount(amount);
                result.add(stack);
                remaining -= amount;
            }
        }
        return result;
    }
}
