package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.InvokeExpression;
import it.unipr.ailab.jadescript.jadescript.Literal;
import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.c2feature.MessageReceivedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnBehaviourFailureHandledContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnExceptionHandledContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.PerceptPerceivedContext;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.TupledExpressions;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.VirtualIdentifier;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

/**
 * Created on 28/12/16.
 */
@Singleton
public class PrimaryExpressionSemantics extends AssignableExpressionSemantics<Primary> {

    public PrimaryExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<Primary> input) {
        final List<Maybe<RValueExpression>> exprs =
            Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        if (exprs.size() == 1) {
            return Stream.of(exprs.get(0).<SemanticsBoundToExpression<?>>extract(
                x -> new SemanticsBoundToExpression<>(
                    module.get(RValueExpressionSemantics.class),
                    x
                )));
        }
        return Stream.empty();
    }

    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(Maybe<Primary> input, StaticState state) {
        final Maybe<String> agent = input.__(Primary::getAgent);
        final Maybe<String> message = input.__(Primary::getMessage);
        final Maybe<String> percept = input.__(Primary::getPercept);
        final Maybe<String> exception = input.__(Primary::getException);
        final Maybe<String> behaviour = input.__(Primary::getBehaviour);
        final List<Maybe<RValueExpression>> exprs =
            Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;

        if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
        } else {
            parenthesizedExpression = nothing();
        }

        if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).describeExpression(parenthesizedExpression, );
        } else if (agent.isPresent()) {
            return List.of("agent");
        } else if (message.isPresent()) {
            return List.of("message");
        } else if (percept.isPresent()) {
            return List.of("percept");
        } else if (exception.isPresent()) {
            return List.of("exception");
        } else if (behaviour.isPresent()) {
            return List.of("behaviour");
        } else {
            return List.of();
        }
    }

    @Override
    protected StaticState advanceInternal(
        Maybe<Primary> input,
        StaticState state
    ) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected String compileInternal(
        Maybe<Primary> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {

        final List<Maybe<RValueExpression>> exprs =
            Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;

        if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
        } else {
            parenthesizedExpression = nothing();
        }

        final Maybe<String> agent = input.__(Primary::getAgent);
        final Maybe<String> message = input.__(Primary::getMessage);
        final Maybe<String> percept = input.__(Primary::getPercept);
        final Maybe<String> exception = input.__(Primary::getException);
        final Maybe<String> behaviour = input.__(Primary::getBehaviour);

        if (parenthesizedExpression.isPresent()) {
            final String result =
                module.get(RValueExpressionSemantics.class).compile(
                    parenthesizedExpression, ,
                    acceptor
                );
            return "(" + result + ")";
        } else if (agent.isPresent()) {
            return THE_AGENT + "()";
        } else if (message.isPresent()) {
            final IJadescriptType messageType = inferType(input, );
            if (messageType.isErroneous()) {
                return MESSAGE_VAR_NAME;
            } else {
                return "(" + messageType.compileAsJavaCast() + " " + MESSAGE_VAR_NAME + ")";

            }
        } else if (exception.isPresent()) {
            final IJadescriptType reasonType = inferType(input, );
            if (reasonType.isErroneous()) {
                return EXCEPTION_REASON_VAR_NAME;
            } else {
                return "(" + reasonType.compileAsJavaCast() + " " + EXCEPTION_REASON_VAR_NAME + ")";
            }
        } else if (behaviour.isPresent()) {
            final IJadescriptType behaviourType = inferType(input, );
            if (behaviourType.isErroneous()) {
                return FAILED_BEHAVIOUR_VAR_NAME;
            } else {
                return "(" + behaviourType.compileAsJavaCast() + " " + FAILED_BEHAVIOUR_VAR_NAME + ")";

            }
        } else if (percept.isPresent()) {
            return module.get(ContextManager.class).currentContext()
                .actAs(PerceptPerceivedContext.class)
                .findFirst()
                .flatMap(ppc ->
                    ppc.getPerceptContentStream(n -> n.equals(PERCEPT_CONTENT_VAR_NAME), null, null)
                        .findAny()
                )
                .map(ns -> ns.compileRead(""))
                .orElse(PERCEPT_CONTENT_VAR_NAME);
        } else return "";
    }

    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<Primary> input,
        StaticState state
    ) {
        if (input == null) {
            return module.get(TypeHelper.class).ANY;
        }
        final List<Maybe<RValueExpression>> exprs =
            Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
        } else {
            parenthesizedExpression = nothing();
        }
        final Maybe<String> agent = input.__(Primary::getAgent);
        final Maybe<String> message = input.__(Primary::getMessage);
        final Maybe<String> percept = input.__(Primary::getPercept);
        final Maybe<String> exception = input.__(Primary::getException);
        final Maybe<String> behaviour = input.__(Primary::getBehaviour);

        if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).inferType(parenthesizedExpression, );
        } else if (agent.isPresent()) {
            return module.get(ContextManager.class).currentContext()
                .searchAs(
                    AgentAssociated.class,
                    a -> a.computeAllAgentAssociations()
                        .sorted()
                        .map(AgentAssociation::getAgent)
                )
                .findFirst()
                .orElseGet(() -> module.get(TypeHelper.class).AGENT);
        } else if (message.isPresent()) {
            return module.get(ContextManager.class).currentContext()
                .actAs(MessageReceivedContext.class)
                .findFirst()
                .map(MessageReceivedContext::getMessageType)
                .orElseGet(() -> module.get(TypeHelper.class).MESSAGE.apply(
                    Collections.singletonList(module.get(TypeHelper.class).SERIALIZABLE))
                );
        } else if (exception.isPresent()) {
            return module.get(ContextManager.class).currentContext()
                .actAs(OnExceptionHandledContext.class)
                .findFirst()
                .map(OnExceptionHandledContext::getExceptionReasonType)
                .orElseGet(() -> module.get(TypeHelper.class).PREDICATE);
        } else if (behaviour.isPresent()) {
            return module.get(ContextManager.class).currentContext()
                .actAs(OnBehaviourFailureHandledContext.class)
                .findFirst()
                .map(OnBehaviourFailureHandledContext::getFailedBehaviourType)
                .orElseGet(() -> module.get(TypeHelper.class).ANYBEHAVIOUR);
        } else if (percept.isPresent()) {
            return module.get(ContextManager.class).currentContext()
                .actAs(PerceptPerceivedContext.class)
                .findFirst()
                .map(PerceptPerceivedContext::getPerceptContentType)
                .orElseGet(() -> module.get(TypeHelper.class).PREDICATE);
        } else return module.get(TypeHelper.class).ANY;
    }


    @Override
    protected boolean mustTraverse(Maybe<Primary> input) {
        final boolean isPlaceholder =
            input.__(Primary::isPlaceholder).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> exprs =
            Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<InvokeExpression> invoke =
            input.__(Primary::getInvokeExpression);
        return exprs.size() > 1// tuple
            || isPlaceholder
            || literal.isPresent()
            || identifier.isPresent()
            || invoke.isPresent();
    }

    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(Maybe<Primary> input) {
        final boolean isPlaceholder =
            input.__(Primary::isPlaceholder).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> exprs =
            Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<TupledExpressions> tuple;

        if (exprs.size() > 1) {
            tuple = TupledExpressions.tupledExpressions(input);
        } else {
            tuple = nothing();
        }
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<InvokeExpression> invoke =
            input.__(Primary::getInvokeExpression);
        if (mustTraverse(input)) {
            if (isPlaceholder) {
                return Optional.of(new SemanticsBoundToExpression<>(
                    module.get(PlaceholderExpressionSemantics.class),
                    input
                ));
            } else if (tuple.isPresent()) {
                return Optional.of(new SemanticsBoundToExpression<>(
                    module.get(TupleExpressionSemantics.class),
                    tuple
                ));
            } else if (literal.isPresent()) {
                return Optional.of(new SemanticsBoundToExpression<>(
                    module.get(LiteralExpressionSemantics.class),
                    literal
                ));
            } else if (identifier.isPresent()) {
                return Optional.of(new SemanticsBoundToExpression<>(
                    module.get(SingleIdentifierExpressionSemantics.class),
                    VirtualIdentifier.virtualIdentifier(identifier, input)
                ));
            } else if (invoke.isPresent()) {
                return Optional.of(new SemanticsBoundToExpression<>(
                    module.get(InvokeExpressionSemantics.class),
                    invoke
                ));
            }
        }
        return Optional.empty();
    }


    @Override
    protected boolean validateInternal(
        Maybe<Primary> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return VALID;
        final List<Maybe<RValueExpression>> exprs =
            Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;

        if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
        } else {
            parenthesizedExpression = nothing();
        }
        final Maybe<String> agent = input.__(Primary::getAgent);
        final Maybe<String> message = input.__(Primary::getMessage);
        final Maybe<String> percept = input.__(Primary::getPercept);
        final Maybe<String> exception = input.__(Primary::getException);
        final Maybe<String> behaviour = input.__(Primary::getBehaviour);


        if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).validate(parenthesizedExpression, , acceptor);
        } else if (agent.isPresent()) {
            return module.get(ValidationHelper.class)
                .assertCanUseAgentReference(input, acceptor);
        } else if (message.isPresent()) {
            return module.get(ValidationHelper.class).assertion(
                module.get(ContextManager.class).currentContext()
                    .actAs(MessageReceivedContext.class)
                    .findFirst()
                    .isPresent(),
                "MissingMessageReference",
                "Reference to 'message' not available in this context.",
                input,
                acceptor
            );
        } else if (exception.isPresent()) {
            return module.get(ValidationHelper.class).assertion(
                module.get(ContextManager.class).currentContext()
                    .actAs(OnExceptionHandledContext.class)
                    .findFirst()
                    .isPresent(),
                "MissingExceptionReference",
                "Reference to 'exception' not available in this context.",
                input,
                acceptor
            );
        } else if (percept.isPresent()) {
            return module.get(ValidationHelper.class).assertion(
                module.get(ContextManager.class).currentContext()
                    .actAs(PerceptPerceivedContext.class)
                    .findFirst()
                    .isPresent(),
                "MissingPerceptReference",
                "Reference to 'percept' not available in this context.",
                input,
                acceptor
            );
        } else if (behaviour.isPresent()) {
            return module.get(ValidationHelper.class).assertion(
                module.get(ContextManager.class).currentContext()
                    .actAs(OnBehaviourFailureHandledContext.class)
                    .findFirst()
                    .isPresent(),
                "MissingPerceptReference",
                "Reference to 'behaviour' not available in this context.",
                input,
                acceptor
            );
        }

        return VALID;
    }

    @Override
    public void compileAssignmentInternal(
        Maybe<Primary> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        // parenthesized expressions/agent/message/module/behaviour/exception
        // /percept references cannot be assigned
    }

    @Override
    public boolean validateAssignmentInternal(
        Maybe<Primary> input,
        Maybe<RValueExpression> expression,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        // parenthesized expressions/agent/message/module/behaviour/exception
        // /percept references cannot be assigned
        return errorNotLvalue(input, acceptor);
    }

    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<Primary> input,
        ValidationMessageAcceptor acceptor
    ) {
        // parenthesized expressions/agent/message/module/behaviour/exception
        // /percept references cannot be assigned
        return errorNotLvalue(input, acceptor);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<Primary> input) {
        // parenthesized expressions/agent/message/module/behaviour/exception
        // /percept references cannot be assigned
        return false;
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<Primary> input,
        StaticState state) {
        return subPatternEvaluationsAllPure(input, state);
    }

    public boolean syntacticValidateStatement(
        Maybe<Primary> input,
        ValidationMessageAcceptor acceptor
    ) {
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<InvokeExpression> invoke =
            input.__(Primary::getInvokeExpression);
        if (invoke.isPresent()) {
            return VALID;
        } else if (identifier.isPresent()) {
            return module.get(SingleIdentifierExpressionSemantics.class)
                .syntacticValidateStatement(
                    VirtualIdentifier.virtualIdentifier(identifier, input),
                    acceptor
                );
        } else {
            return errorNotStatement(input, acceptor);
        }
    }

    @Override
    protected boolean isHoledInternal(Maybe<Primary> input, StaticState state) {
        return subExpressionsAnyHoled(input, );
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<Primary> input,
                                            StaticState state) {
        return subExpressionsAnyTypelyHoled(input, );
    }

    @Override
    protected boolean isUnboundInternal(Maybe<Primary> input, StaticState state) {
        return subExpressionsAnyUnbound(input, );
    }

    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<Primary> input,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        final List<Maybe<RValueExpression>> exprs =
            Maybe.toListOfMaybes(input.getPattern()
                    .__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
        } else {
            parenthesizedExpression = nothing();
        }
        final Maybe<String> agent = input.getPattern().__(Primary::getAgent);
        final Maybe<String> message =
            input.getPattern().__(Primary::getMessage);
        final Maybe<String> percept =
            input.getPattern().__(Primary::getPercept);
        final Maybe<String> exception =
            input.getPattern().__(Primary::getException);
        final Maybe<String> behaviour =
            input.getPattern().__(Primary::getBehaviour);
        if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).compilePatternMatchInternal(
                input.replacePattern(parenthesizedExpression), ,
                acceptor
            );
        } else if (agent.isPresent() || message.isPresent() || percept.isPresent()
            || exception.isPresent() || behaviour.isPresent()) {
            return compileExpressionEqualityPatternMatch(input, , acceptor);
        } else {
            return input.createEmptyCompileOutput();
        }
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<Primary> input,
                                                StaticState state) {
        final List<Maybe<RValueExpression>> exprs =
            Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
        } else {
            parenthesizedExpression = nothing();
        }
        final Maybe<String> agent = input.__(Primary::getAgent);
        final Maybe<String> message = input.__(Primary::getMessage);
        final Maybe<String> percept = input.__(Primary::getPercept);
        final Maybe<String> exception = input.__(Primary::getException);
        final Maybe<String> behaviour = input.__(Primary::getBehaviour);
        if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).inferPatternTypeInternal(parenthesizedExpression, );
        } else if (agent.isPresent() || message.isPresent() || percept.isPresent()
            || exception.isPresent() || behaviour.isPresent()) {
            return PatternType.simple(inferType(input, ));
        } else {

            return PatternType.empty(module);
        }
    }

    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<Primary> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        final List<Maybe<RValueExpression>> exprs =
            Maybe.toListOfMaybes(input.getPattern()
                    .__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
        } else {
            parenthesizedExpression = nothing();
        }
        final Maybe<String> agent = input.getPattern().__(Primary::getAgent);
        final Maybe<String> message =
            input.getPattern().__(Primary::getMessage);
        final Maybe<String> percept =
            input.getPattern().__(Primary::getPercept);
        final Maybe<String> exception =
            input.getPattern().__(Primary::getException);
        final Maybe<String> behaviour =
            input.getPattern().__(Primary::getBehaviour);
        if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).validatePatternMatchInternal(
                input.replacePattern(parenthesizedExpression), ,
                acceptor
            );
        } else if (agent.isPresent() || message.isPresent() || percept.isPresent()
            || exception.isPresent() || behaviour.isPresent()) {
            return validateExpressionEqualityPatternMatch(input, , acceptor);
        } else {
            return VALID;
        }
    }

    @Override
    protected boolean isAlwaysPureInternal(Maybe<Primary> input,
                                           StaticState state) {
        return subExpressionsAllAlwaysPure(input, state);
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<Primary> input) {
        return subExpressionsAllMatch(input, ExpressionSemantics::canBeHoled);
    }
}
