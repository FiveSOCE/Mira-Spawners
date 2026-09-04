package com.mira.spawners;

import com.mira.core.api.MiraCore;
import com.mira.core.api.MiraCoreProvider;
import com.mira.core.api.ModuleHealth;
import com.mira.spawners.api.MiraSpawnersApi;
import com.mira.spawners.command.MiraSpawnersCommand;
import com.mira.spawners.gui.SpawnerSplitGui;
import com.mira.spawners.listener.*;
import com.mira.spawners.service.*;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MiraSpawnersPlugin extends JavaPlugin {
    private static final int HARD_MAX_SPAWNER_STACK = 64;

    private MiraCore core;
    private SpawnerDataService spawnerData;
    private SpawnerItemService spawnerItems;
    private MobStackService mobStacks;
    private MobSpawnPolicyService mobSpawnPolicy;
    private SpawnerAnalyticsService analytics;
    private SpawnerMultiplierService multipliers;
    private MiraSpawnersApi api;
    private NamespacedKey splitAmountKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        core = MiraCoreProvider.require();
        spawnerData = new SpawnerDataService(this);
        spawnerItems = new SpawnerItemService(this);
        multipliers = new SpawnerMultiplierService();
        mobStacks = new MobStackService(this, spawnerData);
        mobSpawnPolicy = new MobSpawnPolicyService(this, mobStacks);
        analytics = new SpawnerAnalyticsService(this);
        splitAmountKey = new NamespacedKey(this, "split_amount");
        api = new MiraSpawnersApiImpl(this, spawnerItems, mobStacks);
        SpawnerSplitGui splitGui = new SpawnerSplitGui(this, spawnerData, spawnerItems);

        core.modules().register(this, "MiraSpawners");
        core.services().register(MiraSpawnersApi.class, api);

        getServer().getPluginManager().registerEvents(new SpawnerListener(this, core, spawnerData, spawnerItems, mobStacks), this);
        getServer().getPluginManager().registerEvents(new MobSpawnPolicyListener(mobSpawnPolicy), this);
        getServer().getPluginManager().registerEvents(new SpawnerAnalyticsListener(analytics, spawnerData), this);
        getServer().getPluginManager().registerEvents(splitGui, this);
        getServer().getPluginManager().registerEvents(new SpawnerFeatureCommandListener(this, analytics, splitGui), this);

        MiraSpawnersCommand command = new MiraSpawnersCommand(this, core, spawnerItems, mobStacks);
        PluginCommand pluginCommand = getCommand("miraspawners");
        if (pluginCommand == null) {
            core.modules().setHealth(this, ModuleHealth.UNHEALTHY, "miraspawners command missing from plugin.yml");
            throw new IllegalStateException("miraspawners command missing from plugin.yml");
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        core.modules().setHealth(this, ModuleHealth.HEALTHY, "Spawner stacking, multiplier-aware mob stacking, efficiency analytics and faction analytics ready");
        getLogger().info("MiraSpawners v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (core != null) {
            if (api != null) core.services().unregister(MiraSpawnersApi.class, api);
            core.modules().unregister(this);
        }
    }

    public MiraCore core() { return core; }
    public SpawnerDataService spawnerData() { return spawnerData; }
    public SpawnerItemService spawnerItems() { return spawnerItems; }
    public MobStackService mobStacks() { return mobStacks; }
    public MobSpawnPolicyService mobSpawnPolicy() { return mobSpawnPolicy; }
    public SpawnerAnalyticsService analytics() { return analytics; }
    public SpawnerMultiplierService multipliers() { return multipliers; }
    public NamespacedKey splitAmountKey() { return splitAmountKey; }

    public int maxSpawnerStack() { return Math.max(1, Math.min(HARD_MAX_SPAWNER_STACK, getConfig().getInt("spawners.max-stack-size", HARD_MAX_SPAWNER_STACK))); }
    public boolean silkTouchRequired() { return getConfig().getBoolean("spawners.silk-touch-required", true); }
    public boolean naturalSpawnersHarvestable() { return getConfig().getBoolean("spawners.natural-spawners-harvestable", true); }
    public boolean protectSpawnersFromExplosions() { return getConfig().getBoolean("spawners.protect-from-explosions", true); }
    public int maxMobStack() { return Math.max(1, getConfig().getInt("mobs.max-stack-size", 1000)); }
    public double mergeRadius() { return Math.max(0.5D, getConfig().getDouble("mobs.merge-radius", 6.0D)); }
    public boolean showMobStackName() { return getConfig().getBoolean("mobs.show-stack-name", true); }
    public boolean lavaStackKill() { return getConfig().getBoolean("mobs.lava-stack-kill", true); }

    public void reloadPluginConfiguration() {
        reloadConfig();
        if (mobSpawnPolicy != null) mobSpawnPolicy.reload();
    }
}
