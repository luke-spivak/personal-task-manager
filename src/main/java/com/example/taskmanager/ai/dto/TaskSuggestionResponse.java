package com.example.taskmanager.ai.dto;

import com.example.taskmanager.task.TaskPriority;
import com.example.taskmanager.task.TaskStatus;

import java.time.LocalDate;

/**
 * Structured task suggestion returned by the AI endpoint.
 *
 * <p>This mirrors the create-task shape but is not persisted until the client submits it to the CRUD API.</p>
 */
public record TaskSuggestionResponse(
        String title,
        String description,
        LocalDate dueDate,
        TaskPriority priority,
        TaskStatus status
) {
}
