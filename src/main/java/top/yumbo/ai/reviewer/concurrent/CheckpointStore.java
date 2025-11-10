package top.yumbo.ai.reviewer.concurrent;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 断点续传存储
 * 保存分析进度，支持中断后继续执行
 */
public class CheckpointStore {

    private static final Logger log = LoggerFactory.getLogger(CheckpointStore.class);

    private final Path checkpointDir;
    private final Map<String, CheckpointData> memoryCache;
    private final boolean enabled;

    public CheckpointStore(Path checkpointDir, boolean enabled) {
        this.checkpointDir = checkpointDir;
        this.memoryCache = new ConcurrentHashMap<>();
        this.enabled = enabled;

        if (enabled) {
            try {
                Files.createDirectories(checkpointDir);
                log.info("断点续传已启用，存储路径: {}", checkpointDir);
            } catch (IOException e) {
                log.warn("无法创建断点存储目录: {}", e.getMessage());
            }
        }
    }

    /**
     * 保存检查点
     */
    public void save(String chunkId, String analysis, long timestamp) {
        if (!enabled) {
            return;
        }

        CheckpointData data = new CheckpointData(chunkId, analysis, timestamp);
        memoryCache.put(chunkId, data);

        // 异步保存到磁盘
        saveToDisk(chunkId, data);
    }

    /**
     * 加载检查点
     */
    public CheckpointData load(String chunkId) {
        if (!enabled) {
            return null;
        }

        // 先从内存加载
        CheckpointData data = memoryCache.get(chunkId);
        if (data != null) {
            return data;
        }

        // 从磁盘加载
        return loadFromDisk(chunkId);
    }

    /**
     * 检查是否存在
     */
    public boolean exists(String chunkId) {
        if (!enabled) {
            return false;
        }

        if (memoryCache.containsKey(chunkId)) {
            return true;
        }

        Path file = checkpointDir.resolve(chunkId + ".json");
        return Files.exists(file);
    }

    /**
     * 清除所有检查点
     */
    public void clear() {
        memoryCache.clear();

        if (enabled && Files.exists(checkpointDir)) {
            try {
                Files.walk(checkpointDir)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            log.warn("无法删除检查点文件: {}", file);
                        }
                    });
            } catch (IOException e) {
                log.warn("清除检查点失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 保存到磁盘
     */
    private void saveToDisk(String chunkId, CheckpointData data) {
        Path file = checkpointDir.resolve(chunkId + ".json");
        try {
            String json = JSON.toJSONString(data);
            Files.writeString(file, json);
        } catch (IOException e) {
            log.warn("保存检查点失败: {}", e.getMessage());
        }
    }

    /**
     * 从磁盘加载
     */
    private CheckpointData loadFromDisk(String chunkId) {
        Path file = checkpointDir.resolve(chunkId + ".json");
        if (!Files.exists(file)) {
            return null;
        }

        try {
            String json = Files.readString(file);
            CheckpointData data = JSON.parseObject(json, CheckpointData.class);
            memoryCache.put(chunkId, data);
            return data;
        } catch (IOException e) {
            log.warn("加载检查点失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查点数据
     */
    public static class CheckpointData {
        private String chunkId;
        private String analysis;
        private long timestamp;

        public CheckpointData() {
        }

        public CheckpointData(String chunkId, String analysis, long timestamp) {
            this.chunkId = chunkId;
            this.analysis = analysis;
            this.timestamp = timestamp;
        }

        // Getters and Setters
        public String getChunkId() { return chunkId; }
        public void setChunkId(String chunkId) { this.chunkId = chunkId; }

        public String getAnalysis() { return analysis; }
        public void setAnalysis(String analysis) { this.analysis = analysis; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}

