package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * Created on 28/12/16.
 */
@Singleton
public class LogicalAndExpressionSemantics extends ExpressionSemantics<LogicalAnd> {


    public LogicalAndExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<LogicalAnd> input) {
        return Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison))
                .stream()
                .map(x -> new SemanticsBoundToExpression<>(module.get(EqualityComparisonExpressionSemantics.class), x));
    }

    @Override
    protected String compileInternal(Maybe<LogicalAnd> input, CompilationOutputAcceptor acceptor) {
        if (input == null) return "";
        StringBuilder result = new StringBuilder();
        List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison));
        for (int i = 0; i < equs.size(); i++) {
            Maybe<EqualityComparison> equ = equs.get(i);
            final String operandCompiled = module.get(EqualityComparisonExpressionSemantics.class)
                    .compile(equ, acceptor);
            if (i != 0) {
                result.append(" && ").append(operandCompiled);
            } else {
                result = new StringBuilder(operandCompiled);
            }
        }
        return result.toString();
    }


    @Override
    protected List<String> propertyChainInternal(Maybe<LogicalAnd> input) {
        return Collections.emptyList();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<LogicalAnd> input) {
        return mapSubExpressions(input, ExpressionSemantics::computeKB)
                .reduce(ExpressionTypeKB.empty(), module.get(TypeHelper.class)::mergeByGLB);
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<LogicalAnd> input) {
        return module.get(TypeHelper.class).BOOLEAN;
    }


    @Override
    protected boolean mustTraverse(Maybe<LogicalAnd> input) {
        List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison));
        return equs.size() == 1;
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<LogicalAnd> input) {
        if (mustTraverse(input)) {
            List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison));
            return Optional.of(new SemanticsBoundToExpression<>(
                    module.get(EqualityComparisonExpressionSemantics.class),
                    equs.get(0)
            ));
        }
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<LogicalAnd> input) {
        return subExpressionsAllMatch(input, ExpressionSemantics::isPatternEvaluationPure);
    }

    @Override
    protected boolean validateInternal(Maybe<LogicalAnd> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison));
        if (equs.size() > 1) {
            boolean result = VALID;
            for (int i = 0; i < equs.size(); i++) {
                Maybe<EqualityComparison> equ = equs.get(i);
                boolean equValidation = module.get(EqualityComparisonExpressionSemantics.class)
                        .validate(equ, acceptor);
                if (equValidation == VALID) {
                    IJadescriptType type = module.get(EqualityComparisonExpressionSemantics.class).inferType(equ);
                    final boolean operandType = module.get(ValidationHelper.class).assertExpectedType(
                            Boolean.class,
                            type,
                            "InvalidOperandType",
                            input,
                            JadescriptPackage.eINSTANCE.getLogicalAnd_EqualityComparison(),
                            i,
                            acceptor
                    );
                    result = result && operandType;
                } else {
                    result = INVALID;
                }
            }
            return result;
        } else {

            return VALID;
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<LogicalAnd, ?, ?> input, CompilationOutputAcceptor acceptor) {
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<LogicalAnd> input) {
        return PatternType.empty(module);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?>
    validatePatternMatchInternal(
            PatternMatchInput<LogicalAnd, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<LogicalAnd> pattern = input.getPattern();
        final List<Maybe<EqualityComparison>> operands = Maybe.toListOfMaybes(
                pattern.__(LogicalAnd::getEqualityComparison)
        );
        if (mustTraverse(pattern)) {
            return module.get(EqualityComparisonExpressionSemantics.class).validatePatternMatchInternal(
                    input.replacePattern(operands.get(0)),
                    acceptor
            );
        } else {
            return input.createEmptyValidationOutput();
        }
    }


    @Override
    protected boolean isAlwaysPureInternal(Maybe<LogicalAnd> input) {
        return subExpressionsAllAlwaysPure(input);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<LogicalAnd> input) {
        return false;
    }

    @Override
    protected boolean isHoledInternal(Maybe<LogicalAnd> input) {
        return false;
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<LogicalAnd> input) {
        return false;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<LogicalAnd> input) {
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<LogicalAnd> input) {
        return false;
    }


}
