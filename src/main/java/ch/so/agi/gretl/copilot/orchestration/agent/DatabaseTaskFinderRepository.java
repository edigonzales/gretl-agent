package ch.so.agi.gretl.copilot.orchestration.agent;

import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JDBC-backed implementation that queries the GRETL RAG schema.
 */
@Repository
public class DatabaseTaskFinderRepository implements TaskFinderRepository {

    private static final String LEXICAL_SQL = """
            WITH query AS (
                SELECT plainto_tsquery('simple', :query) AS tsq
            )
            SELECT
                dc.task_name,
                COALESCE(dc.heading, '') AS heading,
                COALESCE(dc.url, '') AS url,
                COALESCE(dc.anchor, '') AS anchor,
                COALESCE(dc.content_text, '') AS content,
                ts_rank_cd(
                    to_tsvector('simple', COALESCE(dc.heading, '') || ' ' || COALESCE(dc.content_text, '')),
                    query.tsq
                ) AS lexical_score
            FROM rag.doc_chunks dc
            CROSS JOIN query
            WHERE dc.content_text IS NOT NULL
              AND query.tsq @@ to_tsvector('simple', COALESCE(dc.heading, '') || ' ' || COALESCE(dc.content_text, ''))
            ORDER BY lexical_score DESC
            LIMIT :limit
            """;

    private static final String SEMANTIC_SQL = """
            WITH query AS (
                SELECT CAST(:embedding AS vector) AS embedding
            )
            SELECT
                dc.task_name,
                COALESCE(dc.heading, '') AS heading,
                COALESCE(dc.url, '') AS url,
                COALESCE(dc.anchor, '') AS anchor,
                COALESCE(dc.content_text, '') AS content,
                0.0::double precision AS lexical_score,
                1.0 / (1.0 + (dc.embedding <=> query.embedding)) AS semantic_score
            FROM rag.doc_chunks dc
            CROSS JOIN query
            WHERE dc.embedding IS NOT NULL
            ORDER BY dc.embedding <=> query.embedding ASC
            LIMIT :limit
            """;

    private static final RowMapper<TaskFinderDocument> LEXICAL_MAPPER = DatabaseTaskFinderRepository::mapLexicalRow;
    private static final RowMapper<TaskFinderDocument> SEMANTIC_MAPPER = DatabaseTaskFinderRepository::mapSemanticRow;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DatabaseTaskFinderRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TaskFinderDocument> searchLexical(String query, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        Map<String, Object> params = Map.of(
                "query", query,
                "limit", limit
        );
        return jdbcTemplate.query(LEXICAL_SQL, params, LEXICAL_MAPPER);
    }

    @Override
    public List<TaskFinderDocument> searchSemantic(double[] embedding, int limit) {
        if (embedding == null || embedding.length == 0) {
            return Collections.emptyList();
        }
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("embedding", toVector(embedding));
        params.addValue("limit", limit);
        return jdbcTemplate.query(SEMANTIC_SQL, params, SEMANTIC_MAPPER);
    }

    private static TaskFinderDocument mapLexicalRow(ResultSet rs, int rowNum) throws SQLException {
        return new TaskFinderDocument(
                rs.getString("task_name"),
                rs.getString("heading"),
                rs.getString("url"),
                rs.getString("anchor"),
                rs.getString("content"),
                rs.getDouble("lexical_score"),
                0.0d
        );
    }

    private static TaskFinderDocument mapSemanticRow(ResultSet rs, int rowNum) throws SQLException {
        return new TaskFinderDocument(
                rs.getString("task_name"),
                rs.getString("heading"),
                rs.getString("url"),
                rs.getString("anchor"),
                rs.getString("content"),
                0.0d,
                rs.getDouble("semantic_score")
        );
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
