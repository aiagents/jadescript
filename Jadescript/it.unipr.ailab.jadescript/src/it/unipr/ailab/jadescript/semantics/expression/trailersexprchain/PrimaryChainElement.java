package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.PrimaryExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;

/**
 * Created on 26/08/18.
 *
 */
public class PrimaryChainElement extends TrailersExpressionChainElement {

    private final Maybe<Primary> atom;
    private final PrimaryExpressionSemantics primaryExpressionSemantics;

    public PrimaryChainElement(
            SemanticsModule injector,
            Maybe<Primary> atom
    ) {
        super(injector);
        this.atom = atom;
        this.primaryExpressionSemantics = injector.get(PrimaryExpressionSemantics.class);
    }


    @Override
    public String compile(ReversedDotNotationChain rest) {
        //rest should be empty, so it's ignored
        return primaryExpressionSemantics.compile(atom).orElse("");
    }

    @Override
    public IJadescriptType inferType(ReversedDotNotationChain rest) {
        //rest should be empty, so it's ignored
        return primaryExpressionSemantics.inferType(atom);
    }

    @Override
    public void validate(ReversedDotNotationChain rest, ValidationMessageAcceptor acceptor) {
        //rest should be empty, so it's ignored
        primaryExpressionSemantics.validate(atom, acceptor);
    }

    @Override
    public void validateAssignment(
            ReversedDotNotationChain rest,
            String assignmentOperator,
            Maybe<RValueExpression> rValueExpression,
            IJadescriptType typeOfRExpr,
            ValidationMessageAcceptor acceptor
    ) {
        //rest should be empty, so it's ignored
        primaryExpressionSemantics.validateAssignment(atom, assignmentOperator, rValueExpression, acceptor);
    }

    @Override
    public void syntacticValidateLValue(InterceptAcceptor acceptor) {
        primaryExpressionSemantics.syntacticValidateLValue(atom, acceptor);
    }

    @Override
    public String compileAssignment(
            ReversedDotNotationChain rest,
            String compiledExpression,
            IJadescriptType exprType
    ) {
        //rest should be empty, so it's ignored
        return primaryExpressionSemantics.compileAssignment(atom, compiledExpression, exprType).orElse("");
    }

    @Override
    public boolean isAlwaysPure(ReversedDotNotationChain rest) {
        return primaryExpressionSemantics.isAlwaysPure(atom);
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> getSubExpressions(ReversedDotNotationChain rest) {
        //rest should be empty, so it's ignored
        return primaryExpressionSemantics.getSubExpressions(atom);
    }


}
