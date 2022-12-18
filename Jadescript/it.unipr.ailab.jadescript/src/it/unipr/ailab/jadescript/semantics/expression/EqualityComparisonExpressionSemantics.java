package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.EqualityComparison;
import it.unipr.ailab.jadescript.jadescript.TypeComparison;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.StatementCompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.jadescript.semantics.expression.ExpressionCompilationResult.result;


/**
 * Created on 28/12/16.
 */
@Singleton
public class EqualityComparisonExpressionSemantics extends ExpressionSemantics<EqualityComparison> {

    public static final String EQUALS_OPERATOR = "=";
    public static final String NOT_EQUALS_OPERATOR = "!=";

    public EqualityComparisonExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<EqualityComparison> input) {
        if (mustTraverse(input)) {
            Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }
        return Arrays.asList(
                input.__(EqualityComparison::getLeft)
                        .extract(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(
                                module.get(TypeComparisonExpressionSemantics.class), x)),
                input.__(EqualityComparison::getRight)
                        .extract(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(
                                module.get(TypeComparisonExpressionSemantics.class), x))
        );
    }


    @Override
    public ExpressionCompilationResult compile(
            Maybe<EqualityComparison> input,
            StatementCompilationOutputAcceptor acceptor
    ) {
        Maybe<TypeComparison> left = input.__(EqualityComparison::getLeft);
        final ExpressionCompilationResult leftResult = module.get(TypeComparisonExpressionSemantics.class)
                .compile(left, acceptor);

        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);
        if (right.isNothing()) {
            return leftResult;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(leftResult);

        String equalityOp = input.__(EqualityComparison::getEqualityOp).orElse("");
        if (equalityOp.equals("≠")) {
            equalityOp = "!=";
        }

        String not;
        if (equalityOp.equals(NOT_EQUALS_OPERATOR)) {
            not = "!";
        } else {
            not = "";
        }

        String stringSoFar = sb.toString();
        sb = new StringBuilder();
        sb.append(not);
        sb.append("java.util.Objects.equals(")
                .append(stringSoFar)
                .append(", ")
                .append(module.get(TypeComparisonExpressionSemantics.class).compile(right, acceptor).getGeneratedText())
                .append(")");


        return result(sb.toString());
    }

    @Override
    public IJadescriptType inferType(Maybe<EqualityComparison> input) {
        Maybe<TypeComparison> left = input.__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);

        if (right.isNothing()) {
            return module.get(TypeComparisonExpressionSemantics.class).inferType(left);
        } else {
            return module.get(TypeHelper.class).BOOLEAN;
        }
    }


    @Override
    public boolean mustTraverse(Maybe<EqualityComparison> input) {
        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);
        return right.isNothing();
    }

    @Override
    public Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traverse(Maybe<EqualityComparison> input) {
        if (mustTraverse(input)) {
            return Optional.ofNullable(input.__(EqualityComparison::getLeft))
                    .map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(
                            module.get(TypeComparisonExpressionSemantics.class),
                            x
                    ));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void validate(Maybe<EqualityComparison> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        module.get(TypeComparisonExpressionSemantics.class).validate(input.__(EqualityComparison::getLeft), acceptor);
        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);
        if (right.isPresent()) {
            module.get(TypeComparisonExpressionSemantics.class).validate(right, acceptor);
        }

    }

    @Override
    public boolean isHoled(Maybe<EqualityComparison> input) {
        Maybe<TypeComparison> left = input.__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);
        String equalityOp = input.__(EqualityComparison::getEqualityOp).orElse("");

        return equalityOp.equals(EQUALS_OPERATOR) && (
                module.get(TypeComparisonExpressionSemantics.class).isHoled(left)
                        || module.get(TypeComparisonExpressionSemantics.class).isHoled(right)
        );

    }

    @Override
    public boolean isTypelyHoled(Maybe<EqualityComparison> input) {
        Maybe<TypeComparison> left = input.__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);
        String equalityOp = input.__(EqualityComparison::getEqualityOp).orElse("");

        return equalityOp.equals(EQUALS_OPERATOR) && (
                module.get(TypeComparisonExpressionSemantics.class).isTypelyHoled(left)
                        || module.get(TypeComparisonExpressionSemantics.class).isTypelyHoled(right)
        );
    }

    @Override
    public boolean isUnbound(Maybe<EqualityComparison> input) {
        Maybe<TypeComparison> left = input.__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);
        String equalityOp = input.__(EqualityComparison::getEqualityOp).orElse("");

        return equalityOp.equals(EQUALS_OPERATOR) && (
                module.get(TypeComparisonExpressionSemantics.class).isUnbound(left)
                        || module.get(TypeComparisonExpressionSemantics.class).isUnbound(right)
        );
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<EqualityComparison, ?, ?> input,
            StatementCompilationOutputAcceptor acceptor
    ) {
        Maybe<TypeComparison> left = input.getPattern().__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input.getPattern().__(EqualityComparison::getRight);
        String equalityOp = input.getPattern().__(EqualityComparison::getEqualityOp).orElse("");
        final TypeComparisonExpressionSemantics tces = module.get(TypeComparisonExpressionSemantics.class);
        if (right.isNothing()) {
            return tces.compilePatternMatchInternal(input.replacePattern(left), acceptor);
        } else if (equalityOp.equals(NOT_EQUALS_OPERATOR) || !tces.isHoled(left)) {
            return compileExpressionEqualityPatternMatch(input, acceptor);
        } else {
            final IJadescriptType rType = tces.inferSubPatternType(right).solve(input.getProvidedInputType());
            PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> rightResult =
                    tces.compilePatternMatch(input.subPattern(
                            input.getProvidedInputType(),
                            __ -> right.toNullable(),
                            "_right"
                    ), acceptor);
            final PatternMatchInput.SubPattern<TypeComparison, EqualityComparison, ?, ?> leftSubpattern =
                    input.subPattern(
                            rType,
                            __ -> left.toNullable(),
                            "_left"
                    );
            PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> leftResult =
                    tces.compilePatternMatch(leftSubpattern, acceptor);
            final List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>> subResults =
                    List.of(rightResult, leftResult);
            return input.createCompositeMethodOutput(
                    rType,
                    (Integer i) -> {
                        if (i < 0 || i >= 2) {
                            return "/* Index out of bounds */";
                        } else {
                            return "__x";
                        }
                    },
                    subResults,
                    () -> PatternMatchOutput.collectUnificationResults(subResults),
                    () -> new PatternMatchOutput.WithTypeNarrowing(
                            tces.inferSubPatternType(leftSubpattern.getPattern()).solve(rType)
                    )
            );
        }
    }

    @Override
    public boolean isPatternEvaluationPure(Maybe<EqualityComparison> input) {
        Maybe<TypeComparison> left = input.__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);
        String equalityOp = input.__(EqualityComparison::getEqualityOp).orElse("");
        final TypeComparisonExpressionSemantics tces = module.get(TypeComparisonExpressionSemantics.class);

        if (right.isNothing()) {
            return tces.isPatternEvaluationPure(left);
        } else if (equalityOp.equals(NOT_EQUALS_OPERATOR) || !tces.isHoled(left)) {
            return isAlwaysPure(input);
        } else {
            return tces.isPatternEvaluationPure(right) && tces.isPatternEvaluationPure(left);
        }
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<EqualityComparison> input) {
        Maybe<TypeComparison> left = input.__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);
        String equalityOp = input.__(EqualityComparison::getEqualityOp).orElse("");
        final TypeComparisonExpressionSemantics tces = module.get(TypeComparisonExpressionSemantics.class);
        if (right.isNothing()) {
            return tces.inferPatternTypeInternal(left);
        } else if (equalityOp.equals(NOT_EQUALS_OPERATOR) || !tces.isTypelyHoled(left)) {
            return PatternType.simple(module.get(TypeHelper.class).BOOLEAN);
        } else {
            return tces.inferSubPatternType(right);
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<EqualityComparison, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        Maybe<TypeComparison> left = input.getPattern().__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input.getPattern().__(EqualityComparison::getRight);
        String equalityOp = input.getPattern().__(EqualityComparison::getEqualityOp).orElse("");
        final TypeComparisonExpressionSemantics tces = module.get(TypeComparisonExpressionSemantics.class);
        if (right.isNothing()) {
            return tces.validatePatternMatchInternal(
                    input.replacePattern(left),
                    acceptor
            );
        } else if (equalityOp.equals(NOT_EQUALS_OPERATOR) || !tces.isHoled(left)) {
            return validateExpressionEqualityPatternMatch(input, acceptor);
        } else {
            final IJadescriptType rType = tces.inferSubPatternType(right).solve(input.getProvidedInputType());
            PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> rightResult =
                    tces.validatePatternMatch(input.subPattern(
                            input.getProvidedInputType(),
                            __ -> right.toNullable(),
                            "_right"
                    ), acceptor);
            final PatternMatchInput.SubPattern<TypeComparison, EqualityComparison, ?, ?> leftSubpattern =
                    input.subPattern(
                            rType,
                            __ -> left.toNullable(),
                            "_left"
                    );
            PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> leftResult =
                    tces.validatePatternMatchInternal(leftSubpattern, acceptor);

            final List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?>> subResults =
                    List.of(rightResult, leftResult);

            return input.createValidationOutput(
                    () -> PatternMatchOutput.collectUnificationResults(subResults),
                    () -> new PatternMatchOutput.WithTypeNarrowing(
                            tces.inferSubPatternType(leftSubpattern.getPattern()).solve(rType)
                    )
            );
        }
    }
}
