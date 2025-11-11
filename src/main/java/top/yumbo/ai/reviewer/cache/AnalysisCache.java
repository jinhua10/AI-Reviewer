package top.yumbo.ai.reviewer.cache;

import java.util.Optional;

/**
 * 分析结果缓存接口
 */
public interface AnalysisCache {

    /**
     * 存储分析结果
     * @param key 缓存键
     * @param value 缓存值
     * @param ttlSeconds 过期时间（秒）
     */
    void put(String key, String value, long ttlSeconds);

    /**
     * 获取分析结果
     * @param key 缓存键
     * @return 缓存值，如果不存在返回empty
     */
    Optional<String> get(String key);

    /**
     * 删除缓存
     * @param key 缓存键
     */
    void remove(String key);

    /**
     * 清空所有缓存
     */
    void clear();

    /**
     * 获取缓存统计信息
     */
    CacheStats getStats();

    /**
     * 关闭缓存
     */
    void close();

    /**
     * 缓存统计信息
     */
    class CacheStats {
        private final long hits;
        private final long misses;
        private final long entries;
        private final double hitRate;

        public CacheStats(long hits, long misses, long entries) {
            this.hits = hits;
            this.misses = misses;
            this.entries = entries;
            this.hitRate = entries > 0 ? (double) hits / (hits + misses) : 0.0;
        }

        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getEntries() { return entries; }
        public double getHitRate() { return hitRate; }
    }
}
