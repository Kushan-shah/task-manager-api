package com.taskmanager.service;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.User;
import com.taskmanager.entity.enums.Role;
import com.taskmanager.entity.enums.TaskPriority;
import com.taskmanager.entity.enums.TaskStatus;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.exception.UnauthorizedException;
import com.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AiAnalysisService aiAnalysisService;

    @InjectMocks
    private TaskService taskService;

    private User testUser;
    private User otherUser;
    private Task testTask;
    private TaskRequest taskRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .role(Role.USER)
                .build();

        otherUser = User.builder()
                .id(2L)
                .name("Other User")
                .email("other@example.com")
                .role(Role.USER)
                .build();

        testTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.now().plusDays(7))
                .deleted(false)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();

        taskRequest = TaskRequest.builder()
                .title("New Task")
                .description("New Description")
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(5))
                .build();
    }

    @Test
    @DisplayName("Create task — success")
    void createTask_Success() {
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        TaskResponse response = taskService.createTask(taskRequest, testUser);

        assertNotNull(response);
        assertEquals("Test Task", response.getTitle());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Get task by ID — success")
    void getTaskById_Success() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        TaskResponse response = taskService.getTaskById(1L, testUser);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("Get task — not found throws exception")
    void getTaskById_NotFound_ThrowsException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskById(99L, testUser));
    }

    @Test
    @DisplayName("Get task — unauthorized user throws exception")
    void getTaskById_WrongUser_ThrowsException() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        assertThrows(UnauthorizedException.class, () -> taskService.getTaskById(1L, otherUser));
    }

    @Test
    @DisplayName("Update task — success")
    void updateTask_Success() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        TaskResponse response = taskService.updateTask(1L, taskRequest, testUser);

        assertNotNull(response);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Delete task — soft deletes")
    void deleteTask_SoftDeletes() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        taskService.deleteTask(1L, testUser);

        assertTrue(testTask.getDeleted());
        verify(taskRepository).save(testTask);
    }

    @Test
    @DisplayName("Delete task — wrong user throws exception")
    void deleteTask_WrongUser_ThrowsException() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        assertThrows(UnauthorizedException.class, () -> taskService.deleteTask(1L, otherUser));
    }
}
