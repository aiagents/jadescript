package it.unipr.ailab.sonneteer.classmember;

import it.unipr.ailab.sonneteer.Annotable;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.Writer;

import java.util.ArrayList;
import java.util.List;

/**
 * Writer for function parameters. Each parameter can be annotated,
 * it has a type and a name.
 */
public class ParameterWriter implements Writer, Annotable {

    private final List<String> annotations = new ArrayList<>();
    private final String type;
    private final String name;

    public ParameterWriter(String type, String name){
        this.type = type;
        this.name = name;
    }

    @Override
    public void addAnnotation(String annotation) {
        annotations.add(annotation);
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        annotations.forEach(s::spaced);
        s.spaced(type).add(name);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
