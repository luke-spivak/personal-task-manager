package com.example.taskmanager.ai;

import com.example.taskmanager.ai.dto.TaskSuggestionResponse;

public interface AiClient {

    TaskSuggestionResponse suggestTask(String description);
}
