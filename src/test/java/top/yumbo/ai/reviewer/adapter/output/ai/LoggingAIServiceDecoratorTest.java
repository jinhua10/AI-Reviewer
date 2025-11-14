package top.yumbo.ai.reviewer.adapter.output.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import top.yumbo.ai.reviewer.application.port.output.AIServicePort;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LoggingAIServiceDecorator 单元测试
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-14
 */
class LoggingAIServiceDecoratorTest {

    private AIServicePort mockService;
    private LoggingAIServiceDecorator decorator;

    @BeforeEach
    void setUp() {
        mockService = Mockito.mock(AIServicePort.class);
        when(mockService.getProviderName()).thenReturn("TestProvider");
        decorator = new LoggingAIServiceDecorator(mockService);
    }

    @Test
    void testAnalyzeSuccess() {
        // Arrange
        String prompt = "测试提示词";
        String expectedResult = "测试结果";
        when(mockService.analyze(prompt)).thenReturn(expectedResult);

        // Act
        String result = decorator.analyze(prompt);

        // Assert
        assertEquals(expectedResult, result);
        verify(mockService, times(1)).analyze(prompt);
    }

    @Test
    void testAnalyzeException() {
        // Arrange
        String prompt = "测试提示词";
        RuntimeException exception = new RuntimeException("测试异常");
        when(mockService.analyze(prompt)).thenThrow(exception);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> decorator.analyze(prompt));
        verify(mockService, times(1)).analyze(prompt);
    }

    @Test
    void testAnalyzeAsyncSuccess() throws ExecutionException, InterruptedException {
        // Arrange
        String prompt = "测试提示词";
        String expectedResult = "异步测试结果";
        CompletableFuture<String> future = CompletableFuture.completedFuture(expectedResult);
        when(mockService.analyzeAsync(prompt)).thenReturn(future);

        // Act
        CompletableFuture<String> result = decorator.analyzeAsync(prompt);

        // Assert
        assertEquals(expectedResult, result.get());
        verify(mockService, times(1)).analyzeAsync(prompt);
    }

    @Test
    void testAnalyzeAsyncException() {
        // Arrange
        String prompt = "测试提示词";
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("异步测试异常"));
        when(mockService.analyzeAsync(prompt)).thenReturn(future);

        // Act
        CompletableFuture<String> result = decorator.analyzeAsync(prompt);

        // Assert
        assertTrue(result.isCompletedExceptionally());
        verify(mockService, times(1)).analyzeAsync(prompt);
    }

    @Test
    void testAnalyzeBatchAsyncSuccess() throws ExecutionException, InterruptedException {
        // Arrange
        String[] prompts = {"提示词1", "提示词2", "提示词3"};
        String[] expectedResults = {"结果1", "结果2", "结果3"};
        CompletableFuture<String[]> future = CompletableFuture.completedFuture(expectedResults);
        when(mockService.analyzeBatchAsync(prompts)).thenReturn(future);

        // Act
        CompletableFuture<String[]> result = decorator.analyzeBatchAsync(prompts);

        // Assert
        assertArrayEquals(expectedResults, result.get());
        verify(mockService, times(1)).analyzeBatchAsync(prompts);
    }

    @Test
    void testAnalyzeBatchAsyncException() {
        // Arrange
        String[] prompts = {"提示词1", "提示词2"};
        CompletableFuture<String[]> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("批量异步测试异常"));
        when(mockService.analyzeBatchAsync(prompts)).thenReturn(future);

        // Act
        CompletableFuture<String[]> result = decorator.analyzeBatchAsync(prompts);

        // Assert
        assertTrue(result.isCompletedExceptionally());
        verify(mockService, times(1)).analyzeBatchAsync(prompts);
    }

    @Test
    void testGetProviderName() {
        // Act
        String providerName = decorator.getProviderName();

        // Assert
        assertEquals("TestProvider", providerName);
        verify(mockService, times(2)).getProviderName(); // 一次在构造函数，一次在测试方法
    }

    @Test
    void testIsAvailable() {
        // Arrange
        when(mockService.isAvailable()).thenReturn(true);

        // Act
        boolean available = decorator.isAvailable();

        // Assert
        assertTrue(available);
        verify(mockService, times(1)).isAvailable();
    }

    @Test
    void testGetMaxConcurrency() {
        // Arrange
        when(mockService.getMaxConcurrency()).thenReturn(5);

        // Act
        int maxConcurrency = decorator.getMaxConcurrency();

        // Assert
        assertEquals(5, maxConcurrency);
        verify(mockService, times(1)).getMaxConcurrency();
    }

    @Test
    void testShutdown() {
        // Act
        decorator.shutdown();

        // Assert
        verify(mockService, times(1)).shutdown();
    }

    @Test
    void testShutdownException() {
        // Arrange
        RuntimeException exception = new RuntimeException("关闭异常");
        doThrow(exception).when(mockService).shutdown();

        // Act & Assert
        assertThrows(RuntimeException.class, () -> decorator.shutdown());
        verify(mockService, times(1)).shutdown();
    }

    @Test
    void testTruncateLongPrompt() {
        // Arrange
        String longPrompt = "a".repeat(1000);
        String expectedResult = "测试结果";
        when(mockService.analyze(any())).thenReturn(expectedResult);

        // Act
        String result = decorator.analyze(longPrompt);

        // Assert
        assertEquals(expectedResult, result);
        verify(mockService, times(1)).analyze(longPrompt);
    }

    @Test
    void testTruncateLongResult() {
        // Arrange
        String prompt = "测试提示词";
        String longResult = "b".repeat(2000);
        when(mockService.analyze(prompt)).thenReturn(longResult);

        // Act
        String result = decorator.analyze(prompt);

        // Assert
        assertEquals(longResult, result);
        verify(mockService, times(1)).analyze(prompt);
    }
}

