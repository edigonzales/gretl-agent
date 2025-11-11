package ch.so.agi.gretl.copilot.chat.ui;

import ch.so.agi.gretl.copilot.chat.ChatService;
import ch.so.agi.gretl.copilot.chat.dto.ChatRequest;
import ch.so.agi.gretl.copilot.chat.stream.ChatStreamPublisher;
import ch.so.agi.gretl.copilot.chat.view.ChatMessageView;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Controller
@RequestMapping("/ui/chat")
public class ChatUiController {
    private static final Logger log = LoggerFactory.getLogger(ChatUiController.class);
    
    private final ChatService chatService;
    private final ChatStreamPublisher streamPublisher;
    private final ChatViewRenderer chatViewRenderer;

    public ChatUiController(ChatService chatService,
                            ChatStreamPublisher streamPublisher,
                            ChatViewRenderer chatViewRenderer) {
        this.chatService = chatService;
        this.streamPublisher = streamPublisher;
        this.chatViewRenderer = chatViewRenderer;
    }

    @GetMapping
    public String chatPage(Model model) {
        model.addAttribute("clientId", UUID.randomUUID().toString());
        return "chat/index";
    }

    @PostMapping(path = "/messages", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String postMessage(@RequestParam("message") String message,
                              @RequestParam("clientId") String clientId) {
        String sanitized = message == null ? "" : message.trim();
        if (sanitized.isEmpty()) {
            return "";
        }

        ChatMessageView userMessage = ChatMessageView.user(sanitized);
        String renderedUser = chatViewRenderer.renderMessage(userMessage);
        String removeEmptyState = "<div id=\"empty-state\" hx-swap-oob=\"delete\"></div>";

        CompletableFuture
                .supplyAsync(() -> chatService.respond(new ChatRequest(sanitized)))
                .thenApply(response -> ChatMessageView.assistant(response.answer(), response.goal()))
                .thenApply(chatViewRenderer::renderMessage)
                .thenAccept(rendered -> streamPublisher.publish(clientId, rendered))
                .exceptionally(error -> {
                    log.error("Failed to process chat message", error);
                    streamPublisher.publish(clientId,
                            chatViewRenderer.renderMessage(ChatMessageView.system("We could not process your request right now. Please try again.")));
                    return null;
                });

        return removeEmptyState + renderedUser;
    }
}
