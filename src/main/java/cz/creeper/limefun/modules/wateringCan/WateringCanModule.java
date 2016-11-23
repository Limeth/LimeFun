package cz.creeper.limefun.modules.wateringCan;

import com.google.common.collect.Lists;
import cz.creeper.customitemlibrary.registry.CustomItemService;
import cz.creeper.customitemlibrary.registry.CustomToolDefinition;
import cz.creeper.limefun.LimeFun;
import cz.creeper.limefun.modules.Module;
import lombok.RequiredArgsConstructor;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

@RequiredArgsConstructor
public class WateringCanModule implements Module {
    public static final String TYPE_ID = "watering_can";
    public static final String MODEL_EMPTY = TYPE_ID + "_empty";
    public static final String MODEL_FILLED = TYPE_ID + "_filled";
    public static final String DISPLAY_NAME = "Watering Can";
    private final LimeFun plugin;
    private CustomToolDefinition definition;

    @Override
    public void load(ConfigurationNode node) {}

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
