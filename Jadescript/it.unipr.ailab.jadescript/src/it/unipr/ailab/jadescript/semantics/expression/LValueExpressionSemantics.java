package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Optional;
import java.util.stream.Stream;

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
    traverse(Maybe<LValueExpression> input) {
        return Optional.of(new SemanticsBoundToAssignableExpression<>(
                module.get(OfNotationExpressionSemantics.class),
                input.__(i -> (OfNotation) i)
        ));
    }

}
