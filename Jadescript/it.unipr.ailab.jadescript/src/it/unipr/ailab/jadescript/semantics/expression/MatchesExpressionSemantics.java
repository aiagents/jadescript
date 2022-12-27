package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.DroppingAcceptor;
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
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<Matches> input) {
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        return Stream.of(unary.<SemanticsBoundToExpression<?>>extract(x -> new SemanticsBoundToExpression<>(
                module.get(UnaryPrefixExpressionSemantics.class),
                x
        )));
        //TODO should include patterns?
        //TODO should include whole subpatterns (since they are likely evaluated as expressions)?
    }

    @Override
    protected String compileInternal(Maybe<Matches> input, CompilationOutputAcceptor acceptor) {
        final Maybe<UnaryPrefix> inputExpr = input.__(Matches::getUnaryExpr);
        final Maybe<LValueExpression> pattern = input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        final String compiledInputExpr = module.get(UnaryPrefixExpressionSemantics.class).compile(
                inputExpr,
                acceptor
        );
        final Optional<HandlerWhenExpressionContext> handlerHeaderContext = module.get(ContextManager.class)
                .currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst();
        final List<String> inputExprPropertyChain = module.get(UnaryPrefixExpressionSemantics.class)
                .propertyChain(inputExpr);
        if (handlerHeaderContext.isPresent()) {
            //We are in a handler header, probably in a when-expression
            final IJadescriptType upperBound = handlerHeaderContext.get().upperBoundForInterestedChain(
                    inputExprPropertyChain
            ).orElse(module.get(TypeHelper.class).ANY);

            final PatternMatchOutput<
                    ? extends PatternMatchSemanticsProcess.IsCompilation,
                    PatternMatchOutput.DoesUnification,
                    PatternMatchOutput.WithTypeNarrowing> output =
                    module.get(PatternMatchHelper.class).compileHeaderPatternMatching(
                            upperBound,
                            compiledInputExpr,//TODO check that the input expr is not compiled multiple times...
                            pattern,
                            acceptor
                    );

            //TODO Handle unification...

            return output.getProcessInfo().operationInvocationText(compiledInputExpr);
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

            return output.getProcessInfo()
                    .operationInvocationText(compiledInputExpr);

        }
    }

    private boolean isInHandlerWhenExpression() {
        return module.get(ContextManager.class).currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst().isPresent();
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<Matches> input) {
        return Collections.emptyList();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<Matches> input) {
        final Maybe<LValueExpression> pattern = input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        ExpressionTypeKB subKb = module.get(UnaryPrefixExpressionSemantics.class).computeKB(unary);
        List<String> strings = module.get(UnaryPrefixExpressionSemantics.class).propertyChain(unary);

        IJadescriptType narrowedType = module.get(PatternMatchHelper.class)
                .inferMatchesExpressionPatternType(pattern, unary);

        pattern.safeDo(patternSafe -> {
            subKb.add(FlowTypeInferringTerm.of(narrowedType), strings);
        });
        return subKb;
    }


    @Override
    protected IJadescriptType inferTypeInternal(Maybe<Matches> input) {
        return module.get(TypeHelper.class).BOOLEAN;
    }

    @Override
    protected boolean mustTraverse(Maybe<Matches> input) {
        final Maybe<Pattern> pattern = input.__(Matches::getPattern);
        final boolean isMatches = input.__(Matches::isMatches).extract(nullAsFalse);
        return !isMatches || pattern.isNothing();
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<Matches> input) {
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        return Optional.of(new SemanticsBoundToExpression<>(module.get(UnaryPrefixExpressionSemantics.class), unary));
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<Matches> input) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return true;
    }

    @Override
    protected boolean validateInternal(Maybe<Matches> input, ValidationMessageAcceptor acceptor) {
        final Maybe<UnaryPrefix> inputExpr = input.__(Matches::getUnaryExpr);
        final Maybe<LValueExpression> pattern = input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        final boolean validatedInputExpr = module.get(UnaryPrefixExpressionSemantics.class)
                .validate(inputExpr, acceptor);

        if (validatedInputExpr == INVALID) {
            return INVALID;
        }

        final Optional<HandlerWhenExpressionContext> handlerHeaderContext = module.get(ContextManager.class)
                .currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst();
        final List<String> inputExprPropertyChain = module.get(UnaryPrefixExpressionSemantics.class)
                .propertyChain(inputExpr);
        if (handlerHeaderContext.isPresent()) {
            //We are in a handler header, probably in a when-expression

            final IJadescriptType upperBound = handlerHeaderContext.get().upperBoundForInterestedChain(
                    inputExprPropertyChain
            ).orElse(module.get(TypeHelper.class).ANY);

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

            //TODO use output
            return VALID;

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

            //TODO use output
            return VALID;
        }
    }


    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<Matches, ?, ?> input, CompilationOutputAcceptor acceptor) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<Matches> input) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return PatternType.empty(module);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<Matches, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return input.createEmptyValidationOutput();
    }


    @Override
    protected boolean isAlwaysPureInternal(Maybe<Matches> input) {
        final Maybe<LValueExpression> pattern = input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        return module.get(LValueExpressionSemantics.class).isPatternEvaluationPure(pattern);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<Matches> input) {
        return false;
    }

    @Override
    protected boolean isHoledInternal(Maybe<Matches> input) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return false;
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<Matches> input) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return false;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<Matches> input) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<Matches> input) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return false;
    }
}
