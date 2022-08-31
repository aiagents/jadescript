package it.unipr.ailab.sonneteer.type;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.classmember.ClassMemberWriter;
import it.unipr.ailab.sonneteer.classmember.FieldWriter;
import it.unipr.ailab.sonneteer.classmember.MethodSignatureWriter;
import it.unipr.ailab.sonneteer.classmember.MethodWriter;
import it.unipr.ailab.sonneteer.comment.CommentWriter;
import it.unipr.ailab.sonneteer.expression.SimpleExpressionWriter;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;

import java.util.ArrayList;
import java.util.List;

public class InterfaceDeclarationWriter extends TypeDeclarationWriter {
    private final String name;
    private final List<String> extend = new ArrayList<>();

    private final List<ClassMemberWriter> members = new ArrayList<>();

    public InterfaceDeclarationWriter(Visibility visibility, boolean isFinal, boolean isStatic,
                                      String name) {
        super(visibility, isFinal, isStatic);
        this.name = name;
    }

    public InterfaceDeclarationWriter addImplements(String extendedInterface){
        extend.add(extendedInterface);
        return this;
    }

    public InterfaceDeclarationWriter addMethodSignature(MethodSignatureWriter methodSignaturePoet){
        members.add(methodSignaturePoet);
        return this;
    }

    public MethodWriter addDefaultMethod(String type, String name){
        MethodWriter mp = new MethodWriter(Visibility.PACKAGE, false, false,
                "default "+type, name);
        members.add(mp);
        return mp;
    }

    public InterfaceDeclarationWriter addPSFS(String constName, String value){
        members.add(new FieldWriter(Visibility.PUBLIC, true, true, "String", constName,
                new SimpleExpressionWriter("\""+value+"\"")));
        return this;
    }

    public InterfaceDeclarationWriter addPSFS(String constName, String value, CommentWriter comment){
        FieldWriter f = new FieldWriter(Visibility.PUBLIC, true, true, "String", constName,
                new SimpleExpressionWriter("\""+value+"\""));
        f.addComment(comment);
        members.add(f);
        return this;
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        annotations.forEach(s::line);
        getVisibility().writeSonnet(s);
        if(isFinal()) s.spaced("final");
        if(isStatic()) s.spaced("static");
        s.spaced("interface").spaced(name);
        if(!extend.isEmpty()){
            s.spaced("extends");
            for (int i = 0; i < extend.size(); i++) {
                String x = extend.get(i);
                if(i != extend.size()-1) s.add(x).spaced(",");
                else s.spaced(x);
            }
        }
        s.line("{");
        s.indent();
        {
            for (int i = 0; i < members.size(); i++) {
                ClassMemberWriter m = members.get(i);
                m.writeSonnet(s);
                if(i != members.size()-1) s.line();
            }
        }
        s.dedent();
        s.line("}");
    }

    public String getName() {
        return name;
    }


}
