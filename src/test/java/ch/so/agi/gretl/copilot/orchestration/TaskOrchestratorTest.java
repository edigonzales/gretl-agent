package ch.so.agi.gretl.copilot.orchestration;

import ch.so.agi.gretl.copilot.orchestration.agent.TaskExplanationAgent;
import ch.so.agi.gretl.copilot.orchestration.agent.TaskFinderAgent;
import ch.so.agi.gretl.copilot.orchestration.agent.TaskGeneratorAgent;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskOrchestratorTest {

    private RecordingModel model;
    private TaskOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        model = new RecordingModel();
        orchestrator = new TaskOrchestrator(model, new TaskFinderAgent(), new TaskExplanationAgent(), new TaskGeneratorAgent());
    }

    @Test
    void routesToFinderAgent() {
        model.setNextResponse("FIND_TASK");

        TaskExecutionResult result = orchestrator.orchestrate("Please find me a task");

        assertThat(result.taskType()).isEqualTo(TaskType.FIND_TASK);
        assertThat(result.answer()).contains("Searching");
    }

    @Test
    void routesToExplanationAgent() {
        model.setNextResponse("EXPLAIN_TASK");

        TaskExecutionResult result = orchestrator.orchestrate("Can you explain a task?");

        assertThat(result.taskType()).isEqualTo(TaskType.EXPLAIN_TASK);
        assertThat(result.answer()).contains("Explaining");
    }

    @Test
    void routesToGeneratorAgent() {
        model.setNextResponse("GENERATE_TASK");

        TaskExecutionResult result = orchestrator.orchestrate("Please generate something new");

        assertThat(result.taskType()).isEqualTo(TaskType.GENERATE_TASK);
        assertThat(result.answer()).contains("Generating");
    }

    private static class RecordingModel implements ChatModel {

        private String nextResponse;

        void setNextResponse(String nextResponse) {
            this.nextResponse = nextResponse;
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            String response = this.nextResponse == null ? "FIND_TASK" : this.nextResponse;
            this.nextResponse = null;
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(response))
                    .build();
        }

        @Override
        public String chat(String prompt) {
            String response = this.nextResponse == null ? "FIND_TASK" : this.nextResponse;
            this.nextResponse = null;
            return response;
        }
    }
}
