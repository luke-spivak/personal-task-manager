package com.example.taskmanager.ai;

/**
 * Signals that the AI suggestion path cannot produce a reliable response for the current request.
 *
 * <p>The global exception handler maps this to {@code 503} with a client-safe message.</p>
 */
public class AiServiceUnavailableException extends RuntimeException {

    public AiServiceUnavailableException(String message) {
        super(message);
    }
}
