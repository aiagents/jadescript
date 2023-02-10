package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberNamedCell;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.stream.Stream;

public abstract class NamespaceWithMembers extends BaseNamespace
    implements MemberCallable.Namespace, MemberNamedCell.Namespace {

    public NamespaceWithMembers(SemanticsModule module) {
        super(module);
    }


    @Override
    public abstract Stream<? extends MemberCallable> memberCallables();


    @Override
    public abstract Stream<? extends MemberNamedCell> memberNamedCells();


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        scb.open("--> is NamespaceWithMembers {");
        scb.open("names     = [");
        memberCallables().forEach(ns -> ns.debugDumpNamedMember(scb));
        scb.close("]");
        scb.open("callables = [");
        memberNamedCells().forEach(ns -> ns.debugDumpCallableMember(scb));
        scb.close("]");
        scb.close("}");
    }

}
