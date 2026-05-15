package com.example.taskmanager.task;

import com.example.taskmanager.task.dto.TaskCreateRequest;
import com.example.taskmanager.task.dto.TaskResponse;
import com.example.taskmanager.task.dto.TaskUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class TaskServiceTest {

    @Test
    void createTaskCreatesTaskWithDefaults() {
        FakeTaskRepository taskRepository = new FakeTaskRepository();
        TaskService taskService = new TaskService(taskRepository.repository());
        TaskCreateRequest request = new TaskCreateRequest(
                "  Write tests  ",
                "Cover the CRUD flow",
                LocalDate.of(2026, 5, 20),
                null,
                null
        );

        TaskResponse response = taskService.createTask(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Write tests");
        assertThat(response.description()).isEqualTo("Cover the CRUD flow");
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void listTasksReturnsAllTasks() {
        FakeTaskRepository taskRepository = new FakeTaskRepository();
        TaskService taskService = new TaskService(taskRepository.repository());
        Task first = task(1L, "First", TaskPriority.LOW, TaskStatus.TODO);
        Task second = task(2L, "Second", TaskPriority.HIGH, TaskStatus.DONE);
        taskRepository.add(first);
        taskRepository.add(second);

        List<TaskResponse> responses = taskService.listTasks();

        assertThat(responses)
                .extracting(TaskResponse::title)
                .containsExactly("First", "Second");
    }

    @Test
    void getTaskReturnsTaskById() {
        FakeTaskRepository taskRepository = new FakeTaskRepository();
        TaskService taskService = new TaskService(taskRepository.repository());
        taskRepository.add(task(1L, "Read docs", TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS));

        TaskResponse response = taskService.getTask(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Read docs");
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateTaskUpdatesExistingTask() {
        FakeTaskRepository taskRepository = new FakeTaskRepository();
        TaskService taskService = new TaskService(taskRepository.repository());
        Task existing = task(1L, "Old title", TaskPriority.LOW, TaskStatus.TODO);
        taskRepository.add(existing);
        TaskUpdateRequest request = new TaskUpdateRequest(
                "New title",
                "Updated description",
                LocalDate.of(2026, 6, 1),
                TaskPriority.HIGH,
                TaskStatus.IN_PROGRESS
        );

        TaskResponse response = taskService.updateTask(1L, request);

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.description()).isEqualTo("Updated description");
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void deleteTaskDeletesExistingTask() {
        FakeTaskRepository taskRepository = new FakeTaskRepository();
        TaskService taskService = new TaskService(taskRepository.repository());
        Task existing = task(1L, "Delete me", TaskPriority.MEDIUM, TaskStatus.TODO);
        taskRepository.add(existing);

        taskService.deleteTask(1L);

        assertThat(taskRepository.findById(1L)).isEmpty();
    }

    private Task task(Long id, String title, TaskPriority priority, TaskStatus status) {
        Task task = new Task();
        task.setTitle(title);
        task.setPriority(priority);
        task.setStatus(status);
        return withId(task, id);
    }

    private Task withId(Task task, Long id) {
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }

    private static final class FakeTaskRepository implements InvocationHandler {

        private final Map<Long, Task> tasks = new LinkedHashMap<>();
        private final AtomicLong nextId = new AtomicLong(1);

        private TaskRepository repository() {
            return (TaskRepository) Proxy.newProxyInstance(
                    TaskRepository.class.getClassLoader(),
                    new Class<?>[]{TaskRepository.class},
                    this
            );
        }

        private void add(Task task) {
            tasks.put(task.getId(), task);
            nextId.updateAndGet(current -> Math.max(current, task.getId() + 1));
        }

        private Optional<Task> findById(Long id) {
            return Optional.ofNullable(tasks.get(id));
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "save" -> save((Task) args[0]);
                case "findAll" -> List.copyOf(tasks.values());
                case "findById" -> findById((Long) args[0]);
                case "delete" -> {
                    Task task = (Task) args[0];
                    tasks.remove(task.getId());
                    yield null;
                }
                case "toString" -> "FakeTaskRepository";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException("Unsupported repository method: " + method.getName());
            };
        }

        private Task save(Task task) {
            if (task.getId() == null) {
                ReflectionTestUtils.setField(task, "id", nextId.getAndIncrement());
            }
            tasks.put(task.getId(), task);
            return task;
        }
    }
}
