package it.unipr.ailab.sonneteer.classmember;

import it.unipr.ailab.sonneteer.Annotable;
import it.unipr.ailab.sonneteer.Writer;
import it.unipr.ailab.sonneteer.comment.Commentable;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class representing a writer for a class member. Each class member
 * can be annotated, be final, be static, and have specific visibility.
 */
public abstract class ClassMemberWriter
    extends Commentable
    implements Writer, Annotable {

    protected final Visibility visibility;
    protected final boolean isStatic;
    protected final boolean isFinal;
    protected final List<String> annotations = new ArrayList<>();


    public ClassMemberWriter(
        Visibility visibility,
        boolean isFinal,
        boolean isStatic
    ) {
        this.isStatic = isStatic;
        this.isFinal = isFinal;
        this.visibility = visibility;
    }


    @Override
    public void addAnnotation(String annotation) {
        annotations.add(annotation);
    }


    public List<String> getAnnotations() {
        return annotations;
    }


    public boolean isFinal() {
        return isFinal;
    }


    public boolean isStatic() {
        return isStatic;
    }


    public Visibility getVisibility() {
        return visibility;
    }

}
