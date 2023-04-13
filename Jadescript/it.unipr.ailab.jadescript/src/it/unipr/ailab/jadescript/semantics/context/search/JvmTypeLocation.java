package it.unipr.ailab.jadescript.semantics.context.search;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
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
        final TypeSolver typeSolver = module.get(TypeSolver.class);
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        if (jvmTypeRepresentation instanceof Either.Left) {
            return typeSolver.fromJvmTypeReference(
                ((Either.Left<JvmTypeReference, JvmType>)
                    jvmTypeRepresentation).getLeft()
            );
        } else if (jvmTypeRepresentation instanceof Either.Right) {
            return typeSolver.fromJvmTypeReference(
                jvm.typeRef(((Either.Right<JvmTypeReference, JvmType>)
                    jvmTypeRepresentation).getRight()
                )
            );
        } else {
            //Impossible to reach this, by Either's contract.
            //noinspection ReturnOfNull
            return null;
        }
    }


    @Override
    public String toString() {
        return "(location in JVM: " + getFullyQualifiedName() + ")";
    }

}
