package ch.so.agi.gretl.copilot.chat.ui;

import ch.so.agi.gretl.copilot.chat.stream.ChatStreamPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ui/chat")
public class ChatStreamController {

    private final ChatStreamPublisher streamPublisher;

    public ChatStreamController(ChatStreamPublisher streamPublisher) {
        this.streamPublisher = streamPublisher;
    }

    @GetMapping(path = "/stream/{clientId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@PathVariable String clientId) {
        return streamPublisher.stream(clientId);
    }
}
