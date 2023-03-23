package it.unipr.ailab.jadescript.semantics.context.associations;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.namespace.ImportedMembersNamespace;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.NamespaceWithCompilables;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.emf.ecore.EObject;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

import static it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts.THIS;

public class SelfAssociation
    implements Comparable<SelfAssociation>, Association {

    private final IJadescriptType self;
    private final SelfAssociationKind associationKind;


    public SelfAssociation(
        IJadescriptType self,
        SelfAssociationKind associationKind
    ) {
        this.self = self;
        this.associationKind = associationKind;
    }


    @Override
    public NamespaceWithCompilables importNamespace(
        SemanticsModule module,
        Maybe<? extends EObject> eObject
    ) {
        return ImportedMembersNamespace.importMembersNamespace(
            module,
            acceptor -> SemanticsUtils.getOuterClassThisReference(eObject).orElse(THIS),
            ExpressionDescriptor.thisReference,
            getAssociatedType().namespace()
        );
    }

    public static SelfAssociation applyExtends(SelfAssociation input) {
        return new SelfAssociation(
            input.getSelf(),
            applyExtends(input.getAssociationKind())
        );
    }


    public static SelfAssociationKind applyExtends(SelfAssociationKind input) {
        if (input instanceof SelfAssociation.T) {
            return SelfAssociation.ST.INSTANCE;
        } else {
            return input;
        }
    }


    public IJadescriptType getSelf() {
        return self;
    }


    public SelfAssociationKind getAssociationKind() {
        return associationKind;
    }


    public void debugDump(SourceCodeBuilder scb) {
        scb.open("SelfAssociation{");
        scb.line("self=" + self.getDebugPrint());
        scb.line("associationKind= " +
            associationKind.getClass().getSimpleName());
        scb.close("}");
    }

        /*
    Association kinds ordered by "distance"
T
ST
     */
    @Override
    public int compareTo(@NotNull SelfAssociation o) {
        return Comparator.<SelfAssociation>comparingInt(a ->
                a.getAssociationKind().distanceOrdinal())
            .compare(this, o);
    }


    @Override
    public IJadescriptType getAssociatedType() {
        return getSelf();
    }


    /**
     * Is the current declaration
     * <pre>
     * {@code
     * agent/behaviour T <- THE DECLARATION
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum T implements SelfAssociationKind {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 0;
        }
    }

    /**
     * Is a supertype of the current declaration
     * <pre>
     * {@code
     * behaviour ST <- THE DECLARATION
     *      ...
     *
     * agent/behaviour T extends ST
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum ST implements SelfAssociationKind {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 1;
        }
    }

    public interface SelfAssociationKind {
        int distanceOrdinal();
    }

}
