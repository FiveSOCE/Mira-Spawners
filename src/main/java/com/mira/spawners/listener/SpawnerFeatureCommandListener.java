package com.mira.spawners.listener;

import com.mira.spawners.MiraSpawnersPlugin;
import com.mira.spawners.gui.SpawnerSplitGui;
import com.mira.spawners.service.SpawnerAnalyticsService;
import com.mira.spawners.service.SpawnerItemService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

public final class SpawnerFeatureCommandListener implements Listener {
    private final MiraSpawnersPlugin plugin;
    private final SpawnerAnalyticsService analytics;
    private final SpawnerSplitGui splitGui;

    public SpawnerFeatureCommandListener(MiraSpawnersPlugin plugin, SpawnerAnalyticsService analytics, SpawnerSplitGui splitGui) {
        this.plugin = plugin;
        this.analytics = analytics;
        this.splitGui = splitGui;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        String[] args = raw.substring(1).trim().split("\\s+");
        if (args.length < 2 || !isAlias(args[0]) || !event.getPlayer().hasPermission("miraspawners.admin")) return;
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "split" -> {
                event.setCancelled(true);
                Block block = event.getPlayer().getTargetBlockExact(6);
                if (block == null || block.getType() != Material.SPAWNER || !splitGui.open(event.getPlayer(), block)) {
                    plugin.core().messages().send(event.getPlayer(), "&cLook directly at a stacked spawner within 6 blocks.");
                }
            }
            case "stats", "efficiency" -> {
                event.setCancelled(true);
                showStats(event.getPlayer());
            }
            case "factionstats", "fstats" -> {
                event.setCancelled(true);
                if (args.length < 3) plugin.core().messages().send(event.getPlayer(), "&eUsage: /mspawners factionstats <faction>");
                else showFaction(event.getPlayer(), args[2]);
            }
            default -> { }
        }
    }

    private void showStats(Player player) {
        var top = analytics.top(10);
        plugin.core().messages().send(player, "&dSpawner Efficiency Stats &7(top 10 by produced units)");
        plugin.core().messages().send(player, "&7Estimated server production: &a" + String.format(Locale.US, "%,.1f", analytics.serverUnitsPerHour()) + " units/hour");
        if (top.isEmpty()) plugin.core().messages().send(player, "&7No tracked spawner production yet.");
        int rank = 1;
        for (var stat : top) {
            plugin.core().messages().send(player, "&d#" + rank++ + " &f" + SpawnerItemService.prettyName(stat.type()) + " x" + stat.stackSize()
                    + " &7| produced &a" + stat.unitsProduced() + " &7| &a" + String.format(Locale.US, "%,.1f", analytics.productionPerHour(stat)) + "/h"
                    + " &7| efficiency &f" + String.format(Locale.US, "%.1f%%", analytics.efficiency(stat))
                    + " &7| rate x&f" + String.format(Locale.US, "%.2f", stat.lastRateMultiplier())
                    + " &8@ " + stat.location().getWorld().getName() + " " + stat.location().getBlockX() + "," + stat.location().getBlockY() + "," + stat.location().getBlockZ());
        }
    }

    private void showFaction(Player player, String faction) {
        var stats = analytics.factionStats(faction);
        plugin.core().messages().send(player, "&dFaction Spawner Analytics: &f" + faction);
        plugin.core().messages().send(player, "&7Spawner blocks: &f" + stats.spawnerBlocks() + " &7Physical spawners: &f" + stats.physicalSpawners());
        plugin.core().messages().send(player, "&7Spawn events: &f" + stats.spawnEvents() + " &7Produced units: &a" + stats.unitsProduced());
        plugin.core().messages().send(player, "&7Estimated production: &a" + String.format(Locale.US, "%,.1f", stats.unitsPerHour()) + " units/hour");
        if (!stats.types().isEmpty()) plugin.core().messages().send(player, "&7Types: &f" + stats.types());
    }

    private boolean isAlias(String value) {
        return value.equalsIgnoreCase("miraspawners") || value.equalsIgnoreCase("mspawners") || value.equalsIgnoreCase("mspawn");
    }
}
