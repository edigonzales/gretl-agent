package ch.so.agi.gretl.copilot.chat;

import ch.so.agi.gretl.copilot.chat.dto.ChatRequest;
import ch.so.agi.gretl.copilot.chat.dto.ChatResponse;
import ch.so.agi.gretl.copilot.orchestration.TaskType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @Test
    void returnsChatResponse() throws Exception {
        ChatResponse response = new ChatResponse(TaskType.FIND_TASK, "Handled");
        Mockito.when(chatService.respond(Mockito.any(ChatRequest.class))).thenReturn(response);

        ChatRequest request = new ChatRequest("find something");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goal").value("FIND_TASK"))
                .andExpect(jsonPath("$.answer").value("Handled"));
    }
}
