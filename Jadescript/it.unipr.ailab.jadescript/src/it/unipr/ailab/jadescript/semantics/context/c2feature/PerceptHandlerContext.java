package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;

public class PerceptHandlerContext
        extends HandlerWithWhenExpressionContext
        implements PerceptPerceivedContext {

    private final IJadescriptType perceptContentType;

    public PerceptHandlerContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer,
            String eventType,
            IJadescriptType perceptContentType
    ) {
        super(module, outer, eventType);
        this.perceptContentType = perceptContentType;
    }


    @Override
    public IJadescriptType getPerceptContentType() {
        return perceptContentType;
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is PerceptHandlerContext");
        debugDumpPerception(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return "on percept";
    }
}
