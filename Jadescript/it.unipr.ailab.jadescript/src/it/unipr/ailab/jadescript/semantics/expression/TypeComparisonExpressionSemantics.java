package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.FlowTypeInferringTerm;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
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
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }
        final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);
        final Maybe<TypeExpression> type = input.__(TypeComparison::getType);

        return Arrays.asList(
                left.extract(x -> new SemanticsBoundToExpression<>(
                        module.get(RelationalComparisonExpressionSemantics.class),
                        x
                )),
                type.extract(x -> new SemanticsBoundToExpression<>(
                        module.get(TypeExpressionSemantics.class),
                        x
                ))
        );
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<TypeComparison> input) {
        return Collections.emptyList();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<TypeComparison> input) {
        final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);
        final Maybe<TypeExpression> type = input.__(TypeComparison::getType);

        ExpressionTypeKB subKb = module.get(RelationalComparisonExpressionSemantics.class).computeKB(left);
        List<String> strings = module.get(RelationalComparisonExpressionSemantics.class).propertyChain(left);
        subKb.add(
                FlowTypeInferringTerm.of(module.get(TypeExpressionSemantics.class).toJadescriptType(type)),
                strings
        );
        return subKb;
    }

    @Override
    protected String compileInternal(Maybe<TypeComparison> input, CompilationOutputAcceptor acceptor) {
        if (input == null) return "";

        final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);
        final boolean isOp = input.__(TypeComparison::isIsOp).extract(nullAsFalse);
        final Maybe<TypeExpression> type = input.__(TypeComparison::getType);

        String result = module.get(RelationalComparisonExpressionSemantics.class).compile(left, acceptor);
        if (isOp) {
            IJadescriptType jadescriptType = module.get(TypeExpressionSemantics.class).toJadescriptType(type);
            String compiledTypeExpression = jadescriptType.compileToJavaTypeReference();
            if (module.get(TypeHelper.class).ONTOLOGY.isAssignableFrom(jadescriptType)) {
                result = THE_AGENTCLASS + ".__checkOntology(" + result + ", " +
                        compiledTypeExpression + ".class, " +
                        compiledTypeExpression + ".getInstance())";
            } else {
                //attempt to do the "safest" version if possible (safest === no compiler errors in java generated code)
                if (!compiledTypeExpression.contains("<")) {
                    result = compiledTypeExpression + ".class.isInstance(" + result + ")";
                } else {
                    result = result + " instanceof " + compiledTypeExpression;
                }
            }
        }
        return result;
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<TypeComparison> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);
        final boolean isOp = input.__(TypeComparison::isIsOp).extract(nullAsFalse);

        if (isOp) {
            return module.get(TypeHelper.class).BOOLEAN;
        }
        return module.get(RelationalComparisonExpressionSemantics.class).inferType(left);
    }

    @Override
    protected boolean mustTraverse(Maybe<TypeComparison> input) {
        final boolean isOp = input.__(TypeComparison::isIsOp).extract(nullAsFalse);
        return !isOp;
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<TypeComparison> input) {
        if (mustTraverse(input)) {
            final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);

            return Optional.of(new SemanticsBoundToExpression<>(module.get(RelationalComparisonExpressionSemantics.class), left));
        }
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<TypeComparison> input) {
        if (mustTraverse(input)) {
            final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);

            return module.get(RelationalComparisonExpressionSemantics.class).isPatternEvaluationPure(left);
        }
        return false;
    }

    @Override
    protected boolean validateInternal(Maybe<TypeComparison> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);
        boolean subResult = module.get(RelationalComparisonExpressionSemantics.class)
                .validate(left, acceptor);
        final Maybe<TypeExpression> typeExpr = input.__(TypeComparison::getType);
        IJadescriptType type = module.get(TypeExpressionSemantics.class)
                .toJadescriptType(typeExpr);

        type.validateType(typeExpr, acceptor);

        return subResult;
    }


    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<TypeComparison, ?, ?> input, CompilationOutputAcceptor acceptor) {
        final Maybe<TypeComparison> pattern = input.getPattern();
        if (mustTraverse(pattern)) {
            return module.get(RelationalComparisonExpressionSemantics.class).compilePatternMatchInternal(
                    input.mapPattern(TypeComparison::getRelationalComparison),
                    acceptor
            );
        } else {
            return input.createEmptyCompileOutput();
        }
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<TypeComparison> input) {
        if (mustTraverse(input)) {
            return module.get(RelationalComparisonExpressionSemantics.class).inferPatternTypeInternal(
                    input.__(TypeComparison::getRelationalComparison));
        } else {
            return PatternType.empty(module);
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<TypeComparison, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<TypeComparison> pattern = input.getPattern();
        if (mustTraverse(pattern)) {
            return module.get(RelationalComparisonExpressionSemantics.class).validatePatternMatchInternal(
                    input.mapPattern(TypeComparison::getRelationalComparison),
                    acceptor
            );
        } else {
            return input.createEmptyValidationOutput();
        }
    }
}
