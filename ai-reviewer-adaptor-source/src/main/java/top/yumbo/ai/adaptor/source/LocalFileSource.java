package top.yumbo.ai.adaptor.source;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.api.source.FileSourceConfig;
import top.yumbo.ai.api.source.IFileSource;
import top.yumbo.ai.api.source.SourceFile;
import top.yumbo.ai.common.exception.FileSourceException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Local file system source implementation
 * <p>
 * This is the default file source that reads files from the local file system.
 * It provides backward compatibility with the original file scanning approach.
 *
 * @author AI-Reviewer Team
 * @since 1.1.0
 */
@Slf4j
public class LocalFileSource implements IFileSource {

    private Path basePath;
    private boolean initialized = false;

    public LocalFileSource() {
        // Default constructor
    }

    @Override
    public String getSourceName() {
        return "local";
    }

    @Override
    public boolean support(FileSourceConfig config) {
        return "local".equalsIgnoreCase(config.getSourceType());
    }

    @Override
    public void initialize(FileSourceConfig config) throws Exception {
        if (config.getBasePath() == null || config.getBasePath().trim().isEmpty()) {
            throw new FileSourceException("Base path is required for local file source");
        }

        this.basePath = Paths.get(config.getBasePath());

        if (!Files.exists(basePath)) {
            throw new FileSourceException("Path does not exist: " + basePath);
        }

        if (!Files.isDirectory(basePath)) {
            throw new FileSourceException("Path is not a directory: " + basePath);
        }

        if (!Files.isReadable(basePath)) {
            throw new FileSourceException("Path is not readable: " + basePath);
        }

        this.initialized = true;
        log.info("Local file source initialized: {}", basePath);
    }

    @Override
    public List<SourceFile> listFiles(String path) throws Exception {
        if (!initialized) {
            throw new FileSourceException("File source not initialized");
        }

        Path targetPath = path == null || path.trim().isEmpty()
                ? basePath
                : basePath.resolve(path);

        if (!Files.exists(targetPath)) {
            throw new FileSourceException("Path does not exist: " + targetPath);
        }

        List<SourceFile> result = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(targetPath)) {
            stream.filter(Files::isRegularFile)
                    .forEach(p -> {
                        try {
                            SourceFile sourceFile = SourceFile.builder()
                                    .fileId(p.toString())
                                    .relativePath(basePath.relativize(p).toString().replace("\\", "/"))
                                    .fileName(p.getFileName().toString())
                                    .fileSize(Files.size(p))
                                    .lastModified(LocalDateTime.ofInstant(
                                            Files.getLastModifiedTime(p).toInstant(),
                                            ZoneId.systemDefault()))
                                    .source(this)
                                    .build();

                            result.add(sourceFile);
                        } catch (IOException e) {
                            log.warn("Failed to process file: {}", p, e);
                        }
                    });
        }

        log.info("Listed {} files from local path: {}", result.size(), targetPath);
        return result;
    }

    @Override
    public InputStream readFile(SourceFile file) throws Exception {
        if (!initialized) {
            throw new FileSourceException("File source not initialized");
        }

        Path filePath = Paths.get(file.getFileId());

        if (!Files.exists(filePath)) {
            throw new FileSourceException("File does not exist: " + filePath);
        }

        if (!Files.isReadable(filePath)) {
            throw new FileSourceException("File is not readable: " + filePath);
        }

        return Files.newInputStream(filePath);
    }

    @Override
    public void close() throws Exception {
        // Local file source doesn't need cleanup
        this.initialized = false;
        log.debug("Local file source closed");
    }

    @Override
    public int getPriority() {
        return 100; // Highest priority as the default implementation
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }
}

