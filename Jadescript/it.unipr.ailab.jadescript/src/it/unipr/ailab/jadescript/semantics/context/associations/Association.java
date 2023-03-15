package it.unipr.ailab.jadescript.semantics.context.associations;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.NamespaceWithCompilables;
import it.unipr.ailab.jadescript.semantics.namespace.NamespaceWithMembers;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;

public interface Association {
    IJadescriptType getAssociatedType();
    NamespaceWithCompilables importNamespace(
        SemanticsModule module,
        Maybe<? extends EObject> eObject
    );

}
