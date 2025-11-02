package ch.so.agi.gretl.copilot.chat.view;

public enum ChatMessageAuthor {
    USER("You", "message-user"),
    ASSISTANT("GRETL Copilot", "message-assistant"),
    SYSTEM("System", "message-system");

    private final String displayName;
    private final String cssClass;

    ChatMessageAuthor(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String displayName() {
        return displayName;
    }

    public String cssClass() {
        return cssClass;
    }
}
