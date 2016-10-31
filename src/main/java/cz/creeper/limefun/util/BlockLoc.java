package cz.creeper.limefun.util;

import com.flowpowered.math.vector.Vector3i;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.extent.Extent;

/**
 * Used to store the location of a block in a specific extent
 */
@EqualsAndHashCode(exclude = "location")
public class BlockLoc<E extends Extent> {
    @Getter private final E extent;
    @Getter private final Vector3i blockPosition;
    @Getter(lazy = true) private final Location<E> location = initLocation();

    public BlockLoc(E extent, Vector3i blockPosition) {
        this.extent = extent;
        this.blockPosition = blockPosition;
    }

    public BlockLoc(Location<E> location) {
        extent = location.getExtent();
        blockPosition = location.getBlockPosition();
    }

    private Location<E> initLocation() {
        return new Location<>(extent, blockPosition);
    }
}
