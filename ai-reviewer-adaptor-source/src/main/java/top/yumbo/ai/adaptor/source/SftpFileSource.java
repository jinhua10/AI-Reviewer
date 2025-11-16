package top.yumbo.ai.adaptor.source;

import com.jcraft.jsch.*;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.api.source.FileSourceConfig;
import top.yumbo.ai.api.source.IFileSource;
import top.yumbo.ai.api.source.SourceFile;
import top.yumbo.ai.common.exception.FileSourceException;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * SFTP file source implementation
 *
 * Connects to SFTP servers to retrieve files for analysis.
 * Supports password and private key authentication.
 *
 * @author AI-Reviewer Team
 * @since 1.1.0
 */
@Slf4j
@NoArgsConstructor
public class SftpFileSource implements IFileSource {

    private Session session;
    private ChannelSftp sftpChannel;
    private String basePath;
    private boolean initialized = false;

    @Override
    public String getSourceName() {
        return "sftp";
    }

    @Override
    public boolean support(FileSourceConfig config) {
        return "sftp".equalsIgnoreCase(config.getSourceType());
    }

    @Override
    public void initialize(FileSourceConfig config) throws Exception {
        validateConfig(config);

        JSch jsch = new JSch();

        // Configure private key if provided
        if (config.getPrivateKeyPath() != null && !config.getPrivateKeyPath().trim().isEmpty()) {
            if (config.getPrivateKeyPassphrase() != null) {
                jsch.addIdentity(config.getPrivateKeyPath(), config.getPrivateKeyPassphrase());
            } else {
                jsch.addIdentity(config.getPrivateKeyPath());
            }
            log.debug("Private key configured: {}", config.getPrivateKeyPath());
        }

        // Create session
        int port = config.getPort() != null ? config.getPort() : 22;
        session = jsch.getSession(config.getUsername(), config.getHost(), port);

        if (config.getPassword() != null && !config.getPassword().trim().isEmpty()) {
            session.setPassword(config.getPassword());
        }

        // Configure session properties
        session.setConfig("StrictHostKeyChecking", "no"); // In production, should verify host key
        session.setTimeout(config.getConnectionTimeout());

        // Connect
        log.info("Connecting to SFTP server: {}@{}:{}", config.getUsername(), config.getHost(), port);
        session.connect();

        // Open SFTP channel
        Channel channel = session.openChannel("sftp");
        channel.connect();
        sftpChannel = (ChannelSftp) channel;

        this.basePath = config.getBasePath() != null ? config.getBasePath() : "/";
        this.initialized = true;

        log.info("SFTP connection established: {}@{}", config.getUsername(), config.getHost());
    }

    private void validateConfig(FileSourceConfig config) throws FileSourceException {
        if (config.getHost() == null || config.getHost().trim().isEmpty()) {
            throw new FileSourceException("Host is required for SFTP file source");
        }
        if (config.getUsername() == null || config.getUsername().trim().isEmpty()) {
            throw new FileSourceException("Username is required for SFTP file source");
        }
        // Either password or private key must be provided
        boolean hasPassword = config.getPassword() != null && !config.getPassword().trim().isEmpty();
        boolean hasPrivateKey = config.getPrivateKeyPath() != null && !config.getPrivateKeyPath().trim().isEmpty();
        if (!hasPassword && !hasPrivateKey) {
            throw new FileSourceException("Either password or private key is required for SFTP authentication");
        }
    }

    @Override
    public List<SourceFile> listFiles(String path) throws Exception {
        if (!initialized) {
            throw new FileSourceException("File source not initialized");
        }

        String fullPath = path == null || path.trim().isEmpty()
            ? basePath
            : (basePath + "/" + path).replace("//", "/");

        List<SourceFile> result = new ArrayList<>();
        listFilesRecursive(fullPath, "", result);

        log.info("Listed {} files from SFTP path: {}", result.size(), fullPath);
        return result;
    }

    private void listFilesRecursive(String currentPath, String relativePath, List<SourceFile> result) {
        try {
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> entries = sftpChannel.ls(currentPath);

            for (ChannelSftp.LsEntry entry : entries) {
                String filename = entry.getFilename();

                // Skip . and ..
                if (".".equals(filename) || "..".equals(filename)) {
                    continue;
                }

                SftpATTRS attrs = entry.getAttrs();
                String entryPath = (currentPath + "/" + filename).replace("//", "/");
                String entryRelative = relativePath.isEmpty()
                    ? filename
                    : (relativePath + "/" + filename).replace("//", "/");

                if (attrs.isDir()) {
                    // Recursively list subdirectory
                    listFilesRecursive(entryPath, entryRelative, result);
                } else {
                    SourceFile sourceFile = SourceFile.builder()
                        .fileId(entryPath)
                        .relativePath(entryRelative)
                        .fileName(filename)
                        .fileSize(attrs.getSize())
                        .lastModified(LocalDateTime.ofEpochSecond(
                            attrs.getMTime(), 0, ZoneOffset.UTC))
                        .source(this)
                        .build();

                    result.add(sourceFile);
                }
            }
        } catch (SftpException e) {
            log.warn("Failed to list directory: {}", currentPath, e);
        }
    }

    @Override
    public InputStream readFile(SourceFile file) throws Exception {
        if (!initialized) {
            throw new FileSourceException("File source not initialized");
        }

        try {
            return sftpChannel.get(file.getFileId());
        } catch (SftpException e) {
            throw new FileSourceException("Failed to read file: " + file.getFileId(), e);
        }
    }

    @Override
    public void close() throws Exception {
        if (sftpChannel != null && sftpChannel.isConnected()) {
            sftpChannel.disconnect();
            log.debug("SFTP channel disconnected");
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
            log.debug("SFTP session disconnected");
        }
        this.initialized = false;
        log.info("SFTP connection closed");
    }

    @Override
    public int getPriority() {
        return 50; // Medium priority
    }

    @Override
    public boolean isInitialized() {
        return initialized && sftpChannel != null && sftpChannel.isConnected();
    }
}
