package ch.so.agi.gretl.copilot.chat.ui;

import ch.so.agi.gretl.copilot.orchestration.TaskExecutionResult;
import ch.so.agi.gretl.copilot.orchestration.TaskOrchestrator;
import ch.so.agi.gretl.copilot.orchestration.TaskType;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ChatUiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskOrchestrator taskOrchestrator;

    @Test
    void servesChatPage() throws Exception {
        mockMvc.perform(get("/ui/chat"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(Matchers.containsString("hx-get=\"/ui/chat/messages/poll\"")))
                .andExpect(content().string(Matchers.containsString("hx-trigger=\"load, every 2s\"")))
                .andExpect(content().string(Matchers.containsString("https://unpkg.com/htmx.org@2.0.8")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("htmx-ext-sse"))));
    }

    @Test
    void pollsAssistantReplyAfterSubmittingMessage() throws Exception {
        Mockito.when(taskOrchestrator.orchestrate(Mockito.anyString()))
                .thenReturn(new TaskExecutionResult(TaskType.FIND_TASK, "Mock response"));

        String clientId = UUID.randomUUID().toString();

        mockMvc.perform(post("/ui/chat/messages")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("message", "find a task")
                        .param("clientId", clientId))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("find a task")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Mock response"))));

        String assistantHtml = awaitAssistantHtml(clientId);
        Assertions.assertThat(assistantHtml).contains("Mock response");

        mockMvc.perform(get("/ui/chat/messages/poll").param("clientId", clientId))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    private String awaitAssistantHtml(String clientId) throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            MvcResult pollResult = mockMvc.perform(get("/ui/chat/messages/poll").param("clientId", clientId))
                    .andExpect(status().isOk())
                    .andReturn();
            String body = pollResult.getResponse().getContentAsString();
            if (!body.isBlank()) {
                return body;
            }
            Thread.sleep(100);
        }
        fail("Expected the assistant response to be available during polling");
        return ""; // unreachable
    }
}
