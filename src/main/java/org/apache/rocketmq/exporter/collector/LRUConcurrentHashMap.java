package org.apache.rocketmq.exporter.collector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.rocketmq.exporter.util.MetricsExpireConfigUtils;

class LRUConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {

    private LRUHashMap<K> cache = new LRUHashMap<>(8, this);

    @Override public V put(K key, V value) {
        V v = super.put(key, value);
        cache.put(key, System.currentTimeMillis());
        return v;
    }

    static class LRUHashMap<K> extends LinkedHashMap<K, Long> {
        private ConcurrentHashMap map;
        private ReadWriteLock reentrantLock = new ReentrantReadWriteLock();
        private Lock writeLock = reentrantLock.writeLock();
        private long expireTimeout = MetricsExpireConfigUtils.getConfig().getTimeoutms();
        private K dummy = null;

        public LRUHashMap(int initialCapacity, ConcurrentHashMap map) {
            super(initialCapacity, 0.75f, true);
            this.map = map;
        }

        @Override public Long put(K key, Long value) {
            try {
                writeLock.lock();
                Long old = super.put(key, value);
                super.put(dummy, value); // trigger after node insertion
                return old;
            } finally {
                super.remove(dummy, value);
                writeLock.unlock();
            }
        }

        @Override protected boolean removeEldestEntry(Map.Entry<K, Long> eldest) {
            if ((System.currentTimeMillis() - eldest.getValue()) > expireTimeout) {
                this.map.remove(eldest.getKey());
                return true;
            }
            return false;
        }
    }
}
