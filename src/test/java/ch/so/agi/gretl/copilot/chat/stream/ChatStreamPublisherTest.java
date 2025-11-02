package ch.so.agi.gretl.copilot.chat.stream;

import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.test.StepVerifier;

class ChatStreamPublisherTest {

    @Test
    void publishesPayloadToRegisteredStream() {
        ChatStreamPublisher publisher = new ChatStreamPublisher();

        StepVerifier.create(publisher.stream("client-1")
                        .map(ServerSentEvent::data)
                        .filter(payload -> payload != null && !payload.isBlank())
                        .take(1))
                .then(() -> publisher.publish("client-1", "<div>payload</div>"))
                .expectNext("<div>payload</div>")
                .verifyComplete();
    }
}
