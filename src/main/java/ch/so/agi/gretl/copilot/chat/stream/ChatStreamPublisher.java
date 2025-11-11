package ch.so.agi.gretl.copilot.chat.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class ChatStreamPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamPublisher.class);

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Supplier<SseEmitter> emitterSupplier;

    public ChatStreamPublisher() {
        this(() -> {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.onTimeout(emitter::complete);
            return emitter;
        });
    }

    ChatStreamPublisher(Supplier<SseEmitter> emitterSupplier) {
        this.emitterSupplier = emitterSupplier;
    }

    public SseEmitter openStream(String clientId) {
        SseEmitter emitter = emitterSupplier.get();
        SseEmitter previous = emitters.put(clientId, emitter);
        if (previous != null) {
            previous.complete();
        }
        emitter.onCompletion(() -> emitters.remove(clientId, emitter));
        emitter.onError(error -> emitters.remove(clientId, emitter));
        return emitter;
    }

    public void publish(String clientId, String payload) {
        if (payload == null || payload.isBlank()) {
            return;
        }

        SseEmitter emitter = emitters.get(clientId);
        if (emitter == null) {
            log.debug("No SSE emitter registered for client {}", clientId);
            return;
        }

        try {
            emitter.send(SseEmitter.event().name("message").data(payload));
        } catch (IOException ex) {
            emitters.remove(clientId, emitter);
            emitter.completeWithError(ex);
            log.warn("Failed to publish SSE payload for client {}", clientId, ex);
        }
    }
}
