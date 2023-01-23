package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.classmember.ClassMemberWriter;
import it.unipr.ailab.sonneteer.comment.CommentWriter;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;
import it.unipr.ailab.sonneteer.type.ClassDeclarationWriter;
import it.unipr.ailab.sonneteer.type.IClassDeclarationWriter;

import java.util.List;

public class LocalClassStatementWriter extends StatementWriter implements IClassDeclarationWriter{

    private final IClassDeclarationWriter delegate;


    public LocalClassStatementWriter(String name) {
        delegate = new ClassDeclarationWriter(Visibility.PACKAGE, false, false, name);
    }

    @Override
    public IClassDeclarationWriter addExtends(String extendedClass) {
        return delegate.addExtends(extendedClass);
    }

    @Override
    public IClassDeclarationWriter addImplements(String implementedInterface) {
        return delegate.addImplements(implementedInterface);
    }

    @Override
    public ClassDeclarationWriter addMember(ClassMemberWriter member) {
        return delegate.addMember(member);
    }

    @Override
    public IClassDeclarationWriter addPSFS(String constName, String value) {
        return delegate.addPSFS(constName, value);
    }

    @Override
    public IClassDeclarationWriter addPSFS(String constName, String value, CommentWriter comment) {
        return delegate.addPSFS(constName, value, comment);
    }

    @Override
    public ClassDeclarationWriter addProperty(String type, String name, boolean readOnly) {
        return delegate.addProperty(type, name, readOnly);
    }

    @Override
    public ClassDeclarationWriter.ConstructorWriter addConstructor(Visibility visibility) {
        return delegate.addConstructor(visibility);
    }

    @Override
    public void addAnnotation(String annotation) {
        delegate.addAnnotation(annotation);
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        delegate.writeSonnet(s);
    }

    @Override
    public List<String> getExtend() {
        return delegate.getExtend();
    }

    @Override
    public List<String> getImplement() {
        return delegate.getImplement();
    }

    @Override
    public List<ClassMemberWriter> getMembers() {
        return delegate.getMembers();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean isOrderConvention() {
        return delegate.isOrderConvention();
    }

    @Override
    public void setOrderConvention(boolean orderConvention) {
        delegate.setOrderConvention(orderConvention);
    }

}
