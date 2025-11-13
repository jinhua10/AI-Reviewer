package top.yumbo.ai.reviewer.infrastructure.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import top.yumbo.ai.reviewer.adapter.output.ast.parser.ASTParserFactory;
import top.yumbo.ai.reviewer.application.port.output.AIServicePort;
import top.yumbo.ai.reviewer.application.port.output.ASTParserPort;
import top.yumbo.ai.reviewer.adapter.output.cache.FileCacheAdapter;
import top.yumbo.ai.reviewer.adapter.output.filesystem.LocalFileSystemAdapter;
import top.yumbo.ai.reviewer.adapter.output.repository.GitHubRepositoryAdapter;
import top.yumbo.ai.reviewer.adapter.output.repository.GiteeRepositoryAdapter;
import top.yumbo.ai.reviewer.application.port.input.ProjectAnalysisUseCase;
import top.yumbo.ai.reviewer.application.port.input.ReportGenerationUseCase;
import top.yumbo.ai.reviewer.application.port.output.CachePort;
import top.yumbo.ai.reviewer.application.port.output.FileSystemPort;
import top.yumbo.ai.reviewer.application.port.output.RepositoryPort;
import top.yumbo.ai.reviewer.application.service.ProjectAnalysisService;
import top.yumbo.ai.reviewer.application.service.ReportGenerationService;
import top.yumbo.ai.reviewer.infrastructure.config.Configuration;
import top.yumbo.ai.reviewer.infrastructure.factory.AIServiceFactory;

import java.nio.file.Path;

/**
 * Guice 依赖注入模块
 *
 * 配置所有依赖关系和绑定
 *
 * @author AI-Reviewer Team
 * @version 2.0
 * @since 2025-11-13
 */
public class ApplicationModule extends AbstractModule {

    private final Configuration configuration;

    public ApplicationModule(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        // 绑定配置实例
        bind(Configuration.class).toInstance(configuration);

        // 绑定输入端口（Use Cases）到实现
        bind(ProjectAnalysisUseCase.class).to(ProjectAnalysisService.class).in(Singleton.class);
        bind(ReportGenerationUseCase.class).to(ReportGenerationService.class).in(Singleton.class);

        // 绑定输出端口到实现
        bind(CachePort.class).to(FileCacheAdapter.class).in(Singleton.class);
        bind(FileSystemPort.class).to(LocalFileSystemAdapter.class).in(Singleton.class);
        bind(ASTParserPort.class).to(ASTParserFactory.class).in(Singleton.class);
    }

    /**
     * 提供 AI 服务（根据配置动态创建）
     */
    @Provides
    @Singleton
    public AIServicePort provideAIService(Configuration config) {
        return AIServiceFactory.create(config.getAIServiceConfig());
    }

    /**
     * 提供文件系统适配器配置
     */
    @Provides
    @Singleton
    public LocalFileSystemAdapter.FileSystemConfig provideFileSystemConfig(Configuration config) {
        return new LocalFileSystemAdapter.FileSystemConfig(
                config.getFileSystemIncludePatterns(),
                config.getFileSystemExcludePatterns(),
                config.getMaxFileSizeKB(),
                config.getScanDepth()
        );
    }

    /**
     * 提供 LocalFileSystemAdapter
     */
    @Provides
    @Singleton
    public LocalFileSystemAdapter provideLocalFileSystemAdapter(
            LocalFileSystemAdapter.FileSystemConfig fileSystemConfig) {
        return new LocalFileSystemAdapter(fileSystemConfig);
    }

    /**
     * 提供缓存适配器
     */
    @Provides
    @Singleton
    public FileCacheAdapter provideFileCacheAdapter(Configuration config) {
        return new FileCacheAdapter();
    }

    /**
     * Repository 工厂方法（非单例，每次根据 URL 创建）
     * 注意：这个方法不能直接注入，而是通过工厂类调用
     */
    public static RepositoryPort createRepositoryPort(String url, Path workingDir) {
        if (url.contains("gitee.com")) {
            return new GiteeRepositoryAdapter(workingDir);
        } else if (url.contains("github.com")) {
            return new GitHubRepositoryAdapter(workingDir);
        } else {
            throw new IllegalArgumentException("不支持的仓库 URL: " + url);
        }
    }
}

