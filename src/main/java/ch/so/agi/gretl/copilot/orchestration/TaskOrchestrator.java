package ch.so.agi.gretl.copilot.orchestration;

import ch.so.agi.gretl.copilot.chat.ui.ChatViewRenderer;
import ch.so.agi.gretl.copilot.orchestration.agent.TaskExplanationAgent;
import ch.so.agi.gretl.copilot.orchestration.agent.TaskFinderAgent;
import ch.so.agi.gretl.copilot.orchestration.agent.TaskGeneratorAgent;
import dev.langchain4j.model.chat.ChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class TaskOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(TaskOrchestrator.class);

    private final ChatModel classificationModel;
    private final Map<TaskType, TaskAgent> agents;

    public TaskOrchestrator(@Qualifier("classifierModel") ChatModel classificationModel,
                            TaskFinderAgent finderAgent,
                            TaskExplanationAgent explanationAgent,
                            TaskGeneratorAgent generatorAgent) {
        this.classificationModel = classificationModel;
        this.agents = new EnumMap<>(TaskType.class);
        this.agents.put(TaskType.FIND_TASK, finderAgent);
        this.agents.put(TaskType.EXPLAIN_TASK, explanationAgent);
        this.agents.put(TaskType.GENERATE_TASK, generatorAgent);
    }

    public TaskExecutionResult orchestrate(String userMessage) {
        log.info("**** orchestrate");
        TaskType taskType = classify(userMessage);
        TaskAgent agent = agents.get(taskType);
        if (agent == null) {
            throw new IllegalStateException("No agent registered for task type " + taskType);
        }
        String answer = agent.handle(userMessage);
        return new TaskExecutionResult(taskType, answer);
    }

    TaskType classify(String userMessage) {
        String prompt = buildPrompt(userMessage);
        log.info("**** classify");
        String response = classificationModel.chat(prompt);
        log.info("**** response: " + response);
        return TaskType.fromModelResponse(response);
    }

    private String buildPrompt(String userMessage) {
        return "You are a classifier for a GRETL assistant. Decide which specialized agent should handle the user's request. " +
                "Choose exactly one goal from: FIND_TASK, EXPLAIN_TASK, GENERATE_TASK. " +
                "Respond with only the goal name.\n\n" +
                "User request: " + userMessage;
    }
}
