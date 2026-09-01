package com.mira.spawners.command;

import com.mira.core.api.MiraCore;
import com.mira.spawners.MiraSpawnersPlugin;
import com.mira.spawners.service.MobStackService;
import com.mira.spawners.service.SpawnerItemService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class MiraSpawnersCommand implements TabExecutor {
    private static final int TARGET_DISTANCE = 6;

    private final MiraSpawnersPlugin plugin;
    private final MiraCore core;
    private final SpawnerItemService items;
    private final MobStackService mobs;

    public MiraSpawnersCommand(MiraSpawnersPlugin plugin, MiraCore core,
                               SpawnerItemService items, MobStackService mobs) {
        this.plugin = plugin;
        this.core = core;
        this.items = items;
        this.mobs = mobs;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("miraspawners.admin")) {
            core.messages().send(sender, "&cYou do not have permission to use MiraSpawners administration commands.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> giveSpawner(sender, args);
            case "change" -> changeHeldSpawner(sender, args);
            case "stack" -> stackLookedAtSpawner(sender);
            case "info" -> sendInfo(sender);
            case "test" -> runSelfTest(sender);
            case "reload" -> {
                plugin.reloadPluginConfiguration();
                core.messages().send(sender, "&aMiraSpawners configuration reloaded.");
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void giveSpawner(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            core.messages().send(sender, "&eUsage: /mspawners give <spawner> [amount]");
            return;
        }

        EntityType type = parseSpawnerType(args[1]);
        if (type == null) {
            core.messages().send(sender, "&cUnknown spawner type: " + args[1]);
            return;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                core.messages().send(sender, "&cAmount must be a positive whole number.");
                return;
            }
        }

        if (amount < 1 || amount > 100000) {
            core.messages().send(sender, "&cAmount must be between 1 and 100000.");
            return;
        }

        items.give(player, type, amount);
        core.messages().send(player, "&aGave you &f" + amount + "x &f"
                + SpawnerItemService.prettyName(type) + " Spawner&a.");
    }

    private void changeHeldSpawner(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            core.messages().send(sender, "&eUsage: /mspawners change <spawner>");
            return;
        }

        EntityType type = parseSpawnerType(args[1]);
        if (type == null) {
            core.messages().send(sender, "&cUnknown spawner type: " + args[1]);
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() != Material.SPAWNER) {
            core.messages().send(player, "&cHold a spawner in your main hand first.");
            return;
        }

        int units = Math.max(1, items.totalUnits(held));
        player.getInventory().setItemInMainHand(items.create(type, units));
        core.messages().send(player, "&aChanged your held spawner stack to &f"
                + SpawnerItemService.prettyName(type) + " Spawner&a.");
    }

    private void stackLookedAtSpawner(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }

        Block target = player.getTargetBlockExact(TARGET_DISTANCE);
        if (target == null || target.getType() != Material.SPAWNER
                || !(target.getState() instanceof CreatureSpawner spawner)) {
            core.messages().send(player, "&cLook directly at a spawner within " + TARGET_DISTANCE + " blocks.");
            return;
        }

        EntityType type = spawner.getSpawnedType();
        plugin.spawnerData().setStackSize(spawner, plugin.maxSpawnerStack());
        String name = type == null ? "Unknown" : SpawnerItemService.prettyName(type);
        core.messages().send(player, "&a" + name + " Spawner Stack: &f"
                + plugin.maxSpawnerStack() + "/" + plugin.maxSpawnerStack());
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        core.messages().send(sender, "&cThat command can only be run by a player.");
        return null;
    }

    private EntityType parseSpawnerType(String input) {
        String normalized = input.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if (normalized.endsWith("_SPAWNER")) {
            normalized = normalized.substring(0, normalized.length() - "_SPAWNER".length());
        }
        try {
            EntityType type = EntityType.valueOf(normalized);
            return type.isSpawnable() ? type : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<String> spawnerTypes() {
        return Arrays.stream(EntityType.values())
                .filter(EntityType::isSpawnable)
                .map(type -> type.name().toLowerCase(Locale.ROOT))
                .sorted()
                .toList();
    }

    private void sendInfo(CommandSender sender) {
        core.messages().send(sender, "&dMiraSpawners &fv" + plugin.getPluginMeta().getVersion());
        core.messages().send(sender, "&7Spawner stack maximum: &f" + plugin.maxSpawnerStack());
        core.messages().send(sender, "&7Mob stack maximum: &f" + plugin.maxMobStack());
        core.messages().send(sender, "&7Mob merge radius: &f" + plugin.mergeRadius() + " blocks");
        core.messages().send(sender, "&7Lava stack kill: " + (plugin.lavaStackKill() ? "&aEnabled" : "&cDisabled"));
        core.messages().send(sender, "&7Explosion protection: "
                + (plugin.protectSpawnersFromExplosions() ? "&aEnabled" : "&cDisabled"));
    }

    private void runSelfTest(CommandSender sender) {
        List<TestResult> results = new ArrayList<>();
        results.add(check("MiraCore API", plugin.core() != null));
        results.add(check("MiraCore module registration", core.modules().get(plugin.getName()).isPresent()));
        results.add(check("Spawner hard cap", plugin.maxSpawnerStack() >= 1 && plugin.maxSpawnerStack() <= 64));
        results.add(check("Mob stack configuration", plugin.maxMobStack() >= plugin.maxSpawnerStack()));
        results.add(check("Merge radius configuration", plugin.mergeRadius() >= 0.5D));

        try {
            ItemStack testItem = items.create(EntityType.ZOMBIE, plugin.maxSpawnerStack());
            boolean roundTrip = items.type(testItem).orElse(null) == EntityType.ZOMBIE
                    && testItem.getAmount() == plugin.maxSpawnerStack()
                    && items.unitsPerPhysicalItem(testItem) == 1
                    && items.totalUnits(testItem) == plugin.maxSpawnerStack()
                    && testItem.getItemMeta().hasDisplayName()
                    && !testItem.getItemMeta().hasLore();
            results.add(check("Named spawner item round-trip", roundTrip));
        } catch (RuntimeException ex) {
            results.add(check("Named spawner item round-trip", false));
        }

        results.add(check("Mob stack service", mobs != null));

        long passed = results.stream().filter(TestResult::passed).count();
        core.messages().send(sender, "&dMiraSpawners Self-Test &7(" + passed + "/" + results.size() + ")");
        for (TestResult result : results) {
            core.messages().send(sender, (result.passed() ? "&a✔ " : "&c✘ ") + "&f" + result.name());
        }
        if (passed == results.size()) {
            core.messages().send(sender, "&aAll MiraSpawners checks passed.");
        } else {
            core.messages().send(sender, "&cOne or more checks failed. Check the server console before using MiraSpawners.");
        }
    }

    private static TestResult check(String name, boolean passed) {
        return new TestResult(name, passed);
    }

    private void sendHelp(CommandSender sender) {
        core.messages().send(sender, "&dMiraSpawners Commands");
        core.messages().send(sender, "&f/mspawners give <spawner> [amount] &7- Give yourself spawners");
        core.messages().send(sender, "&f/mspawners change <spawner> &7- Change the held spawner type");
        core.messages().send(sender, "&f/mspawners stack &7- Set the spawner you are looking at to 64");
        core.messages().send(sender, "&f/mspawners help &7- Show this help");
        core.messages().send(sender, "&8Diagnostics: /mspawners info, /mspawners test, /mspawners reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("miraspawners.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filter(List.of("help", "give", "change", "stack", "info", "test", "reload"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("change"))) {
            return filter(spawnerTypes(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filter(List.of("1", "16", "32", "64"), args[2]);
        }
        return Collections.emptyList();
    }

    private static List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }

    private record TestResult(String name, boolean passed) {
    }
}
