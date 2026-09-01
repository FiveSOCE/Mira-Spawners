package com.mira.spawners.service;

import com.mira.spawners.MiraSpawnersPlugin;
import com.mira.spawners.util.StackMath;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SpawnerItemService {
    private final MiraSpawnersPlugin plugin;
    private final NamespacedKey mobTypeKey;
    private final NamespacedKey stackSizeKey;
    private final NamespacedKey tokenKey;

    public SpawnerItemService(MiraSpawnersPlugin plugin) {
        this.plugin = plugin;
        this.mobTypeKey = new NamespacedKey(plugin, "spawner_mob_type");
        this.stackSizeKey = new NamespacedKey(plugin, "spawner_item_stack_size");
        this.tokenKey = new NamespacedKey(plugin, "spawner_item_token");
    }

    public ItemStack create(EntityType type, int stackSize) {
        int safeSize = StackMath.clamp(stackSize, 1, plugin.maxSpawnerStack());
        ItemStack item = new ItemStack(Material.SPAWNER, 1);
        ItemMeta rawMeta = item.getItemMeta();

        PersistentDataContainer pdc = rawMeta.getPersistentDataContainer();
        pdc.set(mobTypeKey, PersistentDataType.STRING, type.name());
        pdc.set(stackSizeKey, PersistentDataType.INTEGER, safeSize);
        pdc.set(tokenKey, PersistentDataType.STRING, UUID.randomUUID().toString());

        rawMeta.displayName(Component.text(prettyName(type) + " Spawner", NamedTextColor.LIGHT_PURPLE));
        rawMeta.lore(List.of(
                Component.text("Stack Size: ", NamedTextColor.GRAY)
                        .append(Component.text(safeSize + "/" + plugin.maxSpawnerStack(), NamedTextColor.GREEN)),
                Component.empty(),
                Component.text("Place to deploy this spawner stack.", NamedTextColor.DARK_GRAY)
        ));

        if (rawMeta instanceof BlockStateMeta blockStateMeta) {
            BlockState state = blockStateMeta.getBlockState();
            if (state instanceof CreatureSpawner spawner) {
                spawner.setSpawnedType(type);
                blockStateMeta.setBlockState(spawner);
            }
        }

        item.setItemMeta(rawMeta);
        return item;
    }

    public Optional<EntityType> type(ItemStack item) {
        if (item == null || item.getType() != Material.SPAWNER || !item.hasItemMeta()) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        String stored = meta.getPersistentDataContainer().get(mobTypeKey, PersistentDataType.STRING);
        if (stored != null) {
            try {
                return Optional.of(EntityType.valueOf(stored));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }

        if (meta instanceof BlockStateMeta blockStateMeta) {
            BlockState state = blockStateMeta.getBlockState();
            if (state instanceof CreatureSpawner spawner && spawner.getSpawnedType() != null) {
                return Optional.of(spawner.getSpawnedType());
            }
        }
        return Optional.empty();
    }

    public int unitsPerPhysicalItem(ItemStack item) {
        if (item == null || item.getType() != Material.SPAWNER || !item.hasItemMeta()) {
            return 1;
        }
        Integer stored = item.getItemMeta().getPersistentDataContainer()
                .get(stackSizeKey, PersistentDataType.INTEGER);
        return StackMath.clamp(stored == null ? 1 : stored, 1, plugin.maxSpawnerStack());
    }

    public int totalUnits(ItemStack item) {
        if (item == null || item.getType() != Material.SPAWNER) {
            return 0;
        }
        long total = (long) unitsPerPhysicalItem(item) * Math.max(1, item.getAmount());
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    public void consumeHeldUnits(Player player, EquipmentSlot hand, int units) {
        if (units <= 0 || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack held = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        Optional<EntityType> type = type(held);
        if (type.isEmpty()) {
            return;
        }

        int remaining = Math.max(0, totalUnits(held) - units);
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(null);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        if (remaining <= 0) {
            return;
        }

        int first = Math.min(plugin.maxSpawnerStack(), remaining);
        ItemStack firstItem = create(type.get(), first);
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(firstItem);
        } else {
            player.getInventory().setItemInMainHand(firstItem);
        }
        remaining -= first;

        while (remaining > 0) {
            int part = Math.min(plugin.maxSpawnerStack(), remaining);
            giveOrDrop(player, create(type.get(), part));
            remaining -= part;
        }
    }

    public void give(Player player, EntityType type, int units) {
        int remaining = units;
        while (remaining > 0) {
            int part = Math.min(plugin.maxSpawnerStack(), remaining);
            giveOrDrop(player, create(type, part));
            remaining -= part;
        }
    }

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        overflow.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    public static String prettyName(EntityType type) {
        String raw = type.name().toLowerCase(Locale.ROOT);
        String[] parts = raw.split("_");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            words.add(Character.toUpperCase(part.charAt(0)) + part.substring(1));
        }
        return String.join(" ", words);
    }
}
