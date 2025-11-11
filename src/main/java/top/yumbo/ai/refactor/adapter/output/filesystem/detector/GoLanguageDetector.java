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
 * Go语言检测器
 * 检测和分析Go项目
 */
@Slf4j
public class GoLanguageDetector implements LanguageDetector {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^package\\s+(\\w+)");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+(?:\"([^\"]+)\"|\\(([^)]+)\\))");
    private static final Pattern FUNC_PATTERN = Pattern.compile("func\\s+(\\w+)\\s*\\(");
    private static final Pattern STRUCT_PATTERN = Pattern.compile("type\\s+(\\w+)\\s+struct\\s*\\{");
    private static final Pattern INTERFACE_PATTERN = Pattern.compile("type\\s+(\\w+)\\s+interface\\s*\\{");

    @Override
    public boolean isProjectOfType(Path projectPath) {
        // 检查是否存在go.mod或go.sum
        boolean hasGoMod = Files.exists(projectPath.resolve("go.mod"));
        boolean hasGoSum = Files.exists(projectPath.resolve("go.sum"));

        if (hasGoMod || hasGoSum) {
            log.info("检测到Go项目特征文件: go.mod={}, go.sum={}", hasGoMod, hasGoSum);
            return true;
        }

        // 检查是否存在.go文件
        try {
            boolean hasGoFiles = Files.walk(projectPath, 3)
                    .anyMatch(p -> p.toString().endsWith(".go"));
            if (hasGoFiles) {
                log.info("检测到Go源文件");
                return true;
            }
        } catch (IOException e) {
            log.warn("检测Go项目时出错", e);
        }

        return false;
    }

    @Override
    public ProjectType getProjectType() {
        return ProjectType.GO;
    }

    @Override
    public List<String> getFilePatterns() {
        return List.of("*.go");
    }

    @Override
    public List<String> getExcludePatterns() {
        return List.of(
                "*_test.go",     // 测试文件
                "vendor/*",      // vendor目录
                "*.pb.go",       // protobuf生成文件
                "*.gen.go"       // 其他生成文件
        );
    }

    @Override
    public List<Path> getProjectFiles(Path projectPath) {
        return List.of(
                projectPath.resolve("go.mod"),
                projectPath.resolve("go.sum"),
                projectPath.resolve("Makefile")
        );
    }

    @Override
    public LanguageFeatures analyzeFeatures(SourceFile file) {
        LanguageFeatures features = new LanguageFeatures();
        features.setLanguage("Go");

        try {
            String content = file.getContent();
            if (content == null || content.isEmpty()) {
                return features;
            }

            // 分析包声明
            Matcher packageMatcher = PACKAGE_PATTERN.matcher(content);
            List<String> packages = new ArrayList<>();
            while (packageMatcher.find()) {
                packages.add(packageMatcher.group(1));
            }
            features.addFeature("packages", packages);

            // 分析import语句
            Matcher importMatcher = IMPORT_PATTERN.matcher(content);
            List<String> imports = new ArrayList<>();
            while (importMatcher.find()) {
                String importPath = importMatcher.group(1);
                if (importPath != null) {
                    imports.add(importPath);
                }
            }
            features.addFeature("imports", imports);
            features.addMetric("import_count", imports.size());

            // 分析函数定义
            Matcher funcMatcher = FUNC_PATTERN.matcher(content);
            List<String> functions = new ArrayList<>();
            while (funcMatcher.find()) {
                functions.add(funcMatcher.group(1));
            }
            features.addFeature("functions", functions);
            features.addMetric("function_count", functions.size());

            // 分析结构体定义
            Matcher structMatcher = STRUCT_PATTERN.matcher(content);
            List<String> structs = new ArrayList<>();
            while (structMatcher.find()) {
                structs.add(structMatcher.group(1));
            }
            features.addFeature("structs", structs);
            features.addMetric("struct_count", structs.size());

            // 分析接口定义
            Matcher interfaceMatcher = INTERFACE_PATTERN.matcher(content);
            List<String> interfaces = new ArrayList<>();
            while (interfaceMatcher.find()) {
                interfaces.add(interfaceMatcher.group(1));
            }
            features.addFeature("interfaces", interfaces);
            features.addMetric("interface_count", interfaces.size());

            // 检测goroutine使用
            boolean hasGoroutines = content.contains("go func") || content.contains("go \\w+\\(");
            features.addFeature("has_goroutines", hasGoroutines);

            // 检测channel使用
            boolean hasChannels = content.contains("chan ") || content.contains("<-");
            features.addFeature("has_channels", hasChannels);

            // 检测defer使用
            int deferCount = countOccurrences(content, "\\bdefer\\b");
            features.addMetric("defer_count", deferCount);

            // 检测error处理
            int errorChecks = countOccurrences(content, "if err != nil");
            features.addMetric("error_checks", errorChecks);

            log.debug("Go文件分析完成: {}, 函数={}, 结构体={}, 接口={}",
                    file.getFileName(), functions.size(), structs.size(), interfaces.size());

        } catch (Exception e) {
            log.error("分析Go文件特性失败: {}", file.getFileName(), e);
        }

        return features;
    }

    @Override
    public String getVersion(Path projectPath) {
        Path goModPath = projectPath.resolve("go.mod");
        if (Files.exists(goModPath)) {
            try {
                String content = Files.readString(goModPath);
                Pattern versionPattern = Pattern.compile("go\\s+(\\d+\\.\\d+(?:\\.\\d+)?)");
                Matcher matcher = versionPattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (IOException e) {
                log.warn("读取go.mod失败", e);
            }
        }
        return "unknown";
    }

    /**
     * 计算模式出现次数
     */
    private int countOccurrences(String content, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(content);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }

    /**
     * 检测Go模块信息
     */
    public GoModuleInfo parseGoMod(Path projectPath) {
        Path goModPath = projectPath.resolve("go.mod");
        if (!Files.exists(goModPath)) {
            return null;
        }

        try {
            String content = Files.readString(goModPath);
            GoModuleInfo info = new GoModuleInfo();

            // 解析module名称
            Pattern modulePattern = Pattern.compile("module\\s+([^\\s]+)");
            Matcher moduleMatcher = modulePattern.matcher(content);
            if (moduleMatcher.find()) {
                info.setModuleName(moduleMatcher.group(1));
            }

            // 解析Go版本
            Pattern goPattern = Pattern.compile("go\\s+(\\d+\\.\\d+(?:\\.\\d+)?)");
            Matcher goMatcher = goPattern.matcher(content);
            if (goMatcher.find()) {
                info.setGoVersion(goMatcher.group(1));
            }

            // 解析依赖
            Pattern requirePattern = Pattern.compile("require\\s+([^\\s]+)\\s+v([^\\s]+)");
            Matcher requireMatcher = requirePattern.matcher(content);
            while (requireMatcher.find()) {
                info.addDependency(requireMatcher.group(1), requireMatcher.group(2));
            }

            return info;
        } catch (IOException e) {
            log.error("解析go.mod失败", e);
            return null;
        }
    }

    /**
     * Go模块信息
     */
    public static class GoModuleInfo {
        private String moduleName;
        private String goVersion;
        private final List<Dependency> dependencies = new ArrayList<>();

        public String getModuleName() {
            return moduleName;
        }

        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }

        public String getGoVersion() {
            return goVersion;
        }

        public void setGoVersion(String goVersion) {
            this.goVersion = goVersion;
        }

        public List<Dependency> getDependencies() {
            return dependencies;
        }

        public void addDependency(String name, String version) {
            dependencies.add(new Dependency(name, version));
        }

        public static class Dependency {
            private final String name;
            private final String version;

            public Dependency(String name, String version) {
                this.name = name;
                this.version = version;
            }

            public String getName() {
                return name;
            }

            public String getVersion() {
                return version;
            }
        }
    }
}

