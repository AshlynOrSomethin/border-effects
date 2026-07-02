package me.ashlynorsomethin.bordereffects.command;

import me.ashlynorsomethin.bordereffects.BorderEffectsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class BorderEffectsCommand implements CommandExecutor, TabCompleter {

    private final BorderEffectsPlugin plugin;

    public BorderEffectsCommand(BorderEffectsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bordereffects.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            sender.sendMessage("§aBorderEffects configuration reloaded.");
            return true;
        }

        sender.sendMessage("§eUsage: /" + label + " reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("bordereffects.admin")) {
            return Collections.singletonList("reload");
        }
        return Collections.emptyList();
    }
}
