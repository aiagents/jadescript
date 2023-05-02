package it.unipr.ailab.jadescript.semantics.context.clashing;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.Located;
import org.eclipse.xtext.util.Strings;

import java.util.stream.Stream;

public interface AutoNameClashValidator extends NameClashValidator {


    @Override
    default Stream<DefinitionClash> checkNameClash(
        String name,
        Located toBeAdded
    ) {
        Stream<DefinitionClash> fromCallables;
        if (this instanceof CompilableCallable.Namespace) {
            fromCallables = Streams.concat(
                ((CompilableCallable.Namespace) this).
                    compilableCallables(name)
                    .filter(c -> c.arity() == 0),
                ((CompilableCallable.Namespace) this).compilableCallables(
                        "get" + Strings.toFirstUpper(name)
                    )
                    .filter(c -> c.arity() == 0),
                ((CompilableCallable.Namespace) this).compilableCallables(
                        "set" + Strings.toFirstUpper(name))
                    .filter(c -> c.arity() == 1)
            ).map(alreadyPresent -> new DefinitionClash(
                toBeAdded,
                alreadyPresent
            ));
        } else {
            fromCallables = Stream.empty();
        }

        Stream<DefinitionClash> fromNameds;
        if (this instanceof CompilableName.Namespace) {
            fromNameds = ((CompilableName.Namespace) this).compilableNames(name)
                .map(alreadyPresent -> new DefinitionClash(
                    toBeAdded,
                    alreadyPresent
                ));
        } else {
            fromNameds = Stream.empty();
        }

        return Streams.concat(
            fromNameds,
            fromCallables
        );
    }

}
