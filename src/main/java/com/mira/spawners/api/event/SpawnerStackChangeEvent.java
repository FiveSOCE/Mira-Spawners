package com.mira.spawners.api.event;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class SpawnerStackChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    public enum Cause {
        PLACE,
        STACK,
        BREAK,
        API
    }

    private final Location location;
    private final EntityType entityType;
    private final int oldAmount;
    private final int newAmount;
    private final Cause cause;
    private final UUID actor;

    public SpawnerStackChangeEvent(Location location, EntityType entityType, int oldAmount, int newAmount, Cause cause, @Nullable UUID actor) {
        this.location = location.clone();
        this.entityType = entityType;
        this.oldAmount = Math.max(0, oldAmount);
        this.newAmount = Math.max(0, newAmount);
        this.cause = cause == null ? Cause.API : cause;
        this.actor = actor;
    }

    public Location location() { return location.clone(); }
    public EntityType entityType() { return entityType; }
    public int oldAmount() { return oldAmount; }
    public int newAmount() { return newAmount; }
    public int delta() { return newAmount - oldAmount; }
    public Cause cause() { return cause; }
    public @Nullable UUID actor() { return actor; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
