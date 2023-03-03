package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class OnPerceptHandlerWhenExpressionContext
        extends HandlerWhenExpressionContext
        implements CompilableName.Namespace, PerceptPerceivedContext {

    public OnPerceptHandlerWhenExpressionContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer
    ) {
        super(module, outer);
    }


    @Override
    public IJadescriptType getPerceptContentType() {
        return module.get(TypeHelper.class).PROPOSITION;
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
        scb.open("--> is OnPerceptHandlerWhenExpressionContext {");
        scb.line("perceptContentType = " +
            getPerceptContentType().getDebugPrint());
        scb.close("}");
        debugDumpPerception(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return "<evaluating when-expression>";
    }
}
