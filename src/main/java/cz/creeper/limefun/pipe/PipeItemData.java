package cz.creeper.limefun.pipe;

import cz.creeper.limefun.LimeFunKeys;
import lombok.*;
import org.apache.commons.lang3.NotImplementedException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.util.Direction;

import java.util.Optional;
import java.util.UUID;

/**
 * Stores data needed to recreate {@link PipeItem}s.
 */
@ToString
public class PipeItemData extends AbstractData<PipeItemData, ImmutablePipeItemData> {
    @NonNull @Getter(AccessLevel.PRIVATE) @Setter(AccessLevel.PRIVATE) private Direction enteringDirection;
    @NonNull @Getter(AccessLevel.PRIVATE) @Setter(AccessLevel.PRIVATE) private Direction exitingDirection;
    @NonNull @Getter(AccessLevel.PRIVATE) @Setter(AccessLevel.PRIVATE) private double distanceTravelled;
    @NonNull @Getter(AccessLevel.PRIVATE) @Setter(AccessLevel.PRIVATE) private UUID armorStandId;

    public PipeItemData() {
        this(Direction.NORTH, Direction.NORTH, 0.0, new UUID(0, 0));
    }

    public PipeItemData(Direction enteringDirection, Direction exitingDirection,
                        double distanceTravelled, UUID armorStandId) {
        this.enteringDirection = enteringDirection;
        this.exitingDirection = exitingDirection;
        this.distanceTravelled = distanceTravelled;
        this.armorStandId = armorStandId;
        registerGettersAndSetters();
    }

    public Value<Direction> enteringDirection() {
        return Sponge.getRegistry().getValueFactory().createValue(LimeFunKeys.PIPE_ENTERING_DIRECTION, this.enteringDirection, Direction.NORTH);
    }

    public Value<Direction> exitingDirection() {
        return Sponge.getRegistry().getValueFactory().createValue(LimeFunKeys.PIPE_EXITING_DIRECTION, this.exitingDirection, Direction.NORTH);
    }

    public Value<Double> distanceTravelled() {
        return Sponge.getRegistry().getValueFactory().createValue(LimeFunKeys.PIPE_DISTANCE_TRAVELLED, this.distanceTravelled, 0.0);
    }

    public Value<UUID> armorStandId() {
        return Sponge.getRegistry().getValueFactory().createValue(LimeFunKeys.PIPE_ARMOR_STAND_ID, this.armorStandId, new UUID(0, 0));
    }

    @Override
    protected void registerGettersAndSetters() {
        registerFieldGetter(LimeFunKeys.PIPE_ENTERING_DIRECTION, this::getEnteringDirection);
        registerFieldGetter(LimeFunKeys.PIPE_EXITING_DIRECTION, this::getExitingDirection);
        registerFieldGetter(LimeFunKeys.PIPE_DISTANCE_TRAVELLED, this::getDistanceTravelled);
        registerFieldGetter(LimeFunKeys.PIPE_ARMOR_STAND_ID, this::getArmorStandId);

        registerFieldSetter(LimeFunKeys.PIPE_ENTERING_DIRECTION, this::setEnteringDirection);
        registerFieldSetter(LimeFunKeys.PIPE_EXITING_DIRECTION, this::setExitingDirection);
        registerFieldSetter(LimeFunKeys.PIPE_DISTANCE_TRAVELLED, this::setDistanceTravelled);
        registerFieldSetter(LimeFunKeys.PIPE_ARMOR_STAND_ID, this::setArmorStandId);

        registerKeyValue(LimeFunKeys.PIPE_ENTERING_DIRECTION, this::enteringDirection);
        registerKeyValue(LimeFunKeys.PIPE_EXITING_DIRECTION, this::exitingDirection);
        registerKeyValue(LimeFunKeys.PIPE_DISTANCE_TRAVELLED, this::distanceTravelled);
        registerKeyValue(LimeFunKeys.PIPE_ARMOR_STAND_ID, this::armorStandId);
    }

    @Override
    public Optional<PipeItemData> fill(DataHolder dataHolder, MergeFunction overlap) {
        throw new NotImplementedException("Not yet implemented");  // TODO
    }

    @Override
    public Optional<PipeItemData> from(DataContainer container) {
        if(!container.contains(LimeFunKeys.PIPE_ENTERING_DIRECTION)
            || !container.contains(LimeFunKeys.PIPE_EXITING_DIRECTION)
            || !container.contains(LimeFunKeys.PIPE_DISTANCE_TRAVELLED)
            || !container.contains(LimeFunKeys.PIPE_ARMOR_STAND_ID)) {
            return Optional.empty();
        }

        this.enteringDirection = Direction.valueOf(container.getString(LimeFunKeys.PIPE_ENTERING_DIRECTION.getQuery()).get());
        this.exitingDirection = Direction.valueOf(container.getString(LimeFunKeys.PIPE_EXITING_DIRECTION.getQuery()).get());
        this.distanceTravelled = container.getDouble(LimeFunKeys.PIPE_DISTANCE_TRAVELLED.getQuery()).get();
        this.armorStandId = UUID.fromString(container.getString(LimeFunKeys.PIPE_ARMOR_STAND_ID.getQuery()).get());

        return Optional.of(this);
    }

    @Override
    public PipeItemData copy() {
        return new PipeItemData(enteringDirection, exitingDirection, distanceTravelled, armorStandId);
    }

    @Override
    public ImmutablePipeItemData asImmutable() {
        return new ImmutablePipeItemData(enteringDirection, exitingDirection, distanceTravelled, armorStandId);
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
