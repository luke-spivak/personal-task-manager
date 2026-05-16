package com.example.taskmanager.ai;

import com.example.taskmanager.ai.dto.TaskSuggestionResponse;

/**
 * Boundary around external AI providers.
 *
 * <p>Keeping this as an interface lets controllers and services be tested without network calls and keeps
 * provider-specific request/response details out of the application layer.</p>
 */
public interface AiClient {

    /**
     * Converts natural-language task text into the structured task suggestion used by the API.
     *
     * @param description plain-language task or reminder text
     * @return structured task suggestion from the provider
     */
    TaskSuggestionResponse suggestTask(String description);
}
