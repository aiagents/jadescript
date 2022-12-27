package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


public class PlaceholderExpressionSemantics extends ExpressionSemantics<Primary> {
    public PlaceholderExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    /**
     * Produces an error validator message that notifies that the placeholder cannot be used as normal expression.
     */
    protected boolean errorNotRExpression(
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
        return INVALID;
    }

    @Override
    protected boolean validateInternal(Maybe<Primary> input, ValidationMessageAcceptor acceptor) {
        return errorNotRExpression(input, acceptor);
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<Primary> input) {
        return Collections.emptyList();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<Primary> input) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<Primary> input) {
        return Stream.empty();
    }

    @Override
    protected String compileInternal(Maybe<Primary> input, CompilationOutputAcceptor acceptor) {
        return "/*ERROR: placeholder compiled*/null";
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<Primary> input) {
        return module.get(TypeHelper.class).TOP.apply(
                "Cannot compute the type of a '_' placeholder used as expression."
        );
    }

    @Override
    protected boolean mustTraverse(Maybe<Primary> input) {
        return false;
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<Primary> input) {
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<Primary> input) {
        return true;
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<Primary, ?, ?> input,
            CompilationOutputAcceptor acceptor
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
    protected boolean isHoledInternal(Maybe<Primary> input) {
        return true;
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<Primary> input) {
        return true;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<Primary> input) {
        return false;
    }

    @Override
    protected boolean isAlwaysPureInternal(Maybe<Primary> input) {
        return true;
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<Primary> input) {
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<Primary> input) {
        return true;
    }
}
