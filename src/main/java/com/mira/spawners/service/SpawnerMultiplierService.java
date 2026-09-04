package com.mira.spawners.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

public final class SpawnerMultiplierService {
    private static final String CHANNEL = "spawner_rate";

    public double effective(Location location) {
        double multiplier = globalBoosterMultiplier();
        multiplier *= outpostMultiplier(location);
        if (!Double.isFinite(multiplier)) return 1.0D;
        return Math.max(0.01D, multiplier);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private double globalBoosterMultiplier() {
        try {
            Class<?> apiClass = Class.forName("com.mira.boosters.api.MiraBoostersApi");
            Object api = Bukkit.getServicesManager().load((Class) apiClass);
            if (api == null) return 1.0D;
            Object value = apiClass.getMethod("globalMultiplier", String.class).invoke(api, CHANNEL);
            return value instanceof Number number ? Math.max(0.01D, number.doubleValue()) : 1.0D;
        } catch (Throwable ignored) {
            return 1.0D;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private double outpostMultiplier(Location location) {
        String faction = territoryFaction(location);
        if (faction == null || faction.isBlank()) return 1.0D;
        try {
            Class<?> apiClass = Class.forName("gg.mira.outposts.MiraOutpostsPlugin$OutpostsApi");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration((Class) apiClass);
            if (registration == null) return 1.0D;
            Object api = registration.getProvider();
            Method outpostsMethod = apiClass.getMethod("outposts");
            Object views = outpostsMethod.invoke(api);
            if (!(views instanceof Collection<?> collection)) return 1.0D;

            double result = 1.0D;
            for (Object view : collection) {
                if (view == null) continue;
                Method ownerName = view.getClass().getMethod("ownerName");
                Method channel = view.getClass().getMethod("channel");
                Method multiplier = view.getClass().getMethod("multiplier");
                Object owner = ownerName.invoke(view);
                Object channelValue = channel.invoke(view);
                Object multiplierValue = multiplier.invoke(view);
                if (owner != null
                        && owner.toString().equalsIgnoreCase(faction)
                        && channelValue != null
                        && CHANNEL.equalsIgnoreCase(channelValue.toString())
                        && multiplierValue instanceof Number number) {
                    result *= Math.max(0.01D, number.doubleValue());
                }
            }
            return result;
        } catch (Throwable ignored) {
            return 1.0D;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String territoryFaction(Location location) {
        if (location == null) return null;
        try {
            Class<?> apiClass = Class.forName("com.mira.factions.api.MiraFactionsApi");
            Object api = Bukkit.getServicesManager().load((Class) apiClass);
            if (api == null) return null;
            Object result = apiClass.getMethod("territoryFaction", Location.class).invoke(api, location);
            if (result instanceof Optional<?> optional) return optional.map(Object::toString).orElse(null);
        } catch (Throwable ignored) {
        }
        return null;
    }
}
