package ch.so.agi.gretl.copilot.chat;

import ch.so.agi.gretl.copilot.chat.dto.ChatRequest;
import ch.so.agi.gretl.copilot.chat.dto.ChatResponse;
import ch.so.agi.gretl.copilot.orchestration.TaskExecutionResult;
import ch.so.agi.gretl.copilot.orchestration.TaskOrchestrator;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final TaskOrchestrator orchestrator;

    public ChatService(TaskOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public ChatResponse respond(ChatRequest request) {
        TaskExecutionResult result = orchestrator.orchestrate(request.message());
        return new ChatResponse(result.taskType(), result.answer());
    }
}
