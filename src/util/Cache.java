package util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A generic cache class that stores key-value pairs with optional expiration.
 * Demonstrates the use of generics in the project.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public class Cache<K, V> {

    private final Map<K, CacheEntry<V>> cache = new HashMap<>();
    private final long defaultTtlMillis;

    /**
     * Entry wrapper that holds the value and its expiration time.
     */
    private static class CacheEntry<V> {
        final V value;
        final long expiresAt;

        CacheEntry(V value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return expiresAt > 0 && System.currentTimeMillis() > expiresAt;
        }
    }

    /**
     * Create a cache with no default expiration.
     */
    public Cache() {
        this(0);
    }

    /**
     * Create a cache with a default TTL (time-to-live) in milliseconds.
     *
     * @param defaultTtlMillis default expiration time, 0 for no expiration
     */
    public Cache(long defaultTtlMillis) {
        this.defaultTtlMillis = defaultTtlMillis;
    }

    /**
     * Store a value in the cache.
     *
     * @param key   the key
     * @param value the value to store
     */
    public void put(K key, V value) {
        long expiresAt = defaultTtlMillis > 0 ? System.currentTimeMillis() + defaultTtlMillis : 0;
        cache.put(key, new CacheEntry<>(value, expiresAt));
    }

    /**
     * Store a value with a custom TTL.
     *
     * @param key       the key
     * @param value     the value to store
     * @param ttlMillis custom expiration time in milliseconds
     */
    public void put(K key, V value, long ttlMillis) {
        long expiresAt = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : 0;
        cache.put(key, new CacheEntry<>(value, expiresAt));
    }

    /**
     * Get a value from the cache.
     *
     * @param key the key
     * @return Optional containing the value if present and not expired
     */
    public Optional<V> get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value);
    }

    /**
     * Get a value from the cache, or compute and store it if absent.
     *
     * @param key      the key
     * @param supplier supplier to compute the value if not present
     * @return the cached or computed value
     */
    public V getOrCompute(K key, Supplier<V> supplier) {
        return get(key).orElseGet(() -> {
            V value = supplier.get();
            put(key, value);
            return value;
        });
    }

    /**
     * Remove a value from the cache.
     *
     * @param key the key to remove
     * @return true if the key was present
     */
    public boolean remove(K key) {
        return cache.remove(key) != null;
    }

    /**
     * Clear all entries from the cache.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Get the number of entries in the cache (including expired ones).
     */
    public int size() {
        return cache.size();
    }

    /**
     * Remove all expired entries from the cache.
     *
     * @return the number of entries removed
     */
    public int cleanup() {
        int initialSize = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return initialSize - cache.size();
    }
}
