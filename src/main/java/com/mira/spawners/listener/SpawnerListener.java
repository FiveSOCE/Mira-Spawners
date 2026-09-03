package com.mira.spawners.listener;

import com.mira.core.api.MiraCore;
import com.mira.spawners.MiraSpawnersPlugin;
import com.mira.spawners.api.event.SpawnerStackChangeEvent;
import com.mira.spawners.service.MobStackService;
import com.mira.spawners.service.SpawnerDataService;
import com.mira.spawners.service.SpawnerItemService;
import com.mira.spawners.util.StackMath;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class SpawnerListener implements Listener {
    private final MiraSpawnersPlugin plugin;
    private final MiraCore core;
    private final SpawnerDataService data;
    private final SpawnerItemService items;
    private final MobStackService mobs;

    public SpawnerListener(MiraSpawnersPlugin plugin, MiraCore core, SpawnerDataService data,
                           SpawnerItemService items, MobStackService mobs) {
        this.plugin = plugin;
        this.core = core;
        this.data = data;
        this.items = items;
        this.mobs = mobs;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerInteract(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.SPAWNER
                || !(clicked.getState() instanceof CreatureSpawner spawner)) return;

        Player player = event.getPlayer();
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (player.hasPermission("miraspawners.inspect")) {
                EntityType type = spawner.getSpawnedType();
                String name = type == null ? "Unknown" : SpawnerItemService.prettyName(type);
                core.messages().send(player, "&f" + name + " Spawner Stack: &a" + data.stackSize(spawner) + "/" + plugin.maxSpawnerStack());
            }
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || player.isSneaking() || !player.hasPermission("miraspawners.stack")) return;
        ItemStack held = event.getItem();
        Optional<EntityType> heldType = items.type(held);
        EntityType existingType = spawner.getSpawnedType();
        EquipmentSlot hand = event.getHand();
        if (heldType.isEmpty() || existingType == null || hand == null || heldType.get() != existingType) return;

        if (plugin.mobSpawnPolicy().isFullyBlocked(existingType)) {
            event.setCancelled(true);
            core.messages().send(player, "&cThat mob type is disabled by MiraSpawners.");
            return;
        }

        int current = data.stackSize(spawner);
        int incoming = items.totalUnits(held);
        StackMath.Transfer transfer = StackMath.transfer(current, incoming, plugin.maxSpawnerStack());
        event.setCancelled(true);
        if (transfer.accepted() <= 0) {
            core.messages().send(player, "&cThat " + SpawnerItemService.prettyName(existingType)
                    + " Spawner is already at the maximum stack size of " + plugin.maxSpawnerStack() + ".");
            return;
        }

        data.setStackSize(spawner, current + transfer.accepted(), player.getUniqueId());
        items.consumeHeldUnits(player, hand, transfer.accepted());
        core.messages().send(player, "&f" + SpawnerItemService.prettyName(existingType)
                + " Spawner Stack: &a" + data.stackSize(spawner) + "/" + plugin.maxSpawnerStack());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.SPAWNER
                || !(event.getBlockPlaced().getState() instanceof CreatureSpawner spawner)) return;

        ItemStack placedItem = event.getItemInHand();
        EntityType type = items.type(placedItem).orElse(spawner.getSpawnedType());
        if (type != null && plugin.mobSpawnPolicy().isFullyBlocked(type)) {
            event.setCancelled(true);
            core.messages().send(event.getPlayer(), "&c" + SpawnerItemService.prettyName(type) + " spawners are disabled by MiraSpawners.");
            return;
        }
        if (type != null) spawner.setSpawnedType(type);
        int amount = items.unitsPerPhysicalItem(placedItem);
        data.write(spawner, amount, true, SpawnerStackChangeEvent.Cause.PLACE, event.getPlayer().getUniqueId());
        if (type != null && amount == 1) {
            plugin.getServer().getPluginManager().callEvent(new SpawnerStackChangeEvent(
                    spawner.getLocation(), type, 0, 1, SpawnerStackChangeEvent.Cause.PLACE, event.getPlayer().getUniqueId()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER
                || !(event.getBlock().getState() instanceof CreatureSpawner spawner)) return;

        EntityType type = spawner.getSpawnedType();
        int oldAmount = data.stackSize(spawner);
        if (type != null) {
            plugin.getServer().getPluginManager().callEvent(new SpawnerStackChangeEvent(
                    spawner.getLocation(), type, oldAmount, 0, SpawnerStackChangeEvent.Cause.BREAK, event.getPlayer().getUniqueId()));
        }

        if (type != null && plugin.mobSpawnPolicy().isFullyBlocked(type)) {
            event.setDropItems(false);
            event.setExpToDrop(0);
            core.messages().send(event.getPlayer(), "&7Removed disabled " + SpawnerItemService.prettyName(type) + " spawner without a drop.");
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("miraspawners.mine")) return;
        if (!plugin.naturalSpawnersHarvestable() && !data.isManaged(spawner)) return;
        if (plugin.silkTouchRequired() && player.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.SILK_TOUCH) <= 0) return;

        event.setDropItems(false);
        event.setExpToDrop(0);
        if (player.getGameMode() == GameMode.CREATIVE || type == null) return;
        ItemStack drop = items.create(type, oldAmount);
        player.getWorld().dropItemNaturally(event.getBlock().getLocation().add(0.5, 0.5, 0.5), drop);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        if (plugin.protectSpawnersFromExplosions()) event.blockList().removeIf(block -> block.getType() == Material.SPAWNER);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        if (plugin.protectSpawnersFromExplosions()) event.blockList().removeIf(block -> block.getType() == Material.SPAWNER);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (plugin.mobSpawnPolicy().shouldCancelSpawnerSpawn(living, event.getSpawner())) {
            event.setCancelled(true);
            return;
        }
        mobs.handleSpawnerSpawn(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        mobs.handleDeath(event);
    }
}
