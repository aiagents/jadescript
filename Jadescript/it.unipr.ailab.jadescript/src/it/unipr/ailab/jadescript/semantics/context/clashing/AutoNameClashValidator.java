package it.unipr.ailab.jadescript.semantics.context.clashing;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableNamedCell;
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
                    compilableCallables()
                    .filter(c -> c.name().equals(name))
                    .filter(c -> c.arity() == 0),
                ((CompilableCallable.Namespace) this).compilableCallables()
                    .filter(c -> c.name().equals(
                        "get" + Strings.toFirstUpper(name)))
                    .filter(c -> c.arity() == 0),
                ((CompilableCallable.Namespace) this).compilableCallables()
                    .filter(c -> c.name().equals(
                        "set" + Strings.toFirstUpper(name)))
                    .filter(c -> c.arity() == 1)
            ).map(alreadyPresent -> new DefinitionClash(
                toBeAdded,
                alreadyPresent
            ));
        } else {
            fromCallables = Stream.empty();
        }

        Stream<DefinitionClash> fromNameds;
        if (this instanceof CompilableNamedCell.Namespace) {
            fromNameds = ((CompilableNamedCell.Namespace) this)
                .compilableNamedCells()
                .filter(nc -> nc.name().equals(name))
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
