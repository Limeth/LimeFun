package cz.creeper.limefun.modules.wateringCan;

import cz.creeper.limefun.LimeFunKeys;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.Queries;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;

import java.util.Optional;

public class WateringCanManipulatorBuilder extends AbstractDataBuilder<WateringCanData> implements DataManipulatorBuilder<WateringCanData, ImmutableWateringCanData> {
    public WateringCanManipulatorBuilder() {
        super(WateringCanData.class, 1);
    }

    @Override
    public WateringCanData create() {
        return new WateringCanData();
    }

    @Override
    public Optional<WateringCanData> createFrom(DataHolder dataHolder) {
        return Optional.of(dataHolder.get(WateringCanData.class).orElse(create()));
    }

    @Override
    protected Optional<WateringCanData> buildContent(DataView container) throws InvalidDataException {
        switch(container.getInt(Queries.CONTENT_VERSION).get()) {
            case 1:
                if (!container.contains(LimeFunKeys.WATERING_CAN_USES_LEFT)) {
                    return Optional.empty();
                }

                return Optional.of(new WateringCanData(
                        container.getInt(LimeFunKeys.WATERING_CAN_USES_LEFT.getQuery()).get()
                ));

            default:
                return Optional.empty();
        }
    }
}
