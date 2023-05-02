package it.unipr.ailab.sonneteer.qualifiers;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.Writer;

public enum Visibility implements Writer {
    PRIVATE("private"),
    PUBLIC("public"),
    PACKAGE(""),
    PROTECTED("protected");

    private final String text;


    Visibility(String s) {
        this.text = s;
    }


    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        s.spaced(text);
    }
}
