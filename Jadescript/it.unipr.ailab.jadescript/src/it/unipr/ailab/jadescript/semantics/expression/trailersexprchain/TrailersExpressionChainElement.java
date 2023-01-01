package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.AssignableExpressionSemantics;
import it.unipr.ailab.sonneteer.WriterFactory;

/**
 * Created on 26/08/18.
 */
public abstract class TrailersExpressionChainElement {

    public static final WriterFactory w = WriterFactory.getInstance();

    protected final SemanticsModule module;

    public TrailersExpressionChainElement(
        SemanticsModule module
    ) {
        this.module = module;
    }

    public abstract AssignableExpressionSemantics
        .SemanticsBoundToAssignableExpression<?>
    resolveChain(ReversedTrailerChain withoutFirst);
}
