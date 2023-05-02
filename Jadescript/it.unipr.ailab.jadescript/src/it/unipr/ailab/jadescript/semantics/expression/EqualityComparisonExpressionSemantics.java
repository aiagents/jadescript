package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.EqualityComparison;
import it.unipr.ailab.jadescript.jadescript.TypeComparison;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.SubPattern;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * Created on 28/12/16.
 */
@Singleton
public class EqualityComparisonExpressionSemantics
    extends ExpressionSemantics<EqualityComparison> {

    public static final String EQUALS_OPERATOR = "=";
    public static final String NOT_EQUALS_OPERATOR = "!=";


    public EqualityComparisonExpressionSemantics(
        SemanticsModule semanticsModule
    ) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<EqualityComparison> input
    ) {
        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);
        return Stream.of(
                input.__(EqualityComparison::getLeft),
                input.__(EqualityComparison::getRight)
            ).filter(Maybe::isPresent)
            .map(i -> new SemanticsBoundToExpression<>(tces, i));
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<EqualityComparison> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<EqualityComparison> input,
        StaticState state
    ) {
        Maybe<TypeComparison> left = input.__(EqualityComparison::getLeft);
        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);
        final Maybe<ExpressionDescriptor> edLeft = tces.describeExpression(
            left,
            state
        );
        final StaticState afterLeft = tces.advance(left, state);

        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);
        final Maybe<ExpressionDescriptor> edRight = tces.describeExpression(
            right,
            state
        );

        final StaticState afterRight = tces.advance(right, afterLeft);

        return afterRight.assertExpressionsEqual(edLeft, edRight);
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<EqualityComparison> input,
        StaticState state
    ) {
        Maybe<TypeComparison> left =
            input.getPattern().__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right =
            input.getPattern().__(EqualityComparison::getRight);
        String equalityOp =
            input.getPattern().__(EqualityComparison::getEqualityOp).orElse("");
        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);

        if (equalityOp.equals(NOT_EQUALS_OPERATOR)) {
            return advance(input.getPattern(), state);
        } else {

            final SubPattern<TypeComparison, EqualityComparison>
                rightSubpattern =
                input.subPattern(
                    input.getProvidedInputType(),
                    __ -> right.toNullable(),
                    "_eqright"
                );
            final IJadescriptType rType = tces.inferPatternType(
                rightSubpattern,
                state
            ).solve(input.getProvidedInputType());

            StaticState afterRight = tces.advancePattern(
                rightSubpattern,
                state
            );

            afterRight = tces.assertDidMatch(
                rightSubpattern,
                afterRight
            );


            final SubPattern<
                TypeComparison,
                EqualityComparison
                > leftSubpattern = input.subPattern(
                rType,
                __ -> left.toNullable(),
                "_eqleft"
            );


            return tces.advancePattern(
                leftSubpattern,
                afterRight
            );
        }
    }


    @Override
    protected String compileInternal(
        Maybe<EqualityComparison> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        Maybe<TypeComparison> left = input.__(EqualityComparison::getLeft);
        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);

        final String leftResult = tces.compile(left, state, acceptor);
        final StaticState afterLeft = tces.advance(left, state);

        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);


        String equalityOp = input.__(EqualityComparison::getEqualityOp)
            .orElse("");
        if (equalityOp.equals("≠")) {
            equalityOp = "!=";
        }

        String not;
        if (equalityOp.equals(NOT_EQUALS_OPERATOR)) {
            not = "!";
        } else {
            not = "";
        }

        final String rightCompiled = tces.compile(right, afterLeft, acceptor);


        return not + "java.util.Objects.equals(" +
            leftResult + ", " +
            rightCompiled + ")";
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<EqualityComparison> input,
        StaticState state
    ) {
        return module.get(BuiltinTypeProvider.class).boolean_();
    }


    @Override
    protected boolean mustTraverse(Maybe<EqualityComparison> input) {
        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);
        return right.isNothing();
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>>
    traverseInternal(
        Maybe<EqualityComparison> input
    ) {
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
    protected boolean validateInternal(
        Maybe<EqualityComparison> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);
        final Maybe<TypeComparison> left =
            input.__(EqualityComparison::getLeft);

        final boolean leftValidation = tces.validate(left, state, acceptor);
        final StaticState afterLeft = tces.advance(left, state);

        final Maybe<TypeComparison> right =
            input.__(EqualityComparison::getRight);

        final boolean rightValidation = tces.validate(
            right,
            afterLeft,
            acceptor
        );
        return leftValidation && rightValidation;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<EqualityComparison> input,
        StaticState state
    ) {
        Maybe<TypeComparison> left = input.getPattern()
            .__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input.getPattern()
            .__(EqualityComparison::getRight);
        String equalityOp = input.getPattern()
            .__(EqualityComparison::getEqualityOp)
            .orElse("");

        if (!equalityOp.equals(EQUALS_OPERATOR)) {
            return false;
        }

        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);

        final SubPattern<TypeComparison, EqualityComparison>
            rightSubpattern = input.subPattern(
            input.getProvidedInputType(),
            __ -> right.toNullable(),
            "_eqright"
        );

        if (tces.isHoled(rightSubpattern, state)) {
            return true;
        }

        final IJadescriptType rType = tces.inferPatternType(
            rightSubpattern,
            state
        ).solve(input.getProvidedInputType());


        StaticState afterRight = tces.advancePattern(
            rightSubpattern, state
        );

        afterRight = tces.assertDidMatch(
            rightSubpattern,
            afterRight
        );

        final SubPattern<
            TypeComparison,
            EqualityComparison
            > leftSubpattern = input.subPattern(
            rType,
            __ -> left.toNullable(),
            "_eqleft"
        );
        return tces.isHoled(leftSubpattern, afterRight);
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<EqualityComparison> input,
        StaticState state
    ) {
        Maybe<TypeComparison> left = input.getPattern()
            .__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input.getPattern()
            .__(EqualityComparison::getRight);
        String equalityOp = input.getPattern()
            .__(EqualityComparison::getEqualityOp)
            .orElse("");

        if (!equalityOp.equals(EQUALS_OPERATOR)) {
            return false;
        }

        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);

        final SubPattern<TypeComparison, EqualityComparison>
            rightSubpattern = input.subPattern(
            input.getProvidedInputType(),
            __ -> right.toNullable(),
            "_eqright"
        );

        if (tces.isTypelyHoled(rightSubpattern, state)) {
            return true;
        }

        final IJadescriptType rType = tces.inferPatternType(
            rightSubpattern,
            state
        ).solve(input.getProvidedInputType());


        StaticState afterRight = tces.advancePattern(
            rightSubpattern, state
        );

        afterRight = tces.assertDidMatch(
            rightSubpattern,
            afterRight
        );

        final SubPattern<
            TypeComparison,
            EqualityComparison
            > leftSubpattern = input.subPattern(
            rType,
            __ -> left.toNullable(),
            "_eqleft"
        );
        return tces.isTypelyHoled(leftSubpattern, afterRight);
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<EqualityComparison> input,
        StaticState state
    ) {
        Maybe<TypeComparison> left = input.getPattern()
            .__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input.getPattern()
            .__(EqualityComparison::getRight);
        String equalityOp = input.getPattern()
            .__(EqualityComparison::getEqualityOp)
            .orElse("");

        if (!equalityOp.equals(EQUALS_OPERATOR)) {
            return false;
        }

        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);

        final SubPattern<TypeComparison, EqualityComparison>
            rightSubpattern = input.subPattern(
            input.getProvidedInputType(),
            __ -> right.toNullable(),
            "_eqright"
        );

        if (tces.isUnbound(rightSubpattern, state)) {
            return true;
        }

        final IJadescriptType rType = tces.inferPatternType(
            rightSubpattern,
            state
        ).solve(input.getProvidedInputType());


        StaticState afterRight = tces.advancePattern(
            rightSubpattern, state
        );

        afterRight = tces.assertDidMatch(
            rightSubpattern,
            afterRight
        );

        final SubPattern<
            TypeComparison,
            EqualityComparison
            > leftSubpattern = input.subPattern(
            rType,
            __ -> left.toNullable(),
            "_eqleft"
        );
        return tces.isUnbound(leftSubpattern, afterRight);
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<EqualityComparison> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        Maybe<TypeComparison> left = input.getPattern()
            .__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input.getPattern()
            .__(EqualityComparison::getRight);
        String equalityOp = input.getPattern()
            .__(EqualityComparison::getEqualityOp).orElse("");
        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);
        if (equalityOp.equals(NOT_EQUALS_OPERATOR)) {
            return compileExpressionEqualityPatternMatch(
                input,
                state,
                acceptor
            );
        } else {
            final SubPattern<TypeComparison, EqualityComparison>
                rightSubpattern = input.subPattern(
                input.getProvidedInputType(),
                __ -> right.toNullable(),
                "_eqright"
            );
            final IJadescriptType rType = tces.inferPatternType(
                rightSubpattern,
                state
            ).solve(input.getProvidedInputType());
            PatternMatcher rightResult = tces.compilePatternMatch(
                rightSubpattern, state, acceptor);
            StaticState afterRight = tces.advancePattern(
                rightSubpattern, state
            );

            afterRight = tces.assertDidMatch(
                rightSubpattern,
                afterRight
            );

            final SubPattern<
                TypeComparison,
                EqualityComparison
                > leftSubpattern = input.subPattern(
                rType,
                __ -> left.toNullable(),
                "_eqleft"
            );
            PatternMatcher leftResult = tces.compilePatternMatch(
                leftSubpattern,
                afterRight,
                acceptor
            );
            return input.createCompositeMethodOutput(
                rType,
                (Integer i) -> {
                    if (i < 0 || i >= 2) {
                        return "/* Index out of bounds */";
                    } else {
                        return "__x";
                    }
                },
                List.of(
                    rightResult,
                    leftResult
                )
            );
        }
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<EqualityComparison> input,
        StaticState state
    ) {
        Maybe<TypeComparison> left = input
            .getPattern()
            .__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input
            .getPattern()
            .__(EqualityComparison::getRight);
        String equalityOp = input
            .getPattern()
            .__(EqualityComparison::getEqualityOp)
            .orElse("");
        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);

        if (equalityOp.equals(NOT_EQUALS_OPERATOR)) {
            return isWithoutSideEffects(input.getPattern(), state);
        } else {

            final SubPattern<TypeComparison, EqualityComparison>
                rightSubpattern = input.subPattern(
                input.getProvidedInputType(),
                __ -> right.toNullable(),
                "_eqright"
            );

            final IJadescriptType rType = tces.inferPatternType(
                rightSubpattern,
                state
            ).solve(input.getProvidedInputType());

            boolean rightResult = tces.isPatternEvaluationWithoutSideEffects(
                rightSubpattern,
                state
            );

            if (!rightResult) {
                return false;
            }

            StaticState afterRight = tces.advancePattern(
                rightSubpattern, state
            );

            afterRight = tces.assertDidMatch(
                rightSubpattern,
                afterRight
            );

            final SubPattern<
                TypeComparison,
                EqualityComparison
                > leftSubpattern = input.subPattern(
                rType,
                __ -> left.toNullable(),
                "_eqleft"
            );

            return tces.isPatternEvaluationWithoutSideEffects(
                leftSubpattern,
                afterRight
            );
        }
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<EqualityComparison> input,
        StaticState state
    ) {
        Maybe<TypeComparison> left = input.getPattern()
            .__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input.getPattern()
            .__(EqualityComparison::getRight);
        String equalityOp = input.getPattern()
            .__(EqualityComparison::getEqualityOp).orElse("");
        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);
        if (equalityOp.equals(NOT_EQUALS_OPERATOR)) {
            return state;
        } else {
            SubPattern<TypeComparison, EqualityComparison>
                rightSubpattern = input.subPattern(
                input.getProvidedInputType(),
                __ -> right.toNullable(),
                "_eqright"
            );
            final IJadescriptType rType = tces.inferPatternType(
                rightSubpattern,
                state
            ).solve(input.getProvidedInputType());

            StaticState afterRight = tces.advancePattern(
                rightSubpattern,
                state
            );

            afterRight = tces.assertDidMatch(
                rightSubpattern, afterRight
            );

            SubPattern<TypeComparison, EqualityComparison> leftSubpattern =
                input.subPattern(
                    rType,
                    __ -> left.toNullable(),
                    "_eqleft"
                );

            StaticState afterLeft = tces.advancePattern(
                leftSubpattern, afterRight
            );

            return tces.assertDidMatch(
                leftSubpattern, afterLeft
            );
        }
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<EqualityComparison> input,
        StaticState state
    ) {
        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);

        Maybe<TypeComparison> left = input.__(EqualityComparison::getLeft);

        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);


        String equalityOp = input.__(EqualityComparison::getEqualityOp)
            .orElse("");
        if (equalityOp.equals("≠")) {
            equalityOp = "!=";
        }

        if (!equalityOp.equals(NOT_EQUALS_OPERATOR)) { //<- If it is '='
            StaticState afterLeft = tces.advance(left, state);
            return state.assertExpressionsEqual(
                tces.describeExpression(left, state),
                tces.describeExpression(right, afterLeft)
            );
        } else {
            return state;
        }
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<EqualityComparison> input,
        StaticState state
    ) {
        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);

        Maybe<TypeComparison> left = input.__(EqualityComparison::getLeft);

        Maybe<TypeComparison> right = input.__(EqualityComparison::getRight);

        String equalityOp = input.__(EqualityComparison::getEqualityOp)
            .orElse("");
        if (equalityOp.equals("≠")) {
            equalityOp = "!=";
        }

        if (equalityOp.equals(NOT_EQUALS_OPERATOR)) { //<- If it is '!='
            StaticState afterLeft = tces.advance(left, state);
            return state.assertExpressionsEqual(
                tces.describeExpression(left, state),
                tces.describeExpression(right, afterLeft)
            );
        } else {
            return state;
        }
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<EqualityComparison> input,
        StaticState state
    ) {
        Maybe<TypeComparison> right = input.getPattern()
            .__(EqualityComparison::getRight);
        String equalityOp = input.getPattern()
            .__(EqualityComparison::getEqualityOp).orElse("");
        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);
        if (equalityOp.equals(NOT_EQUALS_OPERATOR)) {
            return PatternType.simple(
                module.get(BuiltinTypeProvider.class).boolean_()
            );
        } else {
            return tces.inferPatternType(
                input.subPattern(
                    input.getProvidedInputType(),
                    __ -> right.toNullable(),
                    "_eqright"
                ),
                state
            );
        }
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<EqualityComparison> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        Maybe<TypeComparison> left =
            input.getPattern().__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right =
            input.getPattern().__(EqualityComparison::getRight);
        String equalityOp =
            input.getPattern().__(EqualityComparison::getEqualityOp).orElse("");
        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);

        if (equalityOp.equals(NOT_EQUALS_OPERATOR)) {
            return validateExpressionEqualityPatternMatch(
                input,
                state,
                acceptor
            );
        } else {
            final SubPattern<TypeComparison, EqualityComparison>
                rightSubpattern =
                input.subPattern(
                    input.getProvidedInputType(),
                    __ -> right.toNullable(),
                    "_eqright"
                );
            final IJadescriptType rType = tces.inferPatternType(
                rightSubpattern, state
            ).solve(input.getProvidedInputType());

            boolean rightResult = tces.validatePatternMatch(
                rightSubpattern,
                state,
                acceptor
            );

            StaticState afterRight = tces.advancePattern(
                rightSubpattern,
                state
            );


            afterRight = tces.assertDidMatch(
                rightSubpattern,
                afterRight
            );


            final SubPattern<
                TypeComparison,
                EqualityComparison
                > leftSubpattern = input.subPattern(
                rType,
                __ -> left.toNullable(),
                "_eqleft"
            );

            boolean leftResult = tces.validatePatternMatch(
                leftSubpattern,
                afterRight,
                acceptor
            );


            return rightResult && leftResult;
        }
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<EqualityComparison> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<EqualityComparison> input) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<EqualityComparison> input) {
        return true;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<EqualityComparison> input,
        StaticState state
    ) {
        Maybe<TypeComparison> left = input.getPattern()
            .__(EqualityComparison::getLeft);
        Maybe<TypeComparison> right = input.getPattern()
            .__(EqualityComparison::getRight);
        String equalityOp = input.getPattern()
            .__(EqualityComparison::getEqualityOp).orElse("");
        final TypeComparisonExpressionSemantics tces =
            module.get(TypeComparisonExpressionSemantics.class);
        if (equalityOp.equals(NOT_EQUALS_OPERATOR)) {
            // Just an expression
            return false;
        } else {
            final SubPattern<TypeComparison, EqualityComparison>
                rightSubpattern =
                input.subPattern(
                    input.getProvidedInputType(),
                    __ -> right.toNullable(),
                    "_eqright"
                );

            boolean rightResult = tces.isPredictablePatternMatchSuccess(
                rightSubpattern,
                state
            );

            if (!rightResult) {
                return false;
            }

            final IJadescriptType rType = tces.inferPatternType(
                rightSubpattern, state
            ).solve(input.getProvidedInputType());

            StaticState afterRight = tces.advancePattern(
                rightSubpattern,
                state
            );

            afterRight = tces.assertDidMatch(
                rightSubpattern,
                afterRight
            );

            final SubPattern<
                TypeComparison,
                EqualityComparison
                > leftSubpattern = input.subPattern(
                rType,
                __ -> left.toNullable(),
                "_eqleft"
            );

            return tces.isPredictablePatternMatchSuccess(
                leftSubpattern,
                afterRight
            );

        }

    }


}
