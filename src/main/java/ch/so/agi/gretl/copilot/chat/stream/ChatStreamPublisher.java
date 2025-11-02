package ch.so.agi.gretl.copilot.chat.stream;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatStreamPublisher {

    private final Map<String, Sinks.Many<String>> sinks = new ConcurrentHashMap<>();

    public Flux<ServerSentEvent<String>> stream(String clientId) {
        Sinks.Many<String> sink = Sinks.many().replay().limit(1);
        Sinks.Many<String> previous = sinks.put(clientId, sink);
        if (previous != null) {
            previous.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
        }
        return sink.asFlux()
                .map(payload -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(payload)
                        .build())
                .doFinally(signalType -> sinks.remove(clientId, sink));
    }

    public void publish(String clientId, String payload) {
        Sinks.Many<String> sink = sinks.get(clientId);
        if (sink != null) {
            sink.emitNext(payload, Sinks.EmitFailureHandler.FAIL_FAST);
        }
    }
}
