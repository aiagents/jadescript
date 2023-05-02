package it.unipr.ailab.sonneteer.type;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.classmember.ClassMemberWriter;
import it.unipr.ailab.sonneteer.comment.CommentWriter;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;

import java.util.List;

public interface IClassDeclarationWriter extends ITypeDeclarationWriter {

    IClassDeclarationWriter addExtends(String extendedClass);

    IClassDeclarationWriter addImplements(String implementedInterface);

    ClassDeclarationWriter addMember(ClassMemberWriter member);

    IClassDeclarationWriter addPSFS(String constName, String value);

    IClassDeclarationWriter addPSFS(
        String constName,
        String value,
        CommentWriter comment
    );

    ClassDeclarationWriter addProperty(
        String type,
        String name,
        boolean readOnly
    );

    ClassDeclarationWriter.ConstructorWriter addConstructor(
        Visibility visibility
    );

    void addAnnotation(String annotation);

    void writeSonnet(SourceCodeBuilder s);

    List<String> getExtend();

    List<String> getImplement();

    List<ClassMemberWriter> getMembers();

    String getName();

    boolean isOrderConvention();

    void setOrderConvention(boolean orderConvention);

}
