package cz.creeper.limefun.registry.miningSource;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.spongepowered.api.registry.CatalogRegistryModule;
import org.spongepowered.api.registry.util.RegisterCatalog;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class MiningSourcesRegistryModule implements CatalogRegistryModule<MiningSource> {

    @RegisterCatalog(MiningSources.class)
    public static final Map<String, MiningSource> miningSources = Maps.newHashMap();

    @Override
    public Optional<MiningSource> getById(String id) {
        return Optional.ofNullable(miningSources.get(checkNotNull(id).toLowerCase(Locale.ENGLISH)));
    }

    @Override
    public Collection<MiningSource> getAll() {
        return ImmutableList.copyOf(miningSources.values());
    }

    @Override
    public void registerDefaults() {
        register(MiningSources.CHALCOPYRITE);
    }

    private void register(MiningSource miningSource) {
        miningSources.put(miningSource.getId(), miningSource);
    }
}
