package me.ashlynorsomethin.bordereffects;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * Periodically checks every online player's proximity to the world border and
 * applies or removes configured potion effects accordingly.
 */
public class BorderCheckTask implements Runnable {

    private static final int DEFAULT_AMPLIFIER = 0;
    private static final int DEFAULT_DURATION = 60;

    private final BorderEffectsPlugin plugin;

    public BorderCheckTask(BorderEffectsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        double warningDistance = plugin.getConfig().getDouble("warning-distance", 20.0);
        int checkInterval = plugin.getConfig().getInt("check-interval", 10);
        List<PotionEffect> effects = loadEffects();

        if (effects.isEmpty()) return;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            boolean near = isNearAnyBorder(player, warningDistance);

            if (near) {
                applyEffects(player, effects, checkInterval);
            } else {
                removeEffects(player, effects);
            }
        }
    }

    private boolean isNearAnyBorder(Player player, double warningDistance) {
        // Check ChunkyBorder first when available, then fall back to vanilla.
        if (plugin.isChunkyBorderAvailable()) {
            try {
                if (ChunkyBorderChecker.isNearBorder(player, warningDistance)) {
                    return true;
                }
            } catch (Exception e) {
                plugin.getLogger().fine("ChunkyBorder check failed for " + player.getName() + ": " + e.getMessage());
            }
        }

        return VanillaBorderChecker.isNearBorder(player, warningDistance);
    }

    private void applyEffects(Player player, List<PotionEffect> effects, int checkInterval) {
        for (PotionEffect effect : effects) {
            PotionEffect current = player.getPotionEffect(effect.getType());
            // Reapply only when effect is absent or running low to avoid packet spam.
            if (current == null || current.getDuration() < checkInterval + 5) {
                player.addPotionEffect(effect);
            }
        }
    }

    private void removeEffects(Player player, List<PotionEffect> effects) {
        for (PotionEffect effect : effects) {
            player.removePotionEffect(effect.getType());
        }
    }

    private List<PotionEffect> loadEffects() {
        List<PotionEffect> result = new ArrayList<>();
        List<?> list = plugin.getConfig().getList("effects");
        if (list == null) return result;

        for (Object entry : list) {
            if (!(entry instanceof ConfigurationSection section)) {
                // YAML lists of maps are parsed as Map<String, Object> by Bukkit.
                if (entry instanceof java.util.Map<?, ?> map) {
                    parsePotionEffect(map, result);
                }
                continue;
            }
            String typeName = section.getString("type", "");
            int amplifier = section.getInt("amplifier", DEFAULT_AMPLIFIER);
            int duration = section.getInt("duration", DEFAULT_DURATION);
            addEffect(typeName, amplifier, duration, result);
        }

        return result;
    }

    private void parsePotionEffect(java.util.Map<?, ?> map, List<PotionEffect> result) {
        Object typeObj = map.get("type");
        Object ampObj = map.get("amplifier");
        Object durObj = map.get("duration");

        String typeName = typeObj != null ? typeObj.toString() : "";
        int amplifier = ampObj instanceof Number n ? n.intValue() : DEFAULT_AMPLIFIER;
        int duration = durObj instanceof Number n ? n.intValue() : DEFAULT_DURATION;

        addEffect(typeName, amplifier, duration, result);
    }

    private void addEffect(String typeName, int amplifier, int duration, List<PotionEffect> result) {
        if (typeName.isEmpty()) return;

        PotionEffectType type = PotionEffectType.getByName(typeName);

        if (type == null) {
            // Try as a namespaced key, e.g. "minecraft:blindness" or bare "blindness".
            String keyStr = typeName.toLowerCase();
            if (!keyStr.contains(":")) {
                keyStr = "minecraft:" + keyStr;
            }
            try {
                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(keyStr);
                if (key != null) {
                    type = PotionEffectType.getByKey(key);
                }
            } catch (Exception ignored) {
                // fall through
            }
        }

        if (type == null) {
            plugin.getLogger().warning("Unknown potion effect type in config: " + typeName);
            return;
        }

        result.add(new PotionEffect(type, duration, amplifier, true, false));
    }
}
