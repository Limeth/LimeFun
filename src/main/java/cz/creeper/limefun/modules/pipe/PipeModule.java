package cz.creeper.limefun.modules.pipe;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import cz.creeper.limefun.LimeFun;
import cz.creeper.limefun.modules.Module;
import cz.creeper.limefun.util.BlockLoc;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.carrier.Dropper;
import org.spongepowered.api.block.tileentity.carrier.Hopper;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.spawn.BlockSpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.entity.*;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;

import java.util.*;

@RequiredArgsConstructor
public class PipeModule implements Module {
    public static final double DEFAULT_SPEED = 1.0 / 20.0;
    public static final int DEFAULT_PIPE_CAPACITY = 4;
    @Getter private final LimeFun plugin;
    private final Map<UUID, PipeItem> uuidToItems = new HashMap<>();
    private final HashMultimap<BlockLoc<World>, PipeItem> blockToItems = HashMultimap.create();
    private Task task;
    /**
     * Distance items travel per tick
     */
    @Getter @Setter private double speed = DEFAULT_SPEED;  // TODO: Make configurable
    /**
     * How many items can occur in a pipe block
     */
    @Getter @Setter private int pipeCapacity = DEFAULT_PIPE_CAPACITY;  // TODO: Make configurable

    @Override
    public void registerData(DataManager manager) {
        manager.register(PipeItemData.class, ImmutablePipeItemData.class, new PipeItemManipulatorBuilder());
    }

    @Override
    public void load(ConfigurationNode node) {
        setSpeed(node.getNode("speed").getDouble(PipeModule.DEFAULT_SPEED));
        setPipeCapacity(node.getNode("capacity").getInt(PipeModule.DEFAULT_PIPE_CAPACITY));
    }

    @Override
    public void start() {
        Preconditions.checkArgument(task == null, "PipeModule already started!");

        task = Sponge.getScheduler().createTaskBuilder().execute(this::tick)
                .intervalTicks(1).submit(plugin);
    }

    @Override
    public void stop() {
        Preconditions.checkNotNull(task, "PipeModule not running!");

        task.cancel();
        task = null;
    }

    public boolean isRunning() {
        return task != null;
    }

    private void tick() {
        // Clone the collection so we don't get a ConcurrentModificationException.
        Collection<PipeItem> values = uuidToItems.values();

        for(PipeItem item : values.toArray(new PipeItem[values.size()])) {
            if(item.isItemAvailable())
                item.tick();
            else
                unregisterItem(item.getItemId());
        }
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
        return registerItem(PipeItem.create(this, pipe, item, enteringDirection, distanceTravelledInCurrentPipe), item);
    }

    /**
     * Loads a {@link PipeItem} and registers it in the system.
     */
    public PipeItem loadItem(Item item) {
        return registerItem(PipeItem.load(this, item), item);
    }

    /**
     * Registers a {@link PipeItem} in the system.
     */
    public PipeItem registerItem(PipeItem pipeItem, Item item) {
        uuidToItems.put(item.getUniqueId(), pipeItem);
        blockToItems.put(pipeItem.getPipe(), pipeItem);

        return pipeItem;
    }

    /**
     * Removes the {@link PipeItem} from the system.
     * Does not do anything to the {@link PipeItem} other than that.
     */
    public boolean unregisterItem(PipeItem item) {
        return uuidToItems.remove(item.getItem().getUniqueId()) != null
                & blockToItems.remove(item.getPipe(), item);
    }

    /**
     * Removes the {@link PipeItem} associated with the specified {@link Item} from the system.
     * Does not do anything to the {@link PipeItem} other than that.
     */
    public Optional<PipeItem> unregisterItem(UUID itemId) {
        PipeItem pipeItem = uuidToItems.remove(itemId);

        if(pipeItem == null)
            return Optional.empty();

        blockToItems.remove(pipeItem.getPipe(), pipeItem);

        return Optional.of(pipeItem);
    }

    public Optional<PipeItem> getItem(Item item) {
        return Optional.ofNullable(uuidToItems.get(item.getUniqueId()));
    }

    public boolean isRegistered(Item item) {
        return uuidToItems.containsKey(item.getUniqueId());
    }

