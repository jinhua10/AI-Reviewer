package top.yumbo.ai.refactor.adapter.output.filesystem.detector;

import java.util.HashMap;
import java.util.Map;

/**
 * 语言特性
 * 存储语言分析结果
 */
public class LanguageFeatures {

    private String language;
    private final Map<String, Object> features = new HashMap<>();
    private final Map<String, Integer> metrics = new HashMap<>();

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void addFeature(String name, Object value) {
        features.put(name, value);
    }

    public Object getFeature(String name) {
        return features.get(name);
    }

    public Map<String, Object> getAllFeatures() {
        return new HashMap<>(features);
    }

    public void addMetric(String name, int value) {
        metrics.put(name, value);
    }

    public Integer getMetric(String name) {
        return metrics.get(name);
    }

    public Map<String, Integer> getAllMetrics() {
        return new HashMap<>(metrics);
    }

    public boolean hasFeature(String name) {
        return features.containsKey(name);
    }

    @Override
    public String toString() {
        return "LanguageFeatures{" +
                "language='" + language + '\'' +
                ", features=" + features.size() +
                ", metrics=" + metrics.size() +
                '}';
    }
}

