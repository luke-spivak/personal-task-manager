package com.example.taskmanager.ai;

import com.example.taskmanager.ai.dto.TaskSuggestionRequest;
import com.example.taskmanager.ai.dto.TaskSuggestionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
public class AiTaskController {

    private final AiTaskSuggestionService aiTaskSuggestionService;

    public AiTaskController(AiTaskSuggestionService aiTaskSuggestionService) {
        this.aiTaskSuggestionService = aiTaskSuggestionService;
    }

    @PostMapping("/suggest")
    public TaskSuggestionResponse suggestTask(@Valid @RequestBody TaskSuggestionRequest request) {
        return aiTaskSuggestionService.suggestTask(request);
    }
}
