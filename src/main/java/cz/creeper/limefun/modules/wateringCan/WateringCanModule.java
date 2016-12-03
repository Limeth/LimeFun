package cz.creeper.limefun.modules.wateringCan;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import cz.creeper.customitemlibrary.CustomTool;
import cz.creeper.customitemlibrary.registry.CustomItemService;
import cz.creeper.customitemlibrary.registry.CustomToolDefinition;
import cz.creeper.limefun.LimeFun;
import cz.creeper.limefun.LimeFunKeys;
import cz.creeper.limefun.modules.Module;
import lombok.RequiredArgsConstructor;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class WateringCanModule implements Module {
    public static final String TYPE_ID = "watering_can";
    public static final String MODEL_EMPTY = TYPE_ID + "_empty";
    public static final String MODEL_FILLED = TYPE_ID + "_filled";
    public static final String DISPLAY_NAME = "Watering Can";
    public static final String CAUSE_NAME = "WateringCan";
    public static final int DEFAULT_CAPACITY = 16;
    public static final double DEFAULT_RADIUS = 2;
    public static final int DEFAULT_GROWTH_RATE = 5000;
    public static final List<BlockType> DEFAULT_TYPE_WHITE_LIST = ImmutableList.<BlockType>builder()
            .add(BlockTypes.WHEAT)
            .add(BlockTypes.MELON_STEM)
            .add(BlockTypes.PUMPKIN_STEM)
            .add(BlockTypes.REEDS)
            .add(BlockTypes.POTATOES)
            .add(BlockTypes.CARROTS)
            .add(BlockTypes.BEETROOTS)
            .build();
    private final LimeFun plugin;
    private int capacity = DEFAULT_CAPACITY;
    private double radius = DEFAULT_RADIUS;
    private int growthRate = DEFAULT_GROWTH_RATE;
    private List<BlockType> typeWhiteList = Lists.newArrayList(DEFAULT_TYPE_WHITE_LIST);
    private CustomToolDefinition definition;

    @Override
    public void registerData(DataManager manager) {
        manager.register(WateringCanData.class, ImmutableWateringCanData.class, new WateringCanManipulatorBuilder());
    }

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
    }

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Listener
    public void onGamePostInitialization(GamePostInitializationEvent event) {
        LimeFun.getCustomItemService().register(definition = createDefinition());
    }

    private CustomToolDefinition createDefinition() {
        ItemStack itemStack = ItemStack.of(ItemTypes.SHEARS, 1);
        itemStack.offer(Keys.DISPLAY_NAME, Text.builder()
                .color(TextColors.RESET)
                .append(Text.of(DISPLAY_NAME))
                .build()).getRejectedData().forEach(data -> {
            throw new IllegalStateException("Could not offer a display name to the watering can ItemStackSnapshot.");
        });
        itemStack.offer(new WateringCanData(0));
        return CustomToolDefinition.create(plugin, TYPE_ID, itemStack.createSnapshot(),
            Lists.newArrayList(MODEL_EMPTY, MODEL_FILLED), Lists.newArrayList(
                    "textures/tools/watering_can_empty.png",
                    "textures/tools/watering_can_filled.png"
                ));
    }

    @Listener
    public void onInteractItem(InteractItemEvent.Secondary.MainHand event, @First Player player) {
        CustomItemService cis = LimeFun.getCustomItemService();

        player.getItemInHand(HandTypes.MAIN_HAND)
                .flatMap(cis::getCustomItem)
                .filter(tool -> tool.getDefinition() == definition)
                .map(CustomTool.class::cast)
                .ifPresent(tool -> {
                    Cause cause = event.getCause();
                    // TODO: Change to NamedCause.HIT_TARGET once it's available
                    BlockSnapshot clickedBlock = cause.get("HitTarget", BlockSnapshot.class)
                            .orElse(BlockSnapshot.NONE);
                    onRightClick(clickedBlock, player, tool, cause);
                    player.setItemInHand(HandTypes.MAIN_HAND, tool.getItemStack());
                });
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void onRightClick(BlockSnapshot clickedBlock, Player player, CustomTool wateringCan, Cause cause) {
        ItemStack itemStack = wateringCan.getItemStack();
        int usesLeft = itemStack.getOrElse(LimeFunKeys.WATERING_CAN_USES_LEFT, 0);

        if(usesLeft < capacity) {
            Optional<BlockRayHit<World>> water = BlockRay.from(player).skipFilter(BlockRay.blockTypeFilter(BlockTypes.WATER)).stopFilter(hit -> {
                BlockType type = hit.getLocation().getBlockType();
                return type == BlockTypes.AIR || type == BlockTypes.WATER;
            }).distanceLimit(5).narrowPhase(true).build().end();

            if (water.isPresent()) {
                Location<World> location = water.get().getLocation();
                World world = location.getExtent();

                itemStack.offer(LimeFunKeys.WATERING_CAN_USES_LEFT, capacity);
                wateringCan.setModel(MODEL_FILLED);
                world.playSound(SoundTypes.ITEM_BUCKET_FILL, location.getPosition(), 0.5, 1.0);
                return;
            }
        }

        if(usesLeft > 0 && clickedBlock != BlockSnapshot.NONE) {
            int offset = (int) Math.floor(radius);
            Location<World> location = clickedBlock.getLocation().get();
            World world = location.getExtent();
            ParticleEffect effect = ParticleEffect.builder()
                    .type(ParticleTypes.WATER_SPLASH)
                    .quantity(1)
                    .build();
            boolean used = false;

            for (int x = -offset; x <= offset; x++) {
                for (int z = -offset; z <= offset; z++) {
                    for (int y = -1; y <= 1; y++) {
                        // not counting in the y axis on purpose
                        int distanceSquared = x * x + z * z;

                        if (distanceSquared < radius * radius) {
                            Vector3i offsetVector = new Vector3i(x, y, z);
                            Location<World> currentLocation = location.add(offsetVector);
                            BlockType currentBlockType = currentLocation.getBlockType();

                            if (typeWhiteList.contains(currentBlockType)) {
                                double distance = Math.sqrt(distanceSquared);
                                double distancePortion = distance / radius;
                                double growthRateModifier = 1 - distancePortion * distancePortion;
                                double currentGrowthRate = growthRate * growthRateModifier;
                                Vector3d particlePoint = currentLocation.getPosition().add(0.5, 0.5, 0.5);

                                world.spawnParticles(effect, particlePoint);

                                for (int i = 0; i < currentGrowthRate; i++) {
                                    currentLocation.addScheduledUpdate(0, 0);
                                }

                                if (!used)
                                    used = true;
                            }

                            if (currentBlockType.equals(BlockTypes.FARMLAND)) {
                                BlockState currentState = currentLocation.getBlock();
                                int moisture = currentState.get(Keys.MOISTURE).orElseThrow(() ->
                                        new IllegalStateException("Cannot get the IS_WET property of a FARMLAND block."));

                                if (moisture % 8 != 7) {
                                    currentState = currentState.copy().with(Keys.MOISTURE, 7).get();
                                    PluginContainer pluginContainer = Sponge.getPluginManager().fromInstance(plugin).get();

                                    currentLocation.setBlock(currentState, Cause.source(pluginContainer)
                                            .named(CAUSE_NAME, cause).build());
                                }

                                if (!used)
                                    used = true;
                            }
                        }
                    }
                }
            }

            if (used) {
                itemStack.offer(LimeFunKeys.WATERING_CAN_USES_LEFT, usesLeft - 1);

                double pitch;

                if (usesLeft <= 1) {
                    wateringCan.setModel(MODEL_EMPTY);
                    pitch = 1.0;
                } else {
                    pitch = 1.2;
                }

                world.playSound(SoundTypes.ITEM_BUCKET_EMPTY, location.getPosition().add(0.5, 0.5, 0.5), 0.5, pitch);
            }
        }
    }
}
