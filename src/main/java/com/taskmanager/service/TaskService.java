package com.taskmanager.service;

import com.taskmanager.dto.DashboardResponse;
import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.User;
import com.taskmanager.entity.enums.TaskPriority;
import com.taskmanager.entity.enums.TaskStatus;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.exception.UnauthorizedException;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.util.DtoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // ==================== CRUD ====================

    @CacheEvict(value = "dashboard", allEntries = true)
    public TaskResponse createTask(TaskRequest request, User user) {
        log.info("Creating task '{}' for user: {}", request.getTitle(), user.getEmail());

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .dueDate(request.getDueDate())
                .status(TaskStatus.TODO)
                .user(user)
                .deleted(false)
                .build();

        Task saved = taskRepository.save(task);
        log.info("Task created with ID: {}", saved.getId());
        return DtoMapper.toTaskResponse(saved);
    }

    public TaskResponse getTaskById(Long taskId, User user) {
        log.debug("Fetching task ID: {} for user: {}", taskId, user.getEmail());
        Task task = findTaskAndValidateOwnership(taskId, user);
        return DtoMapper.toTaskResponse(task);
    }

    @CacheEvict(value = "dashboard", allEntries = true)
    public TaskResponse updateTask(Long taskId, TaskRequest request, User user) {
        log.info("Updating task ID: {} by user: {}", taskId, user.getEmail());
        Task task = findTaskAndValidateOwnership(taskId, user);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }

        Task updated = taskRepository.save(task);
        log.info("Task ID: {} updated successfully", updated.getId());
        return DtoMapper.toTaskResponse(updated);
    }

    @CacheEvict(value = "dashboard", allEntries = true)
    public void deleteTask(Long taskId, User user) {
        log.info("Soft-deleting task ID: {} by user: {}", taskId, user.getEmail());
        Task task = findTaskAndValidateOwnership(taskId, user);
        task.setDeleted(true);
        taskRepository.save(task);
        log.info("Task ID: {} soft-deleted", taskId);
    }

    // ==================== FILTERING + PAGINATION ====================

    public Page<TaskResponse> getTasks(User user, TaskStatus status, TaskPriority priority,
                                       int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return taskRepository
                .findAll(TaskSpecification.withFilters(user.getId(), status, priority), pageable)
                .map(DtoMapper::toTaskResponse);
    }

    // ==================== SEARCH ====================

    public List<TaskResponse> searchTasks(User user, String keyword) {
        log.debug("Searching tasks for user: {} with keyword: '{}'", user.getEmail(), keyword);
        return taskRepository.searchByKeyword(user.getId(), keyword)
                .stream()
                .map(DtoMapper::toTaskResponse)
                .toList();
    }

    // ==================== DASHBOARD (CACHED) ====================

    @Cacheable(value = "dashboard", key = "#user.id")
    public DashboardResponse getDashboard(User user) {
        log.info("Computing dashboard stats for user: {} (cache miss)", user.getEmail());
        Long userId = user.getId();
        LocalDate today = LocalDate.now();

        long todoCount = taskRepository.countByUserAndStatus(userId, TaskStatus.TODO);
        long inProgressCount = taskRepository.countByUserAndStatus(userId, TaskStatus.IN_PROGRESS);
        long doneCount = taskRepository.countByUserAndStatus(userId, TaskStatus.DONE);
        long overdueCount = taskRepository.countOverdueByUser(userId, today, TaskStatus.DONE);

        return DashboardResponse.builder()
                .totalTasks(todoCount + inProgressCount + doneCount)
                .todoCount(todoCount)
                .inProgressCount(inProgressCount)
                .doneCount(doneCount)
                .overdueCount(overdueCount)
                .build();
    }

    // ==================== FILE URL ====================

    @CacheEvict(value = "dashboard", allEntries = true)
    public TaskResponse updateFileUrl(Long taskId, User user, String fileUrl) {
        log.info("Attaching file to task ID: {}", taskId);
        Task task = findTaskAndValidateOwnership(taskId, user);
        task.setFileUrl(fileUrl);
        Task updated = taskRepository.save(task);
        return DtoMapper.toTaskResponse(updated);
    }

    // ==================== HELPER ====================

    private Task findTaskAndValidateOwnership(Long taskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        if (task.getDeleted()) {
            throw new ResourceNotFoundException("Task not found with id: " + taskId);
        }

        if (!task.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized access attempt — user {} tried to access task ID: {}", user.getEmail(), taskId);
            throw new UnauthorizedException("You don't have permission to access this task");
        }

        return task;
    }
}
