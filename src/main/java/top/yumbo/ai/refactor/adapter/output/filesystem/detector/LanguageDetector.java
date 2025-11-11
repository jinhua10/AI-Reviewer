package top.yumbo.ai.refactor.adapter.output.filesystem.detector;

import top.yumbo.ai.refactor.domain.model.ProjectType;
import top.yumbo.ai.refactor.domain.model.SourceFile;

import java.nio.file.Path;
import java.util.List;

/**
 * 语言检测器接口
 * 用于检测和分析不同编程语言的项目
 */
public interface LanguageDetector {

    /**
     * 判断项目是否为该语言类型
     */
    boolean isProjectOfType(Path projectPath);

    /**
     * 获取项目类型
     */
    ProjectType getProjectType();

    /**
     * 获取文件匹配模式
     */
    List<String> getFilePatterns();

    /**
     * 获取排除模式
     */
    List<String> getExcludePatterns();

    /**
     * 获取项目特征文件
     */
    List<Path> getProjectFiles(Path projectPath);

    /**
     * 分析源文件特性
     */
    LanguageFeatures analyzeFeatures(SourceFile file);

    /**
     * 获取语言版本
     */
    String getVersion(Path projectPath);
}

