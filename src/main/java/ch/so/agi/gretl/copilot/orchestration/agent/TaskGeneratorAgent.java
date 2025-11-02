package ch.so.agi.gretl.copilot.orchestration.agent;

import ch.so.agi.gretl.copilot.orchestration.TaskAgent;
import org.springframework.stereotype.Component;

@Component
public class TaskGeneratorAgent implements TaskAgent {

    // Prepared for future LangChain4j integration:
    // private final ChatModel generatorModel;
    //
    // public TaskGeneratorAgent(@Qualifier("generatorModel") ChatModel generatorModel) {
    //     this.generatorModel = generatorModel;
    // }

    @Override
    public String handle(String userMessage) {
        return "[Mock] Generating a new GRETL task for: " + userMessage;
    }
}
