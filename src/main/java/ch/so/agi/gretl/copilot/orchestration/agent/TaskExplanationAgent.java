package ch.so.agi.gretl.copilot.orchestration.agent;

import ch.so.agi.gretl.copilot.orchestration.TaskAgent;
import org.springframework.stereotype.Component;

@Component
public class TaskExplanationAgent implements TaskAgent {

    // Prepared for future LangChain4j integration:
    // private final ChatModel explanationModel;
    //
    // public TaskExplanationAgent(@Qualifier("explanationModel") ChatModel explanationModel) {
    //     this.explanationModel = explanationModel;
    // }

    @Override
    public String handle(String userMessage) {
        return "[Mock] Explaining the requested GRETL task: " + userMessage;
    }
}
