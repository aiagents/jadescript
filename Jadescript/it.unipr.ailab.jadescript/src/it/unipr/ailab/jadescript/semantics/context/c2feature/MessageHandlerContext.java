package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import jadescript.lang.Performative;

import java.util.List;

public class MessageHandlerContext
        extends HandlerWithWhenExpressionContext
        implements MessageReceivedContext {
    private final Maybe<String> performative;
    private final IJadescriptType messageContentType;
    private final IJadescriptType messageType;

    public MessageHandlerContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer,
            String eventType,
            Maybe<String> performative,
            List<NamedSymbol> patternMatchAutoDeclaredVariables,
            IJadescriptType messageType,
            IJadescriptType messageContentType
    ) {
        super(module, outer, eventType, patternMatchAutoDeclaredVariables);
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
        scb.line("--> is MessageHandlerContext");
        debugDumpReceivedMessage(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return "on " + performative.orElse("message");
    }


}
