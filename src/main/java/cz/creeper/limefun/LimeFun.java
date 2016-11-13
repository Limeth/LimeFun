package cz.creeper.limefun;

import com.google.inject.Inject;
import cz.creeper.limefun.pipe.ImmutablePipeItemData;
import cz.creeper.limefun.pipe.PipeItemData;
import cz.creeper.limefun.pipe.PipeItemManipulatorBuilder;
import cz.creeper.limefun.pipe.PipeSystem;
import lombok.Getter;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.io.IOException;
import java.nio.file.Path;
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
    @Inject @Getter
    private Logger logger;
    @Inject @DefaultConfig(sharedRoot = true) @Getter
    private Path configPath;
    @Getter
    private final PipeSystem pipeSystem = new PipeSystem(this);
    @Getter
    private final Random random = new Random();

    @Listener
    public void onGamePreInitialization(GamePreInitializationEvent event) {
        Sponge.getDataManager().register(PipeItemData.class, ImmutablePipeItemData.class, new PipeItemManipulatorBuilder());
    }

    @Listener
    public void onGameServerStart(GameStartedServerEvent event) {
        logger.info("Enabling LimeFun...");
        loadConfig();
        registerCommands();
        pipeSystem.start();
        logger.info("LimeFun enabled.");
    }

    @Listener
    public void onGameStoppedServer(GameStoppedServerEvent event) {
        logger.info("Disabling LimeFun...");
        pipeSystem.stop();
        logger.info("LimeFun disabled.");
    }

    public void loadConfig() {
        ConfigurationLoader<CommentedConfigurationNode> loader
                = HoconConfigurationLoader.builder().setPath(configPath).build();
        ConfigurationOptions options = ConfigurationOptions.defaults().setShouldCopyDefaults(true);
        CommentedConfigurationNode rootNode;

        try {
            rootNode = loader.load(options);
        } catch(IOException e) {
            logger.warn("Could not load the config, creating an empty one: " + e.getLocalizedMessage());
            rootNode = loader.createEmptyNode(options);
        }

        pipeSystem.setSpeed(rootNode.getNode("pipes", "speed").getDouble(PipeSystem.DEFAULT_SPEED));
        pipeSystem.setPipeCapacity(rootNode.getNode("pipes", "capacity").getInt(PipeSystem.DEFAULT_PIPE_CAPACITY));

        try {
            loader.save(rootNode);
        } catch (IOException e) {
            logger.error("Could not save the config: " + e.getLocalizedMessage());
        }

        logger.info("Config loaded.");
    }

    public void registerCommands() {
        CommandSpec lfReloadSpec = CommandSpec.builder()
                .description(Text.of("Reloads the config file"))
                .permission("limefun.command.lf.reload")
                .executor((src, args) -> {
                    loadConfig();
                    src.sendMessage(Text.builder().append(Text.of("Config reloaded.")).color(TextColors.GREEN).build());
                    return CommandResult.success();
                })
                .build();
        CommandSpec lfSpec = CommandSpec.builder()
                .description(Text.of("LimeFun commands"))
                .permission("limefun.command.lf")
                .child(lfReloadSpec, "reload")
                .build();

        Sponge.getCommandManager().register(this, lfSpec, "limefun", "lf");
    }
}
