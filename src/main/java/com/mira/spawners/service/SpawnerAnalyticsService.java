package com.mira.spawners.service;

import com.mira.spawners.MiraSpawnersPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

public final class SpawnerAnalyticsService {
    private final MiraSpawnersPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public SpawnerAnalyticsService(MiraSpawnersPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spawner-analytics.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void recordSpawn(CreatureSpawner spawner, int producedUnits) {
        if (spawner == null || spawner.getSpawnedType() == null) return;
        String base = "spawners." + key(spawner.getLocation());
        long now = System.currentTimeMillis();
        if (!yaml.contains(base + ".first-seen")) yaml.set(base + ".first-seen", now);
        yaml.set(base + ".last-seen", now);
        yaml.set(base + ".world", spawner.getWorld().getUID().toString());
        yaml.set(base + ".x", spawner.getX());
        yaml.set(base + ".y", spawner.getY());
        yaml.set(base + ".z", spawner.getZ());
        yaml.set(base + ".type", spawner.getSpawnedType().name());
        yaml.set(base + ".stack-size", plugin.spawnerData().stackSize(spawner));
        yaml.set(base + ".spawn-events", yaml.getLong(base + ".spawn-events", 0L) + 1L);
        yaml.set(base + ".units-produced", yaml.getLong(base + ".units-produced", 0L) + Math.max(1, producedUnits));
        save();
    }

    public synchronized void recordStack(Location location, EntityType type, int newAmount) {
        if (location == null || type == null) return;
        String base = "spawners." + key(location);
        if (newAmount <= 0) {
            yaml.set(base + ".removed-at", System.currentTimeMillis());
            yaml.set(base + ".stack-size", 0);
        } else {
            if (!yaml.contains(base + ".first-seen")) yaml.set(base + ".first-seen", System.currentTimeMillis());
            yaml.set(base + ".world", location.getWorld() == null ? "" : location.getWorld().getUID().toString());
            yaml.set(base + ".x", location.getBlockX());
            yaml.set(base + ".y", location.getBlockY());
            yaml.set(base + ".z", location.getBlockZ());
            yaml.set(base + ".type", type.name());
            yaml.set(base + ".stack-size", newAmount);
            yaml.set(base + ".removed-at", null);
        }
        save();
    }

    public synchronized List<SpawnerStat> top(int limit) {
        List<SpawnerStat> values = allActive();
        values.sort(Comparator.comparingLong(SpawnerStat::unitsProduced).reversed());
        return values.size() <= limit ? values : List.copyOf(values.subList(0, limit));
    }

    public synchronized FactionStats factionStats(String factionName) {
        long units = 0, events = 0;
        int physical = 0, blocks = 0;
        Map<EntityType,Integer> types = new EnumMap<>(EntityType.class);
        for (SpawnerStat stat : allActive()) {
            String owner = territoryFaction(stat.location());
            if (owner == null || !owner.equalsIgnoreCase(factionName)) continue;
            units += stat.unitsProduced();
            events += stat.spawnEvents();
            physical += stat.stackSize();
            blocks++;
            types.merge(stat.type(), stat.stackSize(), Integer::sum);
        }
        return new FactionStats(factionName, blocks, physical, events, units, Map.copyOf(types));
    }

    public double efficiency(SpawnerStat stat) {
        if (stat == null || stat.stackSize() <= 0) return 0D;
        long until = stat.lastSeen() <= stat.firstSeen() ? System.currentTimeMillis() : stat.lastSeen();
        double hours = Math.max(1D / 60D, (until - stat.firstSeen()) / 3_600_000D);
        double target = Math.max(1D, plugin.getConfig().getDouble("analytics.target-units-per-hour-per-spawner", 144D));
        double expected = target * hours * stat.stackSize();
        return expected <= 0 ? 0D : (stat.unitsProduced() / expected) * 100D;
    }

    private List<SpawnerStat> allActive() {
        ConfigurationSection root = yaml.getConfigurationSection("spawners");
        if (root == null) return new ArrayList<>();
        List<SpawnerStat> out = new ArrayList<>();
        for (String id : root.getKeys(false)) {
            String base = "spawners." + id;
            int stack = yaml.getInt(base + ".stack-size", 0);
            if (stack <= 0) continue;
            try {
                UUID worldId = UUID.fromString(yaml.getString(base + ".world", ""));
                World world = Bukkit.getWorld(worldId);
                if (world == null) continue;
                EntityType type = EntityType.valueOf(yaml.getString(base + ".type", "PIG"));
                Location loc = new Location(world, yaml.getInt(base + ".x"), yaml.getInt(base + ".y"), yaml.getInt(base + ".z"));
                out.add(new SpawnerStat(loc, type, stack, yaml.getLong(base + ".spawn-events", 0L), yaml.getLong(base + ".units-produced", 0L), yaml.getLong(base + ".first-seen", 0L), yaml.getLong(base + ".last-seen", 0L)));
            } catch (Exception ignored) { }
        }
        return out;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String territoryFaction(Location location) {
        try {
            Class<?> apiClass = Class.forName("com.mira.factions.api.MiraFactionsApi");
            Object api = Bukkit.getServicesManager().load((Class) apiClass);
            if (api == null) return null;
            Method method = apiClass.getMethod("territoryFaction", Location.class);
            Object result = method.invoke(api, location);
            if (result instanceof Optional<?> optional) return optional.map(Object::toString).orElse(null);
        } catch (Throwable ignored) { }
        return null;
    }

    private String key(Location location) {
        return location.getWorld().getUID() + ";" + location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
    }
    private void save() {
        try { yaml.save(file); }
        catch (Exception ex) { plugin.getLogger().warning("Could not save spawner-analytics.yml: " + ex.getMessage()); }
    }

    public record SpawnerStat(Location location, EntityType type, int stackSize, long spawnEvents, long unitsProduced, long firstSeen, long lastSeen) {}
    public record FactionStats(String faction, int spawnerBlocks, int physicalSpawners, long spawnEvents, long unitsProduced, Map<EntityType,Integer> types) {}
}
