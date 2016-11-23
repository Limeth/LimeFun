package cz.creeper.limefun.modules;

import ninja.leaping.configurate.ConfigurationNode;

public interface Module {
    String getModuleName();
    void load(ConfigurationNode node);
    void start();
    void stop();
}
