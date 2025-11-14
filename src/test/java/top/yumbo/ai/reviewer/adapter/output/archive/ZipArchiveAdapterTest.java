package top.yumbo.ai.reviewer.adapter.output.archive;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZIP 压缩包适配器测试
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-14
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZipArchiveAdapterTest {

    private ZipArchiveAdapter zipAdapter;
    private Path tempDir;
    private Path testZipFile;

    @BeforeAll
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("zip-adapter-test-");
        zipAdapter = new ZipArchiveAdapter(tempDir);
        log.info("测试临时目录: {}", tempDir);
    }

    @AfterAll
    void tearDown() throws IOException {
        // 清理测试文件
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除文件失败: {}", path, e);
                        }
                    });
        }
    }

    @BeforeEach
    void setUpEach() throws IOException {
        // 创建测试 ZIP 文件
        testZipFile = tempDir.resolve("test-project.zip");
        createTestZipFile(testZipFile);
    }

    @AfterEach
    void tearDownEach() throws IOException {
        // 清理测试 ZIP 文件
        if (Files.exists(testZipFile)) {
            Files.delete(testZipFile);
        }
    }

    @Test
    @DisplayName("测试解压 ZIP 文件")
    void testExtractZipFile() throws ZipArchiveAdapter.ZipExtractionException {
        Path extractedDir = zipAdapter.extractZipFile(testZipFile);

        assertNotNull(extractedDir);
        assertTrue(Files.exists(extractedDir));
        assertTrue(Files.isDirectory(extractedDir));

        // 验证文件已解压
        assertTrue(Files.exists(extractedDir.resolve("README.md")));
        assertTrue(Files.exists(extractedDir.resolve("src/Main.java")));

        log.info("解压目录: {}", extractedDir);
    }

    @Test
    @DisplayName("测试解压字符串路径")
    void testExtractZipFileFromString() throws ZipArchiveAdapter.ZipExtractionException {
        String zipPathStr = testZipFile.toString();
        Path extractedDir = zipAdapter.extractZipFile(zipPathStr);

        assertNotNull(extractedDir);
        assertTrue(Files.exists(extractedDir));
        assertTrue(Files.isDirectory(extractedDir));
    }

    @Test
    @DisplayName("测试检查 ZIP 文件格式")
    void testIsZipFile() {
        assertTrue(zipAdapter.isZipFile(testZipFile));
    }

    @Test
    @DisplayName("测试非 ZIP 文件检测")
    void testIsNotZipFile() throws IOException {
        Path textFile = tempDir.resolve("test.txt");
        Files.writeString(textFile, "This is not a zip file");

        assertFalse(zipAdapter.isZipFile(textFile));

        Files.delete(textFile);
    }

    @Test
    @DisplayName("测试 ZIP 文件不存在")
    void testZipFileNotExists() {
        Path nonExistentZip = Paths.get("non-existent.zip");

        assertThrows(ZipArchiveAdapter.ZipExtractionException.class, () -> {
            zipAdapter.extractZipFile(nonExistentZip);
        });
    }

    @Test
    @DisplayName("测试解压嵌套目录")
    void testExtractNestedZipFile() throws IOException, ZipArchiveAdapter.ZipExtractionException {
        // 创建嵌套目录的 ZIP
        Path nestedZip = tempDir.resolve("nested-project.zip");
        createNestedZipFile(nestedZip);

        Path extractedDir = zipAdapter.extractZipFile(nestedZip);

        assertTrue(Files.exists(extractedDir));
        assertTrue(Files.exists(extractedDir.resolve("project/README.md")));
        assertTrue(Files.exists(extractedDir.resolve("project/src/Main.java")));

        Files.delete(nestedZip);
    }

    @Test
    @DisplayName("测试解压空 ZIP 文件")
    void testExtractEmptyZipFile() throws IOException, ZipArchiveAdapter.ZipExtractionException {
        Path emptyZip = tempDir.resolve("empty.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(emptyZip))) {
            // 创建空 ZIP 文件
        }

        Path extractedDir = zipAdapter.extractZipFile(emptyZip);

        assertNotNull(extractedDir);
        assertTrue(Files.exists(extractedDir));

        Files.delete(emptyZip);
    }

    /**
     * 创建测试 ZIP 文件
     */
    private void createTestZipFile(Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            // 添加 README.md
            ZipEntry readmeEntry = new ZipEntry("README.md");
            zos.putNextEntry(readmeEntry);
            zos.write("# Test Project\n\nThis is a test project.".getBytes());
            zos.closeEntry();

            // 添加目录
            ZipEntry srcDirEntry = new ZipEntry("src/");
            zos.putNextEntry(srcDirEntry);
            zos.closeEntry();

            // 添加 Java 文件
            ZipEntry javaFileEntry = new ZipEntry("src/Main.java");
            zos.putNextEntry(javaFileEntry);
            zos.write("public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World\");\n    }\n}".getBytes());
            zos.closeEntry();

            // 添加 pom.xml
            ZipEntry pomEntry = new ZipEntry("pom.xml");
            zos.putNextEntry(pomEntry);
            zos.write("<project></project>".getBytes());
            zos.closeEntry();
        }

        log.info("创建测试 ZIP 文件: {}", zipFile);
    }

    /**
     * 创建嵌套目录的 ZIP 文件
     */
    private void createNestedZipFile(Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            // 添加根目录
            ZipEntry projectDirEntry = new ZipEntry("project/");
            zos.putNextEntry(projectDirEntry);
            zos.closeEntry();

            // 添加 README.md
            ZipEntry readmeEntry = new ZipEntry("project/README.md");
            zos.putNextEntry(readmeEntry);
            zos.write("# Nested Project".getBytes());
            zos.closeEntry();

            // 添加 src 目录
            ZipEntry srcDirEntry = new ZipEntry("project/src/");
            zos.putNextEntry(srcDirEntry);
            zos.closeEntry();

            // 添加 Java 文件
            ZipEntry javaFileEntry = new ZipEntry("project/src/Main.java");
            zos.putNextEntry(javaFileEntry);
            zos.write("public class Main {}".getBytes());
            zos.closeEntry();
        }

        log.info("创建嵌套 ZIP 文件: {}", zipFile);
    }
}

