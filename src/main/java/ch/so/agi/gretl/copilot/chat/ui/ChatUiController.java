package ch.so.agi.gretl.copilot.chat.ui;

import ch.so.agi.gretl.copilot.chat.ChatService;
import ch.so.agi.gretl.copilot.chat.dto.ChatRequest;
import ch.so.agi.gretl.copilot.chat.view.ChatMessageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping("/ui/chat")
public class ChatUiController {
    private static final Logger log = LoggerFactory.getLogger(ChatUiController.class);

    private final ChatService chatService;
    private final ChatViewRenderer chatViewRenderer;
    private final Map<String, Sinks.Many<String>> clientStreams = new ConcurrentHashMap<>();

    public ChatUiController(ChatService chatService,
                            ChatViewRenderer chatViewRenderer) {
        this.chatService = chatService;
        this.chatViewRenderer = chatViewRenderer;
    }

    @GetMapping
    public String chatPage(Model model) {
        model.addAttribute("clientId", UUID.randomUUID().toString());
        return "chat/index";
    }

    @GetMapping(path = "/stream/{clientId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<ServerSentEvent<String>> stream(@PathVariable("clientId") String clientId) {
        Sinks.Many<String> sink = Sinks.many().replay().limit(1);
        Sinks.Many<String> previous = clientStreams.put(clientId, sink);
        if (previous != null) {
            previous.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
        }

        return sink.asFlux()
                .map(payload -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(payload)
                        .build())
                .doFinally(signalType -> clientStreams.remove(clientId, sink));
    }

    @PostMapping(path = "/messages", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public Mono<String> postMessage(@RequestParam("message") String message,
                                    @RequestParam("clientId") String clientId) {
        String sanitized = message == null ? "" : message.trim();
        if (sanitized.isEmpty()) {
            return Mono.just("");
        }

        log.debug("Submitting message for client {}", clientId);

        ChatMessageView userMessage = ChatMessageView.user(sanitized);
        String renderedUser = chatViewRenderer.renderMessage(userMessage);
        String removeEmptyState = "<div id=\"empty-state\" hx-swap-oob=\"delete\"></div>";

        Mono.fromCallable(() -> chatService.respond(new ChatRequest(sanitized)))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> ChatMessageView.assistant(response.answer(), response.goal()))
                .map(chatViewRenderer::renderMessage)
                .doOnNext(rendered -> publishToClient(clientId, rendered))
                .doOnError(error -> publishToClient(clientId,
                        chatViewRenderer.renderMessage(ChatMessageView.system("We could not process your request right now. Please try again."))))
                .onErrorResume(error -> Mono.empty())
                .subscribe();

        return Mono.just(removeEmptyState + renderedUser);
    }

    private void publishToClient(String clientId, String payload) {
        Sinks.Many<String> sink = clientStreams.get(clientId);
        if (sink != null) {
            sink.emitNext(payload, Sinks.EmitFailureHandler.FAIL_FAST);
        }
    }
}
