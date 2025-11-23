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
     */
    public static void cleanupExtractedDir(Path extractedDir) {
        try {
            if (Files.exists(extractedDir)) {
                Files.walk(extractedDir)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete: {}", path, e);
                        }
                    });
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup directory: {}", extractedDir, e);
        }
    }
}

