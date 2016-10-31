package cz.creeper.limefun;

import com.google.inject.Inject;
import cz.creeper.limefun.pipe.PipeSystem;
import lombok.Getter;
import org.slf4j.Logger;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.block.DirectionalData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;

import java.util.Optional;
import java.util.Random;

@Plugin(
        id = "limefun",
        name = "LimeFun",
        description = "Bring things from mods to a server without mods!",
        authors = {
                "Limeth"
        }
)
public class LimeFun {
    @Inject @Getter private final Logger logger = null;
    @Getter private final PipeSystem pipeSystem = new PipeSystem(this);
    @Getter private final Random random = new Random();

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        logger.info("Enabling LimeFun...");
        pipeSystem.start();
        logger.info("LimeFun enabled.");
    }

    @Listener
    public void onGameStoppedServer(GameStoppedServerEvent event) {
        logger.info("Disabling LimeFun...");
        pipeSystem.stop();
        logger.info("LimeFun disabled.");
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event,
                             @org.spongepowered.api.event.filter.Getter("getTargetEntity") Player player) {
        player.sendMessage(Text.of(TextColors.AQUA, TextStyles.BOLD, "Hi " + player.getName()));
    }

    private boolean isValid(Location location) {
        if (location.getBlockType() == BlockTypes.DISPENSER) {
            DirectionalData directionData = (DirectionalData) location.get(DirectionalData.class).get();
            Direction direction = directionData.get(Keys.DIRECTION).get();
            Location facingBlock = location.getRelative(direction);

            System.out.println(direction + " " + facingBlock);
            return facingBlock.getBlockType() == BlockTypes.STAINED_GLASS;
        }

        return false;
    }

    private Direction getNextDirection(Location enteringBlock, Direction enteringDirection) {
        Location frontBlock = enteringBlock.getRelative(enteringDirection);
        BlockType enteringBlockType = enteringBlock.getBlockType();

        if(frontBlock.getBlockType() == enteringBlockType) {  // TODO: Check for byte data
            return enteringDirection;
        }

        Optional<TileEntity> optionalFrontTileEntity = frontBlock.getTileEntity();

        if(optionalFrontTileEntity.isPresent()) {
            TileEntity frontTileEntity = optionalFrontTileEntity.get();

            if(frontTileEntity instanceof TileEntityCarrier) {
                return enteringDirection;  // Possibly can enter the tile entity's inventory
                /*
                TileEntityCarrier frontCarrier = (TileEntityCarrier) frontTileEntity;
                TileEntityInventory<TileEntityCarrier> inventory = frontCarrier.getInventory();

                if(inventory.)
                if(inventory.offer(ItemStack.builder().build()).getType() == InventoryTransactionResult.Type.SUCCESS) {
                    return enteringDirection;  //
                }
                 */
            }
        }

        return enteringDirection.getOpposite();
    }
}
