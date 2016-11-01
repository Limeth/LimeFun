package cz.creeper.limefun.pipe;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import cz.creeper.limefun.LimeFun;
import cz.creeper.limefun.util.BlockLoc;
import lombok.Getter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.spawn.BlockSpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.entity.ConstructEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.entity.item.ItemMergeItemEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;

import java.util.*;

public class PipeSystem {
    @Getter private final LimeFun plugin;
    private final Map<UUID, PipeItem> uuidToItems = new HashMap<>();
    private final HashMultimap<BlockLoc, PipeItem> blockToItems = HashMultimap.create();
    private Task task;
    /**
     * Distance items travel per tick
     */
    @Getter private double speed = 1.0 / 20.0;  // TODO: Make configurable

    public PipeSystem(LimeFun plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Preconditions.checkArgument(task == null, "PipeSystem already started!");

        task = Sponge.getScheduler().createTaskBuilder().execute(this::tick)
                .intervalTicks(1).submit(plugin);

        Sponge.getEventManager().registerListeners(plugin, this);
    }

    public void stop() {
        Preconditions.checkNotNull(task, "PipeSystem not running!");

        task.cancel();
        task = null;

        Sponge.getEventManager().unregisterListeners(this);
    }

    public boolean isRunning() {
        return task != null;
    }

    private void tick() {
        // Clone the collection so we don't get a ConcurrentModificationException.
        Collection<PipeItem> values = uuidToItems.values();

        for(PipeItem item : values.toArray(new PipeItem[values.size()]))
            item.tick();
    }

    public PipeItem createAndRegister(ItemStackSnapshot snapshot, Location<World> location,
                                  BlockLoc<World> pipe, Direction enteringDirection,
                                  double distanceTravelledInCurrentPipe) {
        Extent extent = location.getExtent();
        Item item = (Item) extent.createEntity(EntityTypes.ITEM, location.getPosition());

        item.offer(item.item().set(snapshot));
        extent.spawnEntity(item, Cause.source(plugin).build());

        return registerItem(item, pipe, enteringDirection, distanceTravelledInCurrentPipe);
    }

    /**
     * Creates a {@link PipeItem} and registers it in the system.
     */
    public PipeItem registerItem(Item item, BlockLoc<World> pipe, Direction enteringDirection, double distanceTravelledInCurrentPipe) {
        return registerItem(PipeItem.create(this, pipe, item, enteringDirection, distanceTravelledInCurrentPipe));
    }

    /**
     * Loads a {@link PipeItem} and registers it in the system.
     */
    public PipeItem loadItem(Item item) {
        return registerItem(PipeItem.load(this, item));
    }

    /**
     * Registers a {@link PipeItem} in the system.
     */
    public PipeItem registerItem(PipeItem pipeItem) {
        uuidToItems.put(pipeItem.getItem().getUniqueId(), pipeItem);
        blockToItems.put(pipeItem.getPipe(), pipeItem);

        return pipeItem;
    }

    /**
     * Removes the {@link PipeItem} from the system.
     * Does not do anything to the {@link PipeItem} other than that.
     */
    public void unregisterItem(PipeItem item) {
        uuidToItems.remove(item.getItem().getUniqueId());
        blockToItems.remove(item.getPipe(), item);
    }

    public void swapBlockKey(BlockLoc<World> from, BlockLoc<World> to, PipeItem item) {
        blockToItems.remove(from, item);
        blockToItems.put(to, item);
    }

    @Listener
    public void onItemMergeItem(ItemMergeItemEvent event) {
        if(uuidToItems.containsKey(event.getItemToMerge().getUniqueId())
                || uuidToItems.containsKey(event.getTargetEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onChangeBlock(ChangeBlockEvent event) {
        // TODO
    }

    @Listener
    public void onPostConstructEntity(ConstructEntityEvent.Post event) {
        Entity entity = event.getTargetEntity();

        if(!(entity instanceof Item))
            return;

        Item item = (Item) entity;

        if(item.get(PipeItemData.class).isPresent()) {
            System.out.println(item);  // TODO: this never fires. Why aren't you being loaded, PipeItemData?!
            loadItem(item);
        }
    }

    @Listener
    public void onSpawnEntity(SpawnEntityEvent event) {
        Cause cause = event.getCause();

        cause.first(BlockSpawnCause.class).filter((blockSpawnCause -> blockSpawnCause.getType() == SpawnTypes.DISPENSE))
                .ifPresent((blockSpawnCause) -> {
            BlockSnapshot dropper = blockSpawnCause.getBlockSnapshot();
            BlockState dropperState = dropper.getState();

            if(dropperState.getType() == BlockTypes.DROPPER) {
                Location<World> dropperLocation = dropper.getLocation()
                        .orElseThrow(() -> new IllegalStateException("Weird, this dropper does not have a location. How did we get it in the first place?"));
                Direction dropperDirection = dropperState.get(Keys.DIRECTION)
                        .orElseThrow(() -> new IllegalStateException("Odd, this dropper does not have a direction."));
                Location<World> pipeLocation = dropperLocation.add(dropperDirection.asBlockOffset());

                if(pipeLocation.getBlockType() != BlockTypes.STAINED_GLASS)
                    return;

                Extent extent = dropperLocation.getExtent();
                Iterator<Entity> entities = event.getEntities().iterator();
                Entity droppedEntity = entities.next();

                if(entities.hasNext())
                    throw new IllegalStateException("A dropper must only drop a single entity.");

                if(!(droppedEntity instanceof Item))
                    throw new IllegalStateException("A dropper must always drop an item, dropped "
                            + droppedEntity.getClass().getName() + " instead.");

                event.setCancelled(true);

                Item item = (Item) droppedEntity;
                boolean success = extent.spawnEntity(item, Cause.source(plugin).build());

                if(!success)
                    throw new IllegalStateException("Could not spawn the item entity.");

                BlockLoc<World> pipeBlock = new BlockLoc<>(pipeLocation);

                registerItem(item, pipeBlock, dropperDirection, 0.2);
            }
        });
    }
}
