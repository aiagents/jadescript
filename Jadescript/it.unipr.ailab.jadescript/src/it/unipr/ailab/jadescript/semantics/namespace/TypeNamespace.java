package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.search.WithSupertype;
import it.unipr.ailab.maybe.Maybe;

public abstract class TypeNamespace
    extends NamespaceWithMembers
    implements WithSupertype {

    public TypeNamespace(SemanticsModule module) {
        super(module);
    }


    public abstract Maybe<? extends TypeNamespace> getSuperTypeNamespace();


    @Override
    public Maybe<Searcheable> superTypeSearcheable() {
        return getSuperTypeNamespace().__(x -> x);
    }


    @Override
    public Maybe<? extends TypeNamespace> superSearcheable() {
        return getSuperTypeNamespace();
    }

}
