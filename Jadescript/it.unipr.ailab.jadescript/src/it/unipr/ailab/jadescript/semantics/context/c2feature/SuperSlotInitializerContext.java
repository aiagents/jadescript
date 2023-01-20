package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.Context;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.SuperProperty;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public class SuperSlotInitializerContext extends Context
    implements NamedSymbol.Searcher {

    private final OntologyElementDeclarationContext outer;
    private final Map<String, IJadescriptType> superSlotsInitPairs;


    public SuperSlotInitializerContext(
        SemanticsModule module,
        OntologyElementDeclarationContext outer,
        Map<String, IJadescriptType> superSlotsInitPairs
    ) {
        super(module);
        this.outer = outer;
        this.superSlotsInitPairs = new HashMap<>(superSlotsInitPairs);
    }


    @Override
    public Maybe<? extends Searcheable> superSearcheable() {
        return Maybe.some(outer);
    }


    @Override
    public Stream<? extends NamedSymbol> searchName(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {

        Stream<Map.Entry<String, IJadescriptType>> stream =
            superSlotsInitPairs.entrySet().stream();
        stream = safeFilter(stream, Map.Entry::getKey, name);
        stream = safeFilter(stream, Map.Entry::getValue, readingType);
        stream = safeFilter(stream, (__) -> true, canWrite);
        return stream.map(entry -> new SuperProperty(
            entry.getKey(),
            entry.getValue(),
            currentLocation()
        ));
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        scb.open("--> is SuperSlotInitializerContext {");
        scb.open("superSlotsInitScope = [");
        final Set<Map.Entry<String, IJadescriptType>> entries =
            superSlotsInitPairs.entrySet();
        for (Map.Entry<String, IJadescriptType> entry : entries) {
            scb.line(entry.getKey() + ": " + entry.getValue().getDebugPrint());
        }
        scb.close("]");
        scb.close("}");
    }


    @Override
    public String getCurrentOperationLogName() {
        return outer.getCurrentOperationLogName();
    }


}
