package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.namespace.JvmModelBasedNamespace;
import it.unipr.ailab.maybe.Maybe;

import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts.THE_AGENT;

public interface ForAgentClausedType extends IJadescriptType {
    default Maybe<AgentType> getDirectlyForAgentType() {
        final JvmModelBasedNamespace jvmNamespace = jvmNamespace();
        return Maybe.fromOpt((jvmNamespace == null
                ? Stream.<NamedSymbol>empty()
                : jvmNamespace
                .searchName(THE_AGENT, null, null))
                .filter(i -> i instanceof JvmModelBasedNamespace.JvmFieldSymbol)
                .map(NamedSymbol::readingType)
                .filter(i -> i instanceof AgentType)
                .map(i -> (AgentType) i)
                .findAny());
    }
}
