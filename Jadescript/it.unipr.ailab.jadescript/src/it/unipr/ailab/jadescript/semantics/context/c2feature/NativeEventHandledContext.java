package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public interface NativeEventHandledContext extends SemanticsConsts {

    static ContextGeneratedName nativeEventContextGeneratedReference(
        IJadescriptType contentType
    ) {
        return new ContextGeneratedName(
            "event",
            contentType,
            () -> NATIVE_EVENT_CONTENT_VAR_NAME
        );
    }

    IJadescriptType getNativeEventType();

    default CompilableName getNativeEventName() {
        return nativeEventContextGeneratedReference(getNativeEventType());
    }

    default void debugDumpNativeEventHandled(SourceCodeBuilder scb) {
        scb.open("--> is NativeEventHandledContext {");
        scb.line("nativeEventType = " + getNativeEventType()
            .getDebugPrint());
        scb.close("}");
    }

}
