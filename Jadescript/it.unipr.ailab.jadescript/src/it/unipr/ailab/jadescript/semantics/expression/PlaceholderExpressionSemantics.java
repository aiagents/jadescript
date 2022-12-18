package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.StatementCompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.jadescript.semantics.expression.ExpressionCompilationResult.result;

public class PlaceholderExpressionSemantics extends ExpressionSemantics<Primary> {
    public PlaceholderExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    /**
     * Produces an error validator message that notifies that the placeholder cannot be used as normal expression.
     */
    protected void errorNotRExpression(
            Maybe<Primary> input,
            ValidationMessageAcceptor acceptor
    ) {
        Util.extractEObject(input).safeDo(inputSafe -> {
            acceptor.acceptError(
                    "'_' placeholder cannot be used as evaluateable expression",
                    inputSafe,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidStatement"
            );

        });
    }

    @Override
    public void validate(Maybe<Primary> input, ValidationMessageAcceptor acceptor) {
        errorNotRExpression(input, acceptor);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<Primary> input) {
        return List.of();
    }

    @Override
    public ExpressionCompilationResult compile(Maybe<Primary> input, StatementCompilationOutputAcceptor acceptor) {
        return result("/*ERROR: placeholder compiled*/null");
    }

    @Override
    public IJadescriptType inferType(Maybe<Primary> input) {
        return module.get(TypeHelper.class).TOP.apply(
                "Cannot compute the type of a '_' placeholder used as expression."
        );
    }

    @Override
    public boolean mustTraverse(Maybe<Primary> input) {
        return false;
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<Primary> input) {
        return Optional.empty();
    }

    @Override
    public boolean isPatternEvaluationPure(Maybe<Primary> input) {
        return true;
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<Primary, ?, ?> input,
            StatementCompilationOutputAcceptor acceptor
    ) {
        IJadescriptType solvedPatternType = input.getProvidedInputType();
        return input.createPlaceholderMethodOutput(
                solvedPatternType,
                () -> PatternMatchOutput.EMPTY_UNIFICATION,
                () -> new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
        );
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<Primary> input) {
        return PatternType.holed(t -> t);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<Primary, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        IJadescriptType solvedPatternType = input.getProvidedInputType();
        return input.createValidationOutput(
                () -> PatternMatchOutput.EMPTY_UNIFICATION,
                () -> new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
        );
    }


    @Override
    public boolean isHoled(Maybe<Primary> input) {
        return true;
    }

    @Override
    public boolean isTypelyHoled(Maybe<Primary> input) {
        return true;
    }

    @Override
    public boolean isUnbound(Maybe<Primary> input) {
        return false;
    }
}
