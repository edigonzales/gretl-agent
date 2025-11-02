package ch.so.agi.gretl.copilot.chat.ui;

import ch.so.agi.gretl.copilot.chat.view.ChatMessageView;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ChatViewRenderer {
    private static final Logger log = LoggerFactory.getLogger(ChatViewRenderer.class);

    private final TemplateEngine templateEngine;

    public ChatViewRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String renderMessage(ChatMessageView messageView) {
        log.info("ChatViewRenderer renderMessage");
        StringOutput output = new StringOutput();
        templateEngine.render("chat/message.jte", Map.of("message", messageView), output);
        return output.toString();
    }
}
