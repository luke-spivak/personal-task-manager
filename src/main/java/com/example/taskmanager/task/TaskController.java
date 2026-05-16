package com.example.taskmanager.task;

import com.example.taskmanager.task.dto.TaskCreateRequest;
import com.example.taskmanager.task.dto.TaskResponse;
import com.example.taskmanager.task.dto.TaskUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST entry point for task CRUD operations.
 *
 * <p>All request bodies are validated before they reach the service layer, and
 * domain/service exceptions are translated to JSON errors by {@code GlobalExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Creates a task and returns the persisted representation, including its generated id and timestamps.
     *
     * @param request validated task attributes supplied by the client
     * @return the created task as JSON
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@Valid @RequestBody TaskCreateRequest request) {
        return taskService.createTask(request);
    }

    /**
     * Lists every task currently stored in the in-memory database.
     *
     * @return all tasks in repository order
     */
    @GetMapping
    public List<TaskResponse> listTasks() {
        return taskService.listTasks();
    }

    /**
     * Looks up one task by id.
     *
     * @param id generated task id from the URL
     * @return matching task
     */
    @GetMapping("/{id}")
    public TaskResponse getTask(@PathVariable Long id) {
        return taskService.getTask(id);
    }

    /**
     * Replaces a task's editable fields with the supplied values.
     *
     * @param id generated task id from the URL
     * @param request validated replacement values
     * @return updated task
     */
    @PutMapping("/{id}")
    public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        return taskService.updateTask(id, request);
    }

    /**
     * Deletes a task by id.
     *
     * @param id generated task id from the URL
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }
}
