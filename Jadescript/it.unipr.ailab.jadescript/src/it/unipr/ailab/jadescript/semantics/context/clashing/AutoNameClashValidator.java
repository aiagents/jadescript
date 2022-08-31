package it.unipr.ailab.jadescript.semantics.context.clashing;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.Symbol;
import org.eclipse.xtext.util.Strings;

import java.util.stream.Stream;

public interface AutoNameClashValidator extends NameClashValidator {


    @Override
    default Stream<DefinitionClash> checkNameClash(String name, Symbol toBeAdded) {
        Stream<DefinitionClash> fromCallables;
        if (this instanceof CallableSymbol.Searcher) {
            fromCallables = Streams.concat(
                    ((CallableSymbol.Searcher) this).searchCallable(
                            name,
                            null,
                            (size, names) -> size == 0,
                            (size, types) -> size == 0
                    ), ((CallableSymbol.Searcher) this).searchCallable(
                            "get"+ Strings.toFirstUpper(name),
                            null,
                            (size, names) -> size == 0,
                            (size, types) -> size == 0
                    ), ((CallableSymbol.Searcher) this).searchCallable(
                            "set"+ Strings.toFirstUpper(name),
                            null,
                            (size, names) -> size == 1,
                            (size, types) -> size == 1
                    )
            ).map(alreadyPresent -> new DefinitionClash(toBeAdded, alreadyPresent));
        } else {
            fromCallables = Stream.empty();
        }

        Stream<DefinitionClash> fromNameds;
        if (this instanceof NamedSymbol.Searcher) {
            fromNameds = ((NamedSymbol.Searcher) this).searchName(
                    name,
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
