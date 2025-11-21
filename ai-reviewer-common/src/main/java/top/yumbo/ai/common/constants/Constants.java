package top.yumbo.ai.common.constants;
/**
 * Global constants
 */
public final class Constants {
    private Constants() {
        throw new UnsupportedOperationException("Utility class");
    }
    // File related
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    // Parser related
    public static final String JAVA_FILE_EXTENSION = ".java";
    public static final String PYTHON_FILE_EXTENSION = ".py";
    public static final String JS_FILE_EXTENSION = ".js";
    public static final String JSX_FILE_EXTENSION = ".jsx";
    public static final String TS_FILE_EXTENSION = ".ts";
    public static final String TSX_FILE_EXTENSION = ".tsx";
    public static final String MJS_FILE_EXTENSION = ".mjs";
    public static final String CJS_FILE_EXTENSION = ".cjs";
    public static final String XML_FILE_EXTENSION = ".xml";
    public static final String MD_FILE_EXTENSION = ".md";
    // C# related
    public static final String CSHARP_FILE_EXTENSION = ".cs";
    public static final String CSPROJ_FILE_EXTENSION = ".csproj";
    public static final String SLN_FILE_EXTENSION = ".sln";
    // AI related
    public static final int DEFAULT_MAX_TOKENS = 2000;
    public static final double DEFAULT_TEMPERATURE = 0.7;
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;
    public static final int DEFAULT_MAX_RETRIES = 3;
    // Output formats
    public static final String FORMAT_MARKDOWN = "markdown";
    public static final String FORMAT_HTML = "html";
    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_PDF = "pdf";
}
