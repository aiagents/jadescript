package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.AtomExpr;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionCompilationResult;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.StatementCompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.WriterFactory;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;

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


    public abstract ExpressionCompilationResult compile(
            ReversedTrailerChain rest,
            StatementCompilationOutputAcceptor acceptor
    );

    public abstract IJadescriptType inferType(ReversedTrailerChain rest);

    public abstract void validate(ReversedTrailerChain rest, ValidationMessageAcceptor acceptor);

    public abstract void validateAssignment(
            ReversedTrailerChain rest,
            Maybe<RValueExpression> rValueExpression,
            IJadescriptType typeOfRExpr,
            ValidationMessageAcceptor acceptor
    );

    public abstract void syntacticValidateLValue(InterceptAcceptor acceptor);

    public abstract void compileAssignment(
            ReversedTrailerChain rest,
            String compiledExpression,
            IJadescriptType exprType,
            StatementCompilationOutputAcceptor acceptor
    );

    public abstract boolean isAlwaysPure(ReversedTrailerChain rest);

    public abstract List<ExpressionSemantics.SemanticsBoundToExpression<?>> getSubExpressions(ReversedTrailerChain rest);

    public abstract boolean isHoled(ReversedTrailerChain rest);

    public abstract boolean isUnbounded(ReversedTrailerChain rest);

    public abstract PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest,
            StatementCompilationOutputAcceptor acceptor
    );

    public abstract PatternType inferPatternTypeInternal(
            Maybe<AtomExpr> input,
            ReversedTrailerChain rest
    );

    public abstract PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest,
            ValidationMessageAcceptor acceptor
    );

    public abstract boolean isTypelyHoled(ReversedTrailerChain rest);

    public abstract boolean isValidLexpr(ReversedTrailerChain rest);

    public abstract boolean isPatternEvaluationPure(ReversedTrailerChain rest);
}
