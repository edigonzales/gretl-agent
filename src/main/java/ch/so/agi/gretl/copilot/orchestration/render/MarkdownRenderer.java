package ch.so.agi.gretl.copilot.orchestration.render;

import org.springframework.stereotype.Component;

import java.util.List;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;

@Component
public class MarkdownRenderer {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownRenderer() {
        MutableDataSet options = new MutableDataSet();
        List<Extension> extensions = List.of(TablesExtension.create());
        options.set(Parser.EXTENSIONS, extensions);
        this.parser = Parser.builder(options)
                .extensions(extensions)
                .build();
        this.renderer = HtmlRenderer.builder(options)
                .extensions(extensions)
                .build();
    }

    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }
}
