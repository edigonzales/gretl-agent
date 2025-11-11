package ch.so.agi.gretl.copilot.orchestration.agent;

import ch.so.agi.gretl.copilot.orchestration.render.MarkdownRenderer;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskFinderAgentTest {

    @Test
    void combinesLexicalAndSemanticMatches() {
        StubRepository repository = new StubRepository();
        repository.lexicalResults = List.of(
                new TaskFinderDocument("task-a", "Heading A", "https://example.com/a", "section-a",
                        "Der Task liest INTERLIS-Daten und importiert sie in eine Datenbank.", 0.8d, 0.0d),
                new TaskFinderDocument("task-b", "Heading B", "https://example.com/b", "section-b",
                        "Validierung und Protokollierung für Datenimporte.", 0.6d, 0.0d)
        );
        repository.semanticResults = List.of(
                new TaskFinderDocument("task-b", "Heading B", "https://example.com/b", "section-b",
                        "Validierung und Protokollierung für Datenimporte.", 0.0d, 0.9d),
                new TaskFinderDocument("task-c", "Heading C", "https://example.com/c", "section-c",
                        "Task zum Publizieren von Dateien.", 0.0d, 0.7d)
        );

        ObjectProvider<EmbeddingModel> provider = providerReturning(mockEmbeddingModel());

        TaskFinderAgent agent = new TaskFinderAgent(repository, provider, new MarkdownRenderer());

        String answer = agent.handle("Wie importiere ich INTERLIS-Daten?");

        assertThat(answer).contains("<strong>task-b – Heading B</strong>");
        assertThat(answer.indexOf("task-b – Heading B")).isLessThan(answer.indexOf("task-a – Heading A"));
        assertThat(answer).contains("Gewichtung: BM25 60% / Semantik 40%");
        assertThat(repository.semanticInvocations).isEqualTo(1);
        assertThat(repository.lastEmbedding).isNotEmpty();
    }

    @Test
    void fallsBackToLexicalIfNoEmbeddingModelAvailable() {
        StubRepository repository = new StubRepository();
        repository.lexicalResults = List.of(
                new TaskFinderDocument("task-a", "Heading A", "https://example.com/a", "section-a",
                        "Der Task liest INTERLIS-Daten und importiert sie in eine Datenbank.", 0.4d, 0.0d)
        );

        ObjectProvider<EmbeddingModel> provider = providerReturning(null);
        TaskFinderAgent agent = new TaskFinderAgent(repository, provider, new MarkdownRenderer());

        String answer = agent.handle("Wie importiere ich INTERLIS-Daten?");

        assertThat(answer).contains("task-a");
        assertThat(answer).contains("Semantik deaktiviert");
        assertThat(repository.semanticInvocations).isZero();
    }

    @Test
    void returnsHelpfulMessageWhenNoMatchesAreFound() {
        StubRepository repository = new StubRepository();
        ObjectProvider<EmbeddingModel> provider = providerReturning(null);
        TaskFinderAgent agent = new TaskFinderAgent(repository, provider, new MarkdownRenderer());

        String answer = agent.handle("Unbekannter Task");

        assertThat(answer).contains("keine passenden Tasks");
    }

    private static ObjectProvider<EmbeddingModel> providerReturning(EmbeddingModel model) {
        ObjectProvider<EmbeddingModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(model);
        return provider;
    }

    private static EmbeddingModel mockEmbeddingModel() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        Embedding embedding = new Embedding(new float[]{0.12f, 0.23f});
        when(model.embedAll(anyList())).thenReturn(Response.from(List.of(embedding)));
        return model;
    }

    private static final class StubRepository implements TaskFinderRepository {

        private List<TaskFinderDocument> lexicalResults = List.of();
        private List<TaskFinderDocument> semanticResults = List.of();
        private int semanticInvocations;
        private double[] lastEmbedding = new double[0];

        @Override
        public List<TaskFinderDocument> searchLexical(String query, int limit) {
            return lexicalResults;
        }

        @Override
        public List<TaskFinderDocument> searchSemantic(double[] embedding, int limit) {
            this.semanticInvocations++;
            this.lastEmbedding = embedding;
            return semanticResults;
        }
    }
}
