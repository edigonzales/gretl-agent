package ch.so.agi.gretl.copilot.orchestration;

import ch.so.agi.gretl.copilot.chat.ui.ChatViewRenderer;
import ch.so.agi.gretl.copilot.orchestration.agent.TaskExplanationAgent;
import ch.so.agi.gretl.copilot.orchestration.agent.TaskFinderAgent;
import ch.so.agi.gretl.copilot.orchestration.agent.TaskGeneratorAgent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
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
        log.info("agent: " + agent);
        if (agent == null) {
            throw new IllegalStateException("No agent registered for task type " + taskType);
        }
        String answer = agent.handle(userMessage);
        return new TaskExecutionResult(taskType, answer);
    }

    TaskType classify(String userMessage) {
//        String prompt = buildPrompt(userMessage);
        log.info("**** classify");
//        String response = classificationModel.chat(prompt);
        
        String systemPrompt = """
Du bist ein Klassifizierer für einen GRETL-Assistenten. GRETL ist ein Gradle-Plugin für (spatial) ETL.
Sprache der Eingabe ist Deutsch. Gib als Antwort genau ein Wort (ohne Punkt, Anführungszeichen oder weitere Zeichen) und keine Begründung.

Mögliche Antworten (englisch, GROSSBUCHSTABEN):
- FIND_TASK – wenn der/die Benutzer:in ein Problem beschreibt und wissen will, welcher GRETL-Task dafür passt.
- EXPLAIN_TASK – wenn der/die Benutzer:in einen konkreten Task erklärt haben möchte (Funktionsumfang, Parameter, Verhalten).
- GENERATE_TASK – wenn der/die Benutzer:in Beispiele/Code für einen Task will (inkl. eigenen Inputs).

Tie-Breaker (falls mehrdeutig): GENERATE_TASK > EXPLAIN_TASK > FIND_TASK.

Nur diese vier Tokens sind erlaubt: FIND_TASK, EXPLAIN_TASK, GENERATE_TASK, OTHER. Nutze OTHER, wenn keine der drei Kategorien passt.

Beispiele (nur zur Orientierung):
- "Ich muss eine INTERLIS-Datei validieren. Welchen Task...?" → FIND_TASK
- "Erkläre mir den Task ilivalidator." → EXPLAIN_TASK
- "Mach mir ein Beispiel für ilivalidator mit Datei fubar.xtf." → GENERATE_TASK
- Wie installiere ich Gradle?" → OTHER

Antworte immer nur mit einem der vier Wörter.
                """;
        
        ChatResponse response = classificationModel.chat(List.of(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userMessage)
            ));
        
        
        log.info("**** response: " + response);
        return TaskType.fromModelResponse(response.aiMessage().text());
    }

    private String buildPrompt(String userMessage) {
        return "You are a classifier for a GRETL assistant. Decide which specialized agent should handle the user's request. " +
                "Choose exactly one goal from: FIND_TASK, EXPLAIN_TASK, GENERATE_TASK. " +
                "Respond with only the goal name.\n\n" +
                "User request: " + userMessage;
    }
}
