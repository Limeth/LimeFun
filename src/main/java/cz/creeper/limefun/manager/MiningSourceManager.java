package cz.creeper.limefun.manager;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.NonNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class MiningSourceManager {

    @Getter private UUID worldId;
    private final Map<Vector2i, MiningSourceDistributor> chunkToDistributor;

    MiningSourceManager(@NonNull UUID worldId, Collection<MiningSourceDistributor> distributors) {
        this.worldId = worldId;
        this.chunkToDistributor = distributors.stream()
                .collect(Collectors.toMap(MiningSourceDistributor::getPosition, Function.identity()));

        chunkToDistributor.values().forEach(distributor -> distributor.setManager(this));
    }

    public MiningSourceManager(@NonNull World world) {
        this.worldId = world.getUniqueId();
        this.chunkToDistributor = Maps.newHashMap();
    }

    public Optional<World> getWorld() {
        return Sponge.getServer().getWorld(worldId);
    }

    public World requireWorld() {
        return getWorld().orElseThrow(() -> new IllegalStateException("Could not access the world of this MiningSourceManager. Is it loaded?"));
    }

    public MiningSourceDistributor getDistributorAt(Vector2i chunk) {
        return chunkToDistributor.computeIfAbsent(chunk, _chunk -> new MiningSourceDistributor(this, _chunk));
    }

    public MiningSourceDistributor getDistributorAt(Vector3i chunk) {
        return getDistributorAt(Vector2i.from(chunk.getX(), chunk.getZ()));
    }

    public MiningSourceDistributor getDistributorAt(Chunk chunk) {
        return getDistributorAt(chunk.getPosition());
    }

    public Map<Vector2i, MiningSourceDistributor> getDistributors() {
        return ImmutableMap.copyOf(chunkToDistributor);
    }

}
