package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;
import java.util.function.Function;

public abstract class HandlerWhenExpressionContext
        extends ProceduralFeatureContext
        implements WhenExpressionContext {

    private final Function<List<String>, IJadescriptType> upperBoundComputer;

    public HandlerWhenExpressionContext(
            SemanticsModule module,
            //upperBoundComputer: takes a property chain in input and produces an upper bound type for that property chain
            Function<List<String>, IJadescriptType> upperBoundComputer,
            ProceduralFeatureContainerContext outer
    ) {
        super(module, outer);
        this.upperBoundComputer = upperBoundComputer;
    }

    //TODO generalize "property chains" in "flow-typeable values"
    public IJadescriptType computeUpperBoundForPropertyChain(List<String> propertyChain){
        return upperBoundComputer.apply(propertyChain);
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is HandlerWhenExpressionContext");
        debugDumpIsWhenExpressionContext(scb);
    }
}
