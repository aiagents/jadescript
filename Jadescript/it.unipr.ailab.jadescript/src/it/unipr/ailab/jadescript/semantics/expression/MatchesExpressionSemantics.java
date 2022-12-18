package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c2feature.HandlerWhenExpressionContext;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.FlowTypeInferringTerm;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.PatternMatchHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.StatementCompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.jadescript.semantics.expression.ExpressionCompilationResult.result;
import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

/**
 * Created on 2019-08-18.
 */
@Singleton
public class MatchesExpressionSemantics extends ExpressionSemantics<Matches> {


    public MatchesExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<Matches> input) {
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }

        return Collections.singletonList(
                unary.extract(x -> new SemanticsBoundToExpression<>(module.get(UnaryPrefixExpressionSemantics.class), x)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ExpressionCompilationResult compile(Maybe<Matches> input, StatementCompilationOutputAcceptor acceptor) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                //noinspection rawtypes
                return traversed.get().getSemantics().compile((Maybe) traversed.get().getInput(), acceptor);
            }
        }
        final Maybe<UnaryPrefix> inputExpr = input.__(Matches::getUnaryExpr);
        final Maybe<LValueExpression> pattern = input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        final ExpressionCompilationResult compiledInputExpr = module.get(UnaryPrefixExpressionSemantics.class).compile(
                inputExpr,
                acceptor
        );
        final Optional<HandlerWhenExpressionContext> handlerHeaderContext = module.get(ContextManager.class)
                .currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst();
        final List<String> inputExprPropertyChain = compiledInputExpr.getPropertyChain();//TODO it might be an empty list, check (and check also in other usages)
        if (handlerHeaderContext.isPresent()) {
            //We are in a handler header, probably in a when-expression
            final IJadescriptType upperBound;
            if(inputExprPropertyChain.isEmpty()){
                upperBound = module.get(TypeHelper.class).ANY;
            }else {
                upperBound = handlerHeaderContext.get()
                        .computeUpperBoundForPropertyChain(inputExprPropertyChain);
            }
            final PatternMatchOutput<
                    ? extends PatternMatchSemanticsProcess.IsCompilation,
                    PatternMatchOutput.DoesUnification,
                    PatternMatchOutput.WithTypeNarrowing> output =
                    module.get(PatternMatchHelper.class).compileHeaderPatternMatching(
                            upperBound,
                            compiledInputExpr.toString(),//TODO check that the input expr is not compiled multiple times...
                            pattern,
                            acceptor
                    );

            //TODO Handle unification...

            ExpressionCompilationResult result = result(output.getProcessInfo()
                    .operationInvocationText(compiledInputExpr.toString()));
            if (!inputExprPropertyChain.isEmpty()) {
                result = result
                        .updateFTKB(kb -> kb.add(
                                FlowTypeInferringTerm.of(output.getTypeNarrowingInfo().getNarrowedType()),
                                inputExprPropertyChain
                        ));
            }
            return result;
        } else {
            final PatternMatchOutput<
                    ? extends PatternMatchSemanticsProcess.IsCompilation,
                    PatternMatchOutput.NoUnification,
                    PatternMatchOutput.WithTypeNarrowing> output =
                    module.get(PatternMatchHelper.class).compileMatchesExpressionPatternMatching(
                            inputExpr,
                            pattern,
                            acceptor
                    );

            ExpressionCompilationResult result = result(output.getProcessInfo()
                    .operationInvocationText(compiledInputExpr.toString()));
            if (!inputExprPropertyChain.isEmpty()) {
                result = result
                        .updateFTKB(kb -> kb.add(
                                FlowTypeInferringTerm.of(output.getTypeNarrowingInfo().getNarrowedType()),
                                inputExprPropertyChain
                        ));
            }
            return result;

        }
    }

    private boolean isInHandlerWhenExpression() {
        return module.get(ContextManager.class).currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst().isPresent();
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public IJadescriptType inferType(Maybe<Matches> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return traversed.get().getSemantics().inferType((Maybe) traversed.get().getInput());
            }
        }

        return module.get(TypeHelper.class).BOOLEAN;
    }

    @Override
    public boolean mustTraverse(Maybe<Matches> input) {
        final Maybe<Pattern> pattern = input.__(Matches::getPattern);
        final boolean isMatches = input.__(Matches::isMatches).extract(nullAsFalse);
        return !isMatches || pattern.isNothing();
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<Matches> input) {
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        return Optional.of(new SemanticsBoundToExpression<>(module.get(UnaryPrefixExpressionSemantics.class), unary));
    }

    @Override
    public boolean isPatternEvaluationPure(Maybe<Matches> input) {
        if (mustTraverse(input)) {
            return module.get(UnaryPrefixExpressionSemantics.class).isPatternEvaluationPure(
                    input.__(Matches::getUnaryExpr)
            );
        }else{
            return true;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void validate(Maybe<Matches> input, ValidationMessageAcceptor acceptor) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);

            if (traversed.isPresent()) {
                traversed.get().getSemantics().validate((Maybe) traversed.get().getInput(), acceptor);
                return;
            }
        }

        final Maybe<LValueExpression> pattern = input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        final Maybe<UnaryPrefix> inputExpr = input.__(Matches::getUnaryExpr);

        final Optional<HandlerWhenExpressionContext> handlerHeaderContext = module.get(ContextManager.class)
                .currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst();
        if (handlerHeaderContext.isPresent()) {
            //We are in a handler header, probably in a when-expression
            final List<String> propertyChain = module.get(UnaryPrefixExpressionSemantics.class)
                    .extractPropertyChain(inputExpr);
            final IJadescriptType upperBound = handlerHeaderContext.get()
                    .computeUpperBoundForPropertyChain(propertyChain);
            final PatternMatchOutput<
                    ? extends PatternMatchSemanticsProcess.IsValidation,
                    PatternMatchOutput.DoesUnification,
                    PatternMatchOutput.WithTypeNarrowing> output =
                    module.get(PatternMatchHelper.class).validateHeaderPatternMatching(
                            upperBound,
                            "__",
                            pattern,
                            acceptor
                    );
            //TODO handle unified variables
            //TODO handle narrowing


        } else {
            final PatternMatchOutput<
                    ? extends PatternMatchSemanticsProcess.IsValidation,
                    PatternMatchOutput.NoUnification,
                    PatternMatchOutput.WithTypeNarrowing> output =
                    module.get(PatternMatchHelper.class).validateMatchesExpressionPatternMatching(
                            inputExpr,
                            pattern,
                            acceptor
                    );

            //TODO handle narrowing

        }
    }



    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<Matches, ?, ?> input, StatementCompilationOutputAcceptor acceptor) {
        final Maybe<Matches> pattern = input.getPattern();
        if (mustTraverse(pattern)) {
            return module.get(UnaryPrefixExpressionSemantics.class).compilePatternMatchInternal(
                    input.mapPattern(Matches::getUnaryExpr),
                    acceptor
            );
        } else {
            return input.createEmptyCompileOutput();
        }
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<Matches> input) {
        if (mustTraverse(input)) {
            return module.get(UnaryPrefixExpressionSemantics.class).inferPatternTypeInternal(
                    input.__(Matches::getUnaryExpr)
            );
        } else {
            return PatternType.empty(module);
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<Matches, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<Matches> pattern = input.getPattern();
        if (mustTraverse(pattern)) {
            return module.get(UnaryPrefixExpressionSemantics.class).validatePatternMatchInternal(
                    input.mapPattern(Matches::getUnaryExpr),
                    acceptor
            );
        } else {
            return input.createEmptyValidationOutput();
        }
    }

}
