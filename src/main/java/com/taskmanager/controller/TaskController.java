package com.taskmanager.controller;

import com.taskmanager.dto.AiInsightResponse;
import com.taskmanager.dto.DashboardResponse;
import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.entity.User;
import com.taskmanager.entity.enums.TaskPriority;
import com.taskmanager.entity.enums.TaskStatus;
import com.taskmanager.service.AiAnalysisService;
import com.taskmanager.service.FileService;
import com.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Endpoints for AI-powered task management, requiring JWT authentication")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;
    private final FileService fileService;
    private final AiAnalysisService aiAnalysisService;

    public TaskController(TaskService taskService, FileService fileService, AiAnalysisService aiAnalysisService) {
        this.taskService = taskService;
        this.fileService = fileService;
        this.aiAnalysisService = aiAnalysisService;
    }

    // ==================== CRUD ====================

    @Operation(summary = "Create a new task", description = "Creates a task for the authenticated user. AI analysis is triggered asynchronously in the background.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Task created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal User user
    ) {
        TaskResponse response = taskService.createTask(request, user);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "List all tasks", description = "Returns paginated tasks for the authenticated user. Supports filtering by status & priority, and sorting.")
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getTasks(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Filter by task status") @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "Filter by task priority") @RequestParam(required = false) TaskPriority priority,
            @Parameter(description = "Page number (zero-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field (e.g., createdAt, dueDate, priority)") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        int safeSize = Math.min(size, 100);
        Page<TaskResponse> tasks = taskService.getTasks(user, status, priority, page, safeSize, sortBy, sortDir);
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "Get task by ID", description = "Retrieves a single task. Only accessible by the task owner.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task found"),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content),
            @ApiResponse(responseCode = "403", description = "Not the task owner", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        TaskResponse response = taskService.getTaskById(id, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update task", description = "Updates an existing task. Only the task owner can modify it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task updated"),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal User user
    ) {
        TaskResponse response = taskService.updateTask(id, request, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Soft-delete task", description = "Marks a task as deleted (soft delete). The task is hidden from listings but retained in the database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Task deleted"),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        taskService.deleteTask(id, user);
        return ResponseEntity.noContent().build();
    }

    // ==================== SEARCH ====================

    @Operation(summary = "Search tasks by keyword", description = "Full-text search across task titles and descriptions for the authenticated user.")
    @GetMapping("/search")
    public ResponseEntity<List<TaskResponse>> searchTasks(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Search keyword") @RequestParam String keyword
    ) {
        List<TaskResponse> results = taskService.searchTasks(user, keyword);
        return ResponseEntity.ok(results);
    }

    // ==================== DASHBOARD ====================

    @Operation(summary = "Get dashboard analytics", description = "Returns cached task statistics including counts by status, overdue count, completion rate, and AI analysis metrics.")
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal User user
    ) {
        DashboardResponse response = taskService.getDashboard(user);
        return ResponseEntity.ok(response);
    }

    // ==================== FILE UPLOAD ====================

    @Operation(summary = "Upload file attachment", description = "Uploads a file to AWS S3 (or local fallback) and attaches it to the specified task.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded and attached to task"),
            @ApiResponse(responseCode = "500", description = "Upload failed", content = @Content)
    })
    @PostMapping(value = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskResponse> uploadFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) {
        String fileUrl = fileService.uploadFile(file);
        TaskResponse response = taskService.updateFileUrl(id, user, fileUrl);
        return ResponseEntity.ok(response);
    }

    // ==================== AI INSIGHTS ====================

    @Operation(summary = "Get AI insights for a task", description = "Retrieves AI-generated summary, predicted priority, and auto-extracted tags for a task.")
    @GetMapping("/{id}/ai")
    public ResponseEntity<AiInsightResponse> getAiInsights(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        TaskResponse task = taskService.getTaskById(id, user);
        AiInsightResponse insight = AiInsightResponse.builder()
                .taskId(task.getId())
                .status(task.getAiStatus())
                .summary(task.getAiSummary())
                .predictedPriority(task.getAiPriority())
                .tags(task.getAiTags() != null
                        ? java.util.Arrays.asList(task.getAiTags().split(","))
                        : java.util.Collections.emptyList())
                .build();
        return ResponseEntity.ok(insight);
    }

    @Operation(summary = "Trigger AI analysis", description = "Manually triggers asynchronous AI analysis using Google Gemini. Returns immediately with HTTP 202 Accepted.")
    @ApiResponse(responseCode = "202", description = "AI analysis triggered in the background")
    @PostMapping("/{id}/analyze")
    public ResponseEntity<java.util.Map<String, String>> triggerAiAnalysis(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        // Validate ownership first
        taskService.getTaskById(id, user);
        // Fire and forget
        aiAnalysisService.analyzeTaskAsync(id);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(java.util.Map.of("message", "AI analysis triggered in the background."));
    }
}
