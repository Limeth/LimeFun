package cz.creeper.limefun.modules.wateringCan;

import cz.creeper.limefun.LimeFunKeys;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.NotImplementedException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.Optional;

@ToString
public class WateringCanData extends AbstractData<WateringCanData, ImmutableWateringCanData> {
    @Getter(AccessLevel.PRIVATE) @Setter(AccessLevel.PRIVATE) private int usesLeft;

    public WateringCanData() {
        this(0);
    }

    public WateringCanData(int usesLeft) {
        this.usesLeft = usesLeft;
        registerGettersAndSetters();
    }

    public Value<Integer> usesLeft() {
        return Sponge.getRegistry().getValueFactory().createValue(LimeFunKeys.WATERING_CAN_USES_LEFT, this.usesLeft, 0);
    }

    @Override
    protected void registerGettersAndSetters() {
        registerFieldGetter(LimeFunKeys.WATERING_CAN_USES_LEFT, this::getUsesLeft);
        registerFieldSetter(LimeFunKeys.WATERING_CAN_USES_LEFT, this::setUsesLeft);
        registerKeyValue(LimeFunKeys.WATERING_CAN_USES_LEFT, this::usesLeft);
    }

    @Override
    public Optional<WateringCanData> fill(DataHolder dataHolder, MergeFunction overlap) {
        throw new NotImplementedException("Not yet implemented");  // TODO
    }

    @Override
    public Optional<WateringCanData> from(DataContainer container) {
        if(!container.contains(LimeFunKeys.WATERING_CAN_USES_LEFT)) {
            return Optional.empty();
        }

        this.usesLeft = container.getInt(LimeFunKeys.WATERING_CAN_USES_LEFT.getQuery()).get();

        return Optional.of(this);
    }

    @Override
    public WateringCanData copy() {
        return new WateringCanData(usesLeft);
    }

    @Override
    public ImmutableWateringCanData asImmutable() {
        return new ImmutableWateringCanData(usesLeft);
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
                .set(LimeFunKeys.WATERING_CAN_USES_LEFT.getQuery(), usesLeft);
    }
}
