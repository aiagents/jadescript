package it.unipr.ailab.sonneteer.classmember;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Writer for method signatures (interface method declarations).
 */
public class MethodSignatureWriter extends ClassMemberWriter {

    private final String type;
    private final String name;
    private final List<ParameterWriter> parameters = new ArrayList<>();
    private final List<String> throwsDeclarations = new ArrayList<>();

    public MethodSignatureWriter(String type, String name) {
        super(Visibility.PACKAGE, false, false);
        this.type = type;
        this.name = name;
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        s.spaced(type).add(name).add("(");
        for (int i = 0; i < parameters.size(); i++) {
            ParameterWriter p = parameters.get(i);
            p.writeSonnet(s);
            if (i != parameters.size() - 1) {
                s.spaced(",");
            }
        }
        s.add(")");
        if (!throwsDeclarations.isEmpty()) {
            s.spaced("throws");
            for (int i = 0; i < throwsDeclarations.size(); i++) {
                s.add(throwsDeclarations.get(i));
                if (i != throwsDeclarations.size() - 1) {
                    s.spaced(",");
                }else{
                    s.add(" ");
                }
            }
        }
        s.line(";");

    }

    public MethodSignatureWriter addParameter(ParameterWriter parameterPoet) {
        parameters.add(parameterPoet);
        return this;
    }

    public MethodSignatureWriter addThrows(String exceptionType) {
        this.throwsDeclarations.add(exceptionType);
        return this;
    }

}
