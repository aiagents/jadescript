package it.unipr.ailab.jadescript.semantics.context.clashing;

import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.Located;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.Member;

import java.util.stream.Stream;

public interface NameClashValidator {
    Stream<DefinitionClash> checkNameClash(String name, Located toBeAdded);
}
