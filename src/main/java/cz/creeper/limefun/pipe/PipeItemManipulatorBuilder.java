package cz.creeper.limefun.pipe;

import cz.creeper.limefun.LimeFunKeys;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.Queries;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.util.Direction;

import java.util.Optional;
import java.util.UUID;

public class PipeItemManipulatorBuilder implements DataManipulatorBuilder<PipeItemData, ImmutablePipeItemData> {
    @Override
    public PipeItemData create() {
        return new PipeItemData();
    }

    @Override
    public Optional<PipeItemData> createFrom(DataHolder dataHolder) {
        return Optional.of(dataHolder.get(PipeItemData.class).orElse(create()));
    }

    @Override
    public Optional<PipeItemData> build(DataView container) throws InvalidDataException {
        switch(container.getInt(Queries.CONTENT_VERSION).get()) {
            case 1:
                if (!container.contains(LimeFunKeys.PIPE_ENTERING_DIRECTION,
                        LimeFunKeys.PIPE_EXITING_DIRECTION,
                        LimeFunKeys.PIPE_DISTANCE_TRAVELLED,
                        LimeFunKeys.PIPE_ARMOR_STAND_ID)) {
                    return Optional.empty();
                }

                return Optional.of(new PipeItemData(
                        Direction.valueOf(container.getString(LimeFunKeys.PIPE_ENTERING_DIRECTION.getQuery()).get()),
                        Direction.valueOf(container.getString(LimeFunKeys.PIPE_EXITING_DIRECTION.getQuery()).get()),
                        container.getDouble(LimeFunKeys.PIPE_DISTANCE_TRAVELLED.getQuery()).get(),
                        UUID.fromString(container.getString(LimeFunKeys.PIPE_ARMOR_STAND_ID.getQuery()).get())
                ));

            default:
                return Optional.empty();
        }
    }
}
