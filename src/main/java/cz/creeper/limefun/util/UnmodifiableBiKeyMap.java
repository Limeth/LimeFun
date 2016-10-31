package cz.creeper.limefun.util;

import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.Set;

@AllArgsConstructor
public class UnmodifiableBiKeyMap<K1, K2, V> implements BiKeyMap<K1, K2, V> {
    private final BiKeyMap<K1, K2, V> map;

    public static <K1, K2, V> UnmodifiableBiKeyMap<K1, K2, V> of(BiKeyMap<K1, K2, V> map) {
        return new UnmodifiableBiKeyMap<>(map);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKeyFirst(K1 key) {
        return map.containsKeyFirst(key);
    }

    @Override
    public boolean containsKeySecond(K2 key) {
        return map.containsKeySecond(key);
    }

    @Override
    public boolean containsValue(V value) {
        return map.containsValue(value);
    }

    @Override
    public V getFirst(K1 key) {
        return map.getFirst(key);
    }

    @Override
    public V getSecond(K2 key) {
        return map.getSecond(key);
    }

    @Override
    public K1 getFirstKey(K2 secondKey) {
        return map.getFirstKey(secondKey);
    }

    @Override
    public K2 getSecondKey(K1 firstKey) {
        return map.getSecondKey(firstKey);
    }

    @Override
    public V put(K1 firstKey, K2 secondKey, V value) {
        return deny();
    }

    @Override
    public V removeFirst(K1 key) {
        return deny();
    }

    @Override
    public V removeSecond(K2 key) {
        return deny();
    }

    @Override
    public void clear() {
        deny();
    }

    @Override
    public Pair<Set<K1>, Set<K2>> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Pair<Pair<K1, K2>, V>> entrySet() {
        return map.entrySet();
    }

    private static final <A> A deny(){
        throw new IllegalStateException("This map is immutable.");
    }
}
