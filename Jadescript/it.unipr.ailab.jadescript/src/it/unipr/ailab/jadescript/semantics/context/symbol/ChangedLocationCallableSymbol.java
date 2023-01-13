package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;

public class ChangedLocationCallableSymbol
    extends CallableSymbolWrapper {

    private final SearchLocation location;


    public ChangedLocationCallableSymbol(
        CallableSymbol wrapped,
        SearchLocation location
    ) {
        super(wrapped);
        this.location = location;
    }


    @Override
    public SearchLocation sourceLocation() {
        return location;
    }

}
