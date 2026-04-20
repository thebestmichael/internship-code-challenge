package com.logicgate.farm.exception;

/**
 * Checked exception thrown when farm data cannot be parsed or fails validation.
 *
 * <p>This exception is raised by {@link com.logicgate.farm.util.JsonParser} for
 * conditions such as:
 * <ul>
 *   <li>The input file does not exist</li>
 *   <li>The JSON is syntactically malformed</li>
 *   <li>Required fields ("livestock", "animal", "barn") are missing or blank</li>
 *   <li>The livestock array is empty</li>
 * </ul>
 */
public class FarmDataException extends Exception {

    /**
     * Constructs a FarmDataException with a descriptive message.
     *
     * @param message explanation of what went wrong
     */
    public FarmDataException(String message) {
        super(message);
    }

    /**
     * Constructs a FarmDataException wrapping a lower-level cause
     * (e.g., a Jackson {@code IOException}).
     *
     * @param message explanation of what went wrong
     * @param cause   the underlying exception
     */
    public FarmDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
