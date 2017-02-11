package cz.creeper.limefun.registry.miningSource;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.annotation.CatalogedBy;
import org.spongepowered.api.world.Chunk;

@CatalogedBy(MiningSources.class)
public interface MiningSource extends CatalogType {

    /**
     * Returns the {@link ItemStackSnapshot} this {@link MiningSource} produces.
     * @return the {@link ItemStackSnapshot} this {@link MiningSource} produces
     */
    ItemStackSnapshot getProduct(int quantity);

    /**
     * Returns the {@link ItemStackSnapshot} this {@link MiningSource} produces.
     * @return the {@link ItemStackSnapshot} this {@link MiningSource} produces
     */
    default ItemStackSnapshot getProduct() {
        return getProduct(1);
    }

    /**
     * Returns the maximum possible amount of the product per chunk.
     * Typically, a chunk uses about 10% of this value, if the
     * {@link #getPrevalenceInChunk(long, Vector2i)} is based on noise.
     *
     * @return the maximum possible amount of the product per chunk
     */
    int getMaxProductAmountPerChunk();

    /**
     * The amount of the product present in the specified chunk.
     *
     * @param seed The world seed
     * @param chunk The chunk
     * @return A value between 0 and 1, indicating the prevalence of the product
     *         in the chunk
     */
    double getPrevalenceInChunk(long seed, Vector2i chunk);

    /**
     * The amount of the product present in the specified chunk.
     *
     * @param seed The world seed
     * @param chunk The chunk
     * @return A value between 0 and 1, indicating the prevalence of the product
     *         in the chunk
     */
    default double getPrevalenceInChunk(long seed, Vector3i chunk) {
        return getPrevalenceInChunk(seed, Vector2i.from(chunk.getX(), chunk.getZ()));
    }

    /**
     * The amount of the product present in the specified chunk.
     *
     * @param seed The world seed
     * @param chunk The chunk
     * @return A value between 0 and 1, indicating the prevalence of the product
     *         in the chunk
     */
    default double getPrevalenceInChunk(long seed, Chunk chunk) {
        return getPrevalenceInChunk(seed, chunk.getPosition());
    }

    /**
     * The original amount of product in the specified chunk, before any mining
     * has taken place.
     *
     * @param seed The world seed
     * @param chunk The chunk
     * @return the original amount of product in the specified chunk
     */
    default int getOriginalProductAmountInChunk(long seed, Vector2i chunk) {
        return (int) Math.ceil(getMaxProductAmountPerChunk() * getPrevalenceInChunk(seed, chunk));
    }

    /**
     * The original amount of product in the specified chunk, before any mining
     * has taken place.
     *
     * @param seed The world seed
     * @param chunk The chunk
     * @return the original amount of product in the specified chunk
     */
    default int getOriginalProductAmountInChunk(long seed, Vector3i chunk) {
        return getOriginalProductAmountInChunk(seed, Vector2i.from(chunk.getX(), chunk.getZ()));
    }

    /**
     * The original amount of product in the specified chunk, before any mining
     * has taken place.
     *
     * @param seed The world seed
     * @param chunk The chunk
     * @return the original amount of product in the specified chunk
     */
    default int getOriginalProductAmountInChunk(long seed, Chunk chunk) {
        return getOriginalProductAmountInChunk(seed, chunk.getPosition());
    }

}
