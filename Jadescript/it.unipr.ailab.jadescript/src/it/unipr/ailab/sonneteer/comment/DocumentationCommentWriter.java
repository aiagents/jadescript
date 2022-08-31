package it.unipr.ailab.sonneteer.comment;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public class DocumentationCommentWriter extends MultilineCommentWriter {

    public DocumentationCommentWriter(String firstLine) {
        super(firstLine);
    }

    public DocumentationCommentWriter addAuthor(String name){
        super.addLine("@author "+name);
        return this;
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        s.line(   "/**");
        getLines().forEach(l -> s.add(" * ").line(l));
        s.line(   " */");
    }
}
