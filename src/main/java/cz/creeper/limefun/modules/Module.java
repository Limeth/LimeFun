package cz.creeper.limefun.modules;

import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.data.DataManager;

public interface Module {
    default String getModuleName() {
        String className = getClass().getName();
        int lastIndex = className.lastIndexOf('.');
        int secondLastIndex = className.lastIndexOf('.', lastIndex - 1);

        if(secondLastIndex == -1) {
            return className.substring(0, lastIndex);
        }

        return className.substring(secondLastIndex + 1, lastIndex);
    }

    void registerData(DataManager manager);
    void load(ConfigurationNode node);
    void start();
    void stop();
}
