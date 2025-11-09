package ch.so.agi.gretl.copilot.chat.session;

import ch.so.agi.gretl.copilot.chat.view.ChatMessageView;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class ChatSessionRegistry {

    private final Map<String, PendingResponses> sessions = new ConcurrentHashMap<>();

    public void enqueueResponse(String clientId, ChatMessageView message) {
        sessions.computeIfAbsent(clientId, key -> new PendingResponses()).add(message);
    }

    public List<ChatMessageView> drainResponses(String clientId) {
        PendingResponses responses = sessions.get(clientId);
        if (responses == null) {
            return List.of();
        }

        List<ChatMessageView> drained = responses.drain();
        if (drained.isEmpty()) {
            sessions.remove(clientId, responses);
        }
        return drained;
    }

    private static final class PendingResponses {
        private final Queue<ChatMessageView> queue = new ConcurrentLinkedQueue<>();

        void add(ChatMessageView message) {
            if (message != null) {
                queue.offer(message);
            }
        }

        List<ChatMessageView> drain() {
            List<ChatMessageView> drained = new ArrayList<>();
            ChatMessageView message;
            while ((message = queue.poll()) != null) {
                drained.add(message);
            }
            return drained;
        }
    }
}
