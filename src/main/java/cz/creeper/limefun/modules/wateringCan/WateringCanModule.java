package cz.creeper.limefun.modules.wateringCan;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import cz.creeper.customitemlibrary.registry.CustomItemService;
import cz.creeper.customitemlibrary.registry.CustomToolDefinition;
import cz.creeper.limefun.LimeFun;
import cz.creeper.limefun.modules.Module;
import lombok.RequiredArgsConstructor;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.generator.dummy.DummyObjectProvider;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class WateringCanModule implements Module {
    public static final String TYPE_ID = "watering_can";
    public static final String MODEL_EMPTY = TYPE_ID + "_empty";
    public static final String MODEL_FILLED = TYPE_ID + "_filled";
    public static final String DISPLAY_NAME = "Watering Can";
    public static final int DEFAULT_CAPACITY = 16;
    public static final double DEFAULT_RADIUS = 2;
    public static final int DEFAULT_GROWTH_RATE = 5000;
    public static final List<BlockType> DEFAULT_TYPE_WHITE_LIST = ImmutableList.<BlockType>builder()
            .add(BlockTypes.WHEAT)
            .build();
    private final LimeFun plugin;
    private int capacity = DEFAULT_CAPACITY;
    private double radius = DEFAULT_RADIUS;
    private int growthRate = DEFAULT_GROWTH_RATE;
    private List<BlockType> typeWhiteList = Lists.newArrayList(DEFAULT_TYPE_WHITE_LIST);
    private CustomToolDefinition definition;

    @Override
    public void load(ConfigurationNode node) {
        capacity = node.getNode("capacity").getInt(DEFAULT_CAPACITY);
        radius = node.getNode("radius").getDouble(DEFAULT_RADIUS);
        growthRate = node.getNode("growthRate").getInt(DEFAULT_GROWTH_RATE);
        ConfigurationNode whiteListNode = node.getNode("typeWhiteList");
        List<String> defaultTypeNameWhiteList = DEFAULT_TYPE_WHITE_LIST.stream()
                .map(BlockType::getName)
                .collect(Collectors.toList());

        if(whiteListNode.isVirtual()) {
            whiteListNode.setValue(defaultTypeNameWhiteList);
            typeWhiteList = Lists.newArrayList(DEFAULT_TYPE_WHITE_LIST);
        } else {
            List<String> typeNameWhiteList = whiteListNode
                    .getList(Object::toString, defaultTypeNameWhiteList);
            typeWhiteList = typeNameWhiteList.stream()
                    .map(name -> Sponge.getRegistry().getType(BlockType.class, name))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        }
        System.out.println(typeWhiteList);
    }

    @Listener
    public void onGamePostInitialization(GamePostInitializationEvent event) {
        CustomItemService cis = Sponge.getServiceManager().provide(CustomItemService.class)
                .orElseThrow(() -> new IllegalStateException("Could not access the CustomItemService."));
        cis.register(createDefinition());
    }

    private CustomToolDefinition createDefinition() {
        ItemStack itemStack = ItemStack.of(ItemTypes.SHEARS, 1);
        itemStack.offer(Keys.DISPLAY_NAME, Text.builder()
                .color(TextColors.RESET)
                .append(Text.of(DISPLAY_NAME))
                .build()).getRejectedData().forEach(data -> {
            throw new IllegalStateException("Could not offer a display name to the watering can ItemStackSnapshot.");
        });
        return definition = CustomToolDefinition.create(plugin, TYPE_ID, itemStack.createSnapshot(),
            Lists.newArrayList(MODEL_EMPTY, MODEL_FILLED), Lists.newArrayList());
    }

    @Listener
    public void onInteractBlock(InteractBlockEvent event, @First Player player) {
        player.getItemInHand(HandTypes.MAIN_HAND).ifPresent(itemInHand -> {
            System.out.println(event.getCause());
        });
    }

    @Override
    public void start() {}

    @Override
    public void stop() {}
}
