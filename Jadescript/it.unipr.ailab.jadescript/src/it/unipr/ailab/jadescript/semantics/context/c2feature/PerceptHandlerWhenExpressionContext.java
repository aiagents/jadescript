package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class PerceptHandlerWhenExpressionContext
        extends HandlerWhenExpressionContext
        implements NamedSymbol.Searcher, PerceptPerceivedContext {
    private final IJadescriptType perceptContentType;

    public PerceptHandlerWhenExpressionContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer,
            IJadescriptType perceptContentType
    ) {
        super(module, outer);
        this.perceptContentType = perceptContentType;
    }


    @Override
    public IJadescriptType getPerceptContentType() {
        return perceptContentType;
    }

    @Override
    public Stream<? extends NamedSymbol> searchName(
            Predicate<String> name,
            Predicate<IJadescriptType> readingType,
            Predicate<Boolean> canWrite
    ) {
        return getPerceptContentStream(name, readingType, canWrite);
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.open("--> is PerceptHandlerWhenExpressionContext {");
        scb.line("perceptContentType = " + getPerceptContentType().getDebugPrint());
        scb.close("}");
        debugDumpPerception(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return "<evaluating when-expression>";
    }
}
