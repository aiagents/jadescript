package it.unipr.ailab.jadescript.semantics.context.search;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.GenerationParameters;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.Function;
import java.util.stream.Stream;

public interface Searcheable {
    default <O, T> Stream<T> searchAs(
            Class<? extends O> clazz,
            Function<O, Stream<T>> localSearcher
    ) {

        return search(searcheable -> {
            if (GenerationParameters.DEBUG_SEARCH) {
                System.out.println("[DEBUG_SEARCH]: Search-as " + clazz.getName() + ":");
            }
            if (clazz.isInstance(searcheable)) {
                if (GenerationParameters.DEBUG_SEARCH) {
                    System.out.println("[DEBUG_SEARCH]: " + searcheable.getClass().getName() +
                            " is instance of " + clazz.getName());
                }
                return localSearcher.apply(clazz.cast(searcheable));
            } else {
                if (GenerationParameters.DEBUG_SEARCH) {
                    System.out.println("[DEBUG_SEARCH]: " + searcheable.getClass().getName() +
                            " is not instance of " + clazz.getName());
                }
                return Stream.empty();
            }
        });
    }

    default <O> Stream<O> actAs(
            Class<? extends O> clazz
    ) {
        return searchAs(clazz, Stream::of);
    }

    default <T> Stream<T> search(
            Function<? super Searcheable, Stream<T>> localSearcher
    ) {
        final Maybe<? extends Searcheable> nextSearcheable = superSearcheable();
        if (GenerationParameters.DEBUG_SEARCH) {
            System.out.println("[DEBUG_SEARCH]: Searching in " + this.getClass().getName() +
                    (nextSearcheable.isPresent()
                            ? "(next is :" + nextSearcheable.toNullable().getClass().getName() + ")"
                            : ""
                    ));
        }
        return Streams.concat(
                localSearcher.apply(this),
                nextSearcheable
                        .__(ns -> ns.search(localSearcher))
                        .orElseGet(Stream::empty)
        );
    }

    /**
     * Where to look next when performing a search.
     */
    Maybe<? extends Searcheable> superSearcheable();

    SearchLocation currentLocation();

    default void debugDump(SourceCodeBuilder scb) {
        scb.line("Implement debugDump for " + this.getClass().getName());
    }


}
