package com.mira.spawners.gui;

import com.mira.spawners.MiraSpawnersPlugin;
import com.mira.spawners.service.SpawnerDataService;
import com.mira.spawners.service.SpawnerItemService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class SpawnerSplitGui implements Listener {
    private record Holder(String world, int x, int y, int z) implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    private final MiraSpawnersPlugin plugin;
    private final SpawnerDataService data;
    private final SpawnerItemService items;

    public SpawnerSplitGui(MiraSpawnersPlugin plugin, SpawnerDataService data, SpawnerItemService items) {
        this.plugin = plugin;
        this.data = data;
        this.items = items;
    }

    public boolean open(Player player, Block block) {
        if (!plugin.getConfig().getBoolean("spawners.split-gui.enabled", true)) return false;
        if (block == null || block.getType() != Material.SPAWNER || !(block.getState() instanceof CreatureSpawner spawner)) return false;
        EntityType type = spawner.getSpawnedType();
        if (type == null) return false;
        int stack = data.stackSize(spawner);
        if (stack <= 1) {
            plugin.core().messages().send(player, "&7That spawner stack only contains one spawner.");
            return true;
        }
        Inventory inv = Bukkit.createInventory(new Holder(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()), 27,
                plugin.core().messages().deserialize("&5Split " + SpawnerItemService.prettyName(type) + " Spawner"));
        int[] amounts = {1, 8, 16, 32};
        int[] slots = {10, 11, 12, 13};
        for (int i = 0; i < amounts.length; i++) inv.setItem(slots[i], button(Material.SPAWNER, "&dTake " + amounts[i], amounts[i]));
        inv.setItem(15, button(Material.CHEST, "&dTake Half", Math.max(1, stack / 2)));
        inv.setItem(16, button(Material.HOPPER, "&dTake All But One", stack - 1));
        inv.setItem(22, info(Material.PAPER, "&fCurrent Stack: &a" + stack + "/" + plugin.maxSpawnerStack(), List.of("&7Splitting always leaves at least one", "&7spawner in the placed block.")));
        player.openInventory(inv);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getInventory()) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta() || clicked.getItemMeta().getPersistentDataContainer().has(plugin.splitAmountKey()) == false) return;
        Integer requested = clicked.getItemMeta().getPersistentDataContainer().get(plugin.splitAmountKey(), org.bukkit.persistence.PersistentDataType.INTEGER);
        if (requested == null || requested <= 0) return;
        var world = Bukkit.getWorld(holder.world());
        if (world == null) { player.closeInventory(); return; }
        Block block = world.getBlockAt(holder.x(), holder.y(), holder.z());
        if (block.getType() != Material.SPAWNER || !(block.getState() instanceof CreatureSpawner spawner) || spawner.getSpawnedType() == null) { player.closeInventory(); return; }
        int current = data.stackSize(spawner);
        int take = Math.min(requested, current - 1);
        if (take <= 0) { player.closeInventory(); return; }
        EntityType type = spawner.getSpawnedType();
        data.setStackSize(spawner, current - take, player.getUniqueId());
        items.give(player, type, take);
        plugin.core().messages().send(player, "&aSplit &f" + take + "x " + SpawnerItemService.prettyName(type) + " Spawner&a. Remaining: &f" + (current - take));
        player.closeInventory();
    }

    private ItemStack button(Material material, String name, int amount) {
        ItemStack item = info(material, name, List.of("&7Click to remove these spawners", "&7from the placed stack."));
        item.editMeta(meta -> meta.getPersistentDataContainer().set(plugin.splitAmountKey(), org.bukkit.persistence.PersistentDataType.INTEGER, amount));
        return item;
    }

    private ItemStack info(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.core().messages().deserialize(name));
        meta.lore(lore.stream().map(plugin.core().messages()::deserialize).toList());
        item.setItemMeta(meta);
        return item;
    }
}
