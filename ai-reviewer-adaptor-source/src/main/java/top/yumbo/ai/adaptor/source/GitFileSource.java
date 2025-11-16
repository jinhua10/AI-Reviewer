package top.yumbo.ai.adaptor.source;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import top.yumbo.ai.api.source.FileSourceConfig;
import top.yumbo.ai.api.source.IFileSource;
import top.yumbo.ai.api.source.SourceFile;
import top.yumbo.ai.common.exception.FileSourceException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Git repository file source implementation
 * <p>
 * Clones Git repositories (GitHub, GitLab, Gitee, etc.) to a temporary location
 * and provides access to files in the repository.
 * <p>
 * Supports:
 * - HTTPS and SSH URLs
 * - Branch selection
 * - Specific commit checkout
 * - Token-based authentication
 *
 * @author AI-Reviewer Team
 * @since 1.1.0
 */
@Slf4j
public class GitFileSource implements IFileSource {

    private Repository repository;
    private Path localClonePath;
    private boolean initialized = false;

    public GitFileSource() {

    }

    @Override
    public String getSourceName() {
        return "git";
    }

    @Override
    public boolean support(FileSourceConfig config) {
        return "git".equalsIgnoreCase(config.getSourceType());
    }

    @Override
    public void initialize(FileSourceConfig config) throws Exception {
        validateConfig(config);

        // Create temporary directory for clone
        localClonePath = Files.createTempDirectory("ai-reviewer-git-");
        log.info("Cloning repository: {} to {}", config.getRepositoryUrl(), localClonePath);

        // Configure clone command
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(config.getRepositoryUrl())
                .setDirectory(localClonePath.toFile());

        // Set branch if specified
        String branch = config.getBranch() != null && !config.getBranch().trim().isEmpty()
                ? config.getBranch()
                : "main"; // Default to main
        cloneCommand.setBranch(branch);

        // Configure authentication
        configureAuthentication(cloneCommand, config);

        // Execute clone
        Git git = null;
        try {
            git = cloneCommand.call();
            repository = git.getRepository();

            // Checkout specific commit if provided
            if (config.getCommitId() != null && !config.getCommitId().trim().isEmpty()) {
                log.info("Checking out commit: {}", config.getCommitId());
                git.checkout().setName(config.getCommitId()).call();
            }

            this.initialized = true;
            log.info("Repository cloned successfully: {} files", countFiles(localClonePath));
        } finally {
            if (git != null) {
                git.close();
            }
        }
    }

    private void validateConfig(FileSourceConfig config) throws FileSourceException {
        if (config.getRepositoryUrl() == null || config.getRepositoryUrl().trim().isEmpty()) {
            throw new FileSourceException("Repository URL is required for Git file source");
        }
    }

    private void configureAuthentication(CloneCommand cloneCommand, FileSourceConfig config) {
        // Access token authentication (for HTTPS)
        if (config.getAccessToken() != null && !config.getAccessToken().trim().isEmpty()) {
            cloneCommand.setCredentialsProvider(
                    new UsernamePasswordCredentialsProvider(config.getAccessToken(), ""));
            log.debug("Using token authentication");
        }
        // Username/password authentication
        else if (config.getUsername() != null && config.getPassword() != null) {
            cloneCommand.setCredentialsProvider(
                    new UsernamePasswordCredentialsProvider(
                            config.getUsername(), config.getPassword()));
            log.debug("Using username/password authentication");
        }
        // SSH authentication is handled by JGit automatically via SSH config
    }

    private long countFiles(Path path) throws IOException {
        try (Stream<Path> stream = Files.walk(path)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> !p.toString().contains(".git"))
                    .count();
        }
    }

    @Override
    public List<SourceFile> listFiles(String path) throws Exception {
        if (!initialized) {
            throw new FileSourceException("File source not initialized");
        }

        Path basePath = path == null || path.trim().isEmpty()
                ? localClonePath
                : localClonePath.resolve(path);

        if (!Files.exists(basePath)) {
            throw new FileSourceException("Path does not exist: " + path);
        }

        List<SourceFile> result = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(basePath)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> !p.toString().contains(".git")) // Exclude .git directory
                    .forEach(p -> {
                        try {
                            SourceFile sourceFile = SourceFile.builder()
                                    .fileId(p.toString())
                                    .relativePath(localClonePath.relativize(p).toString().replace("\\", "/"))
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

        log.info("Listed {} files from Git repository path: {}", result.size(), path);
        return result;
    }

    @Override
    public InputStream readFile(SourceFile file) throws Exception {
        if (!initialized) {
            throw new FileSourceException("File source not initialized");
        }

        Path filePath = Path.of(file.getFileId());

        if (!Files.exists(filePath)) {
            throw new FileSourceException("File does not exist: " + file.getFileId());
        }

        return Files.newInputStream(filePath);
    }

    @Override
    public void close() throws Exception {
        if (repository != null) {
            repository.close();
            log.debug("Git repository closed");
        }

        // Clean up temporary clone directory
        if (localClonePath != null && Files.exists(localClonePath)) {
            deleteDirectory(localClonePath.toFile());
            log.info("Cleaned up temporary git clone: {}", localClonePath);
        }

        this.initialized = false;
    }

    /**
     * Recursively delete directory
     */
    private void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        if (!directory.delete()) {
            log.warn("Failed to delete: {}", directory.getAbsolutePath());
        }
    }

    @Override
    public int getPriority() {
        return 60; // Higher priority than SFTP
    }

    @Override
    public boolean isInitialized() {
        return initialized && repository != null;
    }
}

