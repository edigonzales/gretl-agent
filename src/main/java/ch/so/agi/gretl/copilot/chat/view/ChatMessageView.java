package ch.so.agi.gretl.copilot.chat.view;

import ch.so.agi.gretl.copilot.orchestration.TaskType;

public record ChatMessageView(ChatMessageAuthor author, String content, String detail) {

    public static ChatMessageView user(String content) {
        return new ChatMessageView(ChatMessageAuthor.USER, content, null);
    }

    public static ChatMessageView assistant(String content, TaskType goal) {
        String detail = goal == null ? null : goal.name();
        return new ChatMessageView(ChatMessageAuthor.ASSISTANT, content, detail);
    }

    public static ChatMessageView system(String content) {
        return new ChatMessageView(ChatMessageAuthor.SYSTEM, content, null);
    }

    public String cssClass() {
        return "message " + author.cssClass();
    }

    public String header() {
        if (detail == null || detail.isBlank()) {
            return author.displayName();
        }
        return author.displayName() + " • " + detail;
    }
}
