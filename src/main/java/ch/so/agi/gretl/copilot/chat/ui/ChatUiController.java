package ch.so.agi.gretl.copilot.chat.ui;

import ch.so.agi.gretl.copilot.chat.ChatService;
import ch.so.agi.gretl.copilot.chat.dto.ChatRequest;
import ch.so.agi.gretl.copilot.chat.session.ChatSessionRegistry;
import ch.so.agi.gretl.copilot.chat.view.ChatMessageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ui/chat")
public class ChatUiController {
    private static final Logger log = LoggerFactory.getLogger(ChatUiController.class);

    private final ChatService chatService;
    private final ChatViewRenderer chatViewRenderer;
    private final ChatSessionRegistry sessionRegistry;

    public ChatUiController(ChatService chatService,
                            ChatViewRenderer chatViewRenderer,
                            ChatSessionRegistry sessionRegistry) {
        this.chatService = chatService;
        this.chatViewRenderer = chatViewRenderer;
        this.sessionRegistry = sessionRegistry;
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

        log.debug("Submitting message for client {}", clientId);

        ChatMessageView userMessage = ChatMessageView.user(sanitized);
        String renderedUser = chatViewRenderer.renderMessage(userMessage);
        String removeEmptyState = "<div id=\"empty-state\" hx-swap-oob=\"delete\"></div>";

        CompletableFuture
                .supplyAsync(() -> chatService.respond(new ChatRequest(sanitized)))
                .thenApply(response -> ChatMessageView.assistant(response.answer(), response.goal()))
                .exceptionally(error -> {
                    log.warn("Failed to process chat request for client {}", clientId, error);
                    return ChatMessageView.system("We could not process your request right now. Please try again.");
                })
                .thenAccept(messageView -> sessionRegistry.enqueueResponse(clientId, messageView));

        return removeEmptyState + renderedUser;
    }

    @GetMapping(path = "/messages/poll", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String pollMessages(@RequestParam("clientId") String clientId) {
        List<ChatMessageView> responses = sessionRegistry.drainResponses(clientId);
        if (responses.isEmpty()) {
            return "";
        }

        return responses.stream()
                .map(chatViewRenderer::renderMessage)
                .collect(Collectors.joining());
    }
}
