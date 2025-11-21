package top.yumbo.ai.application.hackathon.parser;

import lombok.extern.slf4j.Slf4j;
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
                // C# extensions
                Constants.CSHARP_FILE_EXTENSION,
                Constants.CSPROJ_FILE_EXTENSION,
                Constants.SLN_FILE_EXTENSION
        ).anyMatch(ext -> name.endsWith(ext));
    }

    @Override
    public PreProcessedData parse(File file) throws Exception {
        log.debug("Parsing file: {}", file.getAbsolutePath());
        try {
            String content = FileUtil.readFileToString(file);
            // Determine file type from extension
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
            }

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

    @Override
    public int getPriority() {
        return 10; // Higher priority for Java files
    }
}
