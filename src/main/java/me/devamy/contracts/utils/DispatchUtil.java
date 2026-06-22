package me.devamy.contracts.utils;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.function.Consumer;

import static me.devamy.contracts.Contracts.plugin;

public class DispatchUtil {

    public static ScheduledTask async(Runnable toRun) {
        return Bukkit.getAsyncScheduler().runNow(plugin, task -> toRun.run());
    }

    public static ScheduledTask async(Consumer<ScheduledTask> toRun) {
        return Bukkit.getAsyncScheduler().runNow(plugin, toRun);
    }

    public static ScheduledTask global(Runnable toRun) {
        return Bukkit.getGlobalRegionScheduler().run(plugin, task -> toRun.run());
    }

    public static ScheduledTask region(Location location, Runnable toRun) {
        return Bukkit.getRegionScheduler().run(plugin, location, task -> toRun.run());
    }

    public static ScheduledTask entity(Entity entity, Runnable toRun) {
        return entity.getScheduler().run(plugin, task -> toRun.run(), null);
    }

    public static ScheduledTask entity(Entity entity, Consumer<ScheduledTask> toRun) {
        return entity.getScheduler().run(plugin, toRun, null);
    }
}
