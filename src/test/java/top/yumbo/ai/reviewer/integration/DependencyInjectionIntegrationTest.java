package top.yumbo.ai.reviewer.integration;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.*;
import top.yumbo.ai.reviewer.application.port.input.ProjectAnalysisUseCase;
import top.yumbo.ai.reviewer.application.port.input.ReportGenerationUseCase;
import top.yumbo.ai.reviewer.application.port.output.AIServicePort;
import top.yumbo.ai.reviewer.infrastructure.config.Configuration;
import top.yumbo.ai.reviewer.infrastructure.config.ConfigurationLoader;
import top.yumbo.ai.reviewer.infrastructure.di.ApplicationModule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 依赖注入集成测试
 *
 * 验证完整的依赖注入流程
 *
 * @author AI-Reviewer Team
 * @version 2.0
 * @since 2025-11-13
 */
@DisplayName("依赖注入集成测试")
class DependencyInjectionIntegrationTest {

    private Injector injector;
    private Configuration configuration;

    @BeforeEach
    void setUp() {
        // 设置测试配置
        System.setProperty("ai.provider", "deepseek");
        System.setProperty("ai.apiKey", "test-integration-key");
        System.setProperty("ai.model", "deepseek-chat");

        // 加载配置
        configuration = ConfigurationLoader.load();

        // 创建依赖注入容器
        injector = Guice.createInjector(new ApplicationModule(configuration));
    }

    @AfterEach
    void tearDown() {
        // 清理系统属性
        System.clearProperty("ai.provider");
        System.clearProperty("ai.apiKey");
        System.clearProperty("ai.model");

        // 关闭 AI 服务
        if (injector != null) {
            AIServicePort aiService = injector.getInstance(AIServicePort.class);
            if (aiService != null) {
                aiService.shutdown();
            }
        }
    }

    @Test
    @DisplayName("应该成功创建依赖注入容器")
    void shouldCreateInjector() {
        assertThat(injector).isNotNull();
    }

    @Test
    @DisplayName("应该注入 ProjectAnalysisUseCase")
    void shouldInjectProjectAnalysisUseCase() {
        ProjectAnalysisUseCase useCase = injector.getInstance(ProjectAnalysisUseCase.class);

        assertThat(useCase).isNotNull();
    }

    @Test
    @DisplayName("应该注入 ReportGenerationUseCase")
    void shouldInjectReportGenerationUseCase() {
        ReportGenerationUseCase useCase = injector.getInstance(ReportGenerationUseCase.class);

        assertThat(useCase).isNotNull();
    }

    @Test
    @DisplayName("应该注入 AIServicePort")
    void shouldInjectAIServicePort() {
        AIServicePort aiService = injector.getInstance(AIServicePort.class);

        assertThat(aiService).isNotNull();
        assertThat(aiService.getProviderName()).isNotNull();
    }

    @Test
    @DisplayName("应该注入相同的单例实例")
    void shouldInjectSingletonInstances() {
        ProjectAnalysisUseCase useCase1 = injector.getInstance(ProjectAnalysisUseCase.class);
        ProjectAnalysisUseCase useCase2 = injector.getInstance(ProjectAnalysisUseCase.class);

        assertThat(useCase1).isSameAs(useCase2);
    }

    @Test
    @DisplayName("应该根据配置创建正确的 AI 服务")
    void shouldCreateCorrectAIService() {
        AIServicePort aiService = injector.getInstance(AIServicePort.class);

        assertThat(aiService.getProviderName()).isEqualTo("DeepSeek");
    }

    @Test
    @DisplayName("应该支持切换 AI 服务提供商")
    void shouldSupportSwitchingAIProvider() {
        // 切换到 OpenAI
        System.setProperty("ai.provider", "openai");
        System.setProperty("ai.apiKey", "test-openai-key");

        Configuration newConfig = ConfigurationLoader.load();
        Injector newInjector = Guice.createInjector(new ApplicationModule(newConfig));

        AIServicePort aiService = newInjector.getInstance(AIServicePort.class);

        assertThat(aiService.getProviderName()).isEqualTo("OpenAI");

        aiService.shutdown();
    }

    @Test
    @DisplayName("应该正确传递配置参数")
    void shouldPassConfigurationCorrectly() {
        assertThat(configuration.getAiProvider()).isEqualTo("deepseek");
        assertThat(configuration.getAiApiKey()).isEqualTo("test-integration-key");
        assertThat(configuration.getAiModel()).isEqualTo("deepseek-chat");
    }

    @Test
    @DisplayName("应该支持配置优先级覆盖")
    void shouldRespectConfigurationPriority() {
        // 系统属性应该覆盖默认值
        System.setProperty("ai.maxTokens", "8000");

        Configuration newConfig = ConfigurationLoader.load();

        assertThat(newConfig.getAiMaxTokens()).isEqualTo(8000);

        System.clearProperty("ai.maxTokens");
    }

    @Test
    @DisplayName("应该创建完整的依赖图")
    void shouldCreateCompleteDependencyGraph() {
        ProjectAnalysisUseCase analysisUseCase = injector.getInstance(ProjectAnalysisUseCase.class);
        ReportGenerationUseCase reportUseCase = injector.getInstance(ReportGenerationUseCase.class);
        AIServicePort aiService = injector.getInstance(AIServicePort.class);

        assertThat(analysisUseCase).isNotNull();
        assertThat(reportUseCase).isNotNull();
        assertThat(aiService).isNotNull();

        // 验证所有依赖都已正确注入
        assertThat(analysisUseCase).isInstanceOf(ProjectAnalysisUseCase.class);
        assertThat(reportUseCase).isInstanceOf(ReportGenerationUseCase.class);
    }
}

