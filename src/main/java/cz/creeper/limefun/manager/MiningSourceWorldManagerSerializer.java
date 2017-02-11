package cz.creeper.limefun.manager;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.util.List;

public class MiningSourceWorldManagerSerializer implements TypeSerializer<MiningSourceWorldManager> {

    @Override
    public MiningSourceWorldManager deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
        return new MiningSourceWorldManager(
                value.getList(
                        TypeToken.of(MiningSourceManager.class)
                )
        );
    }

    @Override
    public void serialize(TypeToken<?> type, MiningSourceWorldManager obj, ConfigurationNode value) throws ObjectMappingException {
        List<MiningSourceManager> list = Lists.newArrayList(obj.getManagers().values());

        value.setValue(new TypeToken<List<MiningSourceManager>>() {}, list);
    }
}
