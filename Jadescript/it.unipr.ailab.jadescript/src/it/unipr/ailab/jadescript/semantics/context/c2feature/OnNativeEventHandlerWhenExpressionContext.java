package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class OnNativeEventHandlerWhenExpressionContext
        extends HandlerWhenExpressionContext
        implements CompilableName.Namespace, NativeEventHandledContext {

    public OnNativeEventHandlerWhenExpressionContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer
    ) {
        super(module, outer);
    }


    @Override
    public IJadescriptType getNativeEventType() {
        return module.get(TypeHelper.class).PROPOSITION;
    }

     @Override
    public Stream<? extends CompilableName> compilableNames(
        @Nullable String name
    ) {
        return SemanticsUtils.buildStream(
            this::getNativeEventName
        ).filter(n -> name == null || name.equals(n.name()));
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.open("--> is OnNativeEventHandlerWhenExpressionContext {");
        scb.line("nativeEventType = " +
            getNativeEventType().getDebugPrint());
        scb.close("}");
        debugDumpNativeEventHandled(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return "<evaluating when-expression>";
    }
}
