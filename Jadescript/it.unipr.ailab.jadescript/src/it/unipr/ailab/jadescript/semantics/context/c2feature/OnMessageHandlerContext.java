package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import jadescript.lang.Performative;

public class OnMessageHandlerContext
        extends HandlerWithWhenExpressionContext
        implements MessageReceivedContext {
    private final Maybe<String> performative;
    private final IJadescriptType messageContentType;
    private final IJadescriptType messageType;

    public OnMessageHandlerContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer,
            String eventType,
            Maybe<String> performative,
            IJadescriptType messageType,
            IJadescriptType messageContentType
    ) {
        super(module, outer, eventType);
        this.performative = performative;
        this.messageContentType = messageContentType;
        this.messageType = messageType;
    }

    @Override
    public Maybe<Performative> getPerformative() {
        return performative.__(Performative.performativeByName::get);
    }

    @Override
    public IJadescriptType getMessageContentType() {
        return messageContentType;
    }

    @Override
    public IJadescriptType getMessageType() {
        return messageType;
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is OnMessageHandlerContext");
        debugDumpReceivedMessage(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return "on " + performative.orElse("message");
    }


}
