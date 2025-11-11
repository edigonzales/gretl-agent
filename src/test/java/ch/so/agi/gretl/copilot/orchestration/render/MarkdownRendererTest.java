package ch.so.agi.gretl.copilot.orchestration.render;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownRendererTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();

    @Test
    void rendersSimpleMarkdownToHtml() {
        String html = renderer.render("# Titel\n\nEine **fette** Aussage mit Liste:\n- Punkt A\n- Punkt B");

        assertThat(html).contains("<h1>Titel</h1>");
        assertThat(html).contains("<strong>fette</strong>");
        assertThat(html).contains("<li>Punkt A</li>");
        assertThat(html).contains("<li>Punkt B</li>");
    }

    @Test
    void supportsTables() {
        String markdown = """
                | Kopf | Wert |
                | ---- | ---- |
                | A | 1 |
                """;

        String html = renderer.render(markdown);

        assertThat(html).contains("<table>");
        assertThat(html).contains("<th>Kopf</th>");
        assertThat(html).contains("<td>1</td>");
    }

    @Test
    void returnsEmptyStringForBlankInput() {
        assertThat(renderer.render("   ")).isEmpty();
        assertThat(renderer.render(null)).isEmpty();
    }
}
