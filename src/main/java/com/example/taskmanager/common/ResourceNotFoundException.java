package com.example.taskmanager.common;

/**
 * Signals that a requested domain resource does not exist.
 *
 * <p>The global exception handler maps this to {@code 404}.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
