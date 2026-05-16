package com.example.taskmanager.task.dto;

import com.example.taskmanager.task.Task;
import com.example.taskmanager.task.TaskPriority;
import com.example.taskmanager.task.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Public API representation of a persisted task.
 */
public record TaskResponse(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        TaskPriority priority,
        TaskStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Maps the JPA entity to the API DTO so persistence details stay out of controllers.
     */
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getPriority(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
