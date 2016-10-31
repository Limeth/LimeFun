package cz.creeper.limefun;

import com.google.inject.Inject;
import cz.creeper.limefun.pipe.ImmutablePipeItemData;
import cz.creeper.limefun.pipe.PipeItemData;
import cz.creeper.limefun.pipe.PipeItemManipulatorBuilder;
import cz.creeper.limefun.pipe.PipeSystem;
import lombok.Getter;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.plugin.Plugin;

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
    public void onGamePreInitialization(GamePreInitializationEvent event) {
        Sponge.getDataManager().register(PipeItemData.class, ImmutablePipeItemData.class, new PipeItemManipulatorBuilder());
    }

    @Listener
    public void onGameServerStart(GameStartedServerEvent event) {
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
}
