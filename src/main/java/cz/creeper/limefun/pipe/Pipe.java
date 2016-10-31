package cz.creeper.limefun.pipe;

/**
 * A block that holds at least one {@link PipeItem}.
 */
public class Pipe {
/*    @Getter private final PipeSystem system;
    @Getter private final BlockLoc location;
    @Getter private final DyeColor dyeColor;
    private final HashMap<UUID, PipeItem> items = new HashMap<>();

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    Pipe(PipeSystem system, BlockLoc blockLoc, PipeItem pipeItem) {
        Location location = blockLoc.getLocation();

        Preconditions.checkArgument(location.getBlockType() == BlockTypes.STAINED_GLASS, "Invalid block type.");

        DyeableData dyeableData = (DyeableData) location.get(DyeableData.class).get();
        dyeColor = dyeableData.type().get();
        this.system = system;
        this.location = blockLoc;
        items.put(pipeItem.getItem().getUniqueId(), pipeItem);
    }

    public Map<UUID, PipeItem> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public void tick() {
        items.values().forEach(PipeItem::tick);
    }*/
}
