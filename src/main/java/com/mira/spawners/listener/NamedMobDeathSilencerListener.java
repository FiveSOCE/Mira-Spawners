package com.mira.spawners.listener;

import com.mira.spawners.service.MobStackService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Minecraft logs deaths of custom-named entities to the server console. Mira
 * stack labels are presentation, not real pet/name-tag identity, so remove the
 * temporary display name immediately before lethal damage resolves. Stack PDC
 * remains untouched and normal Mira death/drop handling still runs afterward.
 */
public final class NamedMobDeathSilencerListener implements Listener {
    private final MobStackService mobs;

    public NamedMobDeathSilencerListener(MobStackService mobs) {
        this.mobs = mobs;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLethalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!mobs.isManaged(living) || living.getCustomName() == null) return;
        if (event.getFinalDamage() + 1.0E-7D < living.getHealth()) return;

        living.setCustomName(null);
        living.setCustomNameVisible(false);
    }
}
