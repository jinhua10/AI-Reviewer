package top.yumbo.ai.rag.example;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Excel文件夹诊断工具
 * 用于检查为什么找不到Excel文件
 */
public class ExcelFolderDiagnostic {

    public static void main(String[] args) {
        String folderPath = "E:\\excel"; // 您的路径

        if (args.length > 0) {
            folderPath = args[0];
        }

        System.out.println("=".repeat(80));
        System.out.println("Excel文件夹诊断工具");
        System.out.println("=".repeat(80));
        System.out.println("检查路径: " + folderPath);
        System.out.println();

        Path path = Paths.get(folderPath);

        // 1. 检查路径是否存在
        System.out.println("1. 路径存在性检查:");
        if (!Files.exists(path)) {
            System.out.println("   ❌ 路径不存在: " + path.toAbsolutePath());
            System.out.println("   请检查路径是否正确！");
            return;
        }
        System.out.println("   ✓ 路径存在");

        // 2. 检查是否是目录
        System.out.println("\n2. 目录检查:");
        if (!Files.isDirectory(path)) {
            System.out.println("   ❌ 不是目录: " + path.toAbsolutePath());
            return;
        }
        System.out.println("   ✓ 是有效目录");

        // 3. 检查读取权限
        System.out.println("\n3. 权限检查:");
        if (!Files.isReadable(path)) {
            System.out.println("   ❌ 没有读取权限");
            return;
        }
        System.out.println("   ✓ 有读取权限");

        // 4. 列出所有文件
        System.out.println("\n4. 文件列表:");
        File folder = path.toFile();
        File[] files = folder.listFiles();

        if (files == null || files.length == 0) {
            System.out.println("   ⚠️ 文件夹为空");
            return;
        }

        int excelCount = 0;
        int xlsCount = 0;
        int xlsxCount = 0;
        int otherCount = 0;

        System.out.println("   找到 " + files.length + " 个文件/文件夹:");
        for (File file : files) {
            if (file.isDirectory()) {
                System.out.println("   [DIR]  " + file.getName());
            } else {
                String name = file.getName();
                String nameLower = name.toLowerCase();
                long sizeKB = file.length() / 1024;

                if (nameLower.endsWith(".xls")) {
                    xlsCount++;
                    excelCount++;
                    System.out.println("   [XLS]  " + name + " (" + sizeKB + " KB)");
                } else if (nameLower.endsWith(".xlsx")) {
                    xlsxCount++;
                    excelCount++;
                    System.out.println("   [XLSX] " + name + " (" + sizeKB + " KB)");
                } else {
                    otherCount++;
                    System.out.println("   [FILE] " + name + " (" + sizeKB + " KB)");
                }
            }
        }

        // 5. 统计
        System.out.println("\n5. 统计信息:");
        System.out.println("   - 总文件数: " + files.length);
        System.out.println("   - Excel文件(.xls): " + xlsCount);
        System.out.println("   - Excel文件(.xlsx): " + xlsxCount);
        System.out.println("   - Excel总计: " + excelCount);
        System.out.println("   - 其他文件: " + otherCount);

        // 6. 结论
        System.out.println("\n6. 诊断结论:");
        if (excelCount == 0) {
            System.out.println("   ❌ 文件夹中没有Excel文件！");
            System.out.println("   请检查:");
            System.out.println("   1. 文件扩展名是否为 .xls 或 .xlsx");
            System.out.println("   2. 文件是否放在正确的文件夹");
            System.out.println("   3. 路径是否正确: " + path.toAbsolutePath());
        } else {
            System.out.println("   ✓ 找到 " + excelCount + " 个Excel文件");
            System.out.println("   如果程序仍然找不到文件，请检查:");
            System.out.println("   1. 文件名是否以 ~$ 开头（临时文件）");
            System.out.println("   2. 文件大小是否超过100MB");
            System.out.println("   3. 日志级别是否设置正确");
        }

        System.out.println("\n" + "=".repeat(80));
    }
}

