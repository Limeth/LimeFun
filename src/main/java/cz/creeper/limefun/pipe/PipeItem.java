package cz.creeper.limefun.pipe;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import cz.creeper.limefun.LimeFunKeys;
import cz.creeper.limefun.util.BlockLoc;
import cz.creeper.limefun.util.Util;
import lombok.*;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.DyeableData;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.item.inventory.type.TileEntityInventory;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;

import java.util.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PipeItem {
    @NonNull private final PipeSystem system;
    /** Make sure to use {@link #setPipe(BlockLoc)} */
    @NonNull private BlockLoc<World> pipe;
    @NonNull @Setter private UUID itemId;
    @NonNull @Setter private DyeColor pipeColor;

    public static PipeItem create(PipeSystem system, BlockLoc<World> pipe, Item item, Direction enteringDirection, double distanceTravelledInCurrentPipe) {
        Util.setItemDisplayOnly(item, true);
        item.offer(new PipeItemData());
        item.offer(LimeFunKeys.PIPE_ENTERING_DIRECTION, enteringDirection);
        item.offer(LimeFunKeys.PIPE_DISTANCE_TRAVELLED, distanceTravelledInCurrentPipe);

        UUID itemId = item.getUniqueId();
        DyeColor pipeColor = getStainedGlassColor(pipe);
        Location itemLocation = item.getLocation();
        Extent extent = itemLocation.getExtent();
        ArmorStand armorStand = (ArmorStand) extent.createEntity(EntityTypes.ARMOR_STAND, itemLocation.getPosition());
        armorStand.offer(Keys.ARMOR_STAND_IS_SMALL, true);
        armorStand.offer(Keys.HAS_GRAVITY, false);
        armorStand.offer(Keys.ARMOR_STAND_MARKER, true);
        //armorStand.offer(Keys.INVISIBLE, true);
        PipeItem pipeItem = new PipeItem(system, pipe, itemId, pipeColor);

        pipeItem.chooseExitingDirection();

        if(!extent.spawnEntity(armorStand, Cause.source(system.getPlugin()).build()))
            throw new IllegalStateException("Could not spawn the armor stand.");

        pipeItem.setArmorStandId(armorStand.getUniqueId());
        pipeItem.updateDisplayLocation();
        armorStand.addPassenger(item);
        item.offer(LimeFunKeys.PIPE_ARMOR_STAND_ID, armorStand.getUniqueId());

        return pipeItem;
    }

    public static PipeItem load(PipeSystem system, Item item) {
        Preconditions.checkArgument(item.get(PipeItemData.class).isPresent(),
                                    "Cannot load a PipeItem from an Item that is missing PipeItemData.");
        Util.setItemDisplayOnly(item, true);
        UUID itemId = item.getUniqueId();
        Location<World> location = item.getLocation();
        BlockLoc<World> blockLoc = new BlockLoc<>(location);
        DyeColor pipeColor = getStainedGlassColor(blockLoc);

        return new PipeItem(system, blockLoc, itemId, pipeColor);
    }

    public static <E extends Extent> DyeColor getStainedGlassColor(BlockLoc<E> block) {
        BlockType type = block.getLocation().getBlockType();
        Preconditions.checkArgument(type == BlockTypes.STAINED_GLASS,
                "The block is not a stained glass block, it's a " + type.getName() + " block.");
        return block.getLocation().get(DyeableData.class).map((DyeableData data) -> data.type().get()).orElseThrow(() ->
                new IllegalStateException("Could not get the color of a stained glass block.")
        );
    }

    public void chooseExitingDirection() {
        setExitingDirection(chooseExitingDirection(pipe, getEnteringDirection(), pipeColor, system.getPlugin().getRandom()));
    }

    public static <E extends Extent> Direction chooseExitingDirection(BlockLoc<E> pipe, Direction enteringDirection,
                                                                      DyeColor pipeColor, Random random) {
        ArrayList<Direction> availableDirections = new ArrayList<>(Util.getDirectFaces().length - 1);

        for(Direction direction : Util.getDirectFaces()) {
            if(direction == enteringDirection.getOpposite())
                continue;

            BlockLoc adjacentBlock = new BlockLoc<>(pipe.getLocation().getBlockRelative(direction));

            if(couldEnterAdjacent(adjacentBlock, pipeColor))
                availableDirections.add(direction);
        }

        if(!availableDirections.isEmpty()) {
            return availableDirections.get(random.nextInt(availableDirections.size()));
        }

        return enteringDirection;
    }

    /**
     * Decides which direction the item will travel through the block it is entering.
     *
     * @param block the block the item would travel to/through
     * @param pipeColor the color of the pipe. May be {@code null}, the item is spawned here
     * @return {@code true}, if it is a possible path
     */
    public static <E extends Extent> boolean couldEnterAdjacent(BlockLoc<E> block, DyeColor pipeColor) {
        Location<E> location = block.getLocation();
        BlockType type = location.getBlockType();

        if(type == BlockTypes.STAINED_GLASS) {
            DyeColor pipeColorOther = getStainedGlassColor(block);

            return pipeColor == pipeColorOther;
        }

        Optional<TileEntity> optionalFrontTileEntity = location.getTileEntity();

        if(optionalFrontTileEntity.isPresent()) {
            TileEntity frontTileEntity = optionalFrontTileEntity.get();

            if (frontTileEntity instanceof TileEntityCarrier) {
                return true;
            }
        }

        return false;
    }

    /**
     * Decides whether the item can enter another block after having travelled through the current one.
     *
     * @return update location
     */
    public boolean enterBlock() {
        Location<World> location = pipe.getLocation();
        Location<World> enteringLocation = location.getBlockRelative(getExitingDirection());
        BlockLoc<World> enteringBlock = new BlockLoc<>(enteringLocation);
        BlockType type = enteringLocation.getBlockType();

        if(type == BlockTypes.STAINED_GLASS) {
            setPipe(enteringBlock);
            setEnteringDirection(getExitingDirection());
            chooseExitingDirection();
            return true;
        }

        if(type == BlockTypes.AIR) {  // TODO: Check for opacity, not just AIR
            Location<World> droppingLocation = getDroppingLocation();
            Vector3d droppingVelocity = getDroppingVelocity();
            Item item = getItem();

            getArmorStand().setLocation(applyArmorStandOffset(droppingLocation));
            unregisterAndDrop();
            item.setLocation(droppingLocation);
            item.setVelocity(droppingVelocity);
            return false;
        }

        Optional<TileEntity> optionalFrontTileEntity = enteringLocation.getTileEntity();

        if(optionalFrontTileEntity.isPresent()) {
            TileEntity frontTileEntity = optionalFrontTileEntity.get();

            if (frontTileEntity instanceof TileEntityCarrier) {
                TileEntityCarrier carrier = (TileEntityCarrier) frontTileEntity;
                TileEntityInventory<TileEntityCarrier> inventory = carrier.getInventory();
                Item item = getItem();
                ItemStack offering = item.item().get().createStack();
                InventoryTransactionResult result = inventory.offer(offering);
                List<ItemStackSnapshot> returnedItems = Lists.newArrayList(result.getRejectedItems());
                Location<World> exitingLocation = getExitingLocation();

                returnedItems.addAll(result.getReplacedItems());
                unregisterAndDespawn();

                for(ItemStackSnapshot snapshot : returnedItems) {
                    system.createAndRegister(snapshot, exitingLocation, pipe, getExitingDirection().getOpposite(), 0.0);
                }

                return true;
            }
        }

        // Else, bounce back
        setEnteringDirection(getExitingDirection().getOpposite());
        chooseExitingDirection();

        return true;
    }

    public void tick() {
        setDistanceTravelled(getDistanceTravelled() + system.getSpeed());
        double maxTravelDistance;
        boolean updateLocation = true;

        while(getDistanceTravelled() >= (maxTravelDistance = getTravelDistance(getEnteringDirection(),
                                                                               getExitingDirection()))) {
            setDistanceTravelled(getDistanceTravelled() - maxTravelDistance);
            updateLocation = enterBlock();
        }

        if(updateLocation)
            updateDisplayLocation();
    }

    private Location<World> updateDisplayLocation() {
        Direction enteringDirection = getEnteringDirection();
        Direction exitingDirection = getExitingDirection();
        Preconditions.checkArgument(!enteringDirection.isOpposite(exitingDirection),
                "The entering and exiting directions must not be the opposite.");
        double maxDistance = getTravelDistance(enteringDirection, exitingDirection);
        double distanceTravelled = getDistanceTravelled();
        Location<World> enteringLocation = getEnteringLocation();
        Location<World> result;

        if(enteringDirection == exitingDirection) {
            result = enteringLocation.add(enteringDirection.asOffset().mul(distanceTravelled));
        } else {
            double progress = distanceTravelled / maxDistance;
            double gon = progress * Math.PI / 2.0;  // a quarter turn
            result = enteringLocation.add(enteringDirection.asOffset().mul(0.5).mul(Math.sin(gon)))
                    .add(exitingDirection.asOffset().mul(0.5).mul(1.0 - Math.cos(gon)));
        }

        result = applyArmorStandOffset(result);

        getArmorStand().setLocation(result);

        return result;
    }

    private static double getTravelDistance(Direction from, Direction to) {
        if(from == to) {
            return 1.0;
        } else if(from.isOpposite(to)) {
            return 0.0;
        } else {
            // unit circle circumference * radius * quarter turn
            return 2 * Math.PI * 0.5 * 0.25;
        }
    }

    public void unregisterAndDrop() {
        Item item = getItem();

        setDistanceTravelled(0.0);
        system.unregisterItem(this);
        item.setVehicle(null);
        getArmorStand().remove();
        Util.setItemDisplayOnly(item, false);
        item.remove(PipeItemData.class);
    }

    public void unregisterAndDespawn() {
        unregisterAndDrop();
        getItem().remove();
    }

    public void setPipe(BlockLoc<World> pipe) {
        this.system.swapBlockKey(this.pipe, pipe, this);
        this.pipe = pipe;
        this.pipeColor = getStainedGlassColor(pipe);
    }

    public static <E extends Extent> Location<E> applyArmorStandOffset(Location<E> location) {
        return location.add(0, -0.3, 0);
    }

    public Location<World> getEnteringLocation() {
        Direction opposite = getEnteringDirection().getOpposite();
        return pipe.getLocation().add(0.5, 0.5, 0.5)
                .add(opposite.asOffset().mul(0.5));
    }

    public Location<World> getExitingLocation() {
        return pipe.getLocation().add(0.5, 0.5, 0.5)
                .add(getExitingDirection().asOffset().mul(0.5));
    }

    public Location<World> getDroppingLocation() {
        return getExitingLocation().add(getExitingDirection().asOffset().div(8));
    }

    public Vector3d getDroppingVelocity() {
        return getExitingDirection().asOffset().mul(system.getSpeed());
    }

    public Direction getEnteringDirection() {
        return getItem().get(LimeFunKeys.PIPE_ENTERING_DIRECTION)
                .orElseThrow(() -> new IllegalStateException("No entering direction data found."));
    }

    public boolean setEnteringDirection(Direction enteringDirection) {
        return getItem().offer(LimeFunKeys.PIPE_ENTERING_DIRECTION, enteringDirection)
                .isSuccessful();
    }

    public Direction getExitingDirection() {
        return getItem().get(LimeFunKeys.PIPE_EXITING_DIRECTION)
                .orElseThrow(() -> new IllegalStateException("No exiting direction data found."));
    }

    public boolean setExitingDirection(Direction enteringDirection) {
        return getItem().offer(LimeFunKeys.PIPE_EXITING_DIRECTION, enteringDirection)
                .isSuccessful();
    }

    public double getDistanceTravelled() {
        return getItem().get(LimeFunKeys.PIPE_DISTANCE_TRAVELLED)
                .orElseThrow(() -> new IllegalStateException("No distance travelled data found."));
    }

    public boolean setDistanceTravelled(double distanceTravelled) {
        return getItem().offer(LimeFunKeys.PIPE_DISTANCE_TRAVELLED, distanceTravelled)
                .isSuccessful();
    }

    public UUID getArmorStandId() {
        return getItem().get(LimeFunKeys.PIPE_ARMOR_STAND_ID)
                .orElseThrow(() -> new IllegalStateException("No armor stand id data found."));
    }

    public boolean setArmorStandId(UUID armorStandId) {
        return getItem().offer(LimeFunKeys.PIPE_ARMOR_STAND_ID, armorStandId)
                .isSuccessful();
    }

    public ArmorStand getArmorStand() {
        return getWorld().getEntity(getArmorStandId()).map(ArmorStand.class::cast)
                .orElseThrow(() -> new IllegalStateException("Cannot access the armor stand entity."));
    }

    public Item getItem() {
        return getWorld().getEntity(itemId).map(Item.class::cast)
                .orElseThrow(() -> new IllegalStateException("Cannot access the item entity."));
    }

    public boolean isItemAvailable() {
        return getWorld().getEntity(itemId).isPresent();
    }

    public World getWorld() {
        return pipe.getExtent();
    }
}
