package cz.creeper.limefun.registry.miningSource;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.noise.NoiseQuality;
import cz.creeper.limefun.LimeFun;
import cz.creeper.limefun.modules.mining.MiningModule;
import cz.creeper.limefun.util.BalancedPerlin;
import lombok.Getter;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

public final class MiningSources {

    private MiningSources() {}

    // SORTFIELDS:ON

    public static final MiningSource MALACHITE = new MiningSource() {
        public static final double FREQUENCY = 1.0 / 4.0;
        public static final int OCTAVES = 8;
        public static final double THRESHOLD = 0.53;

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
            return MiningModule.getInstance().getMalachite()
                    .createItem(Cause.source(LimeFun.getInstance()).build())
                    .getDataHolder().createSnapshot();
        }

        @Override
        public int getMaxProductAmountPerChunk() {
            return 1024 * 1024;
        }

        @Override
        public double getPrevalenceInChunk(long seed, Vector2i chunk) {
            BalancedPerlin perlin = getPerlin();

            perlin.setSeed(Long.valueOf(seed).intValue());

            double result = perlin.getValue(chunk.getX(), 0, chunk.getY());
            result = (result + 1) / 2;
            result = (result - THRESHOLD) / (1 - THRESHOLD);
            result = Math.max(0.0, Math.min(1.0, result));

            return result;
        }

        @Override
        public String getId() {
            return "limefun:chalcopyrite";
        }

        @Override
        public String getName() {
            return "Chalcopyrite";
        }
    };

    // SORTFIELDS:OFF

}
