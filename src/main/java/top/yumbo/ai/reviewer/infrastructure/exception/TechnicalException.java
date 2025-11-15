package top.yumbo.ai.reviewer.infrastructure.exception;

/**
 * Technical exception base class
 *
 * @author AI-Reviewer Team
 * @version 2.0
 */
public class TechnicalException extends RuntimeException {

    public TechnicalException(String message) {
        super(message);
    }

    public TechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}

