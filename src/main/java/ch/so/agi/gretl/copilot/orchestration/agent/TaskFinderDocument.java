package ch.so.agi.gretl.copilot.orchestration.agent;

import java.util.Objects;

/**
 * Represents a document fragment retrieved from the GRETL RAG store during task search.
 * <p>
 * The record captures both lexical and semantic scores so that the {@link TaskFinderAgent}
 * can fuse them into a hybrid ranking.
 */
public record TaskFinderDocument(
        String taskName,
        String heading,
        String url,
        String anchor,
        String content,
        Double lexicalScore,
        Double semanticScore) {

    public TaskFinderDocument {
        taskName = Objects.requireNonNull(taskName, "taskName must not be null");
        heading = Objects.requireNonNull(heading, "heading must not be null");
        url = Objects.requireNonNull(url, "url must not be null");
        anchor = Objects.requireNonNull(anchor, "anchor must not be null");
        content = Objects.requireNonNull(content, "content must not be null");
        lexicalScore = Objects.requireNonNull(lexicalScore, "lexicalScore must not be null");
        semanticScore = Objects.requireNonNull(semanticScore, "semanticScore must not be null");

        taskName = taskName.isBlank() ? "" : taskName.trim();
        heading = heading.trim();
        url = url.trim();
        anchor = anchor.trim();
        content = content.trim();
    }

    /**
     * Returns a key that uniquely identifies the fragment within the documentation.
     * It is used to merge lexical and semantic hits referring to the same chunk.
     */
    public String compoundKey() {
        return (taskName + "|" + url + "|" + anchor).toLowerCase();
    }
}
