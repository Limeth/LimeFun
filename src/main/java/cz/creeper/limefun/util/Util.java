package cz.creeper.limefun.util;

import lombok.Getter;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.util.Direction;

public class Util {
    @Getter(lazy = true) private static final Direction[] directFaces = initDirectFaces();

    private static Direction[] initDirectFaces() {
        return new Direction[] {
                Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
        };
    }

    public static void setItemDisplayOnly(Item item, boolean displayOnly) {
        item.offer(item.getValue(Keys.INFINITE_DESPAWN_DELAY)
                .orElseThrow(() -> new IllegalStateException("Cannot get the infinite despawn delay key of an item."))
                .set(displayOnly));
        item.offer(item.getValue(Keys.INFINITE_PICKUP_DELAY)
                .orElseThrow(() -> new IllegalStateException("Cannot get the infinite pickup delay key of an item."))
                .set(displayOnly));

        // Reset the despawn delay so it doesn't despawn instantly
        if(!displayOnly) {
            MutableBoundedValue<Integer> delay = item.getValue(Keys.DESPAWN_DELAY)
                    .orElseThrow(() -> new IllegalStateException("Cannot get the despawn delay key of an item."));

            item.offer(delay.set(delay.getDefault()));
        }
    }
}
