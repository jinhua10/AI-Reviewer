package top.yumbo.ai.reviewer.adapter.output.archive;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ZIP 压缩包处理适配器
 * 支持解压 ZIP 文件到临时目录
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-14
 */
@Slf4j
public class ZipArchiveAdapter {

    private final Path tempBaseDir;

    /**
     * 构造函数
     *
     * @param tempBaseDir 临时目录基础路径
     */
    public ZipArchiveAdapter(Path tempBaseDir) {
        this.tempBaseDir = tempBaseDir;
        try {
            if (!Files.exists(tempBaseDir)) {
                Files.createDirectories(tempBaseDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("无法创建临时目录: " + tempBaseDir, e);
        }
    }

    /**
     * 解压 ZIP 文件到临时目录
     *
     * @param zipFilePath ZIP 文件路径
     * @return 解压后的目录路径
     * @throws ZipExtractionException 解压失败时抛出
     */
    public Path extractZipFile(Path zipFilePath) throws ZipExtractionException {
        log.info("开始解压 ZIP 文件: {}", zipFilePath);

        // 验证文件存在
        if (!Files.exists(zipFilePath)) {
            throw new ZipExtractionException("ZIP 文件不存在: " + zipFilePath);
        }

        // 验证是否为 ZIP 文件
        if (!isZipFile(zipFilePath)) {
            throw new ZipExtractionException("不是有效的 ZIP 文件: " + zipFilePath);
        }

        // 创建解压目标目录
        String fileName = zipFilePath.getFileName().toString();
        String dirName = fileName.substring(0, fileName.lastIndexOf('.'));
        Path extractDir = tempBaseDir.resolve(dirName + "-" + System.currentTimeMillis());

        try {
            Files.createDirectories(extractDir);
            log.debug("创建解压目录: {}", extractDir);

            // 解压文件
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFilePath))) {
                ZipEntry entry;
                int fileCount = 0;
                long totalSize = 0;

                while ((entry = zis.getNextEntry()) != null) {
                    Path entryPath = extractDir.resolve(entry.getName());

                    // 安全检查：防止路径遍历攻击
                    if (!entryPath.normalize().startsWith(extractDir.normalize())) {
                        log.warn("跳过不安全的路径: {}", entry.getName());
                        continue;
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        // 确保父目录存在
                        Files.createDirectories(entryPath.getParent());

                        // 解压文件
                        Files.copy(zis, entryPath);
                        fileCount++;
                        totalSize += entry.getSize();

                        log.trace("解压文件: {}", entry.getName());
                    }

                    zis.closeEntry();
                }

                log.info("ZIP 文件解压完成: {} 个文件, 总大小: {} bytes", fileCount, totalSize);
                log.info("解压目录: {}", extractDir);

                return extractDir;

            }

        } catch (IOException e) {
            log.error("解压 ZIP 文件失败", e);
            // 清理已创建的目录
            cleanupDirectory(extractDir);
            throw new ZipExtractionException("解压失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解压 ZIP 文件（从字符串路径）
     *
     * @param zipFilePath ZIP 文件路径字符串
     * @return 解压后的目录路径
     * @throws ZipExtractionException 解压失败时抛出
     */
    public Path extractZipFile(String zipFilePath) throws ZipExtractionException {
        return extractZipFile(Paths.get(zipFilePath));
    }

    /**
     * 检查文件是否为 ZIP 格式
     *
     * @param filePath 文件路径
     * @return 是否为 ZIP 文件
     */
    public boolean isZipFile(Path filePath) {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return false;
        }

        // 检查文件扩展名
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".zip")) {
            return false;
        }

        // 检查文件头（ZIP 文件以 'PK' 开头）
        try {
            byte[] header = new byte[4];
            try (var in = Files.newInputStream(filePath)) {
                int bytesRead = in.read(header);
                if (bytesRead >= 2) {
                    // ZIP 文件头：50 4B (PK)
                    return header[0] == 0x50 && header[1] == 0x4B;
                }
            }
        } catch (IOException e) {
            log.warn("无法读取文件头: {}", filePath, e);
        }

        return false;
    }

    /**
     * 清理目录
     *
     * @param directory 要清理的目录
     */
    private void cleanupDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                Files.walk(directory)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("删除文件失败: {}", path, e);
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("清理目录失败: {}", directory, e);
        }
    }

    /**
     * ZIP 解压异常
     */
    public static class ZipExtractionException extends Exception {
        public ZipExtractionException(String message) {
            super(message);
        }

        public ZipExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

