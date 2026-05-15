package com.example.taskmanager.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskSuggestionRequest(
        @NotBlank(message = "description is required")
        @Size(max = 2_000, message = "description must be 2000 characters or fewer")
        String description
) {
}
