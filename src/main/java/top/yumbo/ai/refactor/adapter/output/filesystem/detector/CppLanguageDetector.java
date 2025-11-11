package top.yumbo.ai.refactor.adapter.output.filesystem.detector;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.refactor.domain.model.ProjectType;
import top.yumbo.ai.refactor.domain.model.SourceFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * C/C++语言检测器
 * 检测和分析C/C++项目
 */
@Slf4j
public class CppLanguageDetector implements LanguageDetector {

    private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s+(\\w+)");
    private static final Pattern STRUCT_PATTERN = Pattern.compile("struct\\s+(\\w+)");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(?:(?:inline|static|virtual|extern)\\s+)*\\w+[\\s*&]+([\\w:]+)\\s*\\([^)]*\\)\\s*(?:const)?\\s*[{;]");
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("#include\\s+[<\"]([^>\"]+)[>\"]");
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("namespace\\s+(\\w+)");
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("template\\s*<[^>]+>");
    private static final Pattern POINTER_PATTERN = Pattern.compile("\\w+\\s*\\*\\s*\\w+");
    private static final Pattern NEW_DELETE_PATTERN = Pattern.compile("\\b(new|delete|malloc|free|calloc|realloc)\\b");

    @Override
    public boolean isProjectOfType(Path projectPath) {
        // 检查CMakeLists.txt
        if (Files.exists(projectPath.resolve("CMakeLists.txt"))) {
            log.info("检测到C/C++项目特征文件: CMakeLists.txt");
            return true;
        }

        // 检查Makefile
        if (Files.exists(projectPath.resolve("Makefile")) ||
            Files.exists(projectPath.resolve("makefile"))) {
            log.info("检测到C/C++项目特征文件: Makefile");
            return true;
        }

        // 检查.c/.cpp/.h/.hpp文件
        try {
            boolean hasCppFiles = Files.walk(projectPath, 3)
                    .anyMatch(p -> {
                        String name = p.toString().toLowerCase();
                        return name.endsWith(".c") || name.endsWith(".cpp") ||
                               name.endsWith(".cc") || name.endsWith(".cxx") ||
                               name.endsWith(".h") || name.endsWith(".hpp") ||
                               name.endsWith(".hxx");
                    });
            if (hasCppFiles) {
                log.info("检测到C/C++源文件");
                return true;
            }
        } catch (IOException e) {
            log.warn("检测C/C++项目时出错", e);
        }

        return false;
    }

    @Override
    public ProjectType getProjectType() {
        return ProjectType.CPP;
    }

    @Override
    public List<String> getFilePatterns() {
        return List.of("*.c", "*.cpp", "*.cc", "*.cxx", "*.h", "*.hpp", "*.hxx");
    }

    @Override
    public List<String> getExcludePatterns() {
        return List.of(
                "*.o", "*.obj",     // 目标文件
                "*.a", "*.lib",     // 静态库
                "*.so", "*.dll",    // 动态库
                "build/*",          // 构建目录
                "cmake-build-*/*"   // CMake构建目录
        );
    }

    @Override
    public List<Path> getProjectFiles(Path projectPath) {
        return List.of(
                projectPath.resolve("CMakeLists.txt"),
                projectPath.resolve("Makefile"),
                projectPath.resolve("configure"),
                projectPath.resolve("meson.build")
        );
    }

    @Override
    public LanguageFeatures analyzeFeatures(SourceFile file) {
        LanguageFeatures features = new LanguageFeatures();

        // 根据文件扩展名判断是C还是C++
        String fileName = file.getFileName();
        boolean isCpp = fileName.endsWith(".cpp") || fileName.endsWith(".cc") ||
                       fileName.endsWith(".cxx") || fileName.endsWith(".hpp") ||
                       fileName.endsWith(".hxx");
        features.setLanguage(isCpp ? "C++" : "C");

        try {
            String content = file.getContent();
            if (content == null || content.isEmpty()) {
                return features;
            }

            // 分析#include语句
            List<String> includes = extractMatches(content, INCLUDE_PATTERN);
            features.addFeature("includes", includes);
            features.addMetric("include_count", includes.size());

            // 分析类定义（C++）
            List<String> classes = extractMatches(content, CLASS_PATTERN);
            features.addFeature("classes", classes);
            features.addMetric("class_count", classes.size());

            // 分析结构体
            List<String> structs = extractMatches(content, STRUCT_PATTERN);
            features.addFeature("structs", structs);
            features.addMetric("struct_count", structs.size());

            // 分析命名空间（C++）
            List<String> namespaces = extractMatches(content, NAMESPACE_PATTERN);
            features.addFeature("namespaces", namespaces);
            features.addMetric("namespace_count", namespaces.size());

            // 检测模板使用（C++）
            int templateCount = countOccurrences(content, TEMPLATE_PATTERN);
            features.addMetric("template_count", templateCount);
            features.addFeature("has_templates", templateCount > 0);

            // 分析函数定义（简化版）
            int functionCount = countOccurrences(content, FUNCTION_PATTERN);
            features.addMetric("function_count", functionCount);

            // 检测指针使用
            int pointerCount = countOccurrences(content, POINTER_PATTERN);
            features.addMetric("pointer_count", pointerCount);

            // 检测内存管理
            Matcher memMatcher = NEW_DELETE_PATTERN.matcher(content);
            int memOpCount = 0;
            while (memMatcher.find()) {
                memOpCount++;
            }
            features.addMetric("memory_operation_count", memOpCount);
            features.addFeature("has_manual_memory_management", memOpCount > 0);

            // 检测C++特性
            if (isCpp) {
                boolean hasStdVector = content.contains("std::vector");
                boolean hasStdString = content.contains("std::string");
                boolean hasStdSharedPtr = content.contains("std::shared_ptr");
                boolean hasStdUniquePtr = content.contains("std::unique_ptr");

                features.addFeature("uses_std_vector", hasStdVector);
                features.addFeature("uses_std_string", hasStdString);
                features.addFeature("uses_smart_pointers", hasStdSharedPtr || hasStdUniquePtr);

                // 检测C++版本特性
                boolean hasCpp11 = content.contains("auto ") || content.contains("nullptr");
                boolean hasCpp14 = content.contains("decltype");
                boolean hasCpp17 = content.contains("std::optional") || content.contains("std::variant");

                if (hasCpp17) features.addFeature("cpp_version", "17+");
                else if (hasCpp14) features.addFeature("cpp_version", "14+");
                else if (hasCpp11) features.addFeature("cpp_version", "11+");
                else features.addFeature("cpp_version", "98");
            }

            log.debug("C/C++文件分析完成: {}, 类={}, 函数={}, 指针={}",
                    file.getFileName(), classes.size(), functionCount, pointerCount);

        } catch (Exception e) {
            log.error("分析C/C++文件特性失败: {}", file.getFileName(), e);
        }

        return features;
    }

    @Override
    public String getVersion(Path projectPath) {
        // 尝试从CMakeLists.txt读取C++标准
        Path cmakeLists = projectPath.resolve("CMakeLists.txt");
        if (Files.exists(cmakeLists)) {
            try {
                String content = Files.readString(cmakeLists);
                Pattern versionPattern = Pattern.compile("CMAKE_CXX_STANDARD\\s+(\\d+)");
                Matcher matcher = versionPattern.matcher(content);
                if (matcher.find()) {
                    return "C++" + matcher.group(1);
                }
            } catch (IOException e) {
                log.warn("读取CMakeLists.txt失败", e);
            }
        }
        return "unknown";
    }

    /**
     * 提取匹配项
     */
    private List<String> extractMatches(String content, Pattern pattern) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            matches.add(matcher.group(1));
        }
        return matches;
    }

    /**
     * 计算模式出现次数
     */
    private int countOccurrences(String content, Pattern pattern) {
        Matcher m = pattern.matcher(content);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }

    /**
     * 解析CMakeLists.txt
     */
    public CMakeConfig parseCMakeLists(Path projectPath) {
        Path cmakeLists = projectPath.resolve("CMakeLists.txt");
        if (!Files.exists(cmakeLists)) {
            return null;
        }

        try {
            String content = Files.readString(cmakeLists);
            CMakeConfig config = new CMakeConfig();

            // 解析项目名
            Pattern projectPattern = Pattern.compile("project\\(([^)]+)\\)");
            Matcher projectMatcher = projectPattern.matcher(content);
            if (projectMatcher.find()) {
                config.setProjectName(projectMatcher.group(1).trim());
            }

            // 解析CMake版本
            Pattern versionPattern = Pattern.compile("cmake_minimum_required\\(VERSION\\s+([^)]+)\\)");
            Matcher versionMatcher = versionPattern.matcher(content);
            if (versionMatcher.find()) {
                config.setCmakeVersion(versionMatcher.group(1).trim());
            }

            // 解析C++标准
            Pattern stdPattern = Pattern.compile("CMAKE_CXX_STANDARD\\s+(\\d+)");
            Matcher stdMatcher = stdPattern.matcher(content);
            if (stdMatcher.find()) {
                config.setCppStandard(stdMatcher.group(1));
            }

            return config;
        } catch (IOException e) {
            log.error("解析CMakeLists.txt失败", e);
            return null;
        }
    }

    /**
     * CMake配置信息
     */
    public static class CMakeConfig {
        private String projectName;
        private String cmakeVersion;
        private String cppStandard;

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public String getCmakeVersion() {
            return cmakeVersion;
        }

        public void setCmakeVersion(String cmakeVersion) {
            this.cmakeVersion = cmakeVersion;
        }

        public String getCppStandard() {
            return cppStandard;
        }

        public void setCppStandard(String cppStandard) {
            this.cppStandard = cppStandard;
        }
    }
}

