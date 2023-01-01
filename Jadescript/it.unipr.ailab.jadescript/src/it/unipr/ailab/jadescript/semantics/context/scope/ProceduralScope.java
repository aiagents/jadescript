package it.unipr.ailab.jadescript.semantics.context.scope;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.search.UserLocalDefinition;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.UserVariable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public abstract class ProceduralScope
    implements Searcheable, NamedSymbol.Searcher {


    protected final Map<String, NamedSymbol> variables = new HashMap<>();


    @Override
    public abstract Maybe<? extends Searcheable> superSearcheable();


    public UserVariable addUserVariable(String name, IJadescriptType type,
                                        boolean canWrite) {
        final UserVariable userVariable = new UserVariable(name, type,
            canWrite);
        this.variables.put(name, userVariable);
        return userVariable;
    }

    public void addNamedElement(NamedSymbol ne) {
        this.variables.put(ne.name(), ne);
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        scb.open("--> is ProceduralScope {");
        scb.open("Variables=[");
        for (NamedSymbol value : this.variables.values()) {
            value.debugDumpNamedSymbol(scb);
        }
        scb.close("]");
        scb.close("}");

    }


    @Override
    public Stream<? extends NamedSymbol> searchName(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {
        Stream<NamedSymbol> stream = variables.values().stream();
        stream = safeFilter(stream, NamedSymbol::name, name);
        stream = safeFilter(stream, NamedSymbol::readingType, readingType);
        stream = safeFilter(stream, NamedSymbol::canWrite, canWrite);
        return stream;
    }

    @Override
    public SearchLocation currentLocation() {
        return UserLocalDefinition.getInstance();
    }
}
