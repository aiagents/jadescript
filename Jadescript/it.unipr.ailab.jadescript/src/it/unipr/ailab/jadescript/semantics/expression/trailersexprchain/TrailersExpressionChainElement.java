package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;

/**
 * Created on 26/08/18.
 *
 */
public abstract class TrailersExpressionChainElement {

    protected final SemanticsModule module;

    public TrailersExpressionChainElement(
            SemanticsModule module
    ) {
        this.module = module;
    }


    public abstract String compile(ReversedDotNotationChain rest);

    public abstract IJadescriptType inferType(ReversedDotNotationChain rest);

    public abstract void validate(ReversedDotNotationChain rest, ValidationMessageAcceptor acceptor);

    public abstract void validateAssignment(
            ReversedDotNotationChain rest,
            String assignmentOperator,
            Maybe<RValueExpression> rValueExpression,
            IJadescriptType typeOfRExpr,
            ValidationMessageAcceptor acceptor
    );

    public abstract void syntacticValidateLValue(InterceptAcceptor acceptor);

    public abstract String compileAssignment(
            ReversedDotNotationChain rest,
            String compiledExpression,
            IJadescriptType exprType
    );

    public abstract boolean isAlwaysPure(ReversedDotNotationChain rest);

    public abstract List<ExpressionSemantics.SemanticsBoundToExpression<?>> getSubExpressions(ReversedDotNotationChain rest);
}
