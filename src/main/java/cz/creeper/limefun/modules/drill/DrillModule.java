package cz.creeper.limefun.modules.drill;

import com.flowpowered.math.vector.Vector2i;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import cz.creeper.customitemlibrary.data.mutable.CustomInventoryData;
import cz.creeper.customitemlibrary.feature.CustomFeatureDefinition;
import cz.creeper.customitemlibrary.feature.TextureId;
import cz.creeper.customitemlibrary.feature.block.CustomBlock;
import cz.creeper.customitemlibrary.feature.block.CustomBlockDefinition;
import cz.creeper.customitemlibrary.feature.block.simple.CorrectToolPredicate;
import cz.creeper.customitemlibrary.feature.block.simple.SimpleCustomBlock;
import cz.creeper.customitemlibrary.feature.inventory.simple
        .AffectCustomSlotListener;
import cz.creeper.customitemlibrary.feature.inventory.simple.CustomSlot;
import cz.creeper.customitemlibrary.feature.inventory.simple.GUIBackground;
import cz.creeper.customitemlibrary.feature.inventory.simple
        .SimpleCustomInventory;
import cz.creeper.customitemlibrary.feature.inventory.simple
        .SimpleCustomInventoryDefinition;
import cz.creeper.customitemlibrary.feature.inventory.simple
        .SimpleCustomInventoryDefinitionBuilder;
import cz.creeper.customitemlibrary.feature.item.CustomItemDefinition;
import cz.creeper.customitemlibrary.util.Block;
import cz.creeper.limefun.LimeFun;
import cz.creeper.limefun.manager.MiningSourceDistributor;
import cz.creeper.limefun.manager.MiningSourceManager;
import cz.creeper.limefun.modules.Module;
import cz.creeper.limefun.modules.mining.MiningModule;
import cz.creeper.limefun.registry.miningSource.MiningSource;
import lombok.RequiredArgsConstructor;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RequiredArgsConstructor
public class DrillModule implements Module {
    public static CustomBlockDefinition electricDrillBlockDefinition;
    public static CustomItemDefinition electricDrillMaterialDefinition;
    public static SimpleCustomInventoryDefinition electricDrillInventoryDefinition;
    private final Random random = new Random();
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

    private void onUpdate(SimpleCustomBlock customBlock) {
        DataHolder dataHolder = customBlock.getDataHolder();
        CustomInventoryData data = electricDrillInventoryDefinition.getCustomInventoryData(dataHolder);
        Block block = customBlock.getBlock();
        World world = block.getWorld().get();
        MiningSourceManager miningSourceManager = MiningModule.getInstance().getMiningSourceWorldManager().getManager(world);
        MiningSourceDistributor distributor = miningSourceManager.getDistributorAt(block.getChunk().get());
        Map<String, ItemStackSnapshot> slotIdToItemStack = data.getSlotIdToItemStack();
        ItemStackSnapshot output = slotIdToItemStack.get("output");

        if(output == ItemStackSnapshot.NONE) {
            distributor.pollNextProduct().ifPresent(nextProduct -> {
                slotIdToItemStack.put("output", nextProduct.getProduct());
                electricDrillInventoryDefinition.setCustomInventoryData(dataHolder, data);
            });
        }
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
                .onUpdate(this::onUpdate)
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
                    .slotId(getMiningSourceSlotId(x))
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

        if(!targetBlock.getChunk().isPresent())
            return;

        LimeFun.getCustomItemService().getBlock(targetBlock).ifPresent(customBlock -> {
            if(customBlock.getDefinition() != electricDrillBlockDefinition)
                return;

            openInventory(customBlock, player, event.getCause());
        });
    }

    private void openInventory(CustomBlock customBlock, Player player, Cause cause) {
        SimpleCustomInventory inventory = electricDrillInventoryDefinition.open(customBlock.getDataHolder(), player, cause);
        List<ItemStackSnapshot> bottomRow = Lists.newArrayListWithCapacity(9);
        Block block = customBlock.getBlock();
        World world = block.getWorld().get();
        Chunk chunk = block.getChunk().get();
        Map<String, Integer> sourceToProductLeft = MiningModule.getInstance()
                .getMiningSourceWorldManager().getManager(world)
                .getDistributorAt(chunk).getSourceToProductLeft();
        List<String> sortedSources = Lists.newLinkedList(sourceToProductLeft.keySet());

        Collections.sort(sortedSources);

        for(int i = 0; i < 9; i++) {
            ItemStackSnapshot snapshot;

            if(sortedSources.size() > 0) {
                String sourceId = sortedSources.remove(0);
                MiningSource source = Sponge.getRegistry().getType(MiningSource.class, sourceId).get();
                snapshot = source.getProduct(Math.min(sourceToProductLeft.get(sourceId), 64));
            } else {
                snapshot = ItemStackSnapshot.NONE;
            }

            bottomRow.add(snapshot);
        }

        random.setSeed(block.hashCode());
        Collections.shuffle(bottomRow, random);

        for(int x = 0; x < 9; x++) {
            CustomSlot slot = inventory.getCustomSlot(getMiningSourceSlotId(x)).get();

            slot.setItemStack(bottomRow.get(x).createStack());
        }
    }

    public static String getMiningSourceSlotId(int x) {
        Preconditions.checkArgument(x >= 0 && x < 9, "Invalid slot index.");

        return "mining_source_" + x;
    }
}
