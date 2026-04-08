package com.taskmanager.controller;

import com.taskmanager.dto.TaskResponse;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.util.DtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only endpoints for managing all users and tasks across the platform")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public AdminController(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "List all tasks (Admin)", description = "Returns all non-deleted tasks from all users. Requires ADMIN role.")
    @GetMapping("/tasks")
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        List<TaskResponse> tasks = taskRepository.findAll()
                .stream()
                .filter(task -> !task.getDeleted())
                .map(DtoMapper::toTaskResponse)
                .toList();
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "List all users (Admin)", description = "Returns all registered users with their roles. Requires ADMIN role.")
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<Map<String, Object>> users = userRepository.findAll()
                .stream()
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail(),
                        "role", user.getRole().name(),
                        "createdAt", user.getCreatedAt().toString()
                ))
                .toList();
        return ResponseEntity.ok(users);
    }
}
