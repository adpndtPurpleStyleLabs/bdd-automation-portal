package com.bdd.portal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendExecutionLog(Long executionId, String message) {
        messagingTemplate.convertAndSend("/topic/execution/" + executionId + "/logs", message);
    }

    public void sendExecutionStatusUpdate(Long executionId, String status) {
        messagingTemplate.convertAndSend("/topic/execution/" + executionId + "/status", status);
    }

    public void broadcastExecutionUpdate(com.bdd.portal.entity.Execution execution) {
        com.bdd.portal.dto.ExecutionEventDto dto = com.bdd.portal.dto.ExecutionEventDto.fromExecution(execution);
        messagingTemplate.convertAndSend("/topic/executions", dto);
    }
}
