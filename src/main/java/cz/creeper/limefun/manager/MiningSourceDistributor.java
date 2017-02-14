package cz.creeper.limefun.manager;

import com.flowpowered.math.vector.Vector2i;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import cz.creeper.limefun.registry.miningSource.MiningSource;
import lombok.Getter;
import lombok.NonNull;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.Sponge;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@ConfigSerializable
public class MiningSourceDistributor {

    private static final Random RANDOM = new Random();

    private MiningSourceManager manager;

    @Setting("position") @Getter private Vector2i position;
    @Setting("productLeft") private final Map<String, Integer> sourceToProductLeft = Maps.newLinkedHashMap();
    @Setting("chosenProduct") private Double chosenProduct;

    private Long totalProduct;

    private MiningSourceDistributor() {}

    public MiningSourceDistributor(@NonNull MiningSourceManager manager, @NonNull Vector2i position) {
        this.manager = manager;
        this.position = position;
    }

    void setManager(@NonNull MiningSourceManager manager) {
        this.manager = manager;
    }

    public Optional<MiningSource> peekNextProduct() {
        long totalProduct = getTotalProduct();

        if(totalProduct <= 0)
            return Optional.empty();

        double chosen = getChosenProduct();
        Iterator<Map.Entry<String, Integer>>
                iterator = updateSourceToProductLeft().entrySet().iterator();

        do {
            Map.Entry<String, Integer> entry = iterator.next();
            int currentProduct = entry.getValue();
            double currentProductPortion = (double) currentProduct / (double) totalProduct;

            if(currentProductPortion >= chosen) {
                return Optional.of(Sponge.getRegistry()
                        .getType(MiningSource.class, entry.getKey())
                        .orElseThrow(() -> new IllegalStateException(
                                "Could not access MiningSource: " + entry
                                        .getKey())));
            }

            chosen -= currentProductPortion;
        } while(true);
    }

    private double getChosenProduct() {
        return chosenProduct != null ? chosenProduct : nextChosenProduct();
    }

    private double nextChosenProduct() {
        return chosenProduct = RANDOM.nextDouble();
    }

    public Optional<MiningSource> pollNextProduct() {
        Optional<MiningSource> nextProduct = peekNextProduct();

        if(nextProduct.isPresent()) {
            String nextProductId = nextProduct.get().getId();
            Map<String, Integer> sourceToProductLeft = updateSourceToProductLeft();

            if(totalProduct != null)
                totalProduct--;

            sourceToProductLeft.put(nextProductId, sourceToProductLeft.get(nextProductId) - 1);
            nextChosenProduct();
        }

        return nextProduct;
    }

    private Map<String, Integer> updateSourceToProductLeft() {
        Sponge.getRegistry().getAllOf(MiningSource.class)
                .forEach(source ->
                        sourceToProductLeft.computeIfAbsent(source.getId(), sourceId ->
                                source.getOriginalProductAmountInChunk(getSeed(), position)));

        return sourceToProductLeft;
    }

    private long getSeed() {
        return manager.requireWorld().getProperties().getSeed();
    }

    public Map<String, Integer> getSourceToProductLeft() {
        return ImmutableMap.copyOf(updateSourceToProductLeft());
    }

    private long updateTotalProduct() {
        return totalProduct = updateSourceToProductLeft().values().stream()
                .map(Integer::longValue)
                .reduce(0L, (a, b) -> a + b);
    }

    public long getTotalProduct() {
        return totalProduct != null ? totalProduct : updateTotalProduct();
    }

}
