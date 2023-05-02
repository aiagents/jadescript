package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.AssignableExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.SubscriptExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.Subscript;
import it.unipr.ailab.maybe.Maybe;


/**
 * Created on 26/08/18.
 */
@SuppressWarnings("restriction")
public class SubscriptionElement extends TrailersExpressionChainElement {

    private final Maybe<RValueExpression> key;


    public SubscriptionElement(
        SemanticsModule module,
        Maybe<RValueExpression> key
    ) {
        super(module);
        this.key = key;
    }


    private Maybe<Subscript> generateSubscript(
        ReversedTrailerChain rest
    ) {
        return Subscript.subscript(key, rest);
    }


    @Override
    public AssignableExpressionSemantics.SemanticsBoundToAssignableExpression<?>
    resolveChain(ReversedTrailerChain rest) {
        return new AssignableExpressionSemantics
            .SemanticsBoundToAssignableExpression<>(
            module.get(SubscriptExpressionSemantics.class),
            generateSubscript(rest)
        );
    }

}
