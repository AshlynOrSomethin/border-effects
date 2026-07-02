package me.ashlynorsomethin.bordereffects;

import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.ChunkyBorderProvider;
import org.popcraft.chunky.shape.Shape;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Checks whether a player is within {@code warningDistance} blocks of a
 * ChunkyBorder world border in their current world.
 *
 * <p>This class must only be referenced when the ChunkyBorder plugin is loaded
 * (i.e., after confirming {@code isChunkyBorderAvailable()} is {@code true}).
 * Java's lazy class loading ensures that the ChunkyBorder classes are not
 * resolved until this class itself is first used at runtime.
 *
 * <p>Since ChunkyBorder supports arbitrary shapes (square, rectangle, circle,
 * ellipse, polygon), proximity is determined by sampling eight points arranged
 * in a circle of radius {@code warningDistance} around the player. If the
 * player is inside the border but any sample point falls outside, the player
 * is considered "near" the border.
 */
public final class ChunkyBorderChecker {

    private static final int SAMPLE_POINTS = 8;

    private ChunkyBorderChecker() {}

    /**
     * Returns {@code true} when {@code player} is inside a ChunkyBorder for
     * their world and within {@code warningDistance} blocks of its edge.
     *
     * @throws IllegalStateException if ChunkyBorder is not loaded
     */
    public static boolean isNearBorder(Player player, double warningDistance) {
        Optional<BorderData> opt = ChunkyBorderProvider.get()
                .getBorder(player.getWorld().getName());

        if (opt.isEmpty()) {
            return false;
        }

        Shape shape = opt.get().getBorder();
        double x = player.getLocation().getX();
        double z = player.getLocation().getZ();

        // Player must be inside the border for the warning zone to be meaningful.
        if (!shape.isBounding(x, z)) {
            return false;
        }

        // Cast rays in SAMPLE_POINTS directions; if any point is outside the
        // border the player is within warningDistance of the edge.
        for (int i = 0; i < SAMPLE_POINTS; i++) {
            double angle = 2.0 * Math.PI * i / SAMPLE_POINTS;
            double nx = x + Math.cos(angle) * warningDistance;
            double nz = z + Math.sin(angle) * warningDistance;
            if (!shape.isBounding(nx, nz)) {
                return true;
            }
        }

        return false;
    }
}
