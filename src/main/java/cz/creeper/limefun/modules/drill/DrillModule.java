package cz.creeper.limefun.modules.drill;

import com.flowpowered.math.vector.Vector2i;
import cz.creeper.customitemlibrary.feature.CustomFeatureDefinition;
import cz.creeper.customitemlibrary.feature.TextureId;
import cz.creeper.customitemlibrary.feature.block.CustomBlockDefinition;
import cz.creeper.customitemlibrary.feature.block.simple.CorrectToolPredicate;
import cz.creeper.customitemlibrary.feature.inventory.CustomInventoryDefinition;
import cz.creeper.customitemlibrary.feature.inventory.simple.AffectCustomSlotListener;
import cz.creeper.customitemlibrary.feature.inventory.simple.GUIBackground;
import cz.creeper.customitemlibrary.feature.inventory.simple.SimpleCustomInventoryDefinitionBuilder;
import cz.creeper.customitemlibrary.feature.item.CustomItemDefinition;
import cz.creeper.customitemlibrary.util.Block;
import cz.creeper.limefun.LimeFun;
import cz.creeper.limefun.modules.Module;
import lombok.RequiredArgsConstructor;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

@RequiredArgsConstructor
public class DrillModule implements Module {
    public static CustomBlockDefinition electricDrillBlockDefinition;
    public static CustomItemDefinition electricDrillMaterialDefinition;
    public static CustomInventoryDefinition electricDrillInventoryDefinition;
    private final LimeFun plugin;

    @Override
    public void registerData(DataManager manager) {

    }

    @Override
    public void load(ConfigurationNode node) {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Listener
    public void onGamePostInitialization(GamePostInitializationEvent event) {
        electricDrillBlockDefinition = CustomFeatureDefinition.simpleBlockBuilder()
                .plugin(LimeFun.getInstance())
                .typeId("electric_drill")
                .defaultModel("electric_drill")
                .effectState(BlockTypes.CAULDRON.getDefaultState())
                .generateDamageIndicatorModels(true)
                .rotateHorizontally(true)
                .correctToolPredicate(CorrectToolPredicate.of(BlockTypes.CAULDRON))
                .build();

        ItemStack electricDrillStack = ItemTypes.SKULL.getTemplate().createStack();

        electricDrillStack.offer(Keys.DISPLAY_NAME, Text.of("Electric mining drill"));

        electricDrillMaterialDefinition = CustomFeatureDefinition.itemMaterialBuilder()
                .plugin(LimeFun.getInstance())
                .typeId("electric_drill")
                .defaultModel("electric_drill")
                .itemStackSnapshot(electricDrillStack.createSnapshot())
                .build();

        SimpleCustomInventoryDefinitionBuilder electricDrillInventoryDefinitionBuilder =
                CustomFeatureDefinition.simpleInventoryBuilder()
                .plugin(LimeFun.getInstance())
                .typeId("electric_drill")
                .height(3)
                .backgroundBuilder()
                        .slotId("background")
                        .defaultBackground(GUIBackground.builder()
                                .textureId(TextureId.builder()
                                        .fileName("electric_drill")
                                        .plugin(LimeFun.getInstance())
                                        .build())
                                .build())
                        .build()
                .emptySlotBuilder()
                        .slotId("output")
                        .position(Vector2i.from(3, 0))
                        .affectCustomSlotListener(AffectCustomSlotListener.output())
                        .persistent(true)
                        .build();

        for(int x = 0; x < 9; x++) {
            electricDrillInventoryDefinitionBuilder = electricDrillInventoryDefinitionBuilder.emptySlotBuilder()
                    .slotId("mining_source_" + x)
                    .affectCustomSlotListener(AffectCustomSlotListener.cancelAll())
                    .position(Vector2i.from(x, 2))
                    .build();
        }

        electricDrillInventoryDefinition = electricDrillInventoryDefinitionBuilder.build();

        LimeFun.getCustomItemService().register(electricDrillBlockDefinition);
        LimeFun.getCustomItemService().register(electricDrillMaterialDefinition);
        LimeFun.getCustomItemService().register(electricDrillInventoryDefinition);
    }

    @Listener
    public void onInteractBlock(InteractBlockEvent.Secondary.MainHand event, @Root Player player) {
        BlockSnapshot targetBlockSnapshot = event.getTargetBlock();
        Block targetBlock = Block.of(player.getWorld(), targetBlockSnapshot.getPosition());

        LimeFun.getCustomItemService().getBlock(targetBlock).ifPresent(customBlock -> {
            if(customBlock.getDefinition() != electricDrillBlockDefinition)
                return;

            electricDrillInventoryDefinition.open(customBlock.getDataHolder(), player, event.getCause());
        });
    }
}
