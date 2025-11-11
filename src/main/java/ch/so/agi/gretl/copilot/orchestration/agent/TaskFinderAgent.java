package ch.so.agi.gretl.copilot.orchestration.agent;

import ch.so.agi.gretl.copilot.orchestration.TaskAgent;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Agent that turns a free-form user question into a ranked list of GRETL tasks.
 * <p>
 * The agent performs a hybrid retrieval across the GRETL RAG store: a BM25-style full text
 * search delivers precise lexical matches, while a semantic search (pgvector cosine similarity)
 * surfaces conceptually related documentation chunks. Both signals are normalized and fused
 * into a single score so that the user receives a concise shortlist of candidates.
 */
@Component
public class TaskFinderAgent implements TaskAgent {

    private static final Logger log = LoggerFactory.getLogger(TaskFinderAgent.class);

    private static final int CANDIDATE_LIMIT = 12;
    private static final int RESULT_LIMIT = 5;
    private static final double LEXICAL_WEIGHT = 0.6d;
    private static final double SEMANTIC_WEIGHT = 0.4d;

    private final TaskFinderRepository repository;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    public TaskFinderAgent(TaskFinderRepository repository,
                           @Qualifier("finderEmbeddingModel") ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        this.repository = repository;
        this.embeddingModelProvider = embeddingModelProvider;
    }

    /**
     * Executes a hybrid retrieval against the GRETL documentation corpus and formats a textual answer.
     *
     * @param userMessage the raw user question
     * @return a ranked shortlist of GRETL tasks including source references
     */
    @Override
    public String handle(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "Bitte beschreibe dein GRETL-Problem etwas genauer, damit ich passende Tasks suchen kann.";
        }

        List<TaskFinderDocument> lexicalMatches = repository.searchLexical(userMessage, CANDIDATE_LIMIT);
        Optional<double[]> queryEmbedding = embed(userMessage);
        List<TaskFinderDocument> semanticMatches = queryEmbedding
                .map(embedding -> repository.searchSemantic(embedding, CANDIDATE_LIMIT))
                .orElseGet(List::of);

        List<RankedDocument> ranked = rank(lexicalMatches, semanticMatches);
        //log.info(ranked.toString());
        for (RankedDocument doc : ranked) {
            log.info(doc.document.taskName() + " --- " + doc.combinedScore + " --- " + doc.lexicalContribution + " --- " + doc.semanticContribution);
        }
        
        if (ranked.isEmpty()) {
            return "Ich konnte in der GRETL-Dokumentation keine passenden Tasks zu deiner Anfrage finden.";
        }
        boolean semanticUsed = queryEmbedding.isPresent() && !semanticMatches.isEmpty();
        return formatResponse(userMessage, ranked, semanticUsed);
    }

    private List<RankedDocument> rank(List<TaskFinderDocument> lexicalMatches, List<TaskFinderDocument> semanticMatches) {
        Map<String, AggregatedScore> aggregation = new HashMap<>();

        double maxLexical = lexicalMatches.stream()
                .mapToDouble(TaskFinderDocument::lexicalScore)
                .filter(score -> score > 0)
                .max()
                .orElse(0.0d);
        double maxSemantic = semanticMatches.stream()
                .mapToDouble(TaskFinderDocument::semanticScore)
                .filter(score -> score > 0)
                .max()
                .orElse(0.0d);

        for (TaskFinderDocument document : lexicalMatches) {
            double normalizedLexical = maxLexical > 0.0d ? document.lexicalScore() / maxLexical : 0.0d;
            AggregatedScore score = aggregation.computeIfAbsent(document.compoundKey(),
                    key -> new AggregatedScore(document));
            score.lexical = Math.max(score.lexical, normalizedLexical);
        }

        for (TaskFinderDocument document : semanticMatches) {
            double normalizedSemantic = maxSemantic > 0.0d ? document.semanticScore() / maxSemantic : 0.0d;
            AggregatedScore score = aggregation.computeIfAbsent(document.compoundKey(),
                    key -> new AggregatedScore(document));
            score.semantic = Math.max(score.semantic, normalizedSemantic);
        }

        List<RankedDocument> ranked = new ArrayList<>(aggregation.size());
        for (AggregatedScore score : aggregation.values()) {
            double combined = LEXICAL_WEIGHT * score.lexical + SEMANTIC_WEIGHT * score.semantic;
            ranked.add(new RankedDocument(score.document, score.lexical, score.semantic, combined));
        }

        ranked.sort(Comparator.comparing(RankedDocument::combinedScore).reversed());
        if (ranked.size() > RESULT_LIMIT) {
            return ranked.subList(0, RESULT_LIMIT);
        }
        return ranked;
    }

    private Optional<double[]> embed(String userMessage) {
        EmbeddingModel model = embeddingModelProvider.getIfAvailable();
        if (model == null) {
            return Optional.empty();
        }
        try {
            Response<List<Embedding>> response = model.embedAll(List.of(TextSegment.from(userMessage)));
            List<Embedding> embeddings = response.content();
            if (embeddings == null || embeddings.isEmpty()) {
                return Optional.empty();
            }
            float[] rawVector = embeddings.getFirst().vector();
            if (rawVector.length == 0) {
                return Optional.empty();
            }
            double[] vector = new double[rawVector.length];
            for (int i = 0; i < rawVector.length; i++) {
                vector[i] = rawVector[i];
            }
            return Optional.of(vector);
        } catch (Exception ex) {
            log.warn("Failed to embed finder query. Falling back to lexical-only search.", ex);
            return Optional.empty();
        }
    }

    private String formatResponse(String userMessage, List<RankedDocument> ranked, boolean semanticUsed) {
        StringBuilder builder = new StringBuilder();
        builder.append("Ich habe nach passenden GRETL-Tasks für \"")
                .append(userMessage.trim())
                .append("\" gesucht und folgende Treffer gefunden:\n\n");

        int index = 1;
        for (RankedDocument document : ranked) {
            TaskFinderDocument source = document.document();
            builder.append(index++)
                    .append(". **")
                    .append(source.taskName());
            if (!source.heading().isBlank()) {
                builder.append(" – ").append(source.heading());
            }
            builder.append("**\n");
            if (!source.content().isBlank()) {
                builder.append("   ")
                        .append(extractSnippet(source.content()))
                        .append('\n');
            }
            if (!source.url().isBlank()) {
                builder.append("   Quelle: ")
                        .append(source.url());
                if (!source.anchor().isBlank()) {
                    builder.append('#').append(source.anchor());
                }
                builder.append('\n');
            }
        }

        builder.append('\n')
                .append(String.format(Locale.GERMANY,
                        "Gewichtung: BM25 %.0f%% / Semantik %.0f%%",
                        LEXICAL_WEIGHT * 100,
                        SEMANTIC_WEIGHT * 100));
        if (!semanticUsed) {
            builder.append(" (Semantik deaktiviert – kein Embedding-Modell verfügbar)");
        }
        return builder.toString();
    }

    private String extractSnippet(String content) {
        String sanitized = content.replaceAll("\s+", " ").trim();
        if (sanitized.length() <= 220) {
            return sanitized;
        }
        return sanitized.substring(0, 217) + "...";
    }

    private static final class AggregatedScore {
        private final TaskFinderDocument document;
        private double lexical;
        private double semantic;

        private AggregatedScore(TaskFinderDocument document) {
            this.document = document;
        }
    }

    private record RankedDocument(TaskFinderDocument document,
                                  double lexicalContribution,
                                  double semanticContribution,
                                  double combinedScore) {
    }
}
