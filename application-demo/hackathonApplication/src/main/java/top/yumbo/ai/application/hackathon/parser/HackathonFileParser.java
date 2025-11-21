package top.yumbo.ai.application.hackathon.parser;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import top.yumbo.ai.api.model.FileMetadata;
import top.yumbo.ai.api.model.PreProcessedData;
import top.yumbo.ai.api.parser.IFileParser;
import top.yumbo.ai.common.constants.Constants;
import top.yumbo.ai.common.exception.ParseException;
import top.yumbo.ai.common.util.FileUtil;

import java.io.File;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Hackathon file parser.
 */
@Slf4j
public class HackathonFileParser implements IFileParser {
    @Override
    public boolean support(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return Stream.of(
                Constants.JAVA_FILE_EXTENSION,
                Constants.JS_FILE_EXTENSION,
                Constants.PYTHON_FILE_EXTENSION,
                Constants.JSX_FILE_EXTENSION,
                Constants.TS_FILE_EXTENSION,
                Constants.TSX_FILE_EXTENSION,
                Constants.MJS_FILE_EXTENSION,
                Constants.CJS_FILE_EXTENSION,
                Constants.XML_FILE_EXTENSION,
                Constants.MD_FILE_EXTENSION,
                // C#
                Constants.CSHARP_FILE_EXTENSION,
                Constants.CSPROJ_FILE_EXTENSION,
                Constants.SLN_FILE_EXTENSION,
                // C / C++
                Constants.C_FILE_EXTENSION,
                Constants.CPP_FILE_EXTENSION,
                Constants.CPP_CC_FILE_EXTENSION,
                Constants.CPP_CXX_FILE_EXTENSION,
                Constants.H_FILE_EXTENSION,
                Constants.HPP_FILE_EXTENSION,
                Constants.HH_FILE_EXTENSION,
                Constants.HXX_FILE_EXTENSION,
                // Other common languages
                Constants.GO_FILE_EXTENSION,
                Constants.RUBY_FILE_EXTENSION,
                Constants.PHP_FILE_EXTENSION,
                Constants.KOTLIN_FILE_EXTENSION,
                Constants.SWIFT_FILE_EXTENSION,
                Constants.SCALA_FILE_EXTENSION,
                Constants.RUST_FILE_EXTENSION,
                Constants.SHELL_FILE_EXTENSION
        ).anyMatch(name::endsWith);
    }

    @Override
    public PreProcessedData parse(File file) throws Exception {
        log.debug("Parsing file: {}", file.getAbsolutePath());
        try {
            String content = FileUtil.readFileToString(file);
            // Determine file type from extension
            String fileType = getFileType(file);

            // Build metadata
            FileMetadata metadata = FileMetadata.builder()
                    .filePath(file.toPath())
                    .fileName(file.getName())
                    .fileType(fileType)
                    .fileSize(file.length())
                    .encoding(Constants.DEFAULT_ENCODING)
                    .build();
            return PreProcessedData.builder()
                    .metadata(metadata)
                    .content(content)
//                    .context(context)
                    .parserName(getParserName())
                    .build();
        } catch (Exception e) {
            log.error("Error parsing file: {}", file.getName(), e);
            throw new ParseException("Parse error: " + file.getName(), e);
        }
    }

    @NotNull
    private static String getFileType(File file) {
        String fileNameLower = file.getName().toLowerCase(Locale.ROOT);
        String fileType = "unknown";
        if (fileNameLower.endsWith(Constants.JAVA_FILE_EXTENSION)) {
            fileType = "java";
        } else if (fileNameLower.endsWith(Constants.PYTHON_FILE_EXTENSION)) {
            fileType = "python";
        } else if (fileNameLower.endsWith(Constants.JS_FILE_EXTENSION) || fileNameLower.endsWith(Constants.MJS_FILE_EXTENSION) || fileNameLower.endsWith(Constants.CJS_FILE_EXTENSION)) {
            fileType = "javascript";
        } else if (fileNameLower.endsWith(Constants.JSX_FILE_EXTENSION) || fileNameLower.endsWith(Constants.TSX_FILE_EXTENSION) || fileNameLower.endsWith(Constants.TS_FILE_EXTENSION)) {
            fileType = "typescript";
        } else if (fileNameLower.endsWith(Constants.XML_FILE_EXTENSION) || fileNameLower.endsWith(Constants.CSPROJ_FILE_EXTENSION)) {
            fileType = "xml";
        } else if (fileNameLower.endsWith(Constants.MD_FILE_EXTENSION)) {
            fileType = "markdown";
        } else if (fileNameLower.endsWith(Constants.CSHARP_FILE_EXTENSION)) {
            fileType = "csharp";
        } else if (fileNameLower.endsWith(Constants.SLN_FILE_EXTENSION)) {
            fileType = "sln";
        } else if (fileNameLower.endsWith(Constants.C_FILE_EXTENSION)) {
            fileType = "c";
        } else if (fileNameLower.endsWith(Constants.CPP_FILE_EXTENSION) || fileNameLower.endsWith(Constants.CPP_CC_FILE_EXTENSION) || fileNameLower.endsWith(Constants.CPP_CXX_FILE_EXTENSION)) {
            fileType = "cpp";
        } else if (fileNameLower.endsWith(Constants.H_FILE_EXTENSION) || fileNameLower.endsWith(Constants.HPP_FILE_EXTENSION) || fileNameLower.endsWith(Constants.HH_FILE_EXTENSION) || fileNameLower.endsWith(Constants.HXX_FILE_EXTENSION)) {
            fileType = "header";
        } else if (fileNameLower.endsWith(Constants.GO_FILE_EXTENSION)) {
            fileType = "go";
        } else if (fileNameLower.endsWith(Constants.RUBY_FILE_EXTENSION)) {
            fileType = "ruby";
        } else if (fileNameLower.endsWith(Constants.PHP_FILE_EXTENSION)) {
            fileType = "php";
        } else if (fileNameLower.endsWith(Constants.KOTLIN_FILE_EXTENSION)) {
            fileType = "kotlin";
        } else if (fileNameLower.endsWith(Constants.SWIFT_FILE_EXTENSION)) {
            fileType = "swift";
        } else if (fileNameLower.endsWith(Constants.SCALA_FILE_EXTENSION)) {
            fileType = "scala";
        } else if (fileNameLower.endsWith(Constants.RUST_FILE_EXTENSION)) {
            fileType = "rust";
        } else if (fileNameLower.endsWith(Constants.SHELL_FILE_EXTENSION)) {
            fileType = "shell";
        }
        return fileType;
    }

    @Override
    public int getPriority() {
        return 10; // Higher priority for Java files
    }
}
