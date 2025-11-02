package ch.so.agi.gretl.copilot.chat.ui;

import ch.so.agi.gretl.copilot.chat.stream.ChatStreamPublisher;
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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ChatUiIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @SpyBean
    private ChatStreamPublisher chatStreamPublisher;

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
                    Assertions.assertThat(html).contains("https://unpkg.com/htmx.org@2.0.8");
                });
    }

    @Test
    void streamsAssistantReplyOverSse() {
        Mockito.when(taskOrchestrator.orchestrate(Mockito.anyString()))
                .thenReturn(new TaskExecutionResult(TaskType.FIND_TASK, "Mock response"));

        String clientId = UUID.randomUUID().toString();

        String html = webTestClient.post()
                .uri("/ui/chat/messages")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("message", "find a task").with("clientId", clientId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertThat(html).contains("find a task");

        Mockito.verify(chatStreamPublisher, Mockito.timeout(2000))
                .publish(Mockito.eq(clientId), Mockito.argThat(payload -> payload.contains("Mock response")));
    }
}
