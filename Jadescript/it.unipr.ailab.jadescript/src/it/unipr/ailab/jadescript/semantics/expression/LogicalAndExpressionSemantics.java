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
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.nothing;

/**
 * Created on 28/12/16.
 *
 * 
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
    public Maybe<String> compile(Maybe<LogicalAnd> input) {
        if (input == null) return nothing();
        StringBuilder sb = new StringBuilder();
        List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison));
        for (int i = 0; i < equs.size(); i++) {
            Maybe<EqualityComparison> equ = equs.get(i);
            if (i != 0) {
                sb.append(" && ");
            }
            sb.append(module.get(EqualityComparisonExpressionSemantics.class).compile(equ));
        }
        return Maybe.of(sb.toString());
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
            ExpressionTypeKB t = module.get(EqualityComparisonExpressionSemantics.class).extractFlowTypeTruths(equs.get(0));
            for (int i = 1; i < equs.size(); i++) {
                ExpressionTypeKB t2 = module.get(EqualityComparisonExpressionSemantics.class).extractFlowTypeTruths(equs.get(i));
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
            return Optional.of(new SemanticsBoundToExpression<>(module.get(EqualityComparisonExpressionSemantics.class), equs.get(0)));
        }
        return Optional.empty();
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
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<LogicalAnd, ?, ?> input) {
        final Maybe<LogicalAnd> pattern = input.getPattern();
        final List<Maybe<EqualityComparison>> operands = Maybe.toListOfMaybes(pattern.__(LogicalAnd::getEqualityComparison));
        if (mustTraverse(pattern)) {
            return module.get(EqualityComparisonExpressionSemantics.class).compilePatternMatchInternal(
                    input.mapPattern(__ -> operands.get(0).toNullable())
            );
        } else {
            return input.createEmptyCompileOutput();
        }
    }

    @Override
    protected PatternType inferPatternTypeInternal(PatternMatchInput<LogicalAnd, ?, ?> input) {
        final Maybe<LogicalAnd> pattern = input.getPattern();
        final List<Maybe<EqualityComparison>> operands = Maybe.toListOfMaybes(pattern.__(LogicalAnd::getEqualityComparison));
        if (mustTraverse(pattern)) {
            return module.get(EqualityComparisonExpressionSemantics.class).inferPatternTypeInternal(
                    input.mapPattern(__ -> operands.get(0).toNullable())
            );
        }else{
            return PatternType.empty(module);
        }
    }

    @Override
    protected PatternMatchOutput<PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<LogicalAnd, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<LogicalAnd> pattern = input.getPattern();
        final List<Maybe<EqualityComparison>> operands = Maybe.toListOfMaybes(pattern.__(LogicalAnd::getEqualityComparison));
        if (mustTraverse(pattern)) {
            return module.get(EqualityComparisonExpressionSemantics.class).validatePatternMatchInternal(
                    input.mapPattern(__ -> operands.get(0).toNullable()),
                    acceptor
            );
        } else {
            return input.createEmptyValidationOutput();
        }
    }


}
