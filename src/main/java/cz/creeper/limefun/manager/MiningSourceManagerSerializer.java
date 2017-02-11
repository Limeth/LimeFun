package cz.creeper.limefun.manager;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.util.List;
import java.util.UUID;

public class MiningSourceManagerSerializer implements TypeSerializer<MiningSourceManager> {

    @Override
    public MiningSourceManager deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
        return new MiningSourceManager(
                value.getNode("world").getValue(TypeToken.of(UUID.class)),
                value.getNode("distributors").getList(
                        TypeToken.of(MiningSourceDistributor.class)
                )
        );
    }

    @Override
    public void serialize(TypeToken<?> type, MiningSourceManager obj, ConfigurationNode value) throws ObjectMappingException {
        List<MiningSourceDistributor> list = Lists.newArrayList(obj.getDistributors().values());

        value.getNode("world").setValue(TypeToken.of(UUID.class), obj.getWorldId());
        value.getNode("distributors").setValue(new TypeToken<List<MiningSourceDistributor>>() {}, list);
    }
}
