package it.unipr.ailab.jadescript.semantics.context.search;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Either;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;

public class JvmTypeLocation extends FQNameLocation {

    private final Either<JvmTypeReference, JvmType> jvmTypeRepresentation;

    public JvmTypeLocation(JvmTypeReference jvmTypeReference) {
        super(jvmTypeReference.getQualifiedName('.'));
        this.jvmTypeRepresentation = new Either.Left<>(jvmTypeReference);
    }

    public JvmTypeLocation(JvmType jvmType) {
        super(jvmType.getQualifiedName('.'));
        this.jvmTypeRepresentation = new Either.Right<>(jvmType);
    }


    @Override
    public IJadescriptType extractType(SemanticsModule module) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (jvmTypeRepresentation instanceof Either.Left) {
            return typeHelper.jtFromJvmTypeRef(
                    ((Either.Left<JvmTypeReference, JvmType>) jvmTypeRepresentation).getLeft()
            );
        } else if (jvmTypeRepresentation instanceof Either.Right) {
            return typeHelper.jtFromJvmTypeRef(typeHelper.typeRef(
                    ((Either.Right<JvmTypeReference, JvmType>) jvmTypeRepresentation).getRight())
            );
        } else {
            //Impossible to reach this, by Either's contract.
            return null;
        }
    }

    @Override
    public String toString() {
        return "(JVM type declaration: " + getFullyQualifiedName() + ")";
    }
}
