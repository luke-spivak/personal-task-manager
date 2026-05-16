package com.example.taskmanager.ai;

import com.example.taskmanager.ai.dto.TaskSuggestionRequest;
import com.example.taskmanager.ai.dto.TaskSuggestionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for AI-assisted task suggestions.
 *
 * <p>The endpoint is stateless: it returns a suggested task shape but does not persist it.</p>
 */
@RestController
@RequestMapping("/tasks")
public class AiTaskController {

    private final AiTaskSuggestionService aiTaskSuggestionService;

    public AiTaskController(AiTaskSuggestionService aiTaskSuggestionService) {
        this.aiTaskSuggestionService = aiTaskSuggestionService;
    }

    /**
     * Suggests a structured task from natural-language input.
     *
     * @param request validated plain-language task request
     * @return suggested task fields suitable for display or later creation
     */
    @PostMapping("/suggest")
    public TaskSuggestionResponse suggestTask(@Valid @RequestBody TaskSuggestionRequest request) {
        return aiTaskSuggestionService.suggestTask(request);
    }
}
