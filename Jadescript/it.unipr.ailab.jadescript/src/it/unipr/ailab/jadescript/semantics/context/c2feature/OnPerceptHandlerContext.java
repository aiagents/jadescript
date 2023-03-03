package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class OnPerceptHandlerContext
        extends HandlerWithWhenExpressionContext
        implements CompilableName.Namespace, PerceptPerceivedContext {

    private final IJadescriptType perceptContentType;

    public OnPerceptHandlerContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer,
            IJadescriptType perceptContentType
    ) {
        super(module, outer, "percept");
        this.perceptContentType = perceptContentType;
    }


    @Override
    public IJadescriptType getPerceptContentType() {
        return perceptContentType;
    }


    @Override
    public Stream<? extends CompilableName> compilableNames(
        @Nullable String name
    ) {
        return Util.buildStream(
            this::getPerceptContentName
        ).filter(n -> name == null || name.equals(n.name()));
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is OnPerceptHandlerContext");
        debugDumpPerception(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return "on percept";
    }
}
