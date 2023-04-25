package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class OnNativeEventHandlerContext
        extends HandlerWithWhenExpressionContext
        implements CompilableName.Namespace, NativeEventHandledContext {

    private final IJadescriptType nativeEventType;

    public OnNativeEventHandlerContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer,
            IJadescriptType nativeEventType
    ) {
        super(module, outer, "native");
        this.nativeEventType = nativeEventType;
    }


    @Override
    public IJadescriptType getNativeEventType() {
        return nativeEventType;
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
        scb.line("--> is OnNativeEventHandlerContext");
        debugDumpNativeEventHandled(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return "on native";
    }
}
