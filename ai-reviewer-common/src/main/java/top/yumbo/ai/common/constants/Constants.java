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
    // C / C++ related
    public static final String C_FILE_EXTENSION = ".c";
    public static final String CPP_FILE_EXTENSION = ".cpp";
    public static final String CPP_CC_FILE_EXTENSION = ".cc";
    public static final String CPP_CXX_FILE_EXTENSION = ".cxx";
    public static final String H_FILE_EXTENSION = ".h";
    public static final String HPP_FILE_EXTENSION = ".hpp";
    public static final String HH_FILE_EXTENSION = ".hh";
    public static final String HXX_FILE_EXTENSION = ".hxx";
    // Other common languages
    public static final String GO_FILE_EXTENSION = ".go";
    public static final String RUBY_FILE_EXTENSION = ".rb";
    public static final String PHP_FILE_EXTENSION = ".php";
    public static final String KOTLIN_FILE_EXTENSION = ".kt";
    public static final String SWIFT_FILE_EXTENSION = ".swift";
    public static final String SCALA_FILE_EXTENSION = ".scala";
    public static final String RUST_FILE_EXTENSION = ".rs";
    public static final String SHELL_FILE_EXTENSION = ".sh";
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
