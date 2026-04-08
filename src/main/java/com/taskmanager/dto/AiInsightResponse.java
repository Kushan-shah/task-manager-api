package com.taskmanager.dto;

import com.taskmanager.entity.enums.AiStatus;
import com.taskmanager.entity.enums.TaskPriority;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiInsightResponse {
    private Long taskId;
    private AiStatus status;
    private String summary;
    private TaskPriority predictedPriority;
    private List<String> tags;
    private String errorMessage;
}
