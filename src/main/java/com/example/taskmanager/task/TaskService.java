package com.example.taskmanager.task;

import com.example.taskmanager.common.ResourceNotFoundException;
import com.example.taskmanager.task.dto.TaskCreateRequest;
import com.example.taskmanager.task.dto.TaskResponse;
import com.example.taskmanager.task.dto.TaskUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Coordinates task persistence and applies task-specific defaults before data is returned to controllers.
 */
@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Creates a new task from a validated request.
     *
     * <p>Missing priority and status are intentionally defaulted to {@code MEDIUM} and {@code TODO} so
     * clients can submit the smallest useful payload.</p>
     *
     * @param request validated task creation request
     * @return persisted task response
     */
    public TaskResponse createTask(TaskCreateRequest request) {
        Task task = new Task();
        applyCreateRequest(task, request);
        return TaskResponse.from(taskRepository.save(task));
    }

    /**
     * Reads all tasks without mutating persistence state.
     *
     * @return all tasks mapped to API response DTOs
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks() {
        return taskRepository.findAll()
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    /**
     * Reads a single task.
     *
     * @param id generated task id
     * @return matching task response
     * @throws ResourceNotFoundException when no task exists for {@code id}
     */
    @Transactional(readOnly = true)
    public TaskResponse getTask(Long id) {
        return TaskResponse.from(findTask(id));
    }

    /**
     * Updates a task's editable fields.
     *
     * <p>Null priority/status keep the current values, which lets clients update textual fields without
     * accidentally resetting workflow metadata.</p>
     *
     * @param id generated task id
     * @param request validated replacement values
     * @return updated task response
     * @throws ResourceNotFoundException when no task exists for {@code id}
     */
    public TaskResponse updateTask(Long id, TaskUpdateRequest request) {
        Task task = findTask(id);
        applyUpdateRequest(task, request);
        return TaskResponse.from(taskRepository.save(task));
    }

    /**
     * Deletes a task after verifying it exists.
     *
     * @param id generated task id
     * @throws ResourceNotFoundException when no task exists for {@code id}
     */
    public void deleteTask(Long id) {
        Task task = findTask(id);
        taskRepository.delete(task);
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task %d was not found".formatted(id)));
    }

    private void applyCreateRequest(Task task, TaskCreateRequest request) {
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        task.setStatus(request.status() == null ? TaskStatus.TODO : request.status());
    }

    private void applyUpdateRequest(Task task, TaskUpdateRequest request) {
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setPriority(request.priority() == null ? task.getPriority() : request.priority());
        task.setStatus(request.status() == null ? task.getStatus() : request.status());
    }
}
