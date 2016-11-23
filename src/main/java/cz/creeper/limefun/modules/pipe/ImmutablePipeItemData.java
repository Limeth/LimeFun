package cz.creeper.limefun.modules.pipe;

import cz.creeper.limefun.LimeFunKeys;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.util.Direction;

import java.util.UUID;

/**
 * Stores data needed to recreate {@link PipeItem}s, immutably.
 */
@ToString
public class ImmutablePipeItemData extends AbstractImmutableData<ImmutablePipeItemData, PipeItemData> {
    @Getter(AccessLevel.PRIVATE) private Direction enteringDirection;
    @Getter(AccessLevel.PRIVATE) private Direction exitingDirection;
    @Getter(AccessLevel.PRIVATE) private double distanceTravelled;
    @Getter(AccessLevel.PRIVATE) private UUID armorStandId;

    public ImmutablePipeItemData() {
        this(null, null, 0.0, null);
    }

    public ImmutablePipeItemData(Direction enteringDirection, Direction exitingDirection,
                        double distanceTravelled, UUID armorStandId) {
        this.enteringDirection = enteringDirection;
        this.exitingDirection = exitingDirection;
        this.distanceTravelled = distanceTravelled;
        this.armorStandId = armorStandId;
        registerGetters();
    }

    public ImmutableValue<Direction> enteringDirection() {
        return Sponge.getRegistry().getValueFactory().createValue(LimeFunKeys.PIPE_ENTERING_DIRECTION, this.enteringDirection, Direction.NORTH).asImmutable();
    }

    public ImmutableValue<Direction> exitingDirection() {
        return Sponge.getRegistry().getValueFactory().createValue(LimeFunKeys.PIPE_EXITING_DIRECTION, this.exitingDirection, Direction.NORTH).asImmutable();
    }

    public ImmutableValue<Double> distanceTravelled() {
        return Sponge.getRegistry().getValueFactory().createValue(LimeFunKeys.PIPE_DISTANCE_TRAVELLED, this.distanceTravelled, 0.0).asImmutable();
    }

    public ImmutableValue<UUID> armorStandId() {
        return Sponge.getRegistry().getValueFactory().createValue(LimeFunKeys.PIPE_ARMOR_STAND_ID, this.armorStandId, new UUID(0, 0)).asImmutable();
    }

    @Override
    protected void registerGetters() {
        registerFieldGetter(LimeFunKeys.PIPE_ENTERING_DIRECTION, this::getEnteringDirection);
        registerFieldGetter(LimeFunKeys.PIPE_EXITING_DIRECTION, this::getExitingDirection);
        registerFieldGetter(LimeFunKeys.PIPE_DISTANCE_TRAVELLED, this::getDistanceTravelled);
        registerFieldGetter(LimeFunKeys.PIPE_ARMOR_STAND_ID, this::getArmorStandId);

        registerKeyValue(LimeFunKeys.PIPE_ENTERING_DIRECTION, this::enteringDirection);
        registerKeyValue(LimeFunKeys.PIPE_EXITING_DIRECTION, this::exitingDirection);
        registerKeyValue(LimeFunKeys.PIPE_DISTANCE_TRAVELLED, this::distanceTravelled);
        registerKeyValue(LimeFunKeys.PIPE_ARMOR_STAND_ID, this::armorStandId);
    }

    @Override
    public PipeItemData asMutable() {
        return new PipeItemData(enteringDirection, exitingDirection, distanceTravelled, armorStandId);
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
                .set(LimeFunKeys.PIPE_ENTERING_DIRECTION.getQuery(), enteringDirection.name())
                .set(LimeFunKeys.PIPE_EXITING_DIRECTION.getQuery(), exitingDirection.name())
                .set(LimeFunKeys.PIPE_DISTANCE_TRAVELLED, distanceTravelled)
                .set(LimeFunKeys.PIPE_ARMOR_STAND_ID.getQuery(), armorStandId.toString());
    }
}

