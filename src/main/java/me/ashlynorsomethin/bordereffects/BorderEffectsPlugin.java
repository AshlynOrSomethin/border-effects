package me.ashlynorsomethin.bordereffects;

import me.ashlynorsomethin.bordereffects.command.BorderEffectsCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class BorderEffectsPlugin extends JavaPlugin {

    private BukkitTask checkTask;
    private boolean chunkyBorderAvailable;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        chunkyBorderAvailable = Bukkit.getPluginManager().getPlugin("ChunkyBorder") != null
                && Bukkit.getPluginManager().isPluginEnabled("ChunkyBorder");

        if (chunkyBorderAvailable) {
            getLogger().info("ChunkyBorder detected – ChunkyBorder support enabled.");
        }

        startCheckTask();

        PluginCommand cmd = getCommand("bordereffects");
        if (cmd != null) {
            BorderEffectsCommand handler = new BorderEffectsCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        getLogger().info("BorderEffects enabled.");
    }

    @Override
    public void onDisable() {
        cancelCheckTask();
        getLogger().info("BorderEffects disabled.");
    }

    // Called by the reload command
    public void reload() {
        reloadConfig();
        cancelCheckTask();
        startCheckTask();
    }

    public boolean isChunkyBorderAvailable() {
        return chunkyBorderAvailable;
    }

    private void startCheckTask() {
        long interval = getConfig().getLong("check-interval", 10L);
        checkTask = Bukkit.getScheduler().runTaskTimer(this, new BorderCheckTask(this), 20L, interval);
    }

    private void cancelCheckTask() {
        if (checkTask != null && !checkTask.isCancelled()) {
            checkTask.cancel();
            checkTask = null;
        }
    }
}
