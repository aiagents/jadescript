package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public abstract class NamespaceWithMembers extends BaseNamespace
    implements MemberCallable.Namespace, MemberName.Namespace {

    public NamespaceWithMembers(SemanticsModule module) {
        super(module);
    }


    @Override
    public abstract Stream<? extends MemberCallable> memberCallables(
        @Nullable String name
    );


    @Override
    public abstract Stream<? extends MemberName> memberNames(
        @Nullable String name
    );


    @Override
    public abstract Maybe<? extends NamespaceWithMembers> superSearcheable();


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        scb.open("--> is NamespaceWithMembers {");
        scb.open("names     = [");
        memberCallables(null).forEach(ns -> ns.debugDumpMemberCallable(scb));
        scb.close("]");
        scb.open("callables = [");
        memberNames(null).forEach(ns -> ns.debugDumpMemberName(scb));
        scb.close("]");
        scb.close("}");
    }

}
