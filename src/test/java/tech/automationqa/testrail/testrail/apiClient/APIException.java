package tech.automationqa.testrail.testrail.apiClient;

/**
 * APIException is a custom exception class that represents errors occurring during API requests.
 * It extends the standard Exception class with additional functionality for handling API-related errors.
 */
public class APIException extends RuntimeException {

    /**
     * Constructor for APIException.
     *
     * @param message A string message providing details about the API exception that occurred.
     */
    public APIException(String message) {
        super(message);
    }
    public APIException(String message, Throwable cause) {
        super(message, cause);
    }
}