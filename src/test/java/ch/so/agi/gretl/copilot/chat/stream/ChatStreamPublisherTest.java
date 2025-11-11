package ch.so.agi.gretl.copilot.chat.stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class ChatStreamPublisherTest {

    @Test
    void publishesPayloadToRegisteredStream() {
        RecordingEmitter emitter = new RecordingEmitter();
        ChatStreamPublisher publisher = new ChatStreamPublisher(() -> emitter);

        SseEmitter registered = publisher.openStream("client-1");
        Assertions.assertThat(registered).isSameAs(emitter);

        publisher.publish("client-1", "<div>payload</div>");

        Assertions.assertThat(emitter.events).hasSize(1);
        Object eventBuilder = emitter.events.get(0);
        Object nameFlag = ReflectionTestUtils.getField(eventBuilder, "hasName");
        Assertions.assertThat(nameFlag).isEqualTo(true);

        Object dataCollection = ReflectionTestUtils.getField(eventBuilder, "dataToSend");
        Assertions.assertThat(dataCollection).isInstanceOf(java.util.Collection.class);

        java.util.List<String> dataLines = ((java.util.Collection<?>) dataCollection).stream()
                .map(item -> ReflectionTestUtils.getField(item, "data"))
                .map(String::valueOf)
                .toList();

        Assertions.assertThat(dataLines).isNotEmpty();
        Assertions.assertThat(dataLines.get(0)).contains("event:message").contains("data:");
        Assertions.assertThat(dataLines).anyMatch(line -> line.contains("payload"));
    }

    private static final class RecordingEmitter extends SseEmitter {
        private final List<SseEventBuilder> events = new CopyOnWriteArrayList<>();

        private RecordingEmitter() {
            super(0L);
        }

        @Override
        public void send(SseEventBuilder event) throws IOException {
            events.add(event);
        }
    }
}
