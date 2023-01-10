package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Matches;
import it.unipr.ailab.jadescript.jadescript.Multiplicative;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.PatternDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created on 28/12/16.
 */
@Singleton
public class MultiplicativeExpressionSemantics
    extends ExpressionSemantics<Multiplicative> {


    public MultiplicativeExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<Multiplicative> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected Maybe<PatternDescriptor> describePatternInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<Multiplicative> input,
        StaticState state
    ) {
        final List<Maybe<Matches>> matches = Maybe.toListOfMaybes(
            input.__(Multiplicative::getMatches)
        );
        return advanceAssociative(matches, state);
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state
    ) {
        return state;
    }


    private String associativeCompile(
        List<Maybe<Matches>> operands,
        List<Maybe<String>> operators,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        if (operands.isEmpty()) return "";
        final MatchesExpressionSemantics mes =
            module.get(MatchesExpressionSemantics.class);

        final String op0c = mes.compile(operands.get(0), state, acceptor);
        final StaticState op0s = mes.advance(operands.get(0), state);
        IJadescriptType t = mes.inferType(operands.get(0), state);
        String result = op0c;
        StaticState runningState = op0s;
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            result = compilePair(
                result,
                runningState,
                t,
                operators.get(i - 1),
                operands.get(i),
                acceptor
            );
            runningState = advancePair(
                runningState,
                operands.get(i)
            );
            t = inferPair(
                t,
                operators.get(i - 1),
                operands.get(i),
                runningState
            );
        }
        return result;
    }


    private StaticState advanceAssociative(
        List<Maybe<Matches>> operands,
        StaticState state
    ) {
        final MatchesExpressionSemantics mes =
            module.get(MatchesExpressionSemantics.class);

        StaticState runningState = mes.advance(operands.get(0), state);
        for (int i = 1; i < operands.size(); i++) {
            runningState = advancePair(
                runningState,
                operands.get(i)
            );
        }
        return runningState;
    }


    private StaticState advancePair(
        StaticState op1s,
        Maybe<Matches> op2
    ) {
        return module.get(MatchesExpressionSemantics.class)
            .advance(op2, op1s);
    }


    private IJadescriptType associativeInfer(
        List<Maybe<Matches>> operands,
        List<Maybe<String>> operators,
        StaticState state
    ) {
        if (operands.isEmpty()) return module.get(TypeHelper.class).NOTHING;
        IJadescriptType t = module.get(MatchesExpressionSemantics.class)
            .inferType(operands.get(0), state);
        StaticState runningState = module.get(MatchesExpressionSemantics.class)
            .advance(operands.get(0), state);
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            t = inferPair(
                t,
                operators.get(i - 1),
                operands.get(i),
                runningState
            );
            runningState = advancePair(runningState, operands.get(i));
        }
        return t;
    }


    private String compilePair(
        String op1c,
        StaticState state,
        IJadescriptType t1,
        Maybe<String> op,
        Maybe<Matches> op2,
        CompilationOutputAcceptor acceptor
    ) {
        IJadescriptType t2 =
            module.get(MatchesExpressionSemantics.class).inferType(op2, state);
        String op2c =
            module.get(MatchesExpressionSemantics.class).compile(
                op2,
                state,
                acceptor
            );
        final TypeHelper typeHelper = module.get(TypeHelper.class);

        if (t1.typeEquals(typeHelper.DURATION)
            && t2.typeEquals(typeHelper.DURATION)) {
            if (op.wrappedEquals("/")) {
                return "(float) jadescript.lang.Duration.divide(" + op1c +
                    ", " + op2c + ")";
            }
        } else if ((t1.typeEquals(typeHelper.INTEGER)
            && t2.typeEquals(typeHelper.DURATION))
            || (t1.typeEquals(typeHelper.DURATION)
            && t2.typeEquals(typeHelper.INTEGER))) {
            if (op.wrappedEquals("*")) {
                return "jadescript.lang.Duration.multiply(" + op1c +
                    ", " + op2c + ")";
            } else if (op.wrappedEquals("/")) {
                return "jadescript.lang.Duration.divide(" + op1c +
                    ", " + op2c + ")";
            }
        } else if ((t1.typeEquals(typeHelper.REAL)
            && t2.typeEquals(typeHelper.DURATION))
            || (t1.typeEquals(typeHelper.DURATION)
            && t2.typeEquals(typeHelper.REAL))) {
            if (op.wrappedEquals("*")) {
                return "jadescript.lang.Duration.multiply(" + op1c +
                    ", " + op2c + ")";
            } else if (op.wrappedEquals("/")) {
                return "jadescript.lang.Duration.divide(" + op1c +
                    ", " + op2c + ")";
            }
        }
        String c1 = op1c;
        String c2 = op2c;
        if (typeHelper.implicitConversionCanOccur(t1, t2)) {
            c1 = typeHelper.compileImplicitConversion(c1, t1, t2);
        }

        if (typeHelper.implicitConversionCanOccur(t2, t1)) {
            c2 = typeHelper.compileImplicitConversion(c2, t2, t1);
        }
        return c1 + " " + op.orElse("*") + " " + c2;

    }


    private IJadescriptType inferPair(
        IJadescriptType t1,
        Maybe<String> op,
        Maybe<Matches> op2,
        StaticState state
    ) {
        IJadescriptType t2 =
            module.get(MatchesExpressionSemantics.class).inferType(
                op2,
                state
            );
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (t1.typeEquals(typeHelper.DURATION)
            && t2.typeEquals(typeHelper.DURATION)) {
            if (op.wrappedEquals("/")) {
                return typeHelper.REAL;
            }
        } else if ((t1.typeEquals(typeHelper.INTEGER)
            && t2.typeEquals(typeHelper.DURATION))
            || (t1.typeEquals(typeHelper.DURATION)
            && t2.typeEquals(typeHelper.INTEGER))) {
            if (op.wrappedEquals("*") || op.wrappedEquals("/")) {
                return typeHelper.DURATION;
            }
        } else if ((t1.typeEquals(typeHelper.REAL)
            && t2.typeEquals(typeHelper.DURATION))
            || (t1.typeEquals(typeHelper.DURATION)
            && t2.typeEquals(typeHelper.REAL))) {
            if (op.wrappedEquals("*") || op.wrappedEquals("/")) {
                return typeHelper.DURATION;
            }
        }
        if (typeHelper.implicitConversionCanOccur(t1, t2)) {
            t1 = t2;
        }

        if (typeHelper.implicitConversionCanOccur(t2, t1)) {
            t2 = t1;
        }

        if (t2.typeEquals(t1)) {
            return t2;
        } else {
            return typeHelper.REAL;
        }
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<Multiplicative> input
    ) {
        return Maybe.toListOfMaybes(
            input.__(Multiplicative::getMatches)).stream().map(
            x -> new SemanticsBoundToExpression<>(module.get(
                MatchesExpressionSemantics.class), x
            )
        );
    }


    @Override
    protected String compileInternal(
        Maybe<Multiplicative> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        if (input == null) return "";
        final List<Maybe<Matches>> matches = Maybe.toListOfMaybes(
            input.__(Multiplicative::getMatches)
        );
        final List<Maybe<String>> multiplicativeOps = Maybe.toListOfMaybes(
            input.__(Multiplicative::getMultiplicativeOp)
        );
        return associativeCompile(matches, multiplicativeOps, state, acceptor);
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<Multiplicative> input,
        StaticState state
    ) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        final List<Maybe<Matches>> matches = Maybe.toListOfMaybes(
            input.__(Multiplicative::getMatches)
        );
        final List<Maybe<String>> multiplicativeOps = Maybe.toListOfMaybes(
            input.__(Multiplicative::getMultiplicativeOp)
        );
        return associativeInfer(matches, multiplicativeOps, state);
    }


    @Override
    protected boolean mustTraverse(Maybe<Multiplicative> input) {
        final List<Maybe<Matches>> matches = Maybe.toListOfMaybes(
            input.__(Multiplicative::getMatches)
        );
        return matches.size() == 1;
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(
        Maybe<Multiplicative> input
    ) {
        if (mustTraverse(input)) {
            final List<Maybe<Matches>> matches = Maybe.toListOfMaybes(input.__(
                Multiplicative::getMatches));
            return Optional.of(new SemanticsBoundToExpression<>(
                module.get(MatchesExpressionSemantics.class),
                matches.get(0)
            ));
        }
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state
    ) {
        return true;
    }


    public boolean validateAssociative(
        List<Maybe<Matches>> operands,
        List<Maybe<String>> operators,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (operands.isEmpty()) return VALID;
        final MatchesExpressionSemantics mes =
            module.get(MatchesExpressionSemantics.class);
        IJadescriptType t = mes.inferType(operands.get(0), state);
        Maybe<Matches> prevOperand = operands.get(0);
        boolean prevOperandCheck = mes.validate(prevOperand, state, acceptor);
        if (prevOperandCheck == INVALID) {
            return INVALID;
        }
        StaticState runningState = mes.advance(prevOperand, state);
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            Maybe<Matches> currentOperand = operands.get(i);
            boolean pairValidation = validatePair(
                prevOperand,
                t,
                operators.get(i - 1),
                currentOperand,
                runningState,
                acceptor
            );
            if (pairValidation == INVALID) {
                return pairValidation;
            }
            t = inferPair(
                t,
                operators.get(i - 1),
                operands.get(i),
                runningState
            );
            runningState = advancePair(
                runningState,
                currentOperand
            );
            prevOperand = currentOperand;
        }
        return VALID;
    }


    private boolean validatePairTypes(
        Maybe<Matches> op1,
        IJadescriptType t1,
        Maybe<Matches> op2,
        IJadescriptType t2,
        Maybe<String> operator,
        ValidationMessageAcceptor acceptor
    ) {
        TypeHelper th = module.get(TypeHelper.class);
        ValidationHelper vh = module.get(ValidationHelper.class);

        List<IJadescriptType> durationIntReal = Arrays.asList(
            th.INTEGER,
            th.REAL,
            th.DURATION
        );
        boolean t1Expected = vh.assertExpectedTypes(
            durationIntReal,
            t1,
            "InvalidMultiplicativeOperation",
            op1,
            acceptor
        );
        boolean t2Expected = vh.assertExpectedTypes(
            durationIntReal,
            t2,
            "InvalidMultiplicativeOperation",
            op2,
            acceptor
        );

        if (t1Expected == INVALID || t2Expected == INVALID) {
            return INVALID;
        }

        List<IJadescriptType> intReal = Arrays.asList(th.INTEGER, th.REAL);
        if (t1.typeEquals(th.DURATION)) {
            if (operator.wrappedEquals("*")) {
                return vh.assertExpectedTypes(
                    intReal,
                    t2,
                    "InvalidMultiplicativeOperation",
                    op2,
                    acceptor
                );
            } else if (operator.wrappedEquals("/")) {
                // always ok
                return VALID;
            } else { // assuming '%'
                op1.safeDo(op1safe -> {
                    acceptor.acceptError(
                        "Invalid operation " + operator.orElse("") +
                            " for duration type",
                        op1safe,
                        null,
                        ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                        "InvalidMultiplicativeOperation"
                    );
                });
                return INVALID;
            }
        } else if (t1.typeEquals(th.INTEGER) || t1.typeEquals(th.REAL)) {
            if (operator.wrappedEquals("*")) {
                // always ok
                return VALID;
            } else if (operator.wrappedEquals("/")
                || operator.wrappedEquals("%")) {
                return vh.assertExpectedTypes(
                    intReal,
                    t2,
                    "InvalidMultiplicativeOperation",
                    op2,
                    acceptor
                );
            }
        }

        return VALID;
    }


    public boolean validatePair(
        Maybe<Matches> op1,
        IJadescriptType t1,
        Maybe<String> op,
        Maybe<Matches> op2,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        boolean op2Validation = module.get(MatchesExpressionSemantics.class)
            .validate(op2, state, acceptor);

        if (op2Validation == INVALID) {
            return INVALID;
        }

        IJadescriptType t2 = module.get(MatchesExpressionSemantics.class)
            .inferType(op2, state);


        return validatePairTypes(op1, t1, op2, t2, op, acceptor);
    }


    @Override
    protected boolean validateInternal(
        Maybe<Multiplicative> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final List<Maybe<Matches>> operands = Maybe.toListOfMaybes(input.__(
            Multiplicative::getMatches));
        final List<Maybe<String>> operators = Maybe.toListOfMaybes(input.__(
            Multiplicative::getMultiplicativeOp));
        return validateAssociative(operands, operators, state, acceptor);
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isAlwaysPureInternal(
        Maybe<Multiplicative> input,
        StaticState state
    ) {
        return subExpressionsAllAlwaysPure(input, state);
    }


    @Override
    protected boolean isValidLExprInternal(Maybe<Multiplicative> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        Maybe<Multiplicative> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<Multiplicative> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<Multiplicative> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<Multiplicative> input) {
        return false;
    }

}
