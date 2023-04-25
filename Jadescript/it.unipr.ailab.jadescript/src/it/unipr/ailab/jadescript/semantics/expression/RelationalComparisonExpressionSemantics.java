package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ContainmentCheck;
import it.unipr.ailab.jadescript.jadescript.RelationalComparison;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.subTypeOrEqual;
import static it.unipr.ailab.maybe.Maybe.some;

/**
 * Created on 28/12/16.
 */
@Singleton
public class RelationalComparisonExpressionSemantics
    extends ExpressionSemantics<RelationalComparison> {


    public RelationalComparisonExpressionSemantics(
        SemanticsModule semanticsModule
    ) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<RelationalComparison> input
    ) {
        final ContainmentCheckExpressionSemantics cces =
            module.get(ContainmentCheckExpressionSemantics.class);

        return Stream.of(
                input.__(RelationalComparison::getLeft),
                input.__(RelationalComparison::getRight)
            ).filter(Maybe::isPresent)
            .map(i -> new SemanticsBoundToExpression<>(cces, i));
    }


    @Override
    protected String compileInternal(
        Maybe<RelationalComparison> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        if (input == null) {
            return "";
        }
        final Maybe<ContainmentCheck> left =
            input.__(RelationalComparison::getLeft);
        final Maybe<ContainmentCheck> right =
            input.__(RelationalComparison::getRight);
        Maybe<String> relationalOp =
            input.__(RelationalComparison::getRelationalOp);
        if (relationalOp.wrappedEquals("≥")) {
            relationalOp = some(">=");
        } else if (relationalOp.wrappedEquals("≤")) {
            relationalOp = some("<=");
        }
        final ContainmentCheckExpressionSemantics cces =
            module.get(ContainmentCheckExpressionSemantics.class);
        String leftCompiled = cces.compile(left, state, acceptor);
        StaticState afterLeft = cces.advance(left, state);
        IJadescriptType t1 = cces.inferType(left, state);

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        String rightCompiled = cces.compile(right, afterLeft, acceptor);
        IJadescriptType t2 = cces.inferType(right, afterLeft);

        if (comparator.compare(t1, builtins.duration())
            .is(subTypeOrEqual())
            && comparator.compare(t2, builtins.duration())
            .is(subTypeOrEqual())) {
            return "jadescript.lang.Duration.compare(" + leftCompiled
                + ", " + rightCompiled + ") " + relationalOp + " 0";
        }

        if (comparator.compare(t1, builtins.timestamp())
            .is(subTypeOrEqual())
            && comparator.compare(t2, builtins.timestamp())
            .is(subTypeOrEqual())) {
            return "jadescript.lang.Timestamp.compare(" + leftCompiled
                + ", " + rightCompiled + ") " + relationalOp + " 0";
        } else {
            return leftCompiled + " " + relationalOp + " " + rightCompiled;
        }
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<RelationalComparison> input,
        StaticState state
    ) {
        if (input == null) {
            return module.get(BuiltinTypeProvider.class).any("");
        }
        return module.get(BuiltinTypeProvider.class).boolean_();
    }


    @Override
    protected boolean mustTraverse(Maybe<RelationalComparison> input) {
        final Maybe<ContainmentCheck> right =
            input.__(RelationalComparison::getRight);
        return right.isNothing();
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>>
    traverseInternal(Maybe<RelationalComparison> input) {
        final Maybe<ContainmentCheck> left =
            input.__(RelationalComparison::getLeft);

        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToExpression<>(
                module.get(ContainmentCheckExpressionSemantics.class),
                left
            ));
        }

        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<RelationalComparison> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<RelationalComparison> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    private boolean isInteger(IJadescriptType type){
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        return comparator.compare(type, builtins.integer())
            .is(subTypeOrEqual());
    }

    private boolean isReal(IJadescriptType type){
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        return comparator.compare(type, builtins.real())
            .is(subTypeOrEqual());
    }

    private boolean isNumber(IJadescriptType type) {
        return isInteger(type) || isReal(type);
    }


    private boolean isDuration(IJadescriptType type) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        return comparator.compare(type, builtins.duration())
            .is(subTypeOrEqual());
    }


    private boolean isTimestamp(IJadescriptType type) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        return comparator.compare(type, builtins.timestamp())
            .is(subTypeOrEqual());
    }


    @Override
    protected boolean validateInternal(
        Maybe<RelationalComparison> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return VALID;
        }
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final Maybe<ContainmentCheck> left =
            input.__(RelationalComparison::getLeft);
        final Maybe<ContainmentCheck> right =
            input.__(RelationalComparison::getRight);
        final ContainmentCheckExpressionSemantics cces =
            module.get(ContainmentCheckExpressionSemantics.class);
        final boolean leftValidate = cces.validate(left, state, acceptor);
        final StaticState afterLeft = cces.advance(left, state);
        final boolean rightValidate = cces.validate(right, afterLeft, acceptor);
        if (leftValidate == VALID && rightValidate == VALID) {
            IJadescriptType typeLeft = cces.inferType(left, state);
            IJadescriptType typeRight = cces.inferType(right, afterLeft);
            final ValidationHelper validationHelper =
                module.get(ValidationHelper.class);
            boolean ltValidation = validationHelper.assertExpectedTypesAny(
                List.of(
                    builtins.integer(),
                    builtins.real(),
                    builtins.timestamp(),
                    builtins.duration()
                ),
                typeLeft,
                "InvalidOperandType",
                left,
                acceptor
            );
            boolean rtValidation = validationHelper.assertExpectedTypesAny(
                List.of(
                    builtins.integer(),
                    builtins.real(),
                    builtins.timestamp(),
                    builtins.duration()
                ),
                typeRight,
                "InvalidOperandType",
                right,
                acceptor
            );

            boolean otherValidation = validationHelper.asserting(
                //implication: if left is NUMBER, right has to be NUMBER too
                (!isNumber(typeLeft) || isNumber(typeRight))
                    //implication: if left is DURATION,
                    // right has to be DURATION too
                    && (!isDuration(typeLeft) || isDuration(typeRight))
                    //implication: if left is TIMESTAMP,
                    // right has to be TIMESTAMP too
                    && (!isTimestamp(typeLeft) || isTimestamp(typeRight)),
                "IncongruentOperandTypes",
                "Incompatible types for comparison: '"
                    + typeLeft.getFullJadescriptName()
                    + "', '" + typeRight.getFullJadescriptName() + "'",
                input,
                acceptor
            );

            return ltValidation && rtValidation && otherValidation;
        }

        return leftValidate && rightValidate;
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<RelationalComparison> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<RelationalComparison> input,
        StaticState state
    ) {
        final Maybe<ContainmentCheck> left =
            input.__(RelationalComparison::getLeft);
        final Maybe<ContainmentCheck> right =
            input.__(RelationalComparison::getRight);
        final ContainmentCheckExpressionSemantics cces =
            module.get(ContainmentCheckExpressionSemantics.class);
        final StaticState afterLeft = cces.advance(left, state);
        return cces.advance(right, afterLeft);
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<RelationalComparison> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<RelationalComparison> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<RelationalComparison> input) {
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state
    ) {
        return false;
    }

}
