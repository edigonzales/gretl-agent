package ch.so.agi.gretl.copilot.orchestration.agent;

import java.util.List;

/**
 * Repository abstraction encapsulating the SQL used for hybrid GRETL task retrieval.
 */
public interface TaskFinderRepository {

    /**
     * Runs a BM25-style full text search over {@code rag.doc_chunks}.
     *
     * @param query the user input
     * @param limit maximum number of rows returned by the database
     * @return lexical candidates ordered by descending relevance
     */
    List<TaskFinderDocument> searchLexical(String query, int limit);

    /**
     * Runs a vector similarity search over {@code rag.doc_chunks} using the pgvector index.
     *
     * @param embedding the embedding of the user input
     * @param limit maximum number of rows returned by the database
     * @return semantic candidates ordered by descending cosine similarity
     */
    List<TaskFinderDocument> searchSemantic(double[] embedding, int limit);
}
