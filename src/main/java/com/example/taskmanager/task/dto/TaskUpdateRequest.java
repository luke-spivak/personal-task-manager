package com.example.taskmanager.task.dto;

import com.example.taskmanager.task.TaskPriority;
import com.example.taskmanager.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for updating a task's editable fields.
 *
 * <p>Title remains required, while null priority/status mean "leave the current value unchanged".</p>
 */
public record TaskUpdateRequest(
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be 255 characters or fewer")
        String title,

        @Size(max = 1_000, message = "description must be 1000 characters or fewer")
        String description,

        LocalDate dueDate,
        TaskPriority priority,
        TaskStatus status
) {
}
