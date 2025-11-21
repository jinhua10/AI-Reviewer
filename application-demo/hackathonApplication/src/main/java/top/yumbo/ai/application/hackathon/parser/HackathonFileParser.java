package top.yumbo.ai.application.hackathon.parser;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.api.model.FileMetadata;
import top.yumbo.ai.api.model.PreProcessedData;
import top.yumbo.ai.api.parser.IFileParser;
import top.yumbo.ai.common.constants.Constants;
import top.yumbo.ai.common.exception.ParseException;
import top.yumbo.ai.common.util.FileUtil;

import java.io.File;
import java.util.stream.Stream;

/**
 * Hackathon file parser.
 */
@Slf4j
public class HackathonFileParser implements IFileParser {
    @Override
    public boolean support(File file) {
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
                Constants.MD_FILE_EXTENSION
        ).anyMatch(ext -> file.getName().endsWith(ext));
    }

    @Override
    public PreProcessedData parse(File file) throws Exception {
        log.debug("Parsing Java file: {}", file.getAbsolutePath());
        try {
            String content = FileUtil.readFileToString(file);
            // Build metadata
            FileMetadata metadata = FileMetadata.builder()
                    .filePath(file.toPath())
                    .fileName(file.getName())
                    .fileType("java")
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
            log.error("Error parsing Java file: {}", file.getName(), e);
            throw new ParseException("Java parse error: " + file.getName(), e);
        }
    }

    @Override
    public int getPriority() {
        return 10; // Higher priority for Java files
    }
}
