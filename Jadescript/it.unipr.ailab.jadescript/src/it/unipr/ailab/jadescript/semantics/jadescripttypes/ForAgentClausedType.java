package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.NameMember;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmTypeNamespace;
import it.unipr.ailab.maybe.Maybe;

import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts.THE_AGENT;

public interface ForAgentClausedType extends IJadescriptType {
    default Maybe<AgentType> getDirectlyForAgentType() {
        final JvmTypeNamespace jvmNamespace = jvmNamespace();
        return Maybe.fromOpt((jvmNamespace == null
                ? Stream.<NameMember>empty()
                : jvmNamespace
                .searchName(THE_AGENT, null, null))
                .filter(i -> i instanceof JvmFieldSymbol)
                .map(NameMember::readingType)
                .filter(i -> i instanceof AgentType)
                .map(i -> (AgentType) i)
                .findAny());
    }
}
