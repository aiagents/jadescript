package it.unipr.ailab.jadescript.semantics.context.search;

import it.unipr.ailab.maybe.Maybe;

/**
 * Interface of all searcheables that are supposed to be equipped with
 * a "Supertype"
 */
public interface WithSupertype {
    Maybe<Searcheable> superTypeSearcheable();
}
