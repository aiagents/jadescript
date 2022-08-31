package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public abstract class HandlerWithWhenExpressionContext
        extends EventHandlerContext
        implements NamedSymbol.Searcher {

    private final List<NamedSymbol> patternMatchAutoDeclaredVariables;

    public HandlerWithWhenExpressionContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer,
            String eventType,
            List<NamedSymbol> patternMatchAutoDeclaredVariables
    ) {
        super(module, outer, eventType);
        this.patternMatchAutoDeclaredVariables = patternMatchAutoDeclaredVariables;
    }

    @Override
    public Stream<? extends NamedSymbol> searchName(
            Predicate<String> name,
            Predicate<IJadescriptType> readingType,
            Predicate<Boolean> canWrite
    ) {
        Stream<NamedSymbol> stream = patternMatchAutoDeclaredVariables.stream();
        stream = safeFilter(stream, NamedSymbol::name, name);
        stream = safeFilter(stream, NamedSymbol::readingType, readingType);
        stream = safeFilter(stream, NamedSymbol::canWrite, canWrite);
        return stream;
    }

    public List<NamedSymbol> getPatternMatchAutoDeclaredVariables() {
        return patternMatchAutoDeclaredVariables;
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.open("--> is HandlerWithWhenExpressionContext {");
        scb.open("patternMatchAutoDeclaredVariables = [");
        for (NamedSymbol patternMatchAutoDeclaredVariable : patternMatchAutoDeclaredVariables) {
            patternMatchAutoDeclaredVariable.debugDumpNamedSymbol(scb);
        }
        scb.close("]");
        scb.close("}");
    }


}
