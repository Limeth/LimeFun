package cz.creeper.limefun.registry.miningSource;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.noise.NoiseQuality;
import cz.creeper.customitemlibrary.feature.item.CustomItemDefinition;
import cz.creeper.limefun.LimeFun;
import cz.creeper.limefun.modules.mining.MiningModule;
import cz.creeper.limefun.util.BalancedPerlin;
import lombok.Builder;
import lombok.Getter;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.util.function.Function;

public final class MiningSources {

    private MiningSources() {}

    // SORTFIELDS:ON

    public static final MiningSource MALACHITE = miningSourceBuilder()
            .id("malachite")
            .name("Malachite")
            .threshold(0.53)
            .maxProductAmountPerChunk(1024 * 1024)
            .definitionProvider(MiningModule::getMalachite)
            .build();

    public static final MiningSource NATIVE_COPPER = miningSourceBuilder()
            .id("native_copper")
            .name("Native Copper")
            .threshold(0.53)
            .maxProductAmountPerChunk(1024 * 1024)
            .definitionProvider(MiningModule::getNativeCopper)
            .build();

    public static final MiningSource TETRAHEDRITE = miningSourceBuilder()
            .id("tetrahedrite")
            .name("Tetrahedrite")
            .threshold(0.53)
            .maxProductAmountPerChunk(1024 * 1024)
            .definitionProvider(MiningModule::getTetrahedrite)
            .build();

    // SORTFIELDS:OFF

    @Builder(builderMethodName = "miningSourceBuilder")
    public static final MiningSource createMiningSource(String id, String name, double threshold, int maxProductAmountPerChunk, Function<MiningModule, CustomItemDefinition> definitionProvider) {
        return new MiningSource() {
            public static final double FREQUENCY = 1.0 / 4.0;
            public static final int OCTAVES = 8;

            @Getter(lazy = true)
            private final BalancedPerlin perlin = initPerlin();

            private BalancedPerlin initPerlin() {
                BalancedPerlin perlin = new BalancedPerlin();

                perlin.setSeed((int) System.currentTimeMillis());
                perlin.setFrequency(FREQUENCY);
                perlin.setNoiseQuality(NoiseQuality.FAST);
                perlin.setOctaveCount(OCTAVES);

                return perlin;
            }

            @Override
            public ItemStackSnapshot getProduct(int quantity) {
                return definitionProvider.apply(MiningModule.getInstance())
                        .createItem(Cause.source(LimeFun.getInstance()).build())
                        .getDataHolder().createSnapshot();
            }

            @Override
            public int getMaxProductAmountPerChunk() {
                return maxProductAmountPerChunk;
            }

            @Override
            public double getPrevalenceInChunk(long seed, Vector2i chunk) {
                BalancedPerlin perlin = getPerlin();
                int perlinSeed = 31 * Long.valueOf(seed).intValue() + id.hashCode();

                perlin.setSeed(perlinSeed);

                double result = perlin.getValue(chunk.getX(), 0, chunk.getY());
                result = (result + 1) / 2;
                result = (result - threshold) / (1 - threshold);
                result = Math.max(0.0, Math.min(1.0, result));

                return result;
            }

            @Override
            public String getId() {
                return "limefun:" + id;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

}
