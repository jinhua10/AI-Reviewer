package top.yumbo.ai.refactor.adapter.output.filesystem.detector;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.refactor.domain.model.ProjectType;
import top.yumbo.ai.refactor.domain.model.SourceFile;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语言检测器注册表
 * 统一管理所有语言检测器，提供自动检测功能
 */
@Slf4j
public class LanguageDetectorRegistry {

    private final Map<ProjectType, LanguageDetector> detectors = new ConcurrentHashMap<>();
    private final List<LanguageDetector> detectorList = new ArrayList<>();

    /**
     * 注册语言检测器
     */
    public void registerDetector(LanguageDetector detector) {
        ProjectType type = detector.getProjectType();
        detectors.put(type, detector);
        detectorList.add(detector);
        log.info("语言检测器已注册: {}", type);
    }

    /**
     * 自动检测项目语言
     * 支持多语言项目
     */
    public List<ProjectType> detectLanguages(Path projectPath) {
        List<ProjectType> detected = new ArrayList<>();

        for (LanguageDetector detector : detectorList) {
            try {
                if (detector.isProjectOfType(projectPath)) {
                    detected.add(detector.getProjectType());
                    log.info("检测到语言: {}", detector.getProjectType());
                }
            } catch (Exception e) {
                log.warn("检测语言失败: {}", detector.getProjectType(), e);
            }
        }

        if (detected.isEmpty()) {
            log.warn("未能检测到任何已知语言，返回UNKNOWN");
            detected.add(ProjectType.UNKNOWN);
        } else if (detected.size() > 1) {
            log.info("检测到多语言项目: {}", detected);
        }

        return detected;
    }

    /**
     * 检测主要语言
     */
    public ProjectType detectPrimaryLanguage(Path projectPath) {
        List<ProjectType> languages = detectLanguages(projectPath);

        if (languages.isEmpty() || languages.get(0) == ProjectType.UNKNOWN) {
            return ProjectType.UNKNOWN;
        }

        // 如果是多语言，返回第一个或MIXED
        if (languages.size() > 1) {
            return ProjectType.MIXED;
        }

        return languages.get(0);
    }

    /**
     * 根据类型获取检测器
     */
    public LanguageDetector getDetector(ProjectType type) {
        return detectors.get(type);
    }

    /**
     * 分析源文件特性
     */
    public LanguageFeatures analyzeSourceFile(SourceFile file, ProjectType type) {
        LanguageDetector detector = detectors.get(type);
        if (detector == null) {
            log.warn("未找到语言检测器: {}", type);
            return new LanguageFeatures();
        }

        try {
            return detector.analyzeFeatures(file);
        } catch (Exception e) {
            log.error("分析源文件特性失败: {}, type={}", file.getFileName(), type, e);
            return new LanguageFeatures();
        }
    }

    /**
     * 获取项目版本信息
     */
    public Map<ProjectType, String> getVersions(Path projectPath) {
        Map<ProjectType, String> versions = new HashMap<>();
        List<ProjectType> languages = detectLanguages(projectPath);

        for (ProjectType type : languages) {
            if (type == ProjectType.UNKNOWN || type == ProjectType.MIXED) {
                continue;
            }

            LanguageDetector detector = detectors.get(type);
            if (detector != null) {
                try {
                    String version = detector.getVersion(projectPath);
                    versions.put(type, version);
                } catch (Exception e) {
                    log.warn("获取语言版本失败: {}", type, e);
                }
            }
        }

        return versions;
    }

    /**
     * 获取所有文件模式
     */
    public List<String> getAllFilePatterns(List<ProjectType> types) {
        Set<String> patterns = new HashSet<>();

        for (ProjectType type : types) {
            LanguageDetector detector = detectors.get(type);
            if (detector != null) {
                patterns.addAll(detector.getFilePatterns());
            }
        }

        return new ArrayList<>(patterns);
    }

    /**
     * 获取所有排除模式
     */
    public List<String> getAllExcludePatterns(List<ProjectType> types) {
        Set<String> patterns = new HashSet<>();

        for (ProjectType type : types) {
            LanguageDetector detector = detectors.get(type);
            if (detector != null) {
                patterns.addAll(detector.getExcludePatterns());
            }
        }

        return new ArrayList<>(patterns);
    }

    /**
     * 获取项目统计信息
     */
    public ProjectLanguageStats getProjectStats(Path projectPath) {
        ProjectLanguageStats stats = new ProjectLanguageStats();
        List<ProjectType> languages = detectLanguages(projectPath);

        stats.setLanguages(languages);
        stats.setIsMixed(languages.size() > 1);
        stats.setPrimaryLanguage(languages.isEmpty() ? ProjectType.UNKNOWN : languages.get(0));

        // 获取版本信息
        Map<ProjectType, String> versions = getVersions(projectPath);
        stats.setVersions(versions);

        return stats;
    }

    /**
     * 获取已注册的检测器数量
     */
    public int getDetectorCount() {
        return detectors.size();
    }

    /**
     * 获取所有支持的语言类型
     */
    public Set<ProjectType> getSupportedLanguages() {
        return detectors.keySet();
    }

    /**
     * 检查是否支持某种语言
     */
    public boolean isLanguageSupported(ProjectType type) {
        return detectors.containsKey(type);
    }

    /**
     * 清空所有检测器
     */
    public void clear() {
        detectors.clear();
        detectorList.clear();
        log.info("语言检测器注册表已清空");
    }

    /**
     * 项目语言统计信息
     */
    public static class ProjectLanguageStats {
        private List<ProjectType> languages;
        private boolean isMixed;
        private ProjectType primaryLanguage;
        private Map<ProjectType, String> versions;

        public List<ProjectType> getLanguages() {
            return languages;
        }

        public void setLanguages(List<ProjectType> languages) {
            this.languages = languages;
        }

        public boolean isMixed() {
            return isMixed;
        }

        public void setIsMixed(boolean mixed) {
            isMixed = mixed;
        }

        public ProjectType getPrimaryLanguage() {
            return primaryLanguage;
        }

        public void setPrimaryLanguage(ProjectType primaryLanguage) {
            this.primaryLanguage = primaryLanguage;
        }

        public Map<ProjectType, String> getVersions() {
            return versions;
        }

        public void setVersions(Map<ProjectType, String> versions) {
            this.versions = versions;
        }

        @Override
        public String toString() {
            return "ProjectLanguageStats{" +
                    "languages=" + languages +
                    ", isMixed=" + isMixed +
                    ", primaryLanguage=" + primaryLanguage +
                    ", versions=" + versions +
                    '}';
        }
    }

    /**
     * 创建默认注册表（包含所有内置检测器）
     */
    public static LanguageDetectorRegistry createDefault() {
        LanguageDetectorRegistry registry = new LanguageDetectorRegistry();

        // 注册所有内置检测器
        registry.registerDetector(new GoLanguageDetector());
        registry.registerDetector(new RustLanguageDetector());
        registry.registerDetector(new CppLanguageDetector());

        log.info("默认语言检测器注册表已创建，包含 {} 个检测器",
                registry.getDetectorCount());

        return registry;
    }
}

