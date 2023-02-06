package it.unipr.ailab.sonneteer.comment;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;

import java.util.ArrayList;
import java.util.List;

public class MultilineCommentWriter
    implements CommentWriter, BlockWriterElement {

    private final List<String> lines = new ArrayList<>();

    public MultilineCommentWriter(String firstLine){
        lines.add(firstLine);
    }

    public MultilineCommentWriter addLine(String l){
        lines.add(l);
        return this;
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        s.line("/* ");
        lines.forEach(l -> s.add(" * ").line(l));
        s.line(" */");
    }


    public List<String> getLines() {
        return lines;
    }
}
