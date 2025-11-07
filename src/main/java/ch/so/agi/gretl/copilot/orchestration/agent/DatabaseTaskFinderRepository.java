package ch.so.agi.gretl.copilot.orchestration.agent;

import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * JDBC-backed implementation that queries the GRETL RAG schema.
 */
@Repository
public class DatabaseTaskFinderRepository implements TaskFinderRepository {

    private static final String LEXICAL_SQL = """
            WITH params AS (
              SELECT lower(:query) AS qtext
            ),
            q_terms AS (
              -- deutsche Stopwörter werden von to_tsvector('german', ...) schon entfernt
              SELECT DISTINCT unnest(tsvector_to_array(to_tsvector('german', (SELECT qtext FROM params)))) AS term
            ),
            q_filtered AS (
              SELECT term
              FROM q_terms
              WHERE length(term) >= 3
            ),
            q_or AS (
              SELECT to_tsquery('german', string_agg(quote_ident(term) || ':*', ' | ')) AS tsq
              FROM q_filtered
            ),
            q_must AS (
              SELECT COALESCE(
                       (SELECT tsq FROM q_or),
                       websearch_to_tsquery('german', (SELECT qtext FROM params))
                     ) AS tsq
            )
            SELECT
              dc.task_name AS taskName,
              COALESCE(dc.heading, '') AS heading,
              COALESCE(dc.url, '') AS url,
              COALESCE(dc.anchor, '') AS anchor,
              COALESCE(dc.content_text, '') AS content,
            ts_rank_cd(
              setweight(to_tsvector('german', COALESCE(dc.heading,'')), 'A') ||
              setweight(to_tsvector('german', COALESCE(dc.content_text,'')), 'C'),
              (SELECT tsq FROM q_must),
              32 -- normalization flag
            ) AS lexicalScore,
              0.0::double precision AS semanticScore
            FROM rag.doc_chunks dc
            WHERE (SELECT tsq FROM q_must) @@ (
              to_tsvector('german', COALESCE(dc.heading,'')) ||
              to_tsvector('german', COALESCE(dc.content_text,''))
            )
            ORDER BY lexicalScore DESC
            LIMIT :limit;
            """;

    private static final String SEMANTIC_SQL = """
            WITH query AS (
                SELECT CAST(:embedding AS vector) AS embedding
            )
            SELECT
                dc.task_name AS taskName,
                COALESCE(dc.heading, '') AS heading,
                COALESCE(dc.url, '') AS url,
                COALESCE(dc.anchor, '') AS anchor,
                COALESCE(dc.content_text, '') AS content,
                0.0::double precision AS lexicalScore,
                1.0 / (1.0 + (dc.embedding <=> query.embedding)) AS semanticScore
            FROM rag.doc_chunks dc
            CROSS JOIN query
            WHERE dc.embedding IS NOT NULL
            ORDER BY dc.embedding <=> query.embedding ASC
            LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public DatabaseTaskFinderRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<TaskFinderDocument> searchLexical(String query, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        return jdbcClient.sql(LEXICAL_SQL)
                .param("query", query)
                .param("limit", limit)
                .query(TaskFinderDocument.class)
                .list();
    }

    @Override
    public List<TaskFinderDocument> searchSemantic(double[] embedding, int limit) {
        if (embedding == null || embedding.length == 0) {
            return Collections.emptyList();
        }
        return jdbcClient.sql(SEMANTIC_SQL)
                .param("embedding", toVector(embedding))
                .param("limit", limit)
                .query(TaskFinderDocument.class)
                .list();
    }

    private static PGobject toVector(double[] embedding) {
        try {
            PGobject vector = new PGobject();
            vector.setType("vector");
            vector.setValue(formatVector(embedding));
            return vector;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to convert embedding to pgvector", e);
        }
    }

    private static String formatVector(double[] embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(String.format(java.util.Locale.US, "%.8f", embedding[i]));
        }
        return builder.append(']').toString();
    }
}
