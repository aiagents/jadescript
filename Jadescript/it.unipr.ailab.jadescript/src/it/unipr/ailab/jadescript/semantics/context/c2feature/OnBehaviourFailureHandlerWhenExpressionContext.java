package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
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
        return module.get(BuiltinTypeProvider.class).proposition();
    }


    @Override
    public IJadescriptType getFailedBehaviourType() {
        return module.get(BuiltinTypeProvider.class).anyBehaviour();
    }


     @Override
    public Stream<? extends CompilableName> compilableNames(
        @Nullable String name
    ) {
        return SemanticsUtils.buildStream(
            this::getFailureReasonName,
            this::getFailedBehaviourName
        ).filter(n -> name == null || name.equals(n.name()));
    }

}
