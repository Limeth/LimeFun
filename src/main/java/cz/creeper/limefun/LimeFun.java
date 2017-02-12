package cz.creeper.limefun;

import com.flowpowered.math.vector.Vector2i;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Injector;
import cz.creeper.customitemlibrary.CustomItemService;
import cz.creeper.limefun.manager.MiningSourceManager;
import cz.creeper.limefun.manager.MiningSourceManagerSerializer;
import cz.creeper.limefun.manager.MiningSourceWorldManager;
import cz.creeper.limefun.manager.MiningSourceWorldManagerSerializer;
import cz.creeper.limefun.modules.Module;
import cz.creeper.limefun.modules.drill.DrillModule;
import cz.creeper.limefun.modules.pipe.PipeModule;
import cz.creeper.limefun.modules.wateringCan.WateringCanModule;
import cz.creeper.limefun.registry.miningSource.MiningSource;
import cz.creeper.limefun.registry.miningSource.MiningSourcesRegistryModule;
import lombok.Getter;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;

@Plugin(
        id = "limefun",
        name = "LimeFun",
        description = "Bring things from mods to a server without mods!",
        authors = {
                "Limeth"
        },
        dependencies = {
                @Dependency(
                        id = "customitemlibrary",
                        version = "[0.2,)"
                )
        }
)
public class LimeFun {
    private static LimeFun INSTANCE;
    @Inject @Getter
    private Injector injector;
    @Inject @Getter
    private Logger logger;
    @Inject @DefaultConfig(sharedRoot = true) @Getter
    private Path configPath;
    private final Map<String, Module> availableModules = Maps.newHashMap();
    private final Map<String, Module> loadedModules = Maps.newHashMap();
    @Getter
    private final Random random = new Random();

    public LimeFun() {
        INSTANCE = this;
    }

    @Listener
    public void onGamePreInitialization(GamePreInitializationEvent event) {
        DataManager manager = Sponge.getDataManager();

        registerRegistryModules();
        registerSerializers();
        initModules();
        availableModules.values().forEach(module -> module.registerData(manager));
    }

    @Listener
    public void onGameInitialization(GameInitializationEvent event) {
        logger.info("Loading LimeFun...");
        loadConfig();
        registerCommands();
        logger.info("LimeFun loaded.");
    }

    @Listener
    public void onGameServerStart(GameStartedServerEvent event) {
        logger.info("Enabling LimeFun...");
        loadedModules.values().forEach(Module::start);
        logger.info("LimeFun enabled.");

        // TODO: Remove this line to not force instantiation
        MiningSourceWorldManager.getInstance();
        int i = 0;

        while(MiningSourceWorldManager.getInstance().getManager(Sponge.getServer().getWorld(Sponge.getServer().getDefaultWorldName()).get()).getDistributorAt(


                Vector2i.from(i, 0)).getTotalProduct() <= 0) {
            i++;
        }

    }

    @Listener
    public void onGameStoppedServer(GameStoppedServerEvent event) {
        logger.info("Disabling LimeFun...");

        loadedModules.values().forEach(Module::stop);
        Sponge.getEventManager().unregisterPluginListeners(this);

        if(MiningSourceWorldManager.isInstantiated())
            MiningSourceWorldManager.getInstance().save();

        logger.info("LimeFun disabled.");
    }

    @Listener
    public void onGameReload(GameReloadEvent event) {
        loadConfig();
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

    private void initModules() {
        availableModules.clear();
        initModule(new PipeModule(this));
        initModule(new WateringCanModule(this));
        initModule(new DrillModule(this));
    }

    private void initModule(Module module) {
        injector.injectMembers(module);
        availableModules.put(module.getModuleName(), module);
    }

    private void loadModule(Module module, ConfigurationNode node) {
        Sponge.getEventManager().registerListeners(this, module);
        loadedModules.put(module.getModuleName(), module);
        module.load(node);
    }

    public void loadConfig() {
        ConfigurationLoader<CommentedConfigurationNode> loader
                = HoconConfigurationLoader.builder().setPath(configPath).build();
        ConfigurationOptions options = ConfigurationOptions.defaults().setShouldCopyDefaults(true);
        CommentedConfigurationNode rootNodeTemp;

        try {
            rootNodeTemp = loader.load(options);
        } catch(IOException e) {
            logger.warn("Could not load the config, creating an empty one: " + e.getLocalizedMessage());
            rootNodeTemp = loader.createEmptyNode(options);
        }

        final CommentedConfigurationNode rootNode = rootNodeTemp;
        CommentedConfigurationNode modulesNode = rootNode.getNode("modules");

        modulesNode.setComment("To enable a module, set the value to `true`. To disable a module, set the value to `false`.");

        loadedModules.clear();
        availableModules.values().forEach(module -> {
            String moduleName = module.getModuleName();
            ConfigurationNode node = rootNode.getNode("modules", moduleName);
            boolean enabled = node.getNode("enabled").getBoolean(true);

            if(!enabled)
                return;

            loadModule(module, node);
        });

        try {
            loader.save(rootNode);
        } catch (IOException e) {
            logger.error("Could not save the config: " + e.getLocalizedMessage());
        }

        logger.info("Config loaded.");
    }

    public void registerCommands() {
        CommandSpec lfSpec = CommandSpec.builder()
                .description(Text.of("LimeFun commands"))
                .permission("limefun.command.lf")
                .executor((src, args) -> {
                    src.sendMessage(Text.of("Nothing to see here, yet."));
                    return CommandResult.success();
                })
                .build();

        Sponge.getCommandManager().register(this, lfSpec, "limefun", "lf");
    }

    public static CustomItemService getCustomItemService() {
        return Sponge.getServiceManager().provide(CustomItemService.class)
                .orElseThrow(() -> new IllegalStateException("Could not access the CustomItemService."));
    }

    public Path getConfigDirectory() {
        return configPath.getParent().resolve(getPluginContainer().getId());
    }

    public PluginContainer getPluginContainer() {
        return Sponge.getPluginManager().fromInstance(this)
                .orElseThrow(() -> new IllegalStateException("Could not access the plugin container."));
    }

    public static LimeFun getInstance() {
        return INSTANCE;
    }
}
