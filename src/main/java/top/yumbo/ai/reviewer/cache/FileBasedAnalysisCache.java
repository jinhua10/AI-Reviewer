package top.yumbo.ai.reviewer.cache;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于文件的分析结果缓存实现
 */
@Slf4j
public class FileBasedAnalysisCache implements AnalysisCache {

    private static final String CACHE_DIR = ".ai-reviewer-cache";
    private static final String CACHE_INDEX_FILE = "cache-index.json";

    private final Path cacheDir;
    private final Map<String, CacheEntry> memoryIndex;
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    public FileBasedAnalysisCache() {
        this(Paths.get(System.getProperty("user.home"), CACHE_DIR));
    }

    public FileBasedAnalysisCache(Path cacheDir) {
        this.cacheDir = cacheDir;
        this.memoryIndex = new ConcurrentHashMap<>();

        try {
            Files.createDirectories(cacheDir);
            loadCacheIndex();
            log.info("初始化文件缓存: {}", cacheDir);
        } catch (IOException e) {
            log.error("创建缓存目录失败", e);
        }
    }

    @Override
    public void put(String key, String value, long ttlSeconds) {
        try {
            String fileName = generateFileName(key);
            Path cacheFile = cacheDir.resolve(fileName);

            CacheEntry entry = new CacheEntry(key, fileName, System.currentTimeMillis(), ttlSeconds * 1000);
            JSONObject jsonEntry = JSON.parseObject(JSON.toJSONString(entry));
            jsonEntry.put("value", value);

            // 写入文件
            Files.writeString(cacheFile, JSON.toJSONString(jsonEntry),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // 更新内存索引
            memoryIndex.put(key, entry);

            // 保存索引
            saveCacheIndex();

            log.debug("缓存写入: key={}, file={}", key, fileName);

        } catch (IOException e) {
            log.error("写入缓存失败: key={}", key, e);
        }
    }

    @Override
    public Optional<String> get(String key) {
        CacheEntry entry = memoryIndex.get(key);
        if (entry == null) {
            misses.incrementAndGet();
            return Optional.empty();
        }

        // 检查是否过期
        if (System.currentTimeMillis() > entry.getExpiryTime()) {
            remove(key);
            misses.incrementAndGet();
            return Optional.empty();
        }

        try {
            Path cacheFile = cacheDir.resolve(entry.getFileName());
            if (!Files.exists(cacheFile)) {
                memoryIndex.remove(key);
                misses.incrementAndGet();
                return Optional.empty();
            }

            String content = Files.readString(cacheFile);
            JSONObject jsonEntry = JSON.parseObject(content);
            String value = jsonEntry.getString("value");

            hits.incrementAndGet();
            log.debug("缓存命中: key={}", key);
            return Optional.of(value);

        } catch (IOException e) {
            log.error("读取缓存失败: key={}", key, e);
            memoryIndex.remove(key);
            misses.incrementAndGet();
            return Optional.empty();
        }
    }

    @Override
    public void remove(String key) {
        CacheEntry entry = memoryIndex.remove(key);
        if (entry != null) {
            try {
                Path cacheFile = cacheDir.resolve(entry.getFileName());
                Files.deleteIfExists(cacheFile);
                saveCacheIndex();
                log.debug("缓存删除: key={}", key);
            } catch (IOException e) {
                log.error("删除缓存文件失败: key={}", key, e);
            }
        }
    }

    @Override
    public void clear() {
        try {
            // 删除所有缓存文件
            Files.list(cacheDir)
                    .filter(path -> !path.getFileName().toString().equals(CACHE_INDEX_FILE))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.error("删除缓存文件失败: {}", path, e);
                        }
                    });

            memoryIndex.clear();
            saveCacheIndex();

            hits.set(0);
            misses.set(0);

            log.info("缓存已清空");

        } catch (IOException e) {
            log.error("清空缓存失败", e);
        }
    }

    @Override
    public CacheStats getStats() {
        return new CacheStats(hits.get(), misses.get(), memoryIndex.size());
    }

    @Override
    public void close() {
        try {
            saveCacheIndex();
        } catch (Exception e) {
            log.error("保存缓存索引失败", e);
        }
    }

    /**
     * 生成缓存文件名
     */
    private String generateFileName(String key) {
        return String.valueOf(Math.abs(key.hashCode())) + ".cache";
    }

    /**
     * 加载缓存索引
     */
    private void loadCacheIndex() {
        Path indexFile = cacheDir.resolve(CACHE_INDEX_FILE);
        if (!Files.exists(indexFile)) {
            return;
        }

        try {
            String content = Files.readString(indexFile);
            JSONObject indexJson = JSON.parseObject(content);

            for (String key : indexJson.keySet()) {
                JSONObject entryJson = indexJson.getJSONObject(key);
                CacheEntry entry = JSON.parseObject(entryJson.toJSONString(), CacheEntry.class);
                memoryIndex.put(key, entry);
            }

            log.info("加载缓存索引: {} 条记录", memoryIndex.size());

        } catch (IOException e) {
            log.error("加载缓存索引失败", e);
        }
    }

    /**
     * 保存缓存索引
     */
    private void saveCacheIndex() {
        try {
            Path indexFile = cacheDir.resolve(CACHE_INDEX_FILE);
            JSONObject indexJson = new JSONObject();

            indexJson.putAll(memoryIndex);

            Files.writeString(indexFile, indexJson.toJSONString(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            log.error("保存缓存索引失败", e);
        }
    }

    /**
     * 清理过期缓存
     */
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        memoryIndex.entrySet().removeIf(entry -> {
            if (now > entry.getValue().getExpiryTime()) {
                try {
                    Path cacheFile = cacheDir.resolve(entry.getValue().getFileName());
                    Files.deleteIfExists(cacheFile);
                    return true;
                } catch (IOException e) {
                    log.error("删除过期缓存文件失败: {}", entry.getKey(), e);
                }
            }
            return false;
        });

        if (!memoryIndex.isEmpty()) {
            saveCacheIndex();
        }
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        private String key;
        private String fileName;
        private long createTime;
        private long ttlMillis;
        private long expiryTime;

        public CacheEntry() {}

        public CacheEntry(String key, String fileName, long createTime, long ttlMillis) {
            this.key = key;
            this.fileName = fileName;
            this.createTime = createTime;
            this.ttlMillis = ttlMillis;
            this.expiryTime = createTime + ttlMillis;
        }

        // Getters and setters
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public long getCreateTime() { return createTime; }
        public void setCreateTime(long createTime) { this.createTime = createTime; }

        public long getTtlMillis() { return ttlMillis; }
        public void setTtlMillis(long ttlMillis) { this.ttlMillis = ttlMillis; }

        public long getExpiryTime() { return expiryTime; }
        public void setExpiryTime(long expiryTime) { this.expiryTime = expiryTime; }
    }
}
