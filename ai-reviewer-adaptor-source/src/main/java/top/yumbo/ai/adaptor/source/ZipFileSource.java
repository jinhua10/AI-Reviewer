package top.yumbo.ai.adaptor.source;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.api.source.FileSourceConfig;
import top.yumbo.ai.api.source.IFileSource;
import top.yumbo.ai.api.source.SourceFile;
import top.yumbo.ai.common.exception.FileSourceException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ZIP archive file source implementation
 * <p>
 * Provides access to files within ZIP archives for code review.
 * Supports nested directories within the ZIP file.
 * <p>
 * Features:
 * - Read files from local ZIP archives
 * - Support for nested directory structures
 * - Automatic extraction to memory
 * - Filter by path patterns within the archive
 *
 * @author AI-Reviewer Team
 * @since 1.1.0
 */
@Slf4j
public class ZipFileSource implements IFileSource {

    private ZipFile zipFile;
    private Path zipFilePath;
    private String basePath;
    private Map<String, byte[]> fileCache;
    private boolean initialized = false;

    public ZipFileSource() {
        // Default constructor
    }

    @Override
    public String getSourceName() {
        return "zip";
    }

    @Override
    public boolean support(FileSourceConfig config) {
        return "zip".equalsIgnoreCase(config.getSourceType());
    }

    @Override
    public void initialize(FileSourceConfig config) throws Exception {
        validateConfig(config);

        this.zipFilePath = Paths.get(config.getBasePath());

        if (!Files.exists(zipFilePath)) {
            throw new FileSourceException("ZIP file does not exist: " + zipFilePath);
        }

        if (!Files.isRegularFile(zipFilePath)) {
            throw new FileSourceException("Path is not a regular file: " + zipFilePath);
        }

        if (!Files.isReadable(zipFilePath)) {
            throw new FileSourceException("ZIP file is not readable: " + zipFilePath);
        }

        // Verify it's a valid ZIP file
        try {
            this.zipFile = new ZipFile(zipFilePath.toFile());
        } catch (IOException e) {
            throw new FileSourceException("Invalid ZIP file: " + zipFilePath, e);
        }

        // Get base path within the ZIP (optional)
        Object customBasePath = config.getCustomParam("zipBasePath");
        this.basePath = customBasePath != null ? customBasePath.toString() : "";
        if (this.basePath.startsWith("/")) {
            this.basePath = this.basePath.substring(1);
        }
        if (this.basePath.endsWith("/") && !this.basePath.isEmpty()) {
            this.basePath = this.basePath.substring(0, this.basePath.length() - 1);
        }

        this.fileCache = new HashMap<>();
        this.initialized = true;

        log.info("ZIP file source initialized: {} (entries: {})",
                zipFilePath, zipFile.size());
    }

    private void validateConfig(FileSourceConfig config) throws FileSourceException {
        if (config.getBasePath() == null || config.getBasePath().trim().isEmpty()) {
            throw new FileSourceException("ZIP file path is required (use basePath)");
        }

        String path = config.getBasePath().toLowerCase();
        if (!path.endsWith(".zip") && !path.endsWith(".jar") && !path.endsWith(".war")) {
            throw new FileSourceException("File must be a ZIP archive (.zip, .jar, or .war)");
        }
    }

    @Override
    public List<SourceFile> listFiles(String path) throws Exception {
        if (!initialized) {
            throw new FileSourceException("File source not initialized");
        }

        // Construct full path within ZIP
        String searchPath = path == null || path.trim().isEmpty()
                ? basePath
                : (basePath.isEmpty() ? path : basePath + "/" + path);

        // Normalize path
        searchPath = searchPath.replace("\\", "/");
        if (searchPath.startsWith("/")) {
            searchPath = searchPath.substring(1);
        }

        List<SourceFile> result = new ArrayList<>();

        // Iterate through all entries in the ZIP file
        var entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            // Skip directories
            if (entry.isDirectory()) {
                continue;
            }

            String entryName = entry.getName().replace("\\", "/");

            // Filter by base path if specified
            if (!searchPath.isEmpty()) {
                if (!entryName.startsWith(searchPath + "/") && !entryName.equals(searchPath)) {
                    continue;
                }
            }

            // Calculate relative path
            String relativePath = basePath.isEmpty()
                    ? entryName
                    : entryName.substring(basePath.length() + 1);

            // Extract file name
            String fileName = entryName.substring(entryName.lastIndexOf('/') + 1);

            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("zipFile", zipFilePath.toString());
            metadata.put("entryName", entry.getName());
            metadata.put("compressed", entry.getCompressedSize());
            metadata.put("method", getCompressionMethod(entry.getMethod()));
            metadata.put("crc", entry.getCrc());

            SourceFile sourceFile = SourceFile.builder()
                    .fileId(entry.getName()) // Use entry name as ID
                    .relativePath(relativePath)
                    .fileName(fileName)
                    .fileSize(entry.getSize())
                    .lastModified(LocalDateTime.ofInstant(
                            entry.getLastModifiedTime().toInstant(),
                            ZoneId.systemDefault()))
                    .metadata(metadata)
                    .source(this)
                    .build();

            result.add(sourceFile);
        }

        log.info("Listed {} files from ZIP archive: {}", result.size(), zipFilePath);
        return result;
    }

    private String getCompressionMethod(int method) {
        return switch (method) {
            case ZipEntry.STORED -> "STORED";
            case ZipEntry.DEFLATED -> "DEFLATED";
            default -> "UNKNOWN(" + method + ")";
        };
    }

    @Override
    public InputStream readFile(SourceFile file) throws Exception {
        if (!initialized) {
            throw new FileSourceException("File source not initialized");
        }

        String entryName = file.getFileId();

        // Check cache first
        if (fileCache.containsKey(entryName)) {
            return new ByteArrayInputStream(fileCache.get(entryName));
        }

        // Get entry from ZIP
        ZipEntry entry = zipFile.getEntry(entryName);
        if (entry == null) {
            throw new FileSourceException("Entry not found in ZIP: " + entryName);
        }

        // Read the entry
        try (InputStream is = zipFile.getInputStream(entry)) {
            byte[] data = is.readAllBytes();

            // Cache for subsequent reads
            fileCache.put(entryName, data);

            return new ByteArrayInputStream(data);
        } catch (IOException e) {
            throw new FileSourceException("Failed to read ZIP entry: " + entryName, e);
        }
    }

    @Override
    public void close() throws Exception {
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (IOException e) {
                log.warn("Failed to close ZIP file", e);
            }
        }

        if (fileCache != null) {
            fileCache.clear();
        }

        this.initialized = false;
        log.info("ZIP file source closed: {}", zipFilePath);
    }

    @Override
    public int getPriority() {
        return 70; // Higher than SFTP and S3, lower than Local and Git
    }

    @Override
    public boolean isInitialized() {
        return initialized && zipFile != null;
    }

    /**
     * Get the ZIP file path
     */
    public Path getZipFilePath() {
        return zipFilePath;
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cachedFiles", fileCache.size());
        stats.put("totalEntries", zipFile != null ? zipFile.size() : 0);
        return stats;
    }

    /**
     * Clear the file cache
     */
    public void clearCache() {
        if (fileCache != null) {
            fileCache.clear();
            log.debug("ZIP file cache cleared");
        }
    }
}

