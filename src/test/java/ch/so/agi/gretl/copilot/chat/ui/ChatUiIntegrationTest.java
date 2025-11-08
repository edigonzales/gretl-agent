package ch.so.agi.gretl.copilot.chat.ui;

import ch.so.agi.gretl.copilot.orchestration.TaskExecutionResult;
import ch.so.agi.gretl.copilot.orchestration.TaskOrchestrator;
import ch.so.agi.gretl.copilot.orchestration.TaskType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import reactor.core.Disposable;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ChatUiIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ChatUiController chatUiController;

    @MockBean
    private TaskOrchestrator taskOrchestrator;

    @Test
    void servesChatPage() {
        webTestClient.get()
                .uri("/ui/chat")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    Assertions.assertThat(html).contains("hx-ext=\"sse\"");
                    Assertions.assertThat(html).contains("sse-swap=\"beforeend\"");
                    Assertions.assertThat(html).contains("https://unpkg.com/htmx.org@2.0.8");
                    Assertions.assertThat(html).contains("https://cdn.jsdelivr.net/npm/htmx-ext-sse");
                    Assertions.assertThat(html).contains("hx-on::after-request=\"this.reset(); htmx.find('#message').focus();\"");
                });
    }

    @Test
    void streamsAssistantReplyOverSse() {
        Mockito.when(taskOrchestrator.orchestrate(Mockito.anyString()))
                .thenReturn(new TaskExecutionResult(TaskType.FIND_TASK, "Mock response"));

        String clientId = UUID.randomUUID().toString();

        var receivedMessages = chatUiController.stream(clientId)
                .map(ServerSentEvent::data)
                .filter(payload -> payload != null && !payload.isBlank());

        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        Disposable subscription = receivedMessages.subscribe(queue::offer);
        try {
            String html = chatUiController.postMessage("find a task", clientId).block();
            Assertions.assertThat(html).isNotNull();
            Assertions.assertThat(html).contains("find a task");

            String payload;
            try {
                payload = queue.poll(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for SSE payload", ex);
            }

            Assertions.assertThat(payload).isNotNull();
            Assertions.assertThat(payload).contains("Mock response");
        } finally {
            subscription.dispose();
        }
    }
}
