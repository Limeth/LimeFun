package cz.creeper.limefun.modules.mining;

import com.google.common.reflect.TypeToken;
import cz.creeper.customitemlibrary.feature.CustomFeatureDefinition;
import cz.creeper.customitemlibrary.feature.item.CustomItemDefinition;
import cz.creeper.customitemlibrary.feature.item.tool.CustomToolDefinition;
import cz.creeper.limefun.LimeFun;
import cz.creeper.limefun.manager.MiningSourceManager;
import cz.creeper.limefun.manager.MiningSourceManagerSerializer;
import cz.creeper.limefun.manager.MiningSourceWorldManager;
import cz.creeper.limefun.manager.MiningSourceWorldManagerSerializer;
import cz.creeper.limefun.modules.Module;
import cz.creeper.limefun.registry.miningSource.MiningSource;
import cz.creeper.limefun.registry.miningSource.MiningSourcesRegistryModule;
import lombok.Getter;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class MiningModule implements Module {
    @Getter
    private static MiningModule instance;
    private final LimeFun plugin;
    @Getter
    private MiningSourceWorldManager miningSourceWorldManager;

    // Copper
    @Getter private CustomItemDefinition malachite;
    @Getter private CustomItemDefinition nativeCopper;
    @Getter private CustomItemDefinition tetrahedrite;

    public MiningModule(LimeFun plugin) {
        this.plugin = plugin;
        instance = this;
    }

    @Override
    public void registerData(DataManager manager) {
        registerRegistryModules();
        registerSerializers();
    }

    private void registerRegistryModules() {
        MiningSourcesRegistryModule miningSourcesRegistryModule = new MiningSourcesRegistryModule();

        miningSourcesRegistryModule.registerDefaults();
        Sponge.getRegistry().registerModule(MiningSource.class, miningSourcesRegistryModule);
    }

    private void registerSerializers() {
        TypeSerializers.getDefaultSerializers()
                .registerType(TypeToken.of(MiningSourceManager.class), new MiningSourceManagerSerializer());
        TypeSerializers.getDefaultSerializers()
                .registerType(TypeToken.of(MiningSourceWorldManager.class), new MiningSourceWorldManagerSerializer());
    }

    @Override
    public void load(ConfigurationNode node) {
        miningSourceWorldManager = MiningSourceWorldManager.load();
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        miningSourceWorldManager.save();
    }

    @Listener
    public void onGamePostInitialization(GamePostInitializationEvent event) {
        malachite = registerOreType("malachite", "Malachite");
        nativeCopper = registerOreType("native_copper", "Native Copper");
        tetrahedrite = registerOreType("tetrahedrite", "Tetrahedrite");
    }

    private CustomToolDefinition registerOreType(String id, String name) {
        ItemStack electricDrillStack = ItemTypes.DIAMOND_SHOVEL.getTemplate().createStack();

        electricDrillStack.offer(Keys.DISPLAY_NAME, Text.of(TextColors.RESET, name));

        CustomToolDefinition definition = CustomFeatureDefinition.itemToolBuilder()
                .plugin(LimeFun.getInstance())
                .typeId(id)
                .defaultModel("ores/" + id)
                .additionalAsset("textures/tools/ores/" + id + ".png")
                .itemStackSnapshot(electricDrillStack.createSnapshot())
                .build();

        LimeFun.getCustomItemService().register(definition);

        return definition;
    }
}
