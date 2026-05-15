package com.example.taskmanager.ai;

import com.example.taskmanager.ai.dto.TaskSuggestionRequest;
import com.example.taskmanager.ai.dto.TaskSuggestionResponse;
import com.example.taskmanager.task.TaskPriority;
import com.example.taskmanager.task.TaskStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiTaskSuggestionService {

    private final ObjectProvider<AiClient> aiClientProvider;

    public AiTaskSuggestionService(ObjectProvider<AiClient> aiClientProvider) {
        this.aiClientProvider = aiClientProvider;
    }

    public TaskSuggestionResponse suggestTask(TaskSuggestionRequest request) {
        AiClient aiClient = aiClientProvider.getIfAvailable();
        if (aiClient == null) {
            throw new AiServiceUnavailableException("AI task suggestions are not configured yet");
        }

        TaskSuggestionResponse suggestion = aiClient.suggestTask(request.description().trim());
        if (suggestion == null || !StringUtils.hasText(suggestion.title())) {
            throw new AiServiceUnavailableException("AI provider did not return a usable task suggestion");
        }

        return withDefaults(suggestion);
    }

    private TaskSuggestionResponse withDefaults(TaskSuggestionResponse suggestion) {
        TaskPriority priority = suggestion.priority() == null ? TaskPriority.MEDIUM : suggestion.priority();
        TaskStatus status = suggestion.status() == null ? TaskStatus.TODO : suggestion.status();

        return new TaskSuggestionResponse(
                suggestion.title().trim(),
                suggestion.description(),
                suggestion.dueDate(),
                priority,
                status
        );
    }
}
