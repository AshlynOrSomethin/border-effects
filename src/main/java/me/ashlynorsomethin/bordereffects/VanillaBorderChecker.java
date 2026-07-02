package me.ashlynorsomethin.bordereffects;

import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

/**
 * Checks whether a player is within {@code warningDistance} blocks of the
 * vanilla Minecraft world border in their current world.
 *
 * <p>Vanilla borders are always axis-aligned squares, so the distance to the
 * nearest edge is {@code min(halfSize - |x - cx|, halfSize - |z - cz|)}.
 *
 * <p>If the border is at or near its default size (≥ 59,000,000 blocks) it is
 * considered "unset" and is ignored, avoiding false positives on servers that
 * have not configured a border.
 */
public final class VanillaBorderChecker {

    private static final double DEFAULT_BORDER_THRESHOLD = 59_000_000.0;

    private VanillaBorderChecker() {}

    public static boolean isNearBorder(Player player, double warningDistance) {
        WorldBorder border = player.getWorld().getWorldBorder();

        // Skip the check when the border is at (or close to) its default size.
        if (border.getSize() >= DEFAULT_BORDER_THRESHOLD) {
            return false;
        }

        Location loc = player.getLocation();

        // Check that the player is currently inside the border.
        if (!border.isInside(loc)) {
            return false;
        }

        double halfSize = border.getSize() / 2.0;
        double centerX = border.getCenter().getX();
        double centerZ = border.getCenter().getZ();

        double distFromEdgeX = halfSize - Math.abs(loc.getX() - centerX);
        double distFromEdgeZ = halfSize - Math.abs(loc.getZ() - centerZ);

        return Math.min(distFromEdgeX, distFromEdgeZ) <= warningDistance;
    }
}
