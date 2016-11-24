package cz.creeper.limefun.modules.wateringCan;

import cz.creeper.limefun.LimeFunKeys;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

@ToString
public class ImmutableWateringCanData extends AbstractImmutableData<ImmutableWateringCanData, WateringCanData> {
    @Getter(AccessLevel.PRIVATE) private int usesLeft;

    public ImmutableWateringCanData() {
        this(0);
    }

    public ImmutableWateringCanData(int usesLeft) {
        this.usesLeft = usesLeft;
        registerGetters();
    }

    public ImmutableValue<Integer> usesLeft() {
        return Sponge.getRegistry().getValueFactory().createValue(LimeFunKeys.WATERING_CAN_USES_LEFT, this.usesLeft, 0).asImmutable();
    }

    @Override
    protected void registerGetters() {
        registerFieldGetter(LimeFunKeys.WATERING_CAN_USES_LEFT, this::getUsesLeft);
        registerKeyValue(LimeFunKeys.WATERING_CAN_USES_LEFT, this::usesLeft);
    }

    @Override
    public WateringCanData asMutable() {
        return new WateringCanData(usesLeft);
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

