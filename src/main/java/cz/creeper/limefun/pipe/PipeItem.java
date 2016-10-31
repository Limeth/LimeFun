package cz.creeper.limefun.pipe;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import cz.creeper.limefun.util.BlockLoc;
import cz.creeper.limefun.util.Util;
import lombok.*;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PipeItem {
    @NonNull private final PipeSystem system;
    /** Make sure to use {@link #setPipe(BlockLoc)} */
    @NonNull private BlockLoc<World> pipe;
    @NonNull @Setter private Item item;
    @NonNull @Setter private ArmorStand armorStand;
    @NonNull @Setter private DyeColor pipeColor;

    public static PipeItem create(PipeSystem system, BlockLoc<World> pipe, Item item, PipeItemData data) {
        Util.setItemDisplayOnly(item, true);

        DyeColor pipeColor = getStainedGlassColor(pipe);
        Location itemLocation = item.getLocation();
        Extent extent = itemLocation.getExtent();
        ArmorStand armorStand = (ArmorStand) extent.createEntity(EntityTypes.ARMOR_STAND, itemLocation.getPosition());
        armorStand.offer(armorStand.small().set(true));
        armorStand.offer(armorStand.gravity().set(false));
        armorStand.offer(armorStand.marker().set(true));
        /*armorStand.offer(armorStand.getValue(Keys.INVISIBLE)
                .orElseThrow(() -> new IllegalStateException("Cannot edit armor stand visibility."))
                .set(true));*/
        PipeItem pipeItem = new PipeItem(system, pipe, item, armorStand, pipeColor);

        pipeItem.chooseExitingDirection();
        pipeItem.updateDisplayLocation();

        if(!extent.spawnEntity(armorStand, Cause.source(system.getPlugin()).build()))
            throw new IllegalStateException("Could not spawn the armor stand.");

        armorStand.addPassenger(item);

        return pipeItem;
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
        data.set(data.exitingDirection().set(
                chooseExitingDirection(pipe, data.enteringDirection().get(), pipeColor, system.getPlugin().getRandom())
        ));
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
        Location<World> enteringLocation = location.getBlockRelative(data.exitingDirection().get());
        BlockLoc<World> enteringBlock = new BlockLoc<>(enteringLocation);
        BlockType type = enteringLocation.getBlockType();

        if(type == BlockTypes.STAINED_GLASS) {
            setPipe(enteringBlock);
            data.enteringDirection().set(data.exitingDirection().get());
            chooseExitingDirection();
            return true;
        }

        if(type == BlockTypes.AIR) {  // TODO: Check for opacity, not just AIR
            Location<World> droppingLocation = getDroppingLocation();
            Vector3d droppingVelocity = getDroppingVelocity();
            armorStand.setLocation(applyArmorStandOffset(droppingLocation));
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
                ItemStack offering = item.item().get().createStack();
                InventoryTransactionResult result = inventory.offer(offering);
                List<ItemStackSnapshot> returnedItems = Lists.newArrayList(result.getRejectedItems());
                Location<World> exitingLocation = getExitingLocation();

                returnedItems.addAll(result.getReplacedItems());
                unregisterAndDespawn();

                for(ItemStackSnapshot snapshot : returnedItems) {
                    system.createAndRegister(snapshot, exitingLocation, pipe, data.exitingDirection().get().getOpposite(), 0.0);
                }

                return true;
            }
        }

        // Else, bounce back
        data.enteringDirection().set(data.exitingDirection().get().getOpposite());
        chooseExitingDirection();

        return true;
    }

    public void tick() {
        data.distanceTravelled().set(data.distanceTravelled().getDefault() + system.getSpeed());
        double maxTravelDistance;
        boolean updateLocation = true;

        while(data.distanceTravelled().get()
              >= (maxTravelDistance = getTravelDistance(data.enteringDirection().get(), data.exitingDirection().get()))) {
            data.distanceTravelled().set()
            distanceTravelledInCurrentPipe -= maxTravelDistance;
            updateLocation = enterBlock();
        }

        if(updateLocation)
            updateDisplayLocation();
    }

    private void updateDisplayLocation() {
        armorStand.setLocation(getDisplayLocation(pipe, enteringDirection, exitingDirection, distanceTravelledInCurrentPipe));
    }

    /*
    private void updateAndSaveArmorStand() {
        updateArmorStand(armorStand, pipe, enteringDirection, exitingDirection, distanceTravelled);
        save();
    }

    private static void updateArmorStand(ArmorStand armorStand, BlockLoc<World> pipe, Direction enteringDirection, Direction exitingDirection, double distanceTravelled) {
        armorStand.setLocation(getDisplayLocation(pipe, enteringDirection, exitingDirection, distanceTravelled));
    }

    public void save() {
        BodyPartRotationalData data = armorStand.getBodyPartRotationalData();
        armorStand.offer(data.leftArmDirection().set(enteringDirection.asOffset()));
        armorStand.offer(data.rightArmDirection().set(exitingDirection.asOffset()));
        armorStand.offer(data.)
    }
    */

    private static <E extends Extent> Location<E> getDisplayLocation(BlockLoc<E> pipe, Direction enteringDirection, Direction exitingDirection, double distanceTravelledInCurrentPipe) {
        Preconditions.checkArgument(!enteringDirection.isOpposite(exitingDirection),
                "The entering and exiting directions must not be the opposite.");
        double maxDistance = getTravelDistance(enteringDirection, exitingDirection);
        Location<E> enteringLocation = getEnteringLocation(enteringDirection, pipe);
        Location<E> result;

        if(enteringDirection == exitingDirection) {
            result = enteringLocation.add(enteringDirection.asOffset().mul(distanceTravelledInCurrentPipe));
        } else {
            double progress = distanceTravelledInCurrentPipe / maxDistance;
            double gon = progress * Math.PI / 2.0;  // a quarter turn
            result = enteringLocation.add(enteringDirection.asOffset().mul(0.5).mul(Math.sin(gon)))
                                     .add(exitingDirection.asOffset().mul(0.5).mul(1.0 - Math.cos(gon)));
        }

        result = applyArmorStandOffset(result);

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
        distanceTravelledInCurrentPipe = 0.0;
        system.unregisterItem(this);
        item.setVehicle(null);
        armorStand.remove();
        Util.setItemDisplayOnly(item, false);
    }

    public void unregisterAndDespawn() {
        unregisterAndDrop();
        item.remove();
    }

    public void setPipe(BlockLoc<World> pipe) {
        this.system.swapBlockKey(this.pipe, pipe, this);
        this.pipe = pipe;
        this.pipeColor = getStainedGlassColor(pipe);
    }

    public static <E extends Extent> Location<E> applyArmorStandOffset(Location<E> location) {
        return location.add(0, -0.3, 0);
    }

    public static <E extends Extent> Location<E> getEnteringLocation(Direction enteringDirection, BlockLoc<E> pipe) {
        Direction opposite = enteringDirection.getOpposite();
        return pipe.getLocation().add(0.5, 0.5, 0.5)
                .add(opposite.asOffset().mul(0.5));
    }

    public Location<World> getEnteringLocation() {
        return getEnteringLocation(enteringDirection, pipe);
    }

    public <E extends Extent> Location<E> getExitingLocation(Direction exitingDirection, BlockLoc<E> pipe) {
        return pipe.getLocation().add(0.5, 0.5, 0.5)
                .add(exitingDirection.asOffset().mul(0.5));
    }

    public Location<World> getExitingLocation() {
        return getExitingLocation(exitingDirection, pipe);
    }

    public Location<World> getDroppingLocation() {
        return getExitingLocation().add(exitingDirection.asOffset().div(8));
    }

    public Vector3d getDroppingVelocity() {
        return exitingDirection.asOffset().mul(system.getSpeed());
    }
}
