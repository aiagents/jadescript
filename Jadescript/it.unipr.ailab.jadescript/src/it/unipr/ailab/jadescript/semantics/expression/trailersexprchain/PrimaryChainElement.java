package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.AtomExpr;
import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.PrimaryExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
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
    public String compile(ReversedTrailerChain rest) {
        //rest should be empty, so it's ignored
        return primaryExpressionSemantics.compile(atom).orElse("");
    }

    @Override
    public IJadescriptType inferType(ReversedTrailerChain rest) {
        //rest should be empty, so it's ignored
        return primaryExpressionSemantics.inferType(atom);
    }

    @Override
    public void validate(ReversedTrailerChain rest, ValidationMessageAcceptor acceptor) {
        //rest should be empty, so it's ignored
        primaryExpressionSemantics.validate(atom, acceptor);
    }

    @Override
    public void validateAssignment(
            ReversedTrailerChain rest,
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
            ReversedTrailerChain rest,
            String compiledExpression,
            IJadescriptType exprType
    ) {
        //rest should be empty, so it's ignored
        return primaryExpressionSemantics.compileAssignment(atom, compiledExpression, exprType).orElse("");
    }

    @Override
    public boolean isAlwaysPure(ReversedTrailerChain rest) {
        return primaryExpressionSemantics.isAlwaysPure(atom);
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> getSubExpressions(ReversedTrailerChain rest) {
        //rest should be empty, so it's ignored
        return primaryExpressionSemantics.getSubExpressions(atom);
    }

    @Override
    public boolean isHoled(ReversedTrailerChain rest) {
        //rest should be empty, so it's ignored
        return primaryExpressionSemantics.isHoled(atom);
    }

    @Override
    public boolean isUnbounded(ReversedTrailerChain rest) {
        //rest should be empty, so it's ignored
        return primaryExpressionSemantics.isUnbounded(atom);
    }

    @Override
    public PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest
    ) {
        //rest should be empty, so it's ignored
        return primaryExpressionSemantics.compilePatternMatch(input.mapPattern(__ -> atom.toNullable()));
    }

    @Override
    public PatternType inferPatternTypeInternal(PatternMatchInput<AtomExpr, ?, ?> input, ReversedTrailerChain rest) {
        //rest should be empty, so it's ignored
        return primaryExpressionSemantics.inferPatternType(input.mapPattern(__ -> atom.toNullable()));
    }

    @Override
    public PatternMatchOutput<PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest,
            ValidationMessageAcceptor acceptor
    ) {
        //rest should be empty, so it's ignored
        return primaryExpressionSemantics.validatePatternMatch(
                input.mapPattern(__ -> atom.toNullable()),
                acceptor
        );
    }

    @Override
    public boolean isTypelyHoled(ReversedTrailerChain rest) {
        return primaryExpressionSemantics.isTypelyHoled(atom);
    }
}
