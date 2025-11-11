package ch.so.agi.gretl.copilot.orchestration;

import ch.so.agi.gretl.copilot.orchestration.agent.TaskExplanationAgent;
import ch.so.agi.gretl.copilot.orchestration.agent.TaskFinderAgent;
import ch.so.agi.gretl.copilot.orchestration.agent.TaskFinderDocument;
import ch.so.agi.gretl.copilot.orchestration.agent.TaskFinderRepository;
import ch.so.agi.gretl.copilot.orchestration.agent.TaskGeneratorAgent;
import ch.so.agi.gretl.copilot.orchestration.render.MarkdownRenderer;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskOrchestratorTest {

    private RecordingModel model;
    private TaskOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        model = new RecordingModel();
        TaskFinderRepository repository = new StubFinderRepository();
        ObjectProvider<EmbeddingModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        TaskFinderAgent finderAgent = new TaskFinderAgent(repository, provider, new MarkdownRenderer());
        orchestrator = new TaskOrchestrator(model, finderAgent, new TaskExplanationAgent(), new TaskGeneratorAgent());
    }

    @Test
    void routesToFinderAgent() {
        model.setNextResponse("FIND_TASK");

        TaskExecutionResult result = orchestrator.orchestrate("Please find me a task");

        assertThat(result.taskType()).isEqualTo(TaskType.FIND_TASK);
        assertThat(result.answer()).contains("task-demo");
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

    private static class StubFinderRepository implements TaskFinderRepository {

        @Override
        public List<TaskFinderDocument> searchLexical(String query, int limit) {
            return List.of(new TaskFinderDocument(
                    "task-demo",
                    "Lexikalischer Treffer",
                    "https://example.org/tasks/demo",
                    "section",
                    "Beispielinhalt für den Unittests",
                    1.0d,
                    0.0d
            ));
        }

        @Override
        public List<TaskFinderDocument> searchSemantic(double[] embedding, int limit) {
            return List.of();
        }
    }
}
