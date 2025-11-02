package ch.so.agi.gretl.copilot.chat;

import ch.so.agi.gretl.copilot.chat.dto.ChatRequest;
import ch.so.agi.gretl.copilot.chat.dto.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.respond(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
