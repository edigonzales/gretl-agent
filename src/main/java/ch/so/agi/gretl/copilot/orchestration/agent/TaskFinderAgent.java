package ch.so.agi.gretl.copilot.orchestration.agent;

import ch.so.agi.gretl.copilot.orchestration.TaskAgent;
import org.springframework.stereotype.Component;

@Component
public class TaskFinderAgent implements TaskAgent {

    // Prepared for future LangChain4j integration:
    // private final ChatModel finderModel;
    //
    // public TaskFinderAgent(@Qualifier("finderModel") ChatModel finderModel) {
    //     this.finderModel = finderModel;
    // }

    @Override
    public String handle(String userMessage) {
        return "[Mock] Searching GRETL tasks related to: " + userMessage;
    }
}
