package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c2feature.HandlerWhenExpressionContext;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.FlowTypeInferringTerm;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
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
    protected String compileInternal(Maybe<Matches> input, StaticState state, CompilationOutputAcceptor acceptor) {
        final Maybe<UnaryPrefix> inputExpr = input.__(Matches::getUnaryExpr);
        final Maybe<LValueExpression> pattern = input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        final String compiledInputExpr = module.get(UnaryPrefixExpressionSemantics.class).compile(
                inputExpr, ,
            acceptor
        );
        final Optional<HandlerWhenExpressionContext> handlerHeaderContext = module.get(ContextManager.class)
                .currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst();
        final List<String> inputExprPropertyChain = module.get(UnaryPrefixExpressionSemantics.class)
                .describeExpression(inputExpr, );
        if (handlerHeaderContext.isPresent()) {
            //We are in a handler header, probably in a when-expression
            //TODO try to understand if this "upperBoundForInterestedChain" is acceptable:
            //  maybe it is not necessary, as the type of the message and of the content might be initialized with
            //   the upper bound types at this point (and this might be enough)
            final IJadescriptType upperBound = handlerHeaderContext.get().upperBoundForInterestedChain(
                    inputExprPropertyChain
            ).orElse(module.get(TypeHelper.class).ANY);

            final PatternMatcher output = module.get(PatternMatchHelper.class).compileHeaderPatternMatching(
                    upperBound,//TODO why only upper bound here?
                    pattern,
                    acceptor
            );

            //NOTE: Unified variables are handled by the event handler semantics

            return output.operationInvocationText(compiledInputExpr);
        } else {
            final PatternMatcher output =
                    module.get(PatternMatchHelper.class).compileMatchesExpressionPatternMatching(
                            inputExpr,
                            pattern,
                            acceptor
                    );

            return output.operationInvocationText(compiledInputExpr);

        }
    }

    private boolean isInHandlerWhenExpression() {
        return module.get(ContextManager.class).currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst().isPresent();
    }

    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(Maybe<Matches> input, StaticState state) {
        return Collections.emptyList();
    }

    @Override
    protected StaticState advanceInternal(Maybe<Matches> input,
                                          StaticState state) {
        final Maybe<LValueExpression> pattern = input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        ExpressionTypeKB subKb = module.get(UnaryPrefixExpressionSemantics.class).advance(unary, );
        List<String> strings = module.get(UnaryPrefixExpressionSemantics.class).describeExpression(unary, );

        IJadescriptType narrowedType = module.get(PatternMatchHelper.class)
                .inferMatchesExpressionPatternType(pattern, unary);

        pattern.safeDo(patternSafe -> {
            subKb.add(FlowTypeInferringTerm.of(narrowedType), strings);
        });
        return subKb;
    }


    @Override
    protected IJadescriptType inferTypeInternal(Maybe<Matches> input,
                                                StaticState state) {
        return module.get(TypeHelper.class).BOOLEAN;
    }

    @Override
    protected boolean mustTraverse(Maybe<Matches> input) {
        final Maybe<Pattern> pattern = input.__(Matches::getPattern);
        final boolean isMatches = input.__(Matches::isMatches).extract(nullAsFalse);
        return !isMatches || pattern.isNothing();
    }

    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(Maybe<Matches> input) {
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        return Optional.of(new SemanticsBoundToExpression<>(module.get(UnaryPrefixExpressionSemantics.class), unary));
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<Matches> input,
        StaticState state) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return true;
    }

    @Override
    protected boolean validateInternal(Maybe<Matches> input, StaticState state, ValidationMessageAcceptor acceptor) {
        final Maybe<UnaryPrefix> inputExpr = input.__(Matches::getUnaryExpr);
        final Maybe<LValueExpression> pattern = input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        final boolean validatedInputExpr = module.get(UnaryPrefixExpressionSemantics.class)
                .validate(inputExpr, , acceptor);

        if (validatedInputExpr == INVALID) {
            return INVALID;
        }

        final Optional<HandlerWhenExpressionContext> handlerHeaderContext = module.get(ContextManager.class)
                .currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst();
        final List<String> inputExprPropertyChain = module.get(UnaryPrefixExpressionSemantics.class)
                .describeExpression(inputExpr, );
        if (handlerHeaderContext.isPresent()) {
            //We are in a handler header, probably in a when-expression

            final IJadescriptType upperBound = handlerHeaderContext.get().upperBoundForInterestedChain(
                    inputExprPropertyChain
            ).orElse(module.get(TypeHelper.class).ANY);

            final boolean patternValidation = module.get(PatternMatchHelper.class).validateHeaderPatternMatching(
                    upperBound,
                    pattern,
                    acceptor
            );
            //TODO handle unified variables

            return patternValidation;

        } else {
            return module.get(PatternMatchHelper.class).validateMatchesExpressionPatternMatching(
                    inputExpr,
                    pattern,
                    acceptor
            );
        }
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(PatternMatchInput<Matches> input, StaticState state, CompilationOutputAcceptor acceptor) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<Matches> input,
                                                StaticState state) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return PatternType.empty(module);
    }

    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<Matches> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return VALID;
    }


    @Override
    protected boolean isAlwaysPureInternal(Maybe<Matches> input,
                                           StaticState state) {
        final Maybe<LValueExpression> pattern = input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        return module.get(LValueExpressionSemantics.class).isPatternEvaluationPure(pattern, );
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<Matches> input) {
        return false;
    }

    @Override
    protected boolean isHoledInternal(Maybe<Matches> input, StaticState state) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return false;
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<Matches> input,
                                            StaticState state) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return false;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<Matches> input, StaticState state) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<Matches> input) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return false;
    }
}
