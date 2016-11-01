package cz.creeper.limefun.util;

import lombok.Getter;
import org.spongepowered.api.data.key.Keys;
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
        item.offer(Keys.INFINITE_DESPAWN_DELAY, displayOnly);
        item.offer(Keys.INFINITE_PICKUP_DELAY, displayOnly);
    }
}
