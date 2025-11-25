package top.yumbo.ai.application.hackathon.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class for ZIP file operations
 */
@Slf4j
public class ZipUtil {

    /**
     * Extract a ZIP file to a target directory
     * @param zipFilePath Path to the ZIP file
     * @param extractToDir Directory to extract to
     * @return Path to the extracted directory
     * @throws IOException if extraction fails
     */
    public static Path extractZip(Path zipFilePath, Path extractToDir) throws IOException {
        String projectName = getProjectNameFromZip(zipFilePath);
        Path targetDir = extractToDir.resolve(projectName);

        // If already extracted, return existing directory
        if (Files.exists(targetDir)) {
            log.debug("Project already extracted: {}", targetDir);
            return targetDir;
        }

        Files.createDirectories(targetDir);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = targetDir.resolve(entry.getName());

                // Prevent zip slip vulnerability
                if (!filePath.normalize().startsWith(targetDir.normalize())) {
                    throw new IOException("Bad zip entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    try (OutputStream os = Files.newOutputStream(filePath)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }

        log.info("Extracted ZIP: {} to {}", zipFilePath.getFileName(), targetDir);
        return targetDir;
    }

    /**
     * Get project name from ZIP filename (without .zip extension)
     */
    public static String getProjectNameFromZip(Path zipFilePath) {
        String fileName = zipFilePath.getFileName().toString();
        if (fileName.toLowerCase().endsWith(".zip")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }

    /**
     * Clean up extracted directory
     * Handles race conditions in multi-threaded environment where files/directories
     * might be deleted by other threads between exists check and delete operation
     */
    public static void cleanupExtractedDir(Path extractedDir) {
        try {
            if (Files.exists(extractedDir)) {
                Files.walk(extractedDir)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            // Don't check exists() first - just try to delete
                            // This avoids TOCTOU (Time-Of-Check-Time-Of-Use) race condition
                            Files.delete(path);
                        } catch (java.nio.file.NoSuchFileException e) {
                            // Silently ignore - file/directory already deleted
                            // This is normal in multi-threaded environments or with symlinks
                            log.trace("Path already deleted: {}", path);
                        } catch (java.nio.file.DirectoryNotEmptyException e) {
                            // Directory still has contents - will be retried in next iteration
                            log.debug("Directory not empty yet: {}", path);
                        } catch (IOException e) {
                            // Log other IO errors as warnings (e.g., permission issues)
                            log.warn("Failed to delete: {} - {}", path, e.getMessage());
                        }
                    });
            }
        } catch (IOException e) {
            log.warn("Failed to walk directory tree: {}", extractedDir, e);
        }
    }
}

