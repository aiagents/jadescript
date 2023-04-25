package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Additive;
import it.unipr.ailab.jadescript.jadescript.Multiplicative;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.implicit.ImplicitConversionsHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.equal;
import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;


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
        return Maybe.someStream(input.__(Additive::getMultiplicative))
            .filter(Maybe::isPresent)
            .map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(MultiplicativeExpressionSemantics.class),
                x
            ));
    }


    private String associativeCompile(
        MaybeList<Multiplicative> operands,
        MaybeList<String> operators,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        if (operands.isEmpty()) {
            return "";
        }
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
        MaybeList<Multiplicative> operands,
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
        MaybeList<Multiplicative> operands,
        MaybeList<String> operators,
        StaticState state
    ) {
        if (operands.isEmpty()) {
            return module.get(BuiltinTypeProvider.class).nothing(
                "No operands found."
            );
        }
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


    private boolean isDuration(IJadescriptType type) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        return comparator.compare(builtins.duration(), type)
            .is(superTypeOrEqual());
    }


    private boolean isTimestamp(IJadescriptType type) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        return comparator.compare(builtins.timestamp(), type)
            .is(superTypeOrEqual());
    }


    private boolean isText(IJadescriptType type) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        return comparator.compare(builtins.text(), type)
            .is(superTypeOrEqual());
    }


    private boolean isNumber(IJadescriptType type) {
        return isInteger(type) || isReal(type);
    }

    private boolean isInteger(IJadescriptType type){
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        return comparator.compare(builtins.integer(), type)
            .is(superTypeOrEqual());
    }

    private boolean isReal(IJadescriptType type){
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        return comparator.compare(builtins.real(), type)
            .is(superTypeOrEqual());
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

        if (isDuration(t1) && isDuration(t2)) {
            if (op.wrappedEquals("-")) {
                return "jadescript.lang.Duration.subtraction("
                    + op1c + ", " + op2c + ")";
            }

            return "jadescript.lang.Duration.sum("
                + op1c + ", " + op2c + ")";
        }

        if ((isTimestamp(t1) && isDuration(t2))
            || (isDuration(t1) && isTimestamp(t2))) {
            if (op.wrappedEquals("-")) {
                return "jadescript.lang.Timestamp.minus("
                    + op1c + ", " + op2c + ")";
            }

            return "jadescript.lang.Timestamp.plus("
                + op1c + ", " + op2c + ")";
        }

        if (isTimestamp(t1) && isTimestamp(t2) && op.wrappedEquals("-")) {
            return "jadescript.lang.Timestamp.subtract("
                + op1c + ", " + op2c + ")";
        }

        if ((isText(t1) || isText(t2)) && op.wrappedEquals("+")) {
            return "java.lang.String.valueOf(" + op1c + ") " +
                "+ java.lang.String.valueOf(" + op2c + ")";
        }

        String c1 = op1c;
        String c2 = op2c;

        final ImplicitConversionsHelper implicits =
            module.get(ImplicitConversionsHelper.class);

        if (implicits.implicitConversionCanOccur(t1, t2)) {
            c1 = implicits.compileImplicitConversion(c1, t1, t2);
        }

        if (implicits.implicitConversionCanOccur(t2, t1)) {
            c2 = implicits.compileImplicitConversion(c2, t2, t1);
        }

        return c1 + " " + op.orElse("+") + " " + c2;
    }


    private IJadescriptType inferPair(
        IJadescriptType t1, Maybe<String> op,
        Maybe<Multiplicative> op2,
        StaticState state
    ) {
        IJadescriptType t2 = module.get(MultiplicativeExpressionSemantics.class)
            .inferType(op2, state);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        ImplicitConversionsHelper impicits =
            module.get(ImplicitConversionsHelper.class);
        if (isDuration(t1) && isDuration(t2)) {
            return builtins.duration();
        } else if ((isTimestamp(t1) && isDuration(t2))
            || (isDuration(t1) && isTimestamp(t2))) {
            return builtins.timestamp();
        } else if (isTimestamp(t1) && isTimestamp(t2)
            && op.wrappedEquals("-")) {
            return builtins.duration();
        } else if ((isText(t1) || isText(t2)) && op.wrappedEquals("+")) {
            return builtins.text();
        } else if (isNumber(t1) || isNumber(t2)) {
            if (impicits.implicitConversionCanOccur(t1, t2)) {
                return t2;
            } else {
                return t1;
            }
        }
        return builtins.any(
            "One or more operands of the + operation has invalid types."
        );
    }


    @Override
    protected String compileInternal(
        Maybe<Additive> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final MaybeList<Multiplicative> operands =
            input.__toList(Additive::getMultiplicative);
        final MaybeList<String> operators =
            input.__toList(Additive::getAdditiveOp);
        return associativeCompile(operands, operators, state, acceptor);
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<Additive> input,
        StaticState state
    ) {
        final MaybeList<Multiplicative> operands =
            input.__toList(Additive::getMultiplicative);
        final MaybeList<String> operators =
            input.__toList(Additive::getAdditiveOp);
        return associativeInfer(operands, operators, state);

    }


    @Override
    protected boolean mustTraverse(Maybe<Additive> input) {
        final MaybeList<Multiplicative> operands =
            input.__toListNullsRemoved(Additive::getMultiplicative);
        return operands.size() == 1;
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>>
    traverseInternal(Maybe<Additive> input) {
        if (!mustTraverse(input)) {
            return Optional.empty();
        }

        return Optional.of(new ExpressionSemantics.SemanticsBoundToExpression<>(
            module.get(MultiplicativeExpressionSemantics.class),
            input.__(Additive::getMultiplicative)
                .__(ms -> !ms.isEmpty() ? ms.get(0) : null)
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
        MaybeList<Multiplicative> operands,
        MaybeList<String> operators,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (operands.isEmpty()) {
            return VALID;
        }
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
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        ValidationHelper vh = module.get(ValidationHelper.class);
        if (isText(t1) || isText(t2)) {
            return VALID;
        }

        if (comparator.compare(builtins.integer(), t1).is(equal())) {
            return vh.assertExpectedTypesAny(
                Arrays.asList(
                    builtins.integer(),
                    builtins.real(),
                    builtins.text()
                ),
                t2,
                "InvalidAdditiveOperation",
                op2,
                acceptor
            );

        }

        if (comparator.compare(builtins.real(), t1).is(equal())) {
            return vh.assertExpectedTypesAny(
                Arrays.asList(
                    builtins.real(),
                    builtins.integer(),
                    builtins.text()
                ),
                t2,
                "InvalidAdditiveOperation",
                op2,
                acceptor
            );

        }
        if (comparator.compare(builtins.integer(), t2).is(equal())) {
            return vh.assertExpectedTypesAny(
                Arrays.asList(
                    builtins.integer(),
                    builtins.real(),
                    builtins.text()
                ),
                t1,
                "InvalidAdditiveOperation",
                op1,
                acceptor
            );

        }

        if (comparator.compare(builtins.real(), t2).is(equal())) {
            return vh.assertExpectedTypesAny(
                Arrays.asList(
                    builtins.real(),
                    builtins.integer(),
                    builtins.text()
                ),
                t1,
                "InvalidAdditiveOperation",
                op1,
                acceptor
            );

        }

        if (isDuration(t1)) {
            return vh.assertExpectedTypesAny(
                Arrays.asList(
                    builtins.duration(),
                    builtins.timestamp(),
                    builtins.text()
                ),
                t2,
                "InvalidAdditiveOperation",
                op2,
                acceptor
            );

        } else if (isDuration(t2)) {
            return vh.assertExpectedTypesAny(
                Arrays.asList(
                    builtins.duration(),
                    builtins.timestamp(),
                    builtins.text()
                ),
                t1,
                "InvalidAdditiveOperation",
                op1,
                acceptor
            );

        } else if (isTimestamp(t1)) {
            return vh.assertExpectedTypesAny(
                Arrays.asList(
                    builtins.timestamp(),
                    builtins.duration(),
                    builtins.text()
                ),
                t2,
                "InvalidAdditiveOperation",
                op2,
                acceptor
            );

        } else if (isTimestamp(t2)) {
            return vh.assertExpectedTypesAny(
                Arrays.asList(
                    builtins.timestamp(),
                    builtins.duration(),
                    builtins.text()
                ),
                t1,
                "InvalidAdditiveOperation",
                op1,
                acceptor
            );

        }
        // at least one of the two operands has to be TEXT, INTEGER,
        // REAL, DURATION or TIMESTAMP
        vh.asserting(
            Stream.of(
                builtins.text(),
                builtins.integer(),
                builtins.real(),
                builtins.duration(),
                builtins.timestamp()
            ).anyMatch(t0 ->
                comparator.compare(t0, t1).is(superTypeOrEqual())
                    || comparator.compare(t0, t2).is(superTypeOrEqual())
            ),
            "InvalidAdditiveOperation",
            "At least one of the two operands has to be of type " +
                "'integer', 'real', " +
                "'text', 'timestamp' or 'duration'.",
            op1,
            acceptor
        );

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

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        if ((isText(t1) || isText(t2)) && op.wrappedEquals("-")) {
            return validationHelper.emitError(
                "InvalidStringConcatenation",
                "Invalid '-' operator in a string concatenation operation.",
                op2,
                acceptor
            );
        }

        boolean cannotSumTimestamps = validationHelper.asserting(
            //Implication: (both timestamps) => (operator is '-')
            !(isTimestamp(t1) && isTimestamp(t2)) || op.wrappedEquals("-"),
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
        final MaybeList<Multiplicative> operands =
            input.__toList(Additive::getMultiplicative);
        final MaybeList<String> operators =
            input.__toList(Additive::getAdditiveOp);
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
        return advanceAssociative(
            input.__toList(Additive::getMultiplicative),
            state
        );
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<Additive> input,
        StaticState state
    ) {
        return state;
    }


}
