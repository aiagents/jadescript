package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.Context;
import it.unipr.ailab.jadescript.semantics.context.scope.ProceduralScope;
import it.unipr.ailab.jadescript.semantics.context.scope.ScopeManager;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public class SuperSlotInitializerContext extends Context implements ScopedContext, NamedSymbol.Searcher {
    private final OntologyElementDeclarationContext outer;
    private final Map<String, IJadescriptType> superSlotsInitScope;
    private final LazyValue<ScopeManager> scopeManager;

    public SuperSlotInitializerContext(
            SemanticsModule module,
            OntologyElementDeclarationContext outer,
            Map<String, IJadescriptType> superSlotsInitScope
    ) {
        super(module);
        this.outer = outer;
        this.superSlotsInitScope = new HashMap<>(superSlotsInitScope);
        this.scopeManager = new LazyValue<>(ScopeManager::new);
    }

    @Override
    public ScopeManager getScopeManager() {
        return scopeManager.get();
    }

    @Override
    public ProceduralScope getCurrentScope() {
        return getScopeManager().getCurrentScope();
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

        Stream<Map.Entry<String, IJadescriptType>> stream = superSlotsInitScope.entrySet().stream();
        stream = safeFilter(stream, Map.Entry::getKey, name);
        stream = safeFilter(stream, Map.Entry::getValue, readingType);
        stream = safeFilter(stream, (__) -> true, canWrite);
        return stream
                .map(entry -> new NamedSymbol() {
                    @Override
                    public SearchLocation sourceLocation() {
                        return currentLocation();
                    }

                    @Override
                    public String name() {
                        return entry.getKey();
                    }

                    @Override
                    public String compileRead(String dereferencePrefix) {
                        return dereferencePrefix + name();
                    }

                    @Override
                    public IJadescriptType readingType() {
                        return entry.getValue();
                    }

                    @Override
                    public boolean canWrite() {
                        return true;
                    }

                    @Override
                    public String compileWrite(String dereferencePrefix, String rexpr) {
                        return dereferencePrefix + name() + " = " + rexpr;
                    }
                });
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        scb.open("--> is SuperSlotInitializerContext {");
        scb.open("superSlotsInitScope = [");
        for (Map.Entry<String, IJadescriptType> entry : superSlotsInitScope.entrySet()) {
            scb.line(entry.getKey() + ": " + entry.getValue().getDebugPrint());
        }
        scb.close("]");
        scb.close("}");
        debugDumpScopedContext(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return outer.getCurrentOperationLogName();
    }
}
