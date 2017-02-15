package cz.creeper.limefun.modules.drill;

import com.flowpowered.math.vector.Vector2i;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import cz.creeper.customitemlibrary.feature.CustomFeatureDefinition;
import cz.creeper.customitemlibrary.feature.TextureId;
import cz.creeper.customitemlibrary.feature.block.CustomBlock;
import cz.creeper.customitemlibrary.feature.block.CustomBlockDefinition;
import cz.creeper.customitemlibrary.feature.block.simple.CorrectToolPredicate;
import cz.creeper.customitemlibrary.feature.block.simple.SimpleCustomBlock;
import cz.creeper.customitemlibrary.feature.inventory.simple.AffectCustomSlotListener;
import cz.creeper.customitemlibrary.feature.inventory.simple.CustomSlot;
import cz.creeper.customitemlibrary.feature.inventory.simple.GUIBackground;
import cz.creeper.customitemlibrary.feature.inventory.simple.SimpleCustomInventory;
import cz.creeper.customitemlibrary.feature.inventory.simple.SimpleCustomInventoryDefinition;
import cz.creeper.customitemlibrary.feature.inventory.simple.SimpleCustomInventoryDefinitionBuilder;
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
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DrillModule implements Module {
    public static CustomBlockDefinition electricDrillBlockDefinition;
    public static CustomItemDefinition electricDrillMaterialDefinition;
    public static SimpleCustomInventoryDefinition electricDrillInventoryDefinition;
    private final Map<Block, SimpleCustomInventory> electricDrillToInventory = Maps.newHashMap();
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
        Block block = customBlock.getBlock();
        Optional<World> world = block.getWorld();
        Optional<Chunk> chunk = block.getChunk();

        if (!world.isPresent() || !chunk.isPresent())
            return;

        MiningSourceManager miningSourceManager = MiningModule.getInstance()
                .getMiningSourceWorldManager().getManager(world.get());
        MiningSourceDistributor distributor = miningSourceManager.getDistributorAt(chunk.get());
        CustomSlot outputSlot = getOutputSlot(customBlock);
        ItemStack output = outputSlot.getItemStack().orElse(null);

        if (output == null || output.getItem() == ItemTypes.NONE || output.getQuantity() <= 0) {
            distributor.pollNextProduct().ifPresent(nextProduct -> {
                SimpleCustomInventory inventory = electricDrillToInventory.get(block);

                outputSlot.setItemStack(nextProduct.getProduct().createStack());

                if(inventory != null) {
                    populateBottomRow(block, inventory);
                }
            });
        }
    }

    private CustomSlot getOutputSlot(SimpleCustomBlock block) {
        return Optional.ofNullable(electricDrillToInventory.get(block.getBlock()))
                .map(inventory -> inventory.getCustomSlot("output"))
                .orElseGet(() -> electricDrillInventoryDefinition.getCustomSlot(
                        block.getDataHolder(),
                        "output"
                ))
                .orElseThrow(() -> new IllegalStateException("Could not access the output slot."));
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
        Block block = customBlock.getBlock();
        SimpleCustomInventory inventory = electricDrillToInventory.computeIfAbsent(block, _block -> {
            SimpleCustomInventory inv = electricDrillInventoryDefinition
                    .create(customBlock.getDataHolder());

            populateBottomRow(block, inv);

            return inv;
        });

        inventory.open(player, cause);
    }

    private void populateBottomRow(Block block, SimpleCustomInventory inventory) {
        List<ItemStackSnapshot> bottomRow =
                Lists.newArrayListWithCapacity(9);
        World world = block.getWorld().get();
        Chunk chunk = block.getChunk().get();
        MiningSourceDistributor distributor = MiningModule.getInstance()
                .getMiningSourceWorldManager().getManager(world)
                .getDistributorAt(chunk);
        List<ProductLeft> sourceToProductLeft =
                distributor.getSourceToProductLeft()
                        .entrySet().stream().map(ProductLeft::new)
                        .collect(Collectors.toList());

        // Mining source display is limited, prioritize those with the most product available

        sourceToProductLeft.sort(Collections.reverseOrder());

        for (int i = 0; i < 9; i++) {
            ItemStackSnapshot snapshot = ItemStackSnapshot.NONE;

            if (sourceToProductLeft.size() > 0) {
                ProductLeft productLeft = sourceToProductLeft.remove(0);

                if (productLeft.productLeft > 0) {
                    snapshot = productLeft.miningSource.getProduct();
                    snapshot = snapshot.with(Keys.DISPLAY_NAME, Text.of(
                            TextColors.GRAY,
                            productLeft.productLeft + "x ",
                            snapshot.get(Keys.DISPLAY_NAME)
                                    .orElse(Text.EMPTY)
                    )).get();
                }
            }

            bottomRow.add(snapshot);
        }

        random.setSeed(hashCodeOf(chunk));
        Collections.shuffle(bottomRow, random);

        for (int x = 0; x < 9; x++) {
            CustomSlot slot =
                    inventory.getCustomSlot(getMiningSourceSlotId(x)).get();

            slot.setItemStack(bottomRow.get(x).createStack());
        }
    }

    public int hashCodeOf(Chunk chunk) {
        int result = chunk.getPosition().hashCode();
        result = 31 * result + chunk.getUniqueId().hashCode();
        return result;
    }

    public static String getMiningSourceSlotId(int x) {
        Preconditions.checkArgument(x >= 0 && x < 9, "Invalid slot index.");

        return "mining_source_" + x;
    }

    private static class ProductLeft implements Comparable<ProductLeft> {
        private final MiningSource miningSource;
        private final int productLeft;

        ProductLeft(Map.Entry<String, Integer> entry) {
            this.miningSource = Sponge.getRegistry().getType(MiningSource.class, entry.getKey()).get();
            this.productLeft = entry.getValue();
        }

        @Override
        public int compareTo(ProductLeft o) {
            return Integer.valueOf(productLeft).compareTo(o.productLeft);
        }
    }
}
