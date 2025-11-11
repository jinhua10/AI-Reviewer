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
 * Rust语言检测器
 * 检测和分析Rust项目
 */
@Slf4j
public class RustLanguageDetector implements LanguageDetector {

    private static final Pattern MOD_PATTERN = Pattern.compile("^(?:pub\\s+)?mod\\s+(\\w+)");
    private static final Pattern USE_PATTERN = Pattern.compile("^use\\s+([^;]+);");
    private static final Pattern FN_PATTERN = Pattern.compile("fn\\s+(\\w+)\\s*(<[^>]*>)?\\s*\\(");
    private static final Pattern STRUCT_PATTERN = Pattern.compile("struct\\s+(\\w+)");
    private static final Pattern ENUM_PATTERN = Pattern.compile("enum\\s+(\\w+)");
    private static final Pattern TRAIT_PATTERN = Pattern.compile("trait\\s+(\\w+)");
    private static final Pattern IMPL_PATTERN = Pattern.compile("impl\\s+(?:<[^>]*>\\s+)?(?:([\\w:]+)\\s+for\\s+)?([\\w:]+)");
    private static final Pattern UNSAFE_PATTERN = Pattern.compile("unsafe\\s+(?:fn|impl|trait)");

    @Override
    public boolean isProjectOfType(Path projectPath) {
        // 检查Cargo.toml
        boolean hasCargoToml = Files.exists(projectPath.resolve("Cargo.toml"));
        boolean hasCargoLock = Files.exists(projectPath.resolve("Cargo.lock"));

        if (hasCargoToml || hasCargoLock) {
            log.info("检测到Rust项目特征文件: Cargo.toml={}, Cargo.lock={}",
                    hasCargoToml, hasCargoLock);
            return true;
        }

        // 检查.rs文件
        try {
            boolean hasRustFiles = Files.walk(projectPath, 3)
                    .anyMatch(p -> p.toString().endsWith(".rs"));
            if (hasRustFiles) {
                log.info("检测到Rust源文件");
                return true;
            }
        } catch (IOException e) {
            log.warn("检测Rust项目时出错", e);
        }

        return false;
    }

    @Override
    public ProjectType getProjectType() {
        return ProjectType.RUST;
    }

    @Override
    public List<String> getFilePatterns() {
        return List.of("*.rs");
    }

    @Override
    public List<String> getExcludePatterns() {
        return List.of(
                "target/*",      // 构建输出
                "*.rlib",        // 库文件
                "*.so", "*.dll", "*.dylib"  // 动态库
        );
    }

    @Override
    public List<Path> getProjectFiles(Path projectPath) {
        return List.of(
                projectPath.resolve("Cargo.toml"),
                projectPath.resolve("Cargo.lock"),
                projectPath.resolve("rust-toolchain.toml")
        );
    }

    @Override
    public LanguageFeatures analyzeFeatures(SourceFile file) {
        LanguageFeatures features = new LanguageFeatures();
        features.setLanguage("Rust");

        try {
            String content = file.getContent();
            if (content == null || content.isEmpty()) {
                return features;
            }

            // 分析模块声明
            List<String> modules = extractMatches(content, MOD_PATTERN);
            features.addFeature("modules", modules);
            features.addMetric("module_count", modules.size());

            // 分析use语句
            List<String> uses = extractMatches(content, USE_PATTERN);
            features.addFeature("uses", uses);
            features.addMetric("use_count", uses.size());

            // 分析函数定义
            List<String> functions = extractMatches(content, FN_PATTERN);
            features.addFeature("functions", functions);
            features.addMetric("function_count", functions.size());

            // 分析结构体
            List<String> structs = extractMatches(content, STRUCT_PATTERN);
            features.addFeature("structs", structs);
            features.addMetric("struct_count", structs.size());

            // 分析枚举
            List<String> enums = extractMatches(content, ENUM_PATTERN);
            features.addFeature("enums", enums);
            features.addMetric("enum_count", enums.size());

            // 分析trait
            List<String> traits = extractMatches(content, TRAIT_PATTERN);
            features.addFeature("traits", traits);
            features.addMetric("trait_count", traits.size());

            // 分析impl块
            Matcher implMatcher = IMPL_PATTERN.matcher(content);
            int implCount = 0;
            while (implMatcher.find()) {
                implCount++;
            }
            features.addMetric("impl_count", implCount);

            // 检测unsafe代码
            Matcher unsafeMatcher = UNSAFE_PATTERN.matcher(content);
            int unsafeCount = 0;
            while (unsafeMatcher.find()) {
                unsafeCount++;
            }
            features.addFeature("has_unsafe", unsafeCount > 0);
            features.addMetric("unsafe_count", unsafeCount);

            // 检测宏使用
            int macroCount = countOccurrences(content, "\\w+!");
            features.addMetric("macro_count", macroCount);

            // 检测生命周期参数
            int lifetimeCount = countOccurrences(content, "'\\w+");
            features.addMetric("lifetime_count", lifetimeCount);

            // 检测Result和Option
            boolean hasResult = content.contains("Result<");
            boolean hasOption = content.contains("Option<");
            features.addFeature("has_result", hasResult);
            features.addFeature("has_option", hasOption);

            log.debug("Rust文件分析完成: {}, 函数={}, 结构体={}, unsafe={}",
                    file.getFileName(), functions.size(), structs.size(), unsafeCount);

        } catch (Exception e) {
            log.error("分析Rust文件特性失败: {}", file.getFileName(), e);
        }

        return features;
    }

    @Override
    public String getVersion(Path projectPath) {
        Path cargoToml = projectPath.resolve("Cargo.toml");
        if (Files.exists(cargoToml)) {
            try {
                String content = Files.readString(cargoToml);
                Pattern versionPattern = Pattern.compile("rust-version\\s*=\\s*\"([^\"]+)\"");
                Matcher matcher = versionPattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (IOException e) {
                log.warn("读取Cargo.toml失败", e);
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
     * 解析Cargo.toml
     */
    public CargoManifest parseCargoToml(Path projectPath) {
        Path cargoToml = projectPath.resolve("Cargo.toml");
        if (!Files.exists(cargoToml)) {
            return null;
        }

        try {
            String content = Files.readString(cargoToml);
            CargoManifest manifest = new CargoManifest();

            // 解析包名
            Pattern namePattern = Pattern.compile("name\\s*=\\s*\"([^\"]+)\"");
            Matcher nameMatcher = namePattern.matcher(content);
            if (nameMatcher.find()) {
                manifest.setName(nameMatcher.group(1));
            }

            // 解析版本
            Pattern versionPattern = Pattern.compile("version\\s*=\\s*\"([^\"]+)\"");
            Matcher versionMatcher = versionPattern.matcher(content);
            if (versionMatcher.find()) {
                manifest.setVersion(versionMatcher.group(1));
            }

            // 解析依赖
            Pattern depPattern = Pattern.compile("([\\w-]+)\\s*=\\s*[\"\\{]([^\"\\}]+)");
            Matcher depMatcher = depPattern.matcher(content);
            while (depMatcher.find()) {
                manifest.addDependency(depMatcher.group(1), depMatcher.group(2));
            }

            return manifest;
        } catch (IOException e) {
            log.error("解析Cargo.toml失败", e);
            return null;
        }
    }

    /**
     * Cargo清单信息
     */
    public static class CargoManifest {
        private String name;
        private String version;
        private final List<Dependency> dependencies = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
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

