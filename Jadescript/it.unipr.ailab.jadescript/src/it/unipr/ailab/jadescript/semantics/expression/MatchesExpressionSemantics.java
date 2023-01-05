package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.LValueExpression;
import it.unipr.ailab.jadescript.jadescript.Matches;
import it.unipr.ailab.jadescript.jadescript.Pattern;
import it.unipr.ailab.jadescript.jadescript.UnaryPrefix;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c2feature.HandlerWhenExpressionContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.FlowTypingRule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.FlowTypingRuleCondition;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.HandlerHeader;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.MatchesExpression;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.PatternMatchHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 2019-08-18.
 */
@Singleton
public class MatchesExpressionSemantics
    extends ExpressionSemantics<Matches> {


    public MatchesExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<Matches> input
    ) {
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        return Stream.of(unary.<SemanticsBoundToExpression<?>>extract(x ->
            new SemanticsBoundToExpression<>(
                module.get(UnaryPrefixExpressionSemantics.class),
                x
            )));
        //TODO should include patterns?
        //TODO should include whole subpatterns (since they are likely
        // evaluated as expressions)?
    }


    @Override
    protected String compileInternal(
        Maybe<Matches> input, StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        final Maybe<UnaryPrefix> inputExpr = input.__(Matches::getUnaryExpr);
        final Maybe<LValueExpression> pattern =
            input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        final UnaryPrefixExpressionSemantics upes =
            module.get(UnaryPrefixExpressionSemantics.class);
        final String compiledInputExpr = upes.compile(
            inputExpr,
            state,
            acceptor
        );
        final StaticState afterLeft = upes.advance(inputExpr, state);
        final Optional<HandlerWhenExpressionContext> handlerHeaderContext =
            module.get(ContextManager.class)
                .currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst();

        final IJadescriptType inputExprType = upes.inferType(inputExpr, state);
        final PatternMatcher output;
        if (handlerHeaderContext.isPresent()) {
            //We are in a handler header, probably in a when-expression
            output = module.get(PatternMatchHelper.class)
                .compileHeaderPatternMatching(
                    inputExprType,
                    pattern,
                    afterLeft,
                    acceptor
                );
        } else {
            output = module.get(PatternMatchHelper.class)
                .compileMatchesExpressionPatternMatching(
                    inputExprType,
                    pattern,
                    afterLeft,
                    acceptor
                );
        }
        return output.operationInvocationText(compiledInputExpr);
    }


    private boolean isInHandlerWhenExpression() {
        return module.get(ContextManager.class).currentContext()
            .actAs(HandlerWhenExpressionContext.class)
            .findFirst().isPresent();
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<Matches> input,
        StaticState state
    ) {

        final Maybe<UnaryPrefix> inputExpr = input.__(Matches::getUnaryExpr);
        final Maybe<LValueExpression> pattern = input.__(Matches::getPattern)
            .__(i -> (LValueExpression) i);
        final Maybe<ExpressionDescriptor> inputExprDesc =
            module.get(UnaryPrefixExpressionSemantics.class)
                .describeExpression(inputExpr, state);
        if (inputExprDesc.isPresent()) {
            return some(new ExpressionDescriptor.MatchesDescriptor(
                inputExprDesc.toNullable(),
                pattern
            ));
        }
        return nothing();

    }


    @Override
    protected StaticState advanceInternal(
        Maybe<Matches> input,
        StaticState state
    ) {
        final Maybe<ExpressionDescriptor> descriptor =
            describeExpression(input, state);
        if (descriptor.isPresent()) {
            return advanceCommon(input, state, true).addRule(
                descriptor.toNullable(),
                new FlowTypingRule(
                    FlowTypingRuleCondition.ReturnedTrue.INSTANCE,
                    s -> advanceCommon(input, s, false)
                )
            );
        } else {
            return advanceCommon(input, state, true);
        }
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<Matches> input,
        StaticState state
    ) {
        return state;
    }


    private StaticState advanceCommon(
        Maybe<Matches> input,
        StaticState state,
        boolean addTypeCheckRule
    ) {
        final Maybe<UnaryPrefix> inputExpr = input.__(Matches::getUnaryExpr);
        final Maybe<LValueExpression> pattern =
            input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        final UnaryPrefixExpressionSemantics upes =
            module.get(UnaryPrefixExpressionSemantics.class);
        final StaticState afterLeft = upes.advance(inputExpr, state);
        final Optional<HandlerWhenExpressionContext> handlerHeaderContext =
            module.get(ContextManager.class)
                .currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst();

        final IJadescriptType inputExprType = upes.inferType(inputExpr, state);

        StaticState result;
        final PatternMatchHelper patternMatchHelper = module.get(
            PatternMatchHelper.class);
        if (handlerHeaderContext.isPresent()) {
            //We are in a handler header, probably in a when-expression
            result = patternMatchHelper.advanceHeaderPatternMatching(
                inputExprType,
                pattern,
                afterLeft
            );
        } else {
            result = patternMatchHelper.advanceMatchesExpressionPatternMatching(
                inputExprType,
                pattern,
                afterLeft
            );
        }

        if (addTypeCheckRule) {
            final Maybe<ExpressionDescriptor> inputExprDescriptor =
                upes.describeExpression(inputExpr, state);
            final Maybe<ExpressionDescriptor> overallDescriptor =
                describeExpression(input, state);
            if (overallDescriptor.isPresent()
                && inputExprDescriptor.isPresent()) {

                final IJadescriptType solvedPatternType;
                if (handlerHeaderContext.isPresent()) {
                    solvedPatternType = patternMatchHelper
                        .inferHandlerHeaderPatternType(
                            inputExprType, pattern,
                            afterLeft
                        );
                } else {
                    solvedPatternType = patternMatchHelper
                        .inferMatchesExpressionPatternType(
                            inputExprType,
                            pattern,
                            afterLeft
                        );

                }

                result = result.addRule(
                    overallDescriptor.toNullable(),
                    new FlowTypingRule(
                        FlowTypingRuleCondition.ReturnedTrue.INSTANCE,
                        s -> s.assertFlowTypingUpperBound(
                            inputExprDescriptor.toNullable(),
                            solvedPatternType
                        )
                    )
                );
            }
        }
        return result;

    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<Matches> input,
        StaticState state
    ) {
        return module.get(TypeHelper.class).BOOLEAN;
    }


    @Override
    protected boolean mustTraverse(Maybe<Matches> input) {
        final Maybe<Pattern> pattern = input.__(Matches::getPattern);
        final boolean isMatches =
            input.__(Matches::isMatches).extract(nullAsFalse);
        return !isMatches || pattern.isNothing();
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(
        Maybe<Matches> input
    ) {
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        return Optional.of(new SemanticsBoundToExpression<>(
            module.get(UnaryPrefixExpressionSemantics.class), unary
        ));
    }


    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<Matches> input,
        StaticState state
    ) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return true;
    }


    @Override
    protected boolean validateInternal(
        Maybe<Matches> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final Maybe<UnaryPrefix> inputExpr = input.__(Matches::getUnaryExpr);
        final Maybe<LValueExpression> pattern =
            input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        final UnaryPrefixExpressionSemantics upes =
            module.get(UnaryPrefixExpressionSemantics.class);
        final boolean validatedInputExpr =
            upes.validate(inputExpr, state, acceptor);
        if (validatedInputExpr == INVALID) {
            return INVALID;
        }

        final IJadescriptType inputExprType = upes.inferType(inputExpr, state);

        StaticState afterInputExpr = upes.advance(inputExpr, state);

        final Optional<HandlerWhenExpressionContext> handlerHeaderContext =
            module.get(ContextManager.class)
                .currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst();
        if (handlerHeaderContext.isPresent()) {
            //We are in a handler header, probably in a when-expression
            return module.get(PatternMatchHelper.class)
                .validateHeaderPatternMatching(
                    inputExprType,
                    pattern,
                    afterInputExpr,
                    acceptor
                );

        } else {
            return module.get(PatternMatchHelper.class)
                .validateMatchesExpressionPatternMatching(
                    inputExprType,
                    pattern,
                    afterInputExpr,
                    acceptor
                );
        }
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<Matches> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<Matches> input,
        StaticState state
    ) {
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
    protected boolean isAlwaysPureInternal(
        Maybe<Matches> input,
        StaticState state
    ) {

        final Maybe<UnaryPrefix> inputExpr =
            input.__(Matches::getUnaryExpr);

        final UnaryPrefixExpressionSemantics upes =
            module.get(UnaryPrefixExpressionSemantics.class);
        if (!upes.isAlwaysPure(inputExpr, state)) {
            return false;
        }

        final Maybe<LValueExpression> pattern =
            input.__(Matches::getPattern).__(i -> (LValueExpression) i);

        final IJadescriptType inputExprType = upes.inferType(inputExpr, state);

        final StaticState afterInputExpr = upes.advance(inputExpr, state);

        final Optional<HandlerWhenExpressionContext> handlerHeaderContext =
            module.get(ContextManager.class)
                .currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst();
        String localClassName =
            "__PatternMatcher" + Util.extractEObject(pattern).hashCode();
        final String variableName = localClassName + "_obj";
        final PatternMatchInput<LValueExpression> pmi;
        if (handlerHeaderContext.isPresent()) {
            pmi = new HandlerHeader<>(
                module,
                inputExprType,
                pattern,
                "__",
                variableName
            );
        } else {
            pmi = new MatchesExpression<>(
                module,
                inputExprType,
                pattern,
                "__",
                variableName
            );
        }
        return module.get(LValueExpressionSemantics.class)
            .isPatternEvaluationPure(pmi, afterInputExpr);
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
    protected boolean isTypelyHoledInternal(
        Maybe<Matches> input,
        StaticState state
    ) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<Matches> input,
        StaticState state
    ) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<Matches> input) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return false;
    }

}
