package it.unipr.ailab.sonneteer.type;

import it.unipr.ailab.sonneteer.Annotable;
import it.unipr.ailab.sonneteer.Writer;
import it.unipr.ailab.sonneteer.classmember.ClassMemberWriter;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;

public abstract class TypeDeclarationWriter
    extends ClassMemberWriter
    implements Writer, Annotable, ITypeDeclarationWriter {

    public TypeDeclarationWriter(
        Visibility visibility,
        boolean isFinal,
        boolean isStatic
    ) {
        super(visibility, isFinal, isStatic);
    }

}
