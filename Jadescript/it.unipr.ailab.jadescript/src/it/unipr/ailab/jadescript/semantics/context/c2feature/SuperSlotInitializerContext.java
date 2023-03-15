package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.Context;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.symbol.SuperProperty;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class SuperSlotInitializerContext extends Context
    implements CompilableName.Namespace {

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
    public Stream<? extends CompilableName> compilableNames(
        @Nullable String name
    ) {

        if (name == null) {
            return superSlotsInitPairs.entrySet().stream()
                .map(entry -> new SuperProperty(
                    entry.getKey(),
                    entry.getValue(),
                    currentLocation()
                ));
        }
        @Nullable final IJadescriptType type =
            superSlotsInitPairs.get(name);

        if (type == null) {
            return Stream.empty();
        }

        return Stream.of(
            new SuperProperty(name, type, currentLocation())
        );
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
