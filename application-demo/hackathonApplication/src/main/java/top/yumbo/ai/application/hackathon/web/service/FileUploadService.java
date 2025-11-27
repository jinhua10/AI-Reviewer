package top.yumbo.ai.application.hackathon.web.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 文件上传服务
 */
@Slf4j
@Service
public class FileUploadService {

    private String projectRootPath;

    public void setProjectRootPath(String projectRootPath) {
        this.projectRootPath = projectRootPath;
    }

    /**
     * 上传ZIP文件到团队文件夹
     */
    public void uploadZipFile(String teamId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("上传的文件为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".zip")) {
            throw new IOException("只允许上传ZIP文件");
        }

        // 创建团队目录(如果不存在)
        Path teamDir = Paths.get(projectRootPath, teamId);
        if (!Files.exists(teamDir)) {
            Files.createDirectories(teamDir);
            log.info("创建团队目录: {}", teamDir);
        }

        // 保存上传的文件
        Path targetPath = teamDir.resolve(fileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("文件上传成功: {} (大小: {} bytes)", targetPath, file.getSize());
    }

    /**
     * 在团队文件夹中创建done.txt文件
     */
    public void createDoneFile(String teamId) throws IOException {
        Path teamDir = Paths.get(projectRootPath, teamId);

        if (!Files.exists(teamDir)) {
            throw new IOException("团队文件夹不存在: " + teamDir);
        }

        // 检查是否至少有一个zip文件
        long zipCount = Files.list(teamDir)
                .filter(p -> p.toString().toLowerCase().endsWith(".zip"))
                .count();

        if (zipCount == 0) {
            throw new IOException("团队文件夹中没有找到ZIP文件。请先上传至少一个ZIP文件。");
        }

        Path doneFile = teamDir.resolve("done.txt");
        if (Files.exists(doneFile)) {
            log.warn("done.txt已存在，团队: {}", teamId);
            return;
        }

        Files.createFile(doneFile);
        log.info("为团队{}创建done.txt", teamId);
    }

    /**
     * 检查done.txt是否存在
     */
    public boolean hasDoneFile(String teamId) {
        Path doneFile = Paths.get(projectRootPath, teamId, "done.txt");
        return Files.exists(doneFile);
    }

    /**
     * 列出已上传的zip文件
     */
    public String[] listZipFiles(String teamId) throws IOException {
        Path teamDir = Paths.get(projectRootPath, teamId);

        if (!Files.exists(teamDir)) {
            return new String[0];
        }

        return Files.list(teamDir)
                .filter(p -> p.toString().toLowerCase().endsWith(".zip"))
                .map(p -> p.getFileName().toString())
                .toArray(String[]::new);
    }

    /**
     * 删除指定的zip文件
     */
    public void deleteZipFile(String teamId, String fileName) throws IOException {
        // 验证文件名
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IOException("文件名不能为空");
        }

        if (!fileName.toLowerCase().endsWith(".zip")) {
            throw new IOException("只能删除ZIP文件");
        }

        // 安全检查：防止路径遍历攻击
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IOException("非法的文件名");
        }

        Path teamDir = Paths.get(projectRootPath, teamId);
        Path filePath = teamDir.resolve(fileName);

        // 检查文件是否存在
        if (!Files.exists(filePath)) {
            throw new IOException("文件不存在: " + fileName);
        }

        // 检查文件是否在团队目录内（安全检查）
        if (!filePath.normalize().startsWith(teamDir.normalize())) {
            throw new IOException("非法的文件路径");
        }

        // 删除文件
        Files.delete(filePath);
        log.info("文件删除成功: {}", filePath);
    }
}

