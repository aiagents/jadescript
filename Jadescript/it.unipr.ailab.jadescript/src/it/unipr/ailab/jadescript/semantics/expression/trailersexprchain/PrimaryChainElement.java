package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.AssignableExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.PrimaryExpressionSemantics;
import it.unipr.ailab.maybe.Maybe;

/**
 * Created on 26/08/18.
 */
public class PrimaryChainElement extends TrailersExpressionChainElement {

    private final Maybe<Primary> atom;
    private final PrimaryExpressionSemantics primaryExpressionSemantics;

    public PrimaryChainElement(
        SemanticsModule module,
        Maybe<Primary> atom
    ) {
        super(module);
        this.atom = atom;
        this.primaryExpressionSemantics =
            module.get(PrimaryExpressionSemantics.class);
    }

    @Override
    public AssignableExpressionSemantics.SemanticsBoundToAssignableExpression<?>
    resolveChain(ReversedTrailerChain withoutFirst) {
        return new AssignableExpressionSemantics
            .SemanticsBoundToAssignableExpression<>(
            primaryExpressionSemantics,
            atom
        );
    }
}
