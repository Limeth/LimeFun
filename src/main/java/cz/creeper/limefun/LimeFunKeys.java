package cz.creeper.limefun;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.util.Direction;

import java.util.UUID;

public class LimeFunKeys {
    public static final Key<Value<Direction>> PIPE_ENTERING_DIRECTION = KeyFactory.makeSingleKey(new TypeToken<Direction>() {}, new TypeToken<Value<Direction>>() {}, DataQuery.of("PipeEnteringDirection"), "limefun:pipe_entering_direction", "Entering direction");
    public static final Key<Value<Direction>> PIPE_EXITING_DIRECTION = KeyFactory.makeSingleKey(new TypeToken<Direction>() {}, new TypeToken<Value<Direction>>() {}, DataQuery.of("PipeExitingDirection"), "limefun:pipe_exiting_direction", "Exiting direction");
    public static final Key<Value<Double>> PIPE_DISTANCE_TRAVELLED = KeyFactory.makeSingleKey(new TypeToken<Double>() {}, new TypeToken<Value<Double>>() {}, DataQuery.of("PipeDistanceTravelled"), "limefun:pipe_distance_travelled", "Distance travelled in the current pipe");
    public static final Key<Value<UUID>> PIPE_ARMOR_STAND_ID = KeyFactory.makeSingleKey(new TypeToken<UUID>() {}, new TypeToken<Value<UUID>>() {}, DataQuery.of("PipeArmorStandId"), "limefun:pipe_armor_stand_id", "Armor stand UUID");
}