    public boolean canTravelTo(BlockLoc<World> pipe) {
        return blockToItems.get(pipe).size() < pipeCapacity;
    }

    public void swapBlockKey(BlockLoc<World> from, BlockLoc<World> to, PipeItem item) {
        blockToItems.remove(from, item);
        blockToItems.put(to, item);
    }

    @Listener
    public void onChangeBlock(ChangeBlockEvent.Post event) {
        event.getTransactions().forEach(transaction -> {
            Location<World> location = transaction.getOriginal().getLocation().get();
            BlockLoc<World> blockLoc = new BlockLoc<>(location);
            Set<PipeItem> pipeItems = blockToItems.get(blockLoc);

            if(pipeItems.isEmpty())
                return;

            PipeItem[] array = pipeItems.toArray(new PipeItem[pipeItems.size()]);

            for(PipeItem item : array) {
                item.unregisterAndDrop();
            }
        });
    }

    @Listener
    public void onSpawnEntityChunkLoad(SpawnEntityEvent.ChunkLoad event) {
        List<Entity> entities = event.getEntities();

        for(Entity entity : entities) {
            if(!(entity instanceof Item))
                continue;

            Item item = (Item) entity;

            if(item.get(PipeItemData.class).isPresent() && !isRegistered(item)) {
                loadItem(item);
            }
        }
    }

    @Listener
    public void onExpireEntity(ExpireEntityEvent event) {
        Entity entity = event.getTargetEntity();

        if(!(entity instanceof Item))
            return;

        Item item = (Item) entity;

        unregisterItem(item.getUniqueId()).ifPresent(pipeItem ->
                pipeItem.getArmorStand().remove()
        );
    }

    @Listener
    public void onDestructEntity(DestructEntityEvent event) {
        Entity target = event.getTargetEntity();

        if(!(target instanceof Item))
            return;

        event.getCause().first(Hopper.class).ifPresent(hopper -> {
            Item item = (Item) target;

            System.out.println("HOPPER: " + event);
        });
    }

    @Listener(order = Order.EARLY)
    public void onEntity(TargetEntityEvent event) {
        Entity entity = event.getTargetEntity();

        if(event instanceof MoveEntityEvent
                || event.getClass().getSimpleName().endsWith("LivingUpdateEvent")
                || event.getClass().getSimpleName().endsWith("EnteringChunk")
                || event.getClass().getSimpleName().endsWith("CanUpdate"))
            return;

        if(!(entity instanceof ArmorStand))
            return;

        UUID uuid = entity.getUniqueId();

        for(PipeItem pipeItem : uuidToItems.values()) {
            if(pipeItem.getArmorStandId().equals(uuid)) {
                System.out.println("TARGET ENTITY EVENT: " + event);
            }
        }
    }

    @Listener
    public void onSpawnEntity(SpawnEntityEvent event) {
        Cause cause = event.getCause();

        cause.first(BlockSpawnCause.class).filter((blockSpawnCause -> blockSpawnCause.getType() == SpawnTypes.DISPENSE))
                .ifPresent((blockSpawnCause) -> {
            BlockSnapshot dropperSnapshot = blockSpawnCause.getBlockSnapshot();
            BlockState dropperState = dropperSnapshot.getState();

            if(dropperState.getType() == BlockTypes.DROPPER) {
                Location<World> dropperLocation = dropperSnapshot.getLocation()
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

                BlockLoc<World> pipeBlock = new BlockLoc<>(pipeLocation);
                Item item = (Item) droppedEntity;

                event.setCancelled(true);  // TODO: This currently does not leave the item in the dropper and that may
                                           //       may be considered a bug. Keep an eye on it, because when it's fixed,
                                           //       it may duplicate items.

                if(canTravelTo(pipeBlock)) {
                    boolean success = extent.spawnEntity(item, Cause.source(plugin).build());

                    if (!success)
                        throw new IllegalStateException("Could not spawn the item entity.");

                    registerItem(item, pipeBlock, dropperDirection, 0.2);
                } else {
                    Dropper dropper = (Dropper) dropperLocation.getTileEntity()
                            .orElseThrow(() -> new IllegalStateException("Could not access the tile entity of a dropper."));

                    dropper.getInventory().offer(item.getItemData().item().get().createStack());
                }
            }
        });
    }
}
