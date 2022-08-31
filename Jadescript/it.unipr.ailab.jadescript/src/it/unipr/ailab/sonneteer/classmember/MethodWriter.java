package it.unipr.ailab.sonneteer.classmember;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;
import it.unipr.ailab.sonneteer.statement.BlockWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Writher for method implementations.
 * It has a return type, a name, parameters and a body.
 */
public class MethodWriter extends ClassMemberWriter {

    private final String returnType;
    private final String name;
    private final List<ParameterWriter> parameters = new ArrayList<>();
    private BlockWriter body = new BlockWriter();
    private final List<String> throwsDeclarations = new ArrayList<>();


    public MethodWriter(Visibility visibility, boolean isFinal, boolean isStatic,
                        String returnType, String name) {
        super(visibility, isFinal, isStatic);
        this.returnType = returnType;
        this.name = name;
    }

    public MethodWriter addParameter(ParameterWriter parameterPoet) {
        this.parameters.add(parameterPoet);
        return this;
    }

    public MethodWriter addThrows(String exceptionType) {
        this.throwsDeclarations.add(exceptionType);
        return this;
    }


    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        getAnnotations().forEach(s::line);
        getVisibility().writeSonnet(s);
        if (isFinal()) {
            s.spaced("final");
        }
        if (isStatic()) {
            s.spaced("static");
        }
        if(!returnType.isEmpty()){
            s.spaced(returnType);
        }
        s.add(name).add("(");
        for (int i = 0; i < parameters.size(); i++) {
            ParameterWriter p = parameters.get(i);
            p.writeSonnet(s);
            if (i != parameters.size() - 1) {
                s.spaced(",");
            }
        }
        s.spaced(")");

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

        body.writeSonnet(s);
    }

    public void setBody(BlockWriter body) {
        this.body = body;
    }

    public BlockWriter getBody() {
        return body;
    }

    public String getName() {
        return name;
    }

    public List<ParameterWriter> getParameters() {
        return parameters;
    }

    public String getReturnType() {
        return returnType;
    }
}
