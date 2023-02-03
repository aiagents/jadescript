package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Additive;
import it.unipr.ailab.jadescript.jadescript.Multiplicative;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
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
public class AdditiveExpressionSemantics extends ExpressionSemantics<Additive> {


    public AdditiveExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<Additive> input
    ) {
        final List<Maybe<Multiplicative>> operands = Maybe.toListOfMaybes(
            input.__(Additive::getMultiplicative)
        );
        return operands.stream()
            .filter(Maybe::isPresent)
            .map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(MultiplicativeExpressionSemantics.class),
                x
            ));
    }


    private String associativeCompile(
        List<Maybe<Multiplicative>> operands,
        List<Maybe<String>> operators,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        if (operands.isEmpty()) return "";
        IJadescriptType t = module.get(MultiplicativeExpressionSemantics.class)
            .inferType(operands.get(0), state);
        String op0c = module.get(MultiplicativeExpressionSemantics.class)
            .compile(operands.get(0), state, acceptor);
        StaticState op0s = module.get(MultiplicativeExpressionSemantics.class)
            .advance(operands.get(0), state);
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
        List<Maybe<Multiplicative>> operands,
        StaticState state
    ) {
        final MultiplicativeExpressionSemantics mes =
            module.get(MultiplicativeExpressionSemantics.class);

        StaticState runningState = mes.advance(operands.get(0), state);
        for (int i = 1; i < operands.size(); i++) {
            runningState = advancePair(
                runningState,
                operands.get(i)
            );
        }
        return runningState;
    }


    private IJadescriptType associativeInfer(
        List<Maybe<Multiplicative>> operands,
        List<Maybe<String>> operators,
        StaticState state
    ) {
        if (operands.isEmpty()) return module.get(TypeHelper.class).NOTHING;
        IJadescriptType t = module.get(MultiplicativeExpressionSemantics.class)
            .inferType(operands.get(0), state);
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            t = inferPair(t, operators.get(i - 1), operands.get(i), state);
        }
        return t;
    }


    private StaticState advancePair(
        StaticState op1s,
        Maybe<Multiplicative> op2
    ) {
        return module.get(MultiplicativeExpressionSemantics.class)
            .advance(op2, op1s);
    }


    private String compilePair(
        String op1c,
        StaticState op1s,
        IJadescriptType t1,
        Maybe<String> op,
        Maybe<Multiplicative> op2,
        BlockElementAcceptor acceptor
    ) {
        final MultiplicativeExpressionSemantics mes =
            module.get(MultiplicativeExpressionSemantics.class);
        IJadescriptType t2 = mes.inferType(op2, op1s);
        String op2c = mes.compile(op2, op1s, acceptor);
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (t1.typeEquals(typeHelper.DURATION)
            && t2.typeEquals(typeHelper.DURATION)) {
            if (op.wrappedEquals("-")) {
                return "jadescript.lang.Duration.subtraction("
                    + op1c + ", " + op2c + ")";
            } else {
                return "jadescript.lang.Duration.sum("
                    + op1c + ", " + op2c + ")";
            }
        } else if ((t1.typeEquals(typeHelper.TIMESTAMP)
            && t2.typeEquals(typeHelper.DURATION))
            || (t1.typeEquals(typeHelper.DURATION)
            && t2.typeEquals(typeHelper.TIMESTAMP))) {
            if (op.wrappedEquals("-")) {
                return "jadescript.lang.Timestamp.minus("
                    + op1c + ", " + op2c + ")";
            } else {
                return "jadescript.lang.Timestamp.plus("
                    + op1c + ", " + op2c + ")";
            }
        } else if (t1.typeEquals(typeHelper.TIMESTAMP)
            && t2.typeEquals(typeHelper.TIMESTAMP)
            && op.wrappedEquals("-")) {
            return "jadescript.lang.Timestamp.subtract("
                + op1c + ", " + op2c + ")";
        } else if ((t1.typeEquals(typeHelper.TEXT)
            || t2.typeEquals(typeHelper.TEXT))
            && op.wrappedEquals("+")) {
            return "java.lang.String.valueOf(" + op1c + ") " +
                "+ java.lang.String.valueOf(" + op2c + ")";
        } else {
            String c1 = op1c;
            String c2 = op2c;

            if (typeHelper.implicitConversionCanOccur(t1, t2)) {
                c1 = typeHelper.compileImplicitConversion(c1, t1, t2);
            }

            if (typeHelper.implicitConversionCanOccur(t2, t1)) {
                c2 = typeHelper.compileImplicitConversion(c2, t2, t1);
            }
            return c1 + " " + op.orElse("+") + " " + c2;
        }
    }


    private IJadescriptType inferPair(
        IJadescriptType t1, Maybe<String> op,
        Maybe<Multiplicative> op2,
        StaticState state
    ) {
        IJadescriptType t2 = module.get(MultiplicativeExpressionSemantics.class)
            .inferType(op2, state);
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (t1.typeEquals(typeHelper.DURATION)
            && t2.typeEquals(typeHelper.DURATION)) {
            return typeHelper.DURATION;
        } else if ((t1.typeEquals(typeHelper.TIMESTAMP)
            && t2.typeEquals(typeHelper.DURATION))
            || (t1.typeEquals(typeHelper.DURATION)
            && t2.typeEquals(typeHelper.TIMESTAMP))) {
            return typeHelper.TIMESTAMP;
        } else if (t1.typeEquals(typeHelper.TIMESTAMP)
            && t2.typeEquals(typeHelper.TIMESTAMP)
            && op.wrappedEquals("-")) {
            return typeHelper.DURATION;
        } else if ((t1.typeEquals(typeHelper.TEXT)
            || t2.typeEquals(typeHelper.TEXT))
            && op.wrappedEquals("+")) {
            return typeHelper.TEXT;
        } else if (typeHelper.NUMBER.isSupEqualTo(t1)
            || typeHelper.NUMBER.isSupEqualTo(t2)) {

            if (typeHelper.implicitConversionCanOccur(t1, t2)) {
                return t2;
            } else {
                return t1;
            }
        }
        return typeHelper.ANY;
    }


    @Override
    protected String compileInternal(
        Maybe<Additive> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final List<Maybe<Multiplicative>> operands =
            Maybe.toListOfMaybes(input.__(Additive::getMultiplicative));
        final List<Maybe<String>> operators =
            Maybe.toListOfMaybes(input.__(Additive::getAdditiveOp));
        return associativeCompile(operands, operators, state, acceptor);
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<Additive> input,
        StaticState state
    ) {
        final List<Maybe<Multiplicative>> operands =
            Maybe.toListOfMaybes(input.__(Additive::getMultiplicative));
        final List<Maybe<String>> operators =
            Maybe.toListOfMaybes(input.__(Additive::getAdditiveOp));
        return associativeInfer(operands, operators, state);

    }


    @Override
    protected boolean mustTraverse(Maybe<Additive> input) {
        final List<Maybe<Multiplicative>> operands =
            Maybe.toListOfMaybes(input.__(Additive::getMultiplicative));
        return operands.size() == 1;
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>>
    traverseInternal(Maybe<Additive> input) {
        final List<Maybe<Multiplicative>> operands =
            Maybe.toListOfMaybes(input.__(Additive::getMultiplicative));
        if (!mustTraverse(input)) {
            return Optional.empty();
        }

        return Optional.of(new ExpressionSemantics.SemanticsBoundToExpression<>(
            module.get(MultiplicativeExpressionSemantics.class),
            operands.get(0)
        ));
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<Additive> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<Additive> input) {
        return false;
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<Additive> input,
        StaticState state
    ) {
        return subPatternEvaluationsAllPure(input, state);
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<Additive> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<Additive> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<Additive> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<Additive> input,
        StaticState state
    ) {
        return subExpressionsAnyHoled(input, state);
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<Additive> input,
        StaticState state
    ) {
        return subExpressionsAnyTypelyHoled(input, state);
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<Additive> input,
        StaticState state
    ) {
        return subExpressionsAnyUnbound(input, state);
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<Additive> input) {
        return true;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<Additive> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<Additive> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<Additive> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<Additive> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    private boolean validateAssociative(
        List<Maybe<Multiplicative>> operands,
        List<Maybe<String>> operators,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (operands.isEmpty()) return VALID;
        IJadescriptType t = module.get(MultiplicativeExpressionSemantics.class)
            .inferType(operands.get(0), state);
        Maybe<Multiplicative> op1 = operands.get(0);
        boolean op1Validation =
            module.get(MultiplicativeExpressionSemantics.class)
                .validate(op1, state, acceptor);
        if (op1Validation == INVALID) {
            return INVALID;
        }
        StaticState finalState = state;
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            Maybe<Multiplicative> op2 = operands.get(i);
            boolean pairValidation = validatePair(
                op1,
                t,
                operators.get(i - 1),
                op2,
                finalState,
                acceptor
            );
            if (pairValidation == INVALID) {
                return INVALID;
            }
            StaticState pairState = advancePair(
                finalState,
                op2
            );
            t = inferPair(
                t,
                operators.get(i - 1),
                operands.get(i),
                pairState
            );
            op1 = op2;
            finalState = pairState;
        }
        return VALID;
    }


    private boolean validatePairTypes(
        Maybe<Multiplicative> op1,
        IJadescriptType t1,
        Maybe<Multiplicative> op2,
        IJadescriptType t2,
        ValidationMessageAcceptor acceptor
    ) {
        TypeHelper th = module.get(TypeHelper.class);
        ValidationHelper vh = module.get(ValidationHelper.class);
        if (th.TEXT.typeEquals(t1) || th.TEXT.typeEquals(t2)) {
            return VALID;
        } else if (th.INTEGER.typeEquals(t1)) {
            return vh.assertExpectedTypes(
                Arrays.asList(th.INTEGER, th.REAL, th.TEXT),
                t2,
                "InvalidAdditiveOperation",
                op2,
                acceptor
            );

        } else if (th.REAL.typeEquals(t1)) {
            return vh.assertExpectedTypes(
                Arrays.asList(th.REAL, th.INTEGER, th.TEXT),
                t2,
                "InvalidAdditiveOperation",
                op2,
                acceptor
            );

        } else if (th.INTEGER.typeEquals(t2)) {
            return vh.assertExpectedTypes(
                Arrays.asList(th.INTEGER, th.REAL, th.TEXT),
                t1,
                "InvalidAdditiveOperation",
                op1,
                acceptor
            );

        } else if (th.REAL.typeEquals(t2)) {
            return vh.assertExpectedTypes(
                Arrays.asList(th.REAL, th.INTEGER, th.TEXT),
                t1,
                "InvalidAdditiveOperation",
                op1,
                acceptor
            );

        } else if (th.DURATION.typeEquals(t1)) {
            return vh.assertExpectedTypes(
                Arrays.asList(th.DURATION, th.TIMESTAMP, th.TEXT),
                t2,
                "InvalidAdditiveOperation",
                op2,
                acceptor
            );

        } else if (th.DURATION.typeEquals(t2)) {
            return vh.assertExpectedTypes(
                Arrays.asList(th.DURATION, th.TIMESTAMP, th.TEXT),
                t1,
                "InvalidAdditiveOperation",
                op1,
                acceptor
            );

        } else if (th.TIMESTAMP.typeEquals(t1)) {
            return vh.assertExpectedTypes(
                Arrays.asList(th.TIMESTAMP, th.DURATION, th.TEXT),
                t2,
                "InvalidAdditiveOperation",
                op2,
                acceptor
            );

        } else if (th.TIMESTAMP.typeEquals(t2)) {
            return vh.assertExpectedTypes(
                Arrays.asList(th.TIMESTAMP, th.DURATION, th.TEXT),
                t1,
                "InvalidAdditiveOperation",
                op1,
                acceptor
            );

        } else {
            // at least one of the two operands has to be TEXT, INTEGER,
            // REAL, DURATION or TIMESTAMP
            vh.asserting(
                th.TEXT.isSupEqualTo(t1) ||
                    th.INTEGER.isSupEqualTo(t1) ||
                    th.REAL.isSupEqualTo(t1) ||
                    th.DURATION.isSupEqualTo(t1) ||
                    th.TIMESTAMP.isSupEqualTo(t1) ||
                    th.TEXT.isSupEqualTo(t2) ||
                    th.INTEGER.isSupEqualTo(t2) ||
                    th.REAL.isSupEqualTo(t2) ||
                    th.DURATION.isSupEqualTo(t2) ||
                    th.TIMESTAMP.isSupEqualTo(t2),
                "InvalidAdditiveOperation",
                "At least one of the two operands has to be of type " +
                    "'integer', 'real', " +
                    "'text', 'timestamp' or 'duration'.",
                op1,
                acceptor
            );
        }

        return VALID;
    }


    private boolean validatePair(
        Maybe<Multiplicative> op1,
        IJadescriptType t1,
        Maybe<String> op,
        Maybe<Multiplicative> op2,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final MultiplicativeExpressionSemantics mes =
            module.get(MultiplicativeExpressionSemantics.class);
        boolean op2Validation = mes.validate(op2, state, acceptor);
        if (op2Validation == INVALID) {
            return INVALID;
        }

        //not needed: StaticState op2s = mes.advance(op2, state);
        IJadescriptType t2 = mes.inferType(op2, state);

        final TypeHelper typeHelper = module.get(TypeHelper.class);
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);
        if ((t1.typeEquals(typeHelper.TEXT) || t2.typeEquals(typeHelper.TEXT))
            && op.wrappedEquals("-")) {
            return validationHelper.emitError(
                "InvalidStringConcatenation",
                "Invalid '-' operator in a string concatenation operation.",
                op2,
                acceptor
            );
        }

        boolean cannotSumTimestamps = validationHelper.asserting(
            Util.implication(
                t1.typeEquals(typeHelper.TIMESTAMP)
                    && t2.typeEquals(typeHelper.TIMESTAMP),
                op.wrappedEquals("-")
            ),
            "InvalidOperation",
            "Can not sum two timestamps.",
            op2,
            acceptor
        );

        if (cannotSumTimestamps == INVALID) {
            return INVALID;
        }

        return validatePairTypes(op1, t1, op2, t2, acceptor);
    }


    @Override
    protected boolean validateInternal(
        Maybe<Additive> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final List<Maybe<Multiplicative>> operands =
            Maybe.toListOfMaybes(input.__(Additive::getMultiplicative));
        final List<Maybe<String>> operators =
            Maybe.toListOfMaybes(input.__(Additive::getAdditiveOp));
        return validateAssociative(operands, operators, state, acceptor);
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<Additive> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<Additive> input,
        StaticState state
    ) {
        final List<Maybe<Multiplicative>> mults = Maybe.toListOfMaybes(
            input.__(Additive::getMultiplicative)
        );
        return advanceAssociative(mults, state);
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<Additive> input,
        StaticState state
    ) {
        return state;
    }


}
