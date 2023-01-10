package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RelationalComparison;
import it.unipr.ailab.jadescript.jadescript.TypeComparison;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.PatternDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.*;

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
        final Maybe<TypeExpression> type = input.__(TypeComparison::getType);

        return Stream.of(
            left.extract(x -> new SemanticsBoundToExpression<>(
                module.get(RelationalComparisonExpressionSemantics.class),
                x
            )),
            //TODO should consider type expressions?
            type.extract(x -> new SemanticsBoundToExpression<>(
                module.get(TypeExpressionSemantics.class),
                x
            ))
        );
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<TypeComparison> input,
        StaticState state
    ) {
        final Maybe<RelationalComparison> left =
            input.__(TypeComparison::getRelationalComparison);
        final Maybe<ExpressionDescriptor> leftDescriptorMaybe =
            module.get(RelationalComparisonExpressionSemantics.class)
                .describeExpression(left, state);

        if (leftDescriptorMaybe.isNothing()) {
            return Maybe.nothing();
        }
        final Maybe<TypeExpression> type = input.__(TypeComparison::getType);
        final IJadescriptType typeResolved =
            module.get(TypeExpressionSemantics.class)
                .toJadescriptType(type);

        return some(new ExpressionDescriptor.TypeCheck(
            leftDescriptorMaybe.toNullable(),
            typeResolved
        ));
    }


    @Override
    protected Maybe<PatternDescriptor> describePatternInternal(
        PatternMatchInput<TypeComparison> input,
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

        Maybe<ExpressionDescriptor> expressionDescriptor =
            rces.describeExpression(left, state);

        StaticState afterLeft = rces.advance(left, state);

        if (expressionDescriptor.isNothing()) {
            return afterLeft;
        }

        final Maybe<TypeExpression> type = input.__(TypeComparison::getType);
        final IJadescriptType typeResolved =
            module.get(TypeExpressionSemantics.class).toJadescriptType(type);

        if (typeResolved.isErroneous()) {
            return afterLeft;
        }

        return afterLeft.assertFlowTypingUpperBound(
            expressionDescriptor.toNullable(),
            typeResolved
        );
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<TypeComparison> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected String compileInternal(
        Maybe<TypeComparison> input,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        if (input == null) return "";

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
        if (module.get(TypeHelper.class).ONTOLOGY.isAssignableFrom(
            jadescriptType)) {
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
        if (input == null) return module.get(TypeHelper.class).ANY;
        return module.get(TypeHelper.class).BOOLEAN;
    }


    @Override
    protected boolean mustTraverse(Maybe<TypeComparison> input) {
        final boolean isOp = input.__(TypeComparison::isIsOp)
            .extract(nullAsFalse);
        return !isOp;
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(
        Maybe<TypeComparison> input
    ) {
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
    protected boolean isPatternEvaluationPureInternal(
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
        if (input == null) return VALID;
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
        CompilationOutputAcceptor acceptor
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
    protected boolean isAlwaysPureInternal(
        Maybe<TypeComparison> input,
        StaticState state
    ) {
        return subExpressionsAllAlwaysPure(input, state);
    }


    @Override
    protected boolean isValidLExprInternal(Maybe<TypeComparison> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        Maybe<TypeComparison> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<TypeComparison> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<TypeComparison> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<TypeComparison> input) {
        return false;
    }

}
