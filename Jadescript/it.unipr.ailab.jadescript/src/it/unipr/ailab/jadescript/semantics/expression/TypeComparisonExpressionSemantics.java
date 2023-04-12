package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RelationalComparison;
import it.unipr.ailab.jadescript.jadescript.TypeComparison;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;
import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

/**
 * Created on 28/12/16.
 */
@Singleton
public class TypeComparisonExpressionSemantics
    extends ExpressionSemantics<TypeComparison> {


    public TypeComparisonExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<TypeComparison> input
    ) {
        final Maybe<RelationalComparison> left =
            input.__(TypeComparison::getRelationalComparison);

        return Stream.of(left)
            .filter(Maybe::isPresent)
            .map(x -> new SemanticsBoundToExpression<>(
                module.get(RelationalComparisonExpressionSemantics.class),
                x
            ));
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<TypeComparison> input,
        StaticState state
    ) {
        return nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<TypeComparison> input,
        StaticState state
    ) {
        final Maybe<RelationalComparison> left =
            input.__(TypeComparison::getRelationalComparison);

        final RelationalComparisonExpressionSemantics rces =
            module.get(RelationalComparisonExpressionSemantics.class);

        return rces.advance(left, state);
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<TypeComparison> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<TypeComparison> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<TypeComparison> input,
        StaticState state
    ) {
        final Maybe<RelationalComparison> left =
            input.__(TypeComparison::getRelationalComparison);

        final RelationalComparisonExpressionSemantics rces =
            module.get(RelationalComparisonExpressionSemantics.class);

        Maybe<ExpressionDescriptor> expressionDescriptor =
            rces.describeExpression(left, state);


        final Maybe<TypeExpression> type = input.__(TypeComparison::getType);
        final IJadescriptType typeResolved =
            module.get(TypeExpressionSemantics.class).toJadescriptType(type);

        if (typeResolved.isErroneous()) {
            return state;
        }

        return state.assertFlowTypingUpperBound(
            expressionDescriptor,
            typeResolved
        );
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<TypeComparison> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected String compileInternal(
        Maybe<TypeComparison> input,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        if (input == null) {
            return "";
        }

        final Maybe<RelationalComparison> left =
            input.__(TypeComparison::getRelationalComparison);
        final Maybe<TypeExpression> type = input.__(TypeComparison::getType);

        final RelationalComparisonExpressionSemantics rces =
            module.get(RelationalComparisonExpressionSemantics.class);
        String result = rces.compile(left, state, acceptor);
        IJadescriptType jadescriptType =
            module.get(TypeExpressionSemantics.class)
                .toJadescriptType(type);
        String compiledTypeExpression =
            jadescriptType.compileToJavaTypeReference();

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        if (comparator.compare(builtins.ontology(), jadescriptType)
            .is(superTypeOrEqual())) {
            result = THE_AGENTCLASS + ".__checkOntology(" + result + ", " +
                compiledTypeExpression + ".class, " +
                compiledTypeExpression + ".getInstance())";
        } else {
            //attempt to do the "safest" version if possible (where safe = no
            // compiler errors in java generated code)
            if (!compiledTypeExpression.contains("<")) {
                result =
                    compiledTypeExpression + ".class.isInstance(" +
                        result + ")";
            } else {
                result = result + " instanceof " + compiledTypeExpression;
            }
        }
        return result;
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<TypeComparison> input,
        StaticState state
    ) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        if (input == null) {
            return builtins.any("");
        }
        return builtins.boolean_();
    }


    @Override
    protected boolean mustTraverse(Maybe<TypeComparison> input) {
        final boolean isOp = input.__(TypeComparison::isIsOp)
            .extract(nullAsFalse);
        return !isOp;
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>>
    traverseInternal(Maybe<TypeComparison> input) {
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToExpression<>(
                    module.get(RelationalComparisonExpressionSemantics.class),
                    input.__(TypeComparison::getRelationalComparison)
                )
            );
        }
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<TypeComparison> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean validateInternal(
        Maybe<TypeComparison> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return VALID;
        }
        final RelationalComparisonExpressionSemantics rces =
            module.get(RelationalComparisonExpressionSemantics.class);
        boolean leftCheck = rces.validate(
            input.__(TypeComparison::getRelationalComparison),
            state,
            acceptor
        );
        final Maybe<TypeExpression> typeExpr =
            input.__(TypeComparison::getType);
        IJadescriptType type = module.get(TypeExpressionSemantics.class)
            .toJadescriptType(typeExpr);

        boolean typeCheck = type.validateType(typeExpr, acceptor);

        return leftCheck && typeCheck;
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<TypeComparison> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<TypeComparison> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<TypeComparison> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<TypeComparison> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<TypeComparison> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<TypeComparison> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<TypeComparison> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<TypeComparison> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<TypeComparison> input) {
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<TypeComparison> input,
        StaticState state
    ) {
        return false;
    }

}
