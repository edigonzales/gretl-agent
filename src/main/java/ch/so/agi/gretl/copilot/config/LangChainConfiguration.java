package ch.so.agi.gretl.copilot.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.DisabledEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LangChainConfiguration {

    @Bean("classifierModel")
    @ConditionalOnProperty(name = "openai.api-key")
    public ChatModel classifierChatModel(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.classifier-model:gpt-4o-mini}") String modelName) {
        System.out.println("******** openai");
        return createOpenAiModel(apiKey, modelName);
    }

    @Bean("finderModel")
    @ConditionalOnProperty(name = "openai.api-key")
    public ChatModel finderChatModel(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.finder-model:gpt-4o-mini}") String modelName) {
        return createOpenAiModel(apiKey, modelName);
    }

    @Bean("finderEmbeddingModel")
    @ConditionalOnProperty(name = "openai.api-key")
    public EmbeddingModel finderEmbeddingModel(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.embedding-model:text-embedding-3-large}") String modelName) {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
//                .logRequests(true)
//                .logResponses(true)
                .build();
    }

    @Bean("explanationModel")
    @ConditionalOnProperty(name = "openai.api-key")
    public ChatModel explanationChatModel(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.explanation-model:gpt-4o-mini}") String modelName) {
        return createOpenAiModel(apiKey, modelName);
    }

    @Bean("generatorModel")
    @ConditionalOnProperty(name = "openai.api-key")
    public ChatModel generatorChatModel(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.generator-model:gpt-4o-mini}") String modelName) {
        return createOpenAiModel(apiKey, modelName);
    }

    @Bean(name = "classifierModel")
    @ConditionalOnMissingBean(name = "classifierModel")
    public ChatModel classifierFallbackModel() {
        System.out.println("******** classifier fake");
        return new KeywordClassifierChatModel();
    }

    @Bean(name = "finderModel")
    @ConditionalOnMissingBean(name = "finderModel")
    public ChatModel finderFallbackModel() {
        return new PrefixedResponseChatModel("[Mock Finder]");
    }

    @Bean(name = "finderEmbeddingModel")
    @ConditionalOnMissingBean(name = "finderEmbeddingModel")
    public EmbeddingModel finderEmbeddingFallbackModel() {
        return new DisabledEmbeddingModel();
    }

    @Bean(name = "explanationModel")
    @ConditionalOnMissingBean(name = "explanationModel")
    public ChatModel explanationFallbackModel() {
        return new PrefixedResponseChatModel("[Mock Explanation]");
    }

    @Bean(name = "generatorModel")
    @ConditionalOnMissingBean(name = "generatorModel")
    public ChatModel generatorFallbackModel() {
        return new PrefixedResponseChatModel("[Mock Generation]");
    }

    private ChatModel createOpenAiModel(String apiKey, String modelName) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
//                .logRequests(true)
//                .logResponses(true)
                .build();
    }

    private static class KeywordClassifierChatModel implements ChatModel {

        @Override
        public ChatResponse doChat(ChatRequest request) {
            String prompt = extractPrompt(request.messages());
            String classification = classify(prompt);
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(classification))
                    .build();
        }

        private String extractPrompt(List<ChatMessage> messages) {
            if (messages == null || messages.isEmpty()) {
                return "";
            }
            ChatMessage lastMessage = messages.get(messages.size() - 1);
            if (lastMessage instanceof UserMessage userMessage && userMessage.hasSingleText()) {
                return userMessage.singleText();
            }
            if (lastMessage instanceof AiMessage aiMessage) {
                return aiMessage.text();
            }
            return lastMessage.toString();
        }

        private String classify(String prompt) {
            String sanitized = prompt == null ? "" : prompt.toLowerCase();
            if (sanitized.contains("explain") || sanitized.contains("erkla")) {
                return "EXPLAIN_TASK";
            }
            if (sanitized.contains("generate") || sanitized.contains("erstellen") || sanitized.contains("create")) {
                return "GENERATE_TASK";
            }
            return "FIND_TASK";
        }
    }

    private static class PrefixedResponseChatModel implements ChatModel {

        private final String prefix;

        private PrefixedResponseChatModel(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            List<ChatMessage> messages = request.messages();
            String prompt = extractPrompt(messages);
            String answer = (prefix + " " + prompt).trim();
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(answer))
                    .build();
        }

        private String extractPrompt(List<ChatMessage> messages) {
            if (messages == null || messages.isEmpty()) {
                return "";
            }
            ChatMessage lastMessage = messages.get(messages.size() - 1);
            if (lastMessage instanceof UserMessage userMessage && userMessage.hasSingleText()) {
                return userMessage.singleText();
            }
            if (lastMessage instanceof AiMessage aiMessage) {
                return aiMessage.text();
            }
            return lastMessage.toString();
        }
    }

}
