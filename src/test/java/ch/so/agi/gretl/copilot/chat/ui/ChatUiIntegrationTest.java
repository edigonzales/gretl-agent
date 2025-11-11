package ch.so.agi.gretl.copilot.chat.ui;

import ch.so.agi.gretl.copilot.chat.stream.ChatStreamPublisher;
import ch.so.agi.gretl.copilot.orchestration.TaskExecutionResult;
import ch.so.agi.gretl.copilot.orchestration.TaskOrchestrator;
import ch.so.agi.gretl.copilot.orchestration.TaskType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ChatUiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private ChatStreamPublisher chatStreamPublisher;

    @MockBean
    private TaskOrchestrator taskOrchestrator;

    @Test
    void servesChatPage() throws Exception {
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/ui/chat"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        Assertions.assertThat(html).contains("hx-ext=\"sse\"");
        Assertions.assertThat(html).contains("https://unpkg.com/htmx.org@2.0.8");
        Assertions.assertThat(html).contains("https://cdn.jsdelivr.net/npm/htmx-ext-sse@2.2.4/sse.min.js");
    }

    @Test
    void streamsAssistantReplyOverSse() throws Exception {
        Mockito.when(taskOrchestrator.orchestrate(Mockito.anyString()))
                .thenReturn(new TaskExecutionResult(TaskType.FIND_TASK, "Mock response"));

        String clientId = UUID.randomUUID().toString();

        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/ui/chat/messages")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("message", "find a task")
                        .param("clientId", clientId))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn();

        String html = result.getResponse().getContentAsString();

        Assertions.assertThat(html).contains("find a task");

        Mockito.verify(chatStreamPublisher, Mockito.timeout(2000))
                .publish(Mockito.eq(clientId), Mockito.argThat(payload -> payload.contains("Mock response")));
    }
}
