package com.mira.spawners.command;

import com.mira.core.api.MiraCore;
import com.mira.spawners.MiraSpawnersPlugin;
import com.mira.spawners.service.MobStackService;
import com.mira.spawners.service.SpawnerItemService;
import org.bukkit.Bukkit;
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
            case "info" -> sendInfo(sender);
            case "test" -> runSelfTest(sender);
            case "reload" -> {
                plugin.reloadPluginConfiguration();
                core.messages().send(sender, "&aMiraSpawners configuration reloaded.");
            }
            case "give" -> giveSpawner(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void giveSpawner(CommandSender sender, String[] args) {
        if (args.length < 3) {
            core.messages().send(sender, "&eUsage: /mspawners give <player> <mob> [count]");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            core.messages().send(sender, "&cThat player is not online.");
            return;
        }

        EntityType type;
        try {
            type = EntityType.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            core.messages().send(sender, "&cUnknown entity type: " + args[2]);
            return;
        }

        if (!type.isSpawnable()) {
            core.messages().send(sender, "&c" + type.name() + " cannot be used as a normal spawner entity.");
            return;
        }

        int count = 1;
        if (args.length >= 4) {
            try {
                count = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                core.messages().send(sender, "&cCount must be a positive whole number.");
                return;
            }
        }
        if (count < 1 || count > 100000) {
            core.messages().send(sender, "&cCount must be between 1 and 100000.");
            return;
        }

        items.give(target, type, count);
        core.messages().send(sender, "&aGave " + target.getName() + " &f" + count + "x &d"
                + SpawnerItemService.prettyName(type) + " Spawner&a units.");
        if (!target.equals(sender)) {
            core.messages().send(target, "&aYou received &f" + count + "x &d"
                    + SpawnerItemService.prettyName(type) + " Spawner&a units.");
        }
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
                    && items.unitsPerPhysicalItem(testItem) == plugin.maxSpawnerStack();
            results.add(check("Spawner item PDC round-trip", roundTrip));
        } catch (RuntimeException ex) {
            results.add(check("Spawner item PDC round-trip", false));
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
        core.messages().send(sender, "&f/mspawners info &7- Show current settings");
        core.messages().send(sender, "&f/mspawners test &7- Run runtime diagnostics");
        core.messages().send(sender, "&f/mspawners give <player> <mob> [count] &7- Give spawner units");
        core.messages().send(sender, "&f/mspawners reload &7- Reload config.yml");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("miraspawners.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filter(List.of("info", "test", "give", "reload", "help"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> types = Arrays.stream(EntityType.values())
                    .filter(EntityType::isSpawnable)
                    .map(type -> type.name().toLowerCase(Locale.ROOT))
                    .toList();
            return filter(types, args[2]);
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
