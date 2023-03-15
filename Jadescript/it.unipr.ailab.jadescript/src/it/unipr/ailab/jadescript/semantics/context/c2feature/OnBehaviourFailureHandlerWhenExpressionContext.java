package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class OnBehaviourFailureHandlerWhenExpressionContext
    extends HandlerWhenExpressionContext
    implements CompilableName.Namespace, BehaviourFailureHandledContext {



    public OnBehaviourFailureHandlerWhenExpressionContext(
        SemanticsModule module,
        ProceduralFeatureContainerContext outer
    ) {
        super(module, outer);
    }


    @Override
    public String getCurrentOperationLogName() {
        return "<evaluating when-expression>";
    }


    @Override
    public IJadescriptType getFailureReasonType() {
        return module.get(TypeHelper.class).PROPOSITION;
    }


    @Override
    public IJadescriptType getFailedBehaviourType() {
        return module.get(TypeHelper.class).ANYBEHAVIOUR;
    }


     @Override
    public Stream<? extends CompilableName> compilableNames(
        @Nullable String name
    ) {
        return Util.buildStream(
            this::getFailureReasonName,
            this::getFailedBehaviourName
        ).filter(n -> name == null || name.equals(n.name()));
    }

}
