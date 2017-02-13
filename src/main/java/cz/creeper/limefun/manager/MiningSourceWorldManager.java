package cz.creeper.limefun.manager;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import cz.creeper.limefun.LimeFun;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ConfigSerializable
public class MiningSourceWorldManager {

    @Setting("worlds")
    private final Map<UUID, MiningSourceManager> worldToManager;

    MiningSourceWorldManager(Collection<MiningSourceManager> managers) {
        this.worldToManager = managers.stream()
                .collect(Collectors.toMap(MiningSourceManager::getWorldId, Function.identity()));
    }

    public MiningSourceWorldManager() {
        this(Collections.emptyList());
    }

    public MiningSourceManager getManager(World world) {
        return worldToManager.computeIfAbsent(world.getUniqueId(), uniqueId -> new MiningSourceManager(world));
    }

    public Map<UUID, MiningSourceManager> getManagers() {
        return ImmutableMap.copyOf(worldToManager);
    }

    public static MiningSourceWorldManager load() {
        try {
            HoconConfigurationLoader loader = getConfigurationLoader();
            CommentedConfigurationNode root = loader.load(ConfigurationOptions.defaults());

            return root.getNode("managers").getValue(TypeToken.of(MiningSourceWorldManager.class),
                    (Supplier<MiningSourceWorldManager>) MiningSourceWorldManager::new);
        } catch (ObjectMappingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save() {
        try {
            Path file = getConfigurationFile();
            HoconConfigurationLoader loader = getConfigurationLoader();
            CommentedConfigurationNode root = loader.createEmptyNode(loader.getDefaultOptions());

            root.getNode("managers").setValue(TypeToken.of(MiningSourceWorldManager.class), this);

            Files.createDirectories(file.getParent());

            if(!Files.isRegularFile(file))
                Files.createFile(file);

            loader.save(root);
        } catch (ObjectMappingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static HoconConfigurationLoader getConfigurationLoader() {
        Path path = getConfigurationFile();

        return HoconConfigurationLoader.builder()
                .setPath(path)
                .build();
    }

    public static Path getConfigurationFile() {
        return LimeFun.getInstance().getConfigDirectory().resolve("miningSources");
    }

}
