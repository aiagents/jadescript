package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.LValueExpression;
import it.unipr.ailab.jadescript.jadescript.OfNotation;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.maybe.Maybe;

import java.util.Optional;

/**
 * Created on 28/12/16.
 */
@Singleton
public class LValueExpressionSemantics
    extends AssignableExpressionSemantics.AssignableAdapter<LValueExpression> {

    public LValueExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected boolean mustTraverse(Maybe<LValueExpression> input) {
        return true;
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(Maybe<LValueExpression> input) {
        return Optional.of(new SemanticsBoundToAssignableExpression<>(
            module.get(OfNotationExpressionSemantics.class),
            input.__(i -> (OfNotation) i)
        ));
    }

}
