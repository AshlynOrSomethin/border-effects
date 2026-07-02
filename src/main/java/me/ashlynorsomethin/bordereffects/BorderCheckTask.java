package me.ashlynorsomethin.bordereffects;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Periodically checks every online player's proximity to the world border and
 * applies or removes configured potion effects accordingly.
 */
public class BorderCheckTask implements Runnable {

    private static final int DEFAULT_AMPLIFIER = 0;
    private static final int DEFAULT_DURATION = 60;
    // Extra ticks of buffer added to the check interval when deciding whether to reapply an effect.
    private static final int DURATION_BUFFER = 5;

    private static final List<String> DEFAULT_MESSAGES = List.of(
            "§7A mysterious fog creeps in around you...",
            "§7You shouldn't wander this far...",
            "§7What lies beyond is unknown..."
    );

    private final BorderEffectsPlugin plugin;
    // Tracks which effect types this plugin applied per player.
    private final Map<UUID, Set<PotionEffectType>> appliedEffects = new HashMap<>();
    // Tracks the last time (ms) a border warning message was sent to each player.
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();

    public BorderCheckTask(BorderEffectsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        double warningDistance = plugin.getConfig().getDouble("warning-distance", 20.0);
        int checkInterval = plugin.getConfig().getInt("check-interval", 10);
        long messageIntervalMs = plugin.getConfig().getLong("message-interval", 60) * 1000L;
        List<PotionEffect> effects = loadEffects();
        List<String> messages = loadMessages();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            boolean near = isNearAnyBorder(player, warningDistance);

            if (near) {
                if (!effects.isEmpty()) {
                    applyEffects(player, effects, checkInterval);
                }
                maybeSendMessage(player, messages, messageIntervalMs);
            } else {
                removeTrackedEffects(player);
                lastMessageTime.remove(player.getUniqueId());
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
        UUID uuid = player.getUniqueId();
        Set<PotionEffectType> tracked = appliedEffects.computeIfAbsent(uuid, k -> new HashSet<>());

        for (PotionEffect effect : effects) {
            PotionEffect current = player.getPotionEffect(effect.getType());
            // Reapply only when effect is absent or running low to avoid packet spam.
            if (current == null || current.getDuration() < checkInterval + DURATION_BUFFER) {
                player.addPotionEffect(effect);
                tracked.add(effect.getType());
            }
        }
    }

    private void removeTrackedEffects(Player player) {
        Set<PotionEffectType> tracked = appliedEffects.remove(player.getUniqueId());
        if (tracked == null) return;

        for (PotionEffectType type : tracked) {
            player.removePotionEffect(type);
        }
    }

    private void maybeSendMessage(Player player, List<String> messages, long intervalMs) {
        if (messages.isEmpty()) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastMessageTime.get(uuid);

        if (last == null || now - last >= intervalMs) {
            String message = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
            player.sendMessage(message);
            lastMessageTime.put(uuid, now);
        }
    }

    private List<String> loadMessages() {
        List<?> list = plugin.getConfig().getList("messages");
        if (list == null || list.isEmpty()) return DEFAULT_MESSAGES;

        List<String> result = new ArrayList<>();
        for (Object entry : list) {
            if (entry != null) {
                result.add(entry.toString());
            }
        }
        return result.isEmpty() ? DEFAULT_MESSAGES : result;
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
