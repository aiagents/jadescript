package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TernaryConditional;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.effectanalysis.EffectfulOperationSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.StatementCompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.jadescript.semantics.expression.ExpressionCompilationResult.empty;
import static it.unipr.ailab.maybe.Maybe.nothing;

/**
 * Created on 27/12/16.
 */
@Singleton
public class RValueExpressionSemantics extends ExpressionSemantics<RValueExpression>
        implements EffectfulOperationSemantics {


    public RValueExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<RValueExpression> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }

        if (input.isInstanceOf(SyntheticExpression.class)) {
            return Collections.singletonList(
                    new SemanticsBoundToExpression<>(
                            module.get(SyntheticExpressionSemantics.class),
                            input.__((i -> (SyntheticExpression) i))
                    ));
        }
        if (input.isInstanceOf(TernaryConditional.class)) {
            return Collections.singletonList(
                    new SemanticsBoundToExpression<>(
                            module.get(TernaryConditionalExpressionSemantics.class),
                            input.__((i -> (TernaryConditional) i))
                    ));
        }

        return Collections.emptyList();
    }

    @Override
    public ExpressionCompilationResult compile(Maybe<RValueExpression> input, StatementCompilationOutputAcceptor acceptor) {
        if (input == null || input.isNothing()) return empty();
        if (input.isInstanceOf(SyntheticExpression.class)) {
            return module.get(SyntheticExpressionSemantics.class).compile(
                    input.__((i -> (SyntheticExpression) i)),
                    acceptor
            );
        }

        if (input.isInstanceOf(TernaryConditional.class)) {
            return module.get(TernaryConditionalExpressionSemantics.class).compile(
                    input.__((i -> (TernaryConditional) i)),
                    acceptor
            );
        }

        throw new UnsupportedNodeType("RExpr can be only a TernaryConditional (or derived) " +
                "- type found: " + input.getClass().getName());
    }

    @Override
    public IJadescriptType inferType(Maybe<RValueExpression> input) {
        if (input == null || input.isNothing()) return module.get(TypeHelper.class).ANY;
        if (input.isInstanceOf(SyntheticExpression.class)) {
            return module.get(SyntheticExpressionSemantics.class).inferType(input.__((i -> (SyntheticExpression) i)));
        }
        return module.get(TernaryConditionalExpressionSemantics.class).inferType(input.__((i -> (TernaryConditional) i)));
    }


    @Override
    public boolean mustTraverse(Maybe<RValueExpression> input) {
        return input.isInstanceOf(SyntheticExpression.class) || input.isInstanceOf(TernaryConditional.class);
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<RValueExpression> input) {
        if (mustTraverse(input)) {
            if (input.isInstanceOf(SyntheticExpression.class)) {
                return Optional.of(
                        new SemanticsBoundToExpression<>(
                                module.get(SyntheticExpressionSemantics.class),
                                input.__((i -> (SyntheticExpression) i))
                        ));
            }
            if (input.isInstanceOf(TernaryConditional.class)) {
                return Optional.of(
                        new SemanticsBoundToExpression<>(
                                module.get(TernaryConditionalExpressionSemantics.class),
                                input.__((i -> (TernaryConditional) i))
                        ));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isPatternEvaluationPure(Maybe<RValueExpression> input) {
        if (input.isInstanceOf(SyntheticExpression.class)) {
            return module.get(SyntheticExpressionSemantics.class).isPatternEvaluationPure(
                    input.__((i -> (SyntheticExpression) i))
            );
        }else if (input.isInstanceOf(TernaryConditional.class)) {
            return module.get(TernaryConditionalExpressionSemantics.class).isPatternEvaluationPure(
                    input.__((i -> (TernaryConditional) i))
            );
        }else {
            return false;
        }
    }


    @Override
    public void validate(Maybe<RValueExpression> input, ValidationMessageAcceptor acceptor) {
        if (input == null || input.isNothing()) return;
        if (input.isInstanceOf(SyntheticExpression.class)) {
            module.get(SyntheticExpressionSemantics.class).validate(input.__((i -> (SyntheticExpression) i)), acceptor);
            return;
        }
        if (input.isInstanceOf(TernaryConditional.class)) {
            module.get(TernaryConditionalExpressionSemantics.class).validate(input.__((i -> (TernaryConditional) i)), acceptor);
            return;
        }
        throw new UnsupportedNodeType("RExpr can be only a TernaryConditional (or derived) " +
                "- type found: " + input.getClass().getName());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public SemanticsBoundToExpression<?> deepTraverse(Maybe<RValueExpression> input) {
        Optional<SemanticsBoundToExpression<?>> x = Optional.of(
                new SemanticsBoundToExpression<>(this, input)
        );
        Optional<SemanticsBoundToExpression<?>> lastPresent = x;
        while (x.isPresent()) {
            lastPresent = x;
            x = x.get().getSemantics().traverse((Maybe) x.get().getInput());
        }
        return lastPresent.get();
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<RValueExpression, ?, ?> input, StatementCompilationOutputAcceptor acceptor) {
        final Maybe<RValueExpression> pattern = input.getPattern();

        if (mustTraverse(pattern)) {
            if (pattern.isInstanceOf(SyntheticExpression.class)) {
                return module.get(SyntheticExpressionSemantics.class).compilePatternMatchInternal(
                        input.mapPattern(x -> (SyntheticExpression) x),
                        acceptor
                );
            }
            if (pattern.isInstanceOf(TernaryConditional.class)) {
                return module.get(TernaryConditionalExpressionSemantics.class).compilePatternMatchInternal(
                        input.mapPattern(x -> (TernaryConditional) x),
                        acceptor
                );
            }

        }

        return input.createEmptyCompileOutput();

    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<RValueExpression> input) {

        if (mustTraverse(input)) {
            if (input.isInstanceOf(SyntheticExpression.class)) {
                return module.get(SyntheticExpressionSemantics.class).inferPatternTypeInternal(
                        input.__(i -> (SyntheticExpression) i));
            }
            if (input.isInstanceOf(TernaryConditional.class)) {
                return module.get(TernaryConditionalExpressionSemantics.class).inferPatternTypeInternal(
                        input.__(i -> (TernaryConditional) i));
            }

        }
        return PatternType.empty(module);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<RValueExpression, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<RValueExpression> pattern = input.getPattern();

        if (mustTraverse(pattern)) {
            if (pattern.isInstanceOf(SyntheticExpression.class)) {
                return module.get(SyntheticExpressionSemantics.class).validatePatternMatchInternal(
                        input.mapPattern(x -> (SyntheticExpression) x),
                        acceptor
                );
            }
            if (pattern.isInstanceOf(TernaryConditional.class)) {
                return module.get(TernaryConditionalExpressionSemantics.class).validatePatternMatchInternal(
                        input.mapPattern(x -> (TernaryConditional) x),
                        acceptor
                );
            }

        }

        return input.createEmptyValidationOutput();
    }

}
