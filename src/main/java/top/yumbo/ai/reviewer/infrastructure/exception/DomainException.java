package top.yumbo.ai.reviewer.infrastructure.exception;

/**
 * Domain exception base class
 *
 * @author AI-Reviewer Team
 * @version 2.0
 */
public class DomainException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DomainException() {
        super();
    }

    public DomainException(String message) {
        super(message);
    }

    public DomainException(Throwable cause) {
        super(cause);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
