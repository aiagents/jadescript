package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.statement.StatementCompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.unipr.ailab.jadescript.semantics.expression.ExpressionCompilationResult.empty;

/**
 * Created on 28/12/16.
 */
@Singleton
public class LogicalAndExpressionSemantics extends ExpressionSemantics<LogicalAnd> {


    public LogicalAndExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<LogicalAnd> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }

        return Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison)).stream()
                .map(x -> new SemanticsBoundToExpression<>(module.get(EqualityComparisonExpressionSemantics.class), x))
                .collect(Collectors.toList());
    }

    @Override
    public ExpressionCompilationResult compile(Maybe<LogicalAnd> input, StatementCompilationOutputAcceptor acceptor) {
        if (input == null) return empty();
        ExpressionCompilationResult result = empty();
        List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison));
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        for (int i = 0; i < equs.size(); i++) {
            Maybe<EqualityComparison> equ = equs.get(i);
            final ExpressionCompilationResult operandCompiled = module.get(EqualityComparisonExpressionSemantics.class)
                    .compile(equ, acceptor);
            if (i != 0) {
                result = ExpressionCompilationResult.result(result + " && " + operandCompiled)
                        .setFTKB(typeHelper.mergeByGLB(
                                result.getFlowTypingKB(),
                                operandCompiled.getFlowTypingKB()
                        ));
            } else {
                result = operandCompiled;
            }
        }
        return result;
    }

    @Override
    public IJadescriptType inferType(Maybe<LogicalAnd> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison));
        if (equs.size() < 1) {
            return module.get(TypeHelper.class).ANY;
        } else if (equs.size() == 1) {
            return module.get(EqualityComparisonExpressionSemantics.class).inferType(equs.get(0));
        } else {
            return module.get(TypeHelper.class).BOOLEAN;
        }
    }


    @Override
    public ExpressionTypeKB extractFlowTypeTruths(Maybe<LogicalAnd> input) {
        List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison));
        if (equs.size() < 1) {
            return ExpressionTypeKB.empty();
        } else if (equs.size() == 1) {
            return module.get(EqualityComparisonExpressionSemantics.class).extractFlowTypeTruths(equs.get(0));
        } else {
            ExpressionTypeKB t = module.get(EqualityComparisonExpressionSemantics.class)
                    .extractFlowTypeTruths(equs.get(0));
            for (int i = 1; i < equs.size(); i++) {
                ExpressionTypeKB t2 = module.get(EqualityComparisonExpressionSemantics.class)
                        .extractFlowTypeTruths(equs.get(i));
                t = module.get(TypeHelper.class).mergeByGLB(t, t2);
            }
            return t;
        }
    }

    @Override
    public boolean mustTraverse(Maybe<LogicalAnd> input) {
        List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison));

        return equs.size() == 1;
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<LogicalAnd> input) {
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
    public boolean isPatternEvaluationPure(Maybe<LogicalAnd> input) {
        List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison));
        if (equs.size() < 1) {
            return true;
        } else {
            return equs.stream()
                    .allMatch(module.get(EqualityComparisonExpressionSemantics.class)::isPatternEvaluationPure);
        }
    }

    @Override
    public void validate(Maybe<LogicalAnd> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison));

        if (equs.size() == 1) {
            module.get(EqualityComparisonExpressionSemantics.class).validate(equs.get(0), acceptor);
            return;
        }

        if (equs.size() > 1) {

            for (int i = 0; i < equs.size(); i++) {
                Maybe<EqualityComparison> equ = equs.get(i);
                InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);
                module.get(EqualityComparisonExpressionSemantics.class).validate(equ, subValidation);
                if (!subValidation.thereAreErrors()) {
                    IJadescriptType type = module.get(EqualityComparisonExpressionSemantics.class).inferType(equ);
                    module.get(ValidationHelper.class).assertExpectedType(Boolean.class, type,
                            "InvalidOperandType",
                            input,
                            JadescriptPackage.eINSTANCE.getLogicalAnd_EqualityComparison(),
                            i,
                            acceptor
                    );
                }
            }
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<LogicalAnd, ?, ?> input, StatementCompilationOutputAcceptor acceptor) {
        final Maybe<LogicalAnd> pattern = input.getPattern();
        final List<Maybe<EqualityComparison>> operands = Maybe.toListOfMaybes(
                pattern.__(LogicalAnd::getEqualityComparison));
        if (mustTraverse(pattern)) {
            return module.get(EqualityComparisonExpressionSemantics.class).compilePatternMatchInternal(
                    input.replacePattern(operands.get(0)),
                    acceptor
            );
        } else {
            return input.createEmptyCompileOutput();
        }
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<LogicalAnd> input) {

        final List<Maybe<EqualityComparison>> operands = Maybe.toListOfMaybes(
                input.__(LogicalAnd::getEqualityComparison));
        if (mustTraverse(input)) {
            return module.get(EqualityComparisonExpressionSemantics.class).inferPatternTypeInternal(operands.get(0));
        } else {
            return PatternType.empty(module);
        }
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


}
