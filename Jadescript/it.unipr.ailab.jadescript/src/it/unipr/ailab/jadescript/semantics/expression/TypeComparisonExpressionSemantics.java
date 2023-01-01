package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.FlowTypeInferringTerm;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

/**
 * Created on 28/12/16.
 */
@Singleton
public class TypeComparisonExpressionSemantics extends ExpressionSemantics<TypeComparison> {


    public TypeComparisonExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<TypeComparison> input) {
        final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);
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
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(Maybe<TypeComparison> input, StaticState state) {
        return Collections.emptyList();
    }

    @Override
    protected StaticState advanceInternal(Maybe<TypeComparison> input,
                                          StaticState state) {
        final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);
        final Maybe<TypeExpression> type = input.__(TypeComparison::getType);

        ExpressionTypeKB subKb = module.get(RelationalComparisonExpressionSemantics.class).advance(left, ).copy();
        List<String> propChain = module.get(RelationalComparisonExpressionSemantics.class).describeExpression(left, );
        subKb.add(
                FlowTypeInferringTerm.of(module.get(TypeExpressionSemantics.class).toJadescriptType(type)),
                propChain
        );
        return subKb;
    }

    @Override
    protected String compileInternal(Maybe<TypeComparison> input,
                                     StaticState state, CompilationOutputAcceptor acceptor) {
        if (input == null) return "";

        final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);
        final Maybe<TypeExpression> type = input.__(TypeComparison::getType);

        String result = module.get(RelationalComparisonExpressionSemantics.class).compile(left, , acceptor);
        IJadescriptType jadescriptType = module.get(TypeExpressionSemantics.class).toJadescriptType(type);
        String compiledTypeExpression = jadescriptType.compileToJavaTypeReference();
        if (module.get(TypeHelper.class).ONTOLOGY.isAssignableFrom(jadescriptType)) {
            result = THE_AGENTCLASS + ".__checkOntology(" + result + ", " +
                    compiledTypeExpression + ".class, " +
                    compiledTypeExpression + ".getInstance())";
        } else {
            //attempt to do the "safest" version if possible (where safe = no compiler errors in java generated code)
            if (!compiledTypeExpression.contains("<")) {
                result = compiledTypeExpression + ".class.isInstance(" + result + ")";
            } else {
                result = result + " instanceof " + compiledTypeExpression;
            }
        }
        return result;
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<TypeComparison> input,
                                                StaticState state) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);
        return module.get(TypeHelper.class).BOOLEAN;
    }

    @Override
    protected boolean mustTraverse(Maybe<TypeComparison> input) {
        final boolean isOp = input.__(TypeComparison::isIsOp).extract(nullAsFalse);
        return !isOp;
    }

    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(Maybe<TypeComparison> input) {
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
    protected boolean isPatternEvaluationPureInternal(PatternMatchInput<TypeComparison> input, StaticState state) {
        return false;
    }

    @Override
    protected boolean validateInternal(Maybe<TypeComparison> input, StaticState state, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        boolean subResult = module.get(RelationalComparisonExpressionSemantics.class)
                .validate(input.__(TypeComparison::getRelationalComparison), , acceptor);
        final Maybe<TypeExpression> typeExpr = input.__(TypeComparison::getType);
        IJadescriptType type = module.get(TypeExpressionSemantics.class)
                .toJadescriptType(typeExpr);

        type.validateType(typeExpr, acceptor);

        return subResult;
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(PatternMatchInput<TypeComparison> input, StaticState state, CompilationOutputAcceptor acceptor) {
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<TypeComparison> input,
                                                StaticState state) {
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
    protected boolean isAlwaysPureInternal(Maybe<TypeComparison> input,
                                           StaticState state) {
        return subExpressionsAllAlwaysPure(input, state);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<TypeComparison> input) {
        return false;
    }

    @Override
    protected boolean isHoledInternal(Maybe<TypeComparison> input,
                                      StaticState state) {
        return false;
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<TypeComparison> input,
                                            StaticState state) {
        return false;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<TypeComparison> input,
                                        StaticState state) {
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<TypeComparison> input) {
        return false;
    }
}
