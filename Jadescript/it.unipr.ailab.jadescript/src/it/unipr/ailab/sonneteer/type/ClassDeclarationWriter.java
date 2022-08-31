package it.unipr.ailab.sonneteer.type;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.classmember.ClassMemberWriter;
import it.unipr.ailab.sonneteer.classmember.FieldWriter;
import it.unipr.ailab.sonneteer.classmember.MethodWriter;
import it.unipr.ailab.sonneteer.classmember.ParameterWriter;
import it.unipr.ailab.sonneteer.comment.CommentWriter;
import it.unipr.ailab.sonneteer.expression.SimpleExpressionWriter;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;
import it.unipr.ailab.sonneteer.statement.AssignmentWriter;
import it.unipr.ailab.sonneteer.statement.ReturnStatementWriter;

import java.util.ArrayList;
import java.util.List;

public class ClassDeclarationWriter extends TypeDeclarationWriter implements IClassDeclarationWriter {

    private final List<String> annotations = new ArrayList<>();
    private final String name;
    private final List<String> extend = new ArrayList<>();
    private final List<String> implement = new ArrayList<>();
    private final List<ClassMemberWriter> members = new ArrayList<>();
    private boolean orderConvention = true;

    public ClassDeclarationWriter(Visibility visibility, boolean isFinal, boolean isStatic,
                                  String name) {
        super(visibility, isFinal, isStatic);
        this.name = name;
    }


    @Override
    public IClassDeclarationWriter addExtends(String extendedClass) {
        extend.add(extendedClass);
        return this;
    }

    @Override
    public IClassDeclarationWriter addImplements(String implementedInterface) {
        implement.add(implementedInterface);
        return this;
    }

    @Override
    public ClassDeclarationWriter addMember(ClassMemberWriter member) {
        members.add(member);
        return this;
    }

    @Override
    public IClassDeclarationWriter addPSFS(String constName, String value) {
        members.add(new FieldWriter(Visibility.PUBLIC, true, true, "String", constName,
                new SimpleExpressionWriter("\"" + value + "\"")));
        return this;
    }

    @Override
    public IClassDeclarationWriter addPSFS(String constName, String value, CommentWriter comment) {
        FieldWriter f = new FieldWriter(Visibility.PUBLIC, true, true, "String", constName,
                new SimpleExpressionWriter("\"" + value + "\""));
        f.addComment(comment);
        members.add(f);
        return this;
    }

    @Override
    public ClassDeclarationWriter addProperty(String type, String name, boolean readOnly) {
        String firstCapitalLetterName = name.substring(0, 1).toUpperCase() + name.substring(1);
        addMember(new FieldWriter(Visibility.PRIVATE, false, false, type, name));
        MethodWriter getter = new MethodWriter(Visibility.PUBLIC, false, false, type, "get" + firstCapitalLetterName);
        getter.getBody().addStatement(new ReturnStatementWriter(new SimpleExpressionWriter("this." + name)));
        addMember(getter);
        if (!readOnly) {
            MethodWriter setter = new MethodWriter(Visibility.PUBLIC, false, false, "void", "set" + firstCapitalLetterName)
                    .addParameter(new ParameterWriter(type, name));
            setter.getBody().addStatement(new AssignmentWriter("this." + name, new SimpleExpressionWriter(name)));
            addMember(setter);
        }
        return this;
    }


    public ConstructorWriter addConstructor(Visibility visibility) {
        ConstructorWriter ctor = new ConstructorWriter(visibility, this.name);
        this.addMember(ctor);
        return ctor;
    }


    @Override
    public void addAnnotation(String annotation) {
        annotations.add(annotation);
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        if (orderConvention)
            sortMembersByConvention();
        getComments().forEach(x -> x.writeSonnet(s));
        annotations.forEach(s::line);
        getVisibility().writeSonnet(s);
        if (isFinal())
            s.spaced("final");
        if (isStatic())
            s.spaced("static");
        s.spaced("class").spaced(name);
        if (!extend.isEmpty()) {
            s.spaced("extends");
            for (int i = 0; i < extend.size(); i++) {
                String x = extend.get(i);
                if (i != extend.size() - 1)
                    s.add(x).spaced(",");
                else s.spaced(x);
            }
        }
        if (!implement.isEmpty()) {
            s.spaced("implements");
            for (int i = 0; i < implement.size(); i++) {
                String x = implement.get(i);
                if (i != implement.size() - 1)
                    s.add(x).spaced(",");
                else s.spaced(x);
            }
        }
        s.line("{");
        s.indent();
        {
            for (int i = 0; i < members.size(); i++) {
                ClassMemberWriter m = members.get(i);
                m.writeSonnet(s);
                if (i != members.size() - 1)
                    s.line();
            }
        }
        s.dedent();
        s.line("}");
    }

    private void sortMembersByConvention() {
        List<FieldWriter> privateFields = new ArrayList<>();
        List<FieldWriter> protectedFields = new ArrayList<>();
        List<FieldWriter> publicFields = new ArrayList<>();
        List<FieldWriter> packageFields = new ArrayList<>();
        List<FieldWriter> staticPrivateFields = new ArrayList<>();
        List<FieldWriter> staticProtectedFields = new ArrayList<>();
        List<FieldWriter> staticPublicFields = new ArrayList<>();
        List<FieldWriter> staticPackageFields = new ArrayList<>();
        List<MethodWriter> methods = new ArrayList<>();
        List<ClassMemberWriter> other = new ArrayList<>();

        members.forEach(m -> {
            if (m instanceof FieldWriter) {
                if (m.isStatic()) {
                    switch (m.getVisibility()) {
                        case PRIVATE:
                            staticPrivateFields.add((FieldWriter) m);
                            break;
                        case PUBLIC:
                            staticPublicFields.add((FieldWriter) m);
                            break;
                        case PACKAGE:
                            staticPackageFields.add((FieldWriter) m);
                            break;
                        case PROTECTED:
                            staticProtectedFields.add((FieldWriter) m);
                            break;
                    }
                } else {
                    switch (m.getVisibility()) {
                        case PRIVATE:
                            privateFields.add((FieldWriter) m);
                            break;
                        case PUBLIC:
                            publicFields.add((FieldWriter) m);
                            break;
                        case PACKAGE:
                            packageFields.add((FieldWriter) m);
                            break;
                        case PROTECTED:
                            protectedFields.add((FieldWriter) m);
                            break;
                    }
                }
            } else if (m instanceof MethodWriter)
                methods.add((MethodWriter) m);
            else other.add(m);
        });

        members.clear();
        members.addAll(staticPublicFields);
        members.addAll(staticProtectedFields);
        members.addAll(staticPackageFields);
        members.addAll(staticPrivateFields);
        members.addAll(publicFields);
        members.addAll(protectedFields);
        members.addAll(packageFields);
        members.addAll(privateFields);
        members.addAll(methods);
        members.addAll(other);
    }


    @Override
    public List<String> getExtend() {
        return extend;
    }

    @Override
    public List<String> getImplement() {
        return implement;
    }

    @Override
    public List<ClassMemberWriter> getMembers() {
        return members;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isOrderConvention() {
        return orderConvention;
    }

    @Override
    public void setOrderConvention(boolean orderConvention) {
        this.orderConvention = orderConvention;
    }

    public static class ConstructorWriter extends MethodWriter {

        public ConstructorWriter(Visibility visibility,
                                 String className) {
            super(visibility, false, false, "", className);
        }
    }
}
