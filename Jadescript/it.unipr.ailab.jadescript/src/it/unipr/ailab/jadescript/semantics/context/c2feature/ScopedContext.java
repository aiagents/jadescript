package it.unipr.ailab.jadescript.semantics.context.c2feature;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.GenerationParameters;
import it.unipr.ailab.jadescript.semantics.context.scope.ScopeManager;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.Function;
import java.util.stream.Stream;

public interface ScopedContext extends Searcheable {
    ScopeManager getScopeManager();


    @Override
    default <T> Stream<T> search(
        Function<? super Searcheable, Stream<T>> localSearcher
    ) {
        final Maybe<? extends Searcheable> nextSearcheable = superSearcheable();
        if (GenerationParameters.DEBUG_SEARCH) {
            System.out.println("[DEBUG_SEARCH]: Searching in "
                + this.getClass().getName() + (nextSearcheable.isPresent()
                ? "(searching first in scopes, then next is :"
                + nextSearcheable.toNullable().getClass().getName() + ")"
                : "(searching first in scopes)"
            ));
        }
        return Streams.concat(
            getScopeManager().getCurrentScope().search(localSearcher),
            //recursively search in all scopes
            localSearcher.apply(this), //then search in the context
            nextSearcheable //finally, search recursively in parent context
                .__(ns -> ns.search(localSearcher))
                .orElseGet(Stream::empty)
        );
    }

    default void debugDumpScopedContext(SourceCodeBuilder scb) {
        scb.open("--> is ScopedContext {");
        scb.line("*** SCOPE DUMP ***");
        getScopeManager().getCurrentScope().debugDump(scb);
        scb.close("}");
    }
}
