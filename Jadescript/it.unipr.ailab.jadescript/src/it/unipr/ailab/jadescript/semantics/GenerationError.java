package it.unipr.ailab.jadescript.semantics;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.StatementWriter;

/**
 * Created on 13/03/18.
 */
public class GenerationError extends RuntimeException {

    private final Throwable e;


    public GenerationError(Throwable e) {
        super(e);
        this.e = e;
    }


    public static String stackTraceToString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
            sb.append(stackTraceElement.toString()).append("\n");
        }
        return sb.toString();
    }


    public StatementWriter buildThrowStatementWriter() {
        return new StatementWriter() {
            @Override
            public void writeSonnet(SourceCodeBuilder s) {
                s.line().add("throw new RuntimeException(\"An error occurred " +
                    "during compilation:\\n\" + ");
                s.indent();
                String[] split = (e.getMessage() + "\n" + stackTraceToString(e))
                    .split("\n");
                for (int i = 0; i < split.length; i++) {
                    String messageLine = split[i];
                    s.line().add("\"").add(messageLine).add("\\n\"");
                    if (i < split.length - 1) {
                        s.add(" + ");
                    }
                }
                s.dedent().line().add(");").line();
            }
        };
    }


}
