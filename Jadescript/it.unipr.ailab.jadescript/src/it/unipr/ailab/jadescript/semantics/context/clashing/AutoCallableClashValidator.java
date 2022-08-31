package it.unipr.ailab.jadescript.semantics.context.clashing;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import org.eclipse.xtext.util.Strings;

import java.util.stream.Stream;

public interface AutoCallableClashValidator extends CallableClashValidator {

    @Override
    default Stream<DefinitionClash> checkCallableClash(CallableSymbol toBeAdded) {
        Stream<DefinitionClash> fromCallables;
        if (this instanceof CallableSymbol.Searcher) {
            fromCallables = ((CallableSymbol.Searcher) this).searchCallable(
                    toBeAdded.name(),
                    null,
                    (size, names) -> size == toBeAdded.parameterNames().size(),
                    (size, types) -> size == toBeAdded.parameterTypes().size()
            ).map(alreadyPresent -> new DefinitionClash(toBeAdded, alreadyPresent));
            if (toBeAdded.parameterTypes().size() == 0) {
                fromCallables = Streams.concat(
                        fromCallables,
                        ((CallableSymbol.Searcher) this).searchCallable(
                                "get" + Strings.toFirstUpper(toBeAdded.name()),
                                null,
                                (size, names) -> size == 0,
                                (size, types) -> size == 0
                        ).map(alreadyPresent -> new DefinitionClash(toBeAdded, alreadyPresent))
                );
            }
            if (toBeAdded.parameterTypes().size() == 1) {
                fromCallables = Streams.concat(
                        fromCallables,
                        ((CallableSymbol.Searcher) this).searchCallable(
                                "set" + Strings.toFirstUpper(toBeAdded.name()),
                                null,
                                (size, names) -> size == 1,
                                (size, types) -> size == 1
                        ).map(alreadyPresent -> new DefinitionClash(toBeAdded, alreadyPresent))
                );
            }
        } else {
            fromCallables = Stream.empty();
        }

        Stream<DefinitionClash> fromNameds;
        if (this instanceof NamedSymbol.Searcher && toBeAdded.parameterNames().size() == 0) {
            fromNameds = ((NamedSymbol.Searcher) this).searchName(
                    toBeAdded.name(),
                    null,
                    null
            ).map(alreadyPresent -> new DefinitionClash(toBeAdded, alreadyPresent));
        } else {
            fromNameds = Stream.empty();
        }

        return Streams.concat(
                fromNameds,
                fromCallables
        );
    }
}
