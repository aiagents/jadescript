package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnBehaviourFailureHandledContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnExceptionHandledContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.MessageReceivedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.PerceptPerceivedContext;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.TupledExpressions;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.VirtualIdentifier;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.*;

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
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }


        final boolean isPlaceholder = input.__(Primary::isPlaceholder).extract(nullAsFalse);
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);
        final List<SemanticsBoundToExpression<?>> result = new ArrayList<>();

        if (literal.isPresent()) {
            result.add(literal.extract(x -> new SemanticsBoundToExpression<>(
                    module.get(LiteralExpressionSemantics.class),
                    x
            )));
        }
        if (invoke.isPresent()) {
            result.add(invoke.extract(x -> new SemanticsBoundToExpression<>(
                    module.get(InvokeExpressionSemantics.class),
                    x
            )));
        }
        if (isPlaceholder) {
            result.add(new SemanticsBoundToExpression<>(
                    module.get(PlaceholderExpressionSemantics.class),
                    input
            ));
        }

        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        if (exprs.size() == 1) {
            result.add(exprs.get(0).extract(x -> new SemanticsBoundToExpression<>(
                    module.get(RValueExpressionSemantics.class),
                    x
            )));
        } else {
            result.add(TupledExpressions.tupledExpressions(input).extract(x -> new SemanticsBoundToExpression<>(
                    module.get(TupleExpressionSemantics.class),
                    x
            )));
        }

        return result;
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<Primary> input) {
        final Maybe<String> agent = input.__(Primary::getAgent);
        final Maybe<String> message = input.__(Primary::getMessage);
        final Maybe<String> percept = input.__(Primary::getPercept);
        final Maybe<String> exception = input.__(Primary::getException);
        final Maybe<String> behaviour = input.__(Primary::getBehaviour);
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;

        if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
        } else {
            parenthesizedExpression = nothing();
        }

        if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).propertyChain(parenthesizedExpression);
        } else if (agent.isPresent()) {
            List<String> result = new ArrayList<>();
            result.add("agent");
            return result;
        } else if (message.isPresent()) {
            List<String> result = new ArrayList<>();
            result.add("message");
            return result;
        } else if (percept.isPresent()) {
            List<String> result = new ArrayList<>();
            result.add("percept");
            return result;
        } else if (exception.isPresent()) {
            List<String> result = new ArrayList<>();
            result.add("exception");
            return result;
        } else if (behaviour.isPresent()) {
            List<String> result = new ArrayList<>();
            result.add("behaviour");
            return result;
        } else {
            return List.of();
        }
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<Primary> input) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected String compileInternal(Maybe<Primary> input, CompilationOutputAcceptor acceptor) {
        if (input == null) {
            return "";
        }

        final boolean isPlaceholder = input.__(Primary::isPlaceholder).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        final Maybe<TupledExpressions> tuple;

        if (exprs.isEmpty()) {
            parenthesizedExpression = nothing();
            tuple = nothing();
        } else if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
            tuple = nothing();
        } else {
            parenthesizedExpression = nothing();
            tuple = TupledExpressions.tupledExpressions(input);
        }

        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<String> agent = input.__(Primary::getAgent);
        final Maybe<String> message = input.__(Primary::getMessage);
        final Maybe<String> percept = input.__(Primary::getPercept);
        final Maybe<String> exception = input.__(Primary::getException);
        final Maybe<String> behaviour = input.__(Primary::getBehaviour);
        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);

        if (isPlaceholder) {
            return module.get(PlaceholderExpressionSemantics.class).compile(input, acceptor);
        }
        if (parenthesizedExpression.isPresent()) {
            final String result = module.get(RValueExpressionSemantics.class).compile(
                    parenthesizedExpression,
                    acceptor
            );
            return "(" + result + ")";
        } else if (tuple.isPresent()) {
            return module.get(TupleExpressionSemantics.class).compile(tuple, acceptor);
        } else if (literal.isPresent()) {
            return module.get(LiteralExpressionSemantics.class).compile(literal, acceptor);
        } else if (identifier.isPresent()) {
            return module.get(SingleIdentifierExpressionSemantics.class).compile(
                    VirtualIdentifier.virtualIdentifier(identifier, input), acceptor);
        } else if (agent.isPresent()) {
            return THE_AGENT + "()";
        } else if (message.isPresent()) {
            final IJadescriptType messageType = inferType(input);
            if (messageType.isErroneous()) {
                return MESSAGE_VAR_NAME;
            } else {
                return "(" + messageType.compileAsJavaCast() + " " + MESSAGE_VAR_NAME + ")";

            }
        } else if (exception.isPresent()) {
            final IJadescriptType reasonType = inferType(input);
            if (reasonType.isErroneous()) {
                return EXCEPTION_REASON_VAR_NAME;
            } else {
                return "(" + reasonType.compileAsJavaCast() + " " + EXCEPTION_REASON_VAR_NAME + ")";
            }
        } else if (behaviour.isPresent()) {
            final IJadescriptType behaviourType = inferType(input);
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
        } else if (invoke.isPresent()) {
            return module.get(InvokeExpressionSemantics.class).compile(invoke, acceptor);
        } else return "";
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<Primary> input) {
        if (input == null) {
            return module.get(TypeHelper.class).ANY;
        }
        final boolean isPlaceholder = input.__(Primary::isPlaceholder).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        final Maybe<TupledExpressions> tuple;
        if (exprs.isEmpty()) {
            parenthesizedExpression = nothing();
            tuple = nothing();
        } else if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
            tuple = nothing();
        } else {
            parenthesizedExpression = nothing();
            tuple = TupledExpressions.tupledExpressions(input);
        }
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<String> agent = input.__(Primary::getAgent);
        final Maybe<String> message = input.__(Primary::getMessage);
        final Maybe<String> percept = input.__(Primary::getPercept);
        final Maybe<String> exception = input.__(Primary::getException);
        final Maybe<String> behaviour = input.__(Primary::getBehaviour);

        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);
        if (isPlaceholder) {
            return module.get(PlaceholderExpressionSemantics.class).inferType(input);
        } else if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).inferType(parenthesizedExpression);
        } else if (tuple.isPresent()) {
            return module.get(TupleExpressionSemantics.class).inferType(tuple);
        } else if (literal.isPresent()) {
            return module.get(LiteralExpressionSemantics.class).inferType(literal);
        } else if (identifier.isPresent()) {
            return module.get(SingleIdentifierExpressionSemantics.class)
                    .inferType(VirtualIdentifier.virtualIdentifier(identifier, input));
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
        } else if (invoke.isPresent()) {
            return module.get(InvokeExpressionSemantics.class).inferType(invoke);
        } else return module.get(TypeHelper.class).ANY;
    }


    @Override
    protected boolean mustTraverse(Maybe<Primary> input) {
        final boolean isPlaceholder = input.__(Primary::isPlaceholder).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);
        return !exprs.isEmpty()//single expression, or tuple
                || isPlaceholder
                || literal.isPresent()
                || identifier.isPresent()
                || invoke.isPresent();
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<Primary> input) {
        final boolean isPlaceholder = input.__(Primary::isPlaceholder).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        final Maybe<TupledExpressions> tuple;

        if (exprs.isEmpty()) {
            parenthesizedExpression = nothing();
            tuple = nothing();
        } else if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
            tuple = nothing();
        } else {
            parenthesizedExpression = nothing();
            tuple = TupledExpressions.tupledExpressions(input);
        }
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);
        if (mustTraverse(input)) {
            if (isPlaceholder) {
                return Optional.of(new SemanticsBoundToExpression<>(
                        module.get(PlaceholderExpressionSemantics.class),
                        input
                ));
            } else if (parenthesizedExpression.isPresent()) {
                return Optional.of(new SemanticsBoundToExpression<>(
                        module.get(RValueExpressionSemantics.class),
                        parenthesizedExpression
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
    protected boolean validateInternal(Maybe<Primary> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        final boolean isPlaceholder = input.__(Primary::isPlaceholder).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        final Maybe<TupledExpressions> tuple;

        if (exprs.isEmpty()) {
            parenthesizedExpression = nothing();
            tuple = nothing();
        } else if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
            tuple = nothing();
        } else {
            parenthesizedExpression = nothing();
            tuple = TupledExpressions.tupledExpressions(input);
        }
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<String> agent = input.__(Primary::getAgent);
        final Maybe<String> message = input.__(Primary::getMessage);
        final Maybe<String> percept = input.__(Primary::getPercept);
        final Maybe<String> exception = input.__(Primary::getException);
        final Maybe<String> behaviour = input.__(Primary::getBehaviour);

        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);

        if (isPlaceholder) {
            return module.get(PlaceholderExpressionSemantics.class).validate(input, acceptor);
        } else if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).validate(parenthesizedExpression, acceptor);
        } else if (tuple.isPresent()) {
            return module.get(TupleExpressionSemantics.class).validate(tuple, acceptor);
        } else if (literal.isPresent()) {
            return module.get(LiteralExpressionSemantics.class).validate(literal, acceptor);
        } else if (identifier.isPresent()) {
            return module.get(SingleIdentifierExpressionSemantics.class)
                    .validate(VirtualIdentifier.virtualIdentifier(
                            identifier,
                            input
                    ), acceptor);
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
        } else if (invoke.isPresent()) {
            return module.get(InvokeExpressionSemantics.class).validate(invoke, acceptor);
        }

        return VALID;
    }

    @Override
    public void compileAssignmentInternal(
            Maybe<Primary> input,
            String compiledExpression,
            IJadescriptType exprType,
            CompilationOutputAcceptor acceptor
    ) {
        if (input == null) {
            return;
        }
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        if (identifier.isPresent()) {
            module.get(SingleIdentifierExpressionSemantics.class).compileAssignment(
                    VirtualIdentifier.virtualIdentifier(identifier, input),
                    compiledExpression,
                    exprType,
                    acceptor
            );

        }
    }

    @Override
    public boolean validateAssignmentInternal(
            Maybe<Primary> input,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return VALID;
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        if (identifier.isPresent()) {
            return module.get(SingleIdentifierExpressionSemantics.class).validateAssignment(
                    VirtualIdentifier.virtualIdentifier(identifier, input),
                    expression,
                    acceptor
            );
        }
        return VALID;
    }

    @Override
    public boolean syntacticValidateLValueInternal(Maybe<Primary> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        if (identifier.isPresent()) {
            return module.get(SingleIdentifierExpressionSemantics.class).syntacticValidateLValue(
                    VirtualIdentifier.virtualIdentifier(identifier, input),
                    acceptor
            );
        } else {
            return errorNotLvalue(input, acceptor);
        }
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<Primary> input) {
        final boolean isPlaceholder = input.__(Primary::isPlaceholder).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        final Maybe<TupledExpressions> tuple;

        if (exprs.isEmpty()) {
            parenthesizedExpression = nothing();
            tuple = nothing();
        } else if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
            tuple = nothing();
        } else {
            parenthesizedExpression = nothing();
            tuple = TupledExpressions.tupledExpressions(input);
        }

        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);
        if (isPlaceholder) {
            return module.get(PlaceholderExpressionSemantics.class).isValidLExpr(input);
        } else if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).isValidLExpr(parenthesizedExpression);
        } else if (tuple.isPresent()) {
            return module.get(TupleExpressionSemantics.class).isValidLExpr(tuple);
        } else if (literal.isPresent()) {
            return module.get(LiteralExpressionSemantics.class).isValidLExpr(literal);
        } else if (identifier.isPresent()) {
            return module.get(SingleIdentifierExpressionSemantics.class).isValidLExpr(
                    VirtualIdentifier.virtualIdentifier(identifier, input)
            );
        } else if (invoke.isPresent()) {
            return module.get(InvokeExpressionSemantics.class).isValidLExpr(
                    invoke
            );
        } else {
            return false;
        }
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<Primary> input) {
        final boolean isPlaceholder = input.__(Primary::isPlaceholder).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        final Maybe<TupledExpressions> tuple;

        if (exprs.isEmpty()) {
            parenthesizedExpression = nothing();
            tuple = nothing();
        } else if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
            tuple = nothing();
        } else {
            parenthesizedExpression = nothing();
            tuple = TupledExpressions.tupledExpressions(input);
        }

        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);
        if (isPlaceholder) {
            return module.get(PlaceholderExpressionSemantics.class).isPatternEvaluationPure(input);
        } else if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).isPatternEvaluationPure(parenthesizedExpression);
        } else if (tuple.isPresent()) {
            return module.get(TupleExpressionSemantics.class).isPatternEvaluationPure(tuple);
        } else if (literal.isPresent()) {
            return module.get(LiteralExpressionSemantics.class).isPatternEvaluationPure(literal);
        } else if (identifier.isPresent()) {
            return module.get(SingleIdentifierExpressionSemantics.class).isPatternEvaluationPure(
                    VirtualIdentifier.virtualIdentifier(identifier, input)
            );
        } else if (invoke.isPresent()) {
            return module.get(InvokeExpressionSemantics.class).isPatternEvaluationPure(
                    invoke
            );
        } else {
            return false;
        }

    }

    public void syntacticValidateStatement(Maybe<Primary> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);
        if (invoke.isPresent()) return;
        if (identifier.isPresent()) {
            module.get(SingleIdentifierExpressionSemantics.class).syntacticValidateStatement(
                    VirtualIdentifier.virtualIdentifier(identifier, input),
                    acceptor
            );
        } else {
            errorNotStatement(input, acceptor);
        }
    }

    @Override
    protected boolean isHoledInternal(Maybe<Primary> input) {
        if (input == null) {
            return false;
        }
        final boolean isPlaceholder = input.__(Primary::isPlaceholder).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        final Maybe<TupledExpressions> tuple;
        if (exprs.isEmpty()) {
            parenthesizedExpression = nothing();
            tuple = nothing();
        } else if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
            tuple = nothing();
        } else {
            parenthesizedExpression = nothing();
            tuple = TupledExpressions.tupledExpressions(input);
        }
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);

        if (isPlaceholder) {
            return module.get(PlaceholderExpressionSemantics.class).isHoled(input);
        } else if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).isHoled(parenthesizedExpression);
        } else if (tuple.isPresent()) {
            return module.get(TupleExpressionSemantics.class).isHoled(tuple);
        } else if (literal.isPresent()) {
            return module.get(LiteralExpressionSemantics.class).isHoled(literal);
        } else if (identifier.isPresent()) {
            return module.get(SingleIdentifierExpressionSemantics.class).isHoled(
                    VirtualIdentifier.virtualIdentifier(identifier, input)
            );
        } else if (invoke.isPresent()) {
            return module.get(InvokeExpressionSemantics.class).isHoled(invoke);
        } else {
            return false;
        }
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<Primary> input) {
        if (input == null) {
            return false;
        }
        final boolean isPlaceholder = input.__(Primary::isPlaceholder).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        final Maybe<TupledExpressions> tuple;
        if (exprs.isEmpty()) {
            parenthesizedExpression = nothing();
            tuple = nothing();
        } else if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
            tuple = nothing();
        } else {
            parenthesizedExpression = nothing();
            tuple = TupledExpressions.tupledExpressions(input);
        }
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);

        if (isPlaceholder) {
            return module.get(PlaceholderExpressionSemantics.class).isTypelyHoled(input);
        } else if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).isTypelyHoled(parenthesizedExpression);
        } else if (tuple.isPresent()) {
            return module.get(TupleExpressionSemantics.class).isTypelyHoled(tuple);
        } else if (literal.isPresent()) {
            return module.get(LiteralExpressionSemantics.class).isTypelyHoled(literal);
        } else if (identifier.isPresent()) {
            return module.get(SingleIdentifierExpressionSemantics.class).isTypelyHoled(
                    VirtualIdentifier.virtualIdentifier(identifier, input)
            );
        } else if (invoke.isPresent()) {
            return module.get(InvokeExpressionSemantics.class).isTypelyHoled(invoke);
        } else {
            return false;
        }
    }

    @Override
    protected boolean isUnboundInternal(Maybe<Primary> input) {
        if (input == null) {
            return false;
        }
        final boolean isPlaceholder = input.__(Primary::isPlaceholder).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        final Maybe<TupledExpressions> tuple;
        if (exprs.isEmpty()) {
            parenthesizedExpression = nothing();
            tuple = nothing();
        } else if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
            tuple = nothing();
        } else {
            parenthesizedExpression = nothing();
            tuple = TupledExpressions.tupledExpressions(input);
        }
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);

        if (isPlaceholder) {
            return module.get(PlaceholderExpressionSemantics.class).isUnbound(input);
        } else if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).isUnbound(parenthesizedExpression);
        } else if (tuple.isPresent()) {
            return module.get(TupleExpressionSemantics.class).isUnbound(tuple);
        } else if (literal.isPresent()) {
            return module.get(LiteralExpressionSemantics.class).isUnbound(literal);
        } else if (identifier.isPresent()) {
            return module.get(SingleIdentifierExpressionSemantics.class).isUnbound(
                    VirtualIdentifier.virtualIdentifier(identifier, input)
            );
        } else if (invoke.isPresent()) {
            return module.get(InvokeExpressionSemantics.class).isUnbound(invoke);
        } else {
            return false;
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<Primary, ?, ?> input,
            CompilationOutputAcceptor acceptor
    ) {
        final boolean isPlaceholder = input.getPattern().__(Primary::isPlaceholder).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.getPattern()
                        .__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        final Maybe<TupledExpressions> tuple;
        if (exprs.isEmpty()) {
            parenthesizedExpression = nothing();
            tuple = nothing();
        } else if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
            tuple = nothing();
        } else {
            parenthesizedExpression = nothing();
            tuple = TupledExpressions.tupledExpressions(input.getPattern());
        }
        final Maybe<Literal> literal = input.getPattern().__(Primary::getLiteral);
        final Maybe<String> identifier = input.getPattern().__(Primary::getIdentifier);
        final Maybe<String> agent = input.getPattern().__(Primary::getAgent);
        final Maybe<String> message = input.getPattern().__(Primary::getMessage);
        final Maybe<String> percept = input.getPattern().__(Primary::getPercept);
        final Maybe<String> exception = input.getPattern().__(Primary::getException);
        final Maybe<String> behaviour = input.getPattern().__(Primary::getBehaviour);
        final Maybe<InvokeExpression> invoke = input.getPattern().__(Primary::getInvokeExpression);
        if (isPlaceholder) {
            return module.get(PlaceholderExpressionSemantics.class).compilePatternMatchInternal(
                    input.replacePattern(input.getPattern()),
                    acceptor
            );
        } else if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).compilePatternMatchInternal(
                    input.replacePattern(parenthesizedExpression),
                    acceptor
            );
        } else if (tuple.isPresent()) {
            return module.get(TupleExpressionSemantics.class).compilePatternMatchInternal(
                    input.replacePattern(tuple),
                    acceptor
            );
        } else if (literal.isPresent()) {
            return module.get(LiteralExpressionSemantics.class).compilePatternMatchInternal(
                    input.replacePattern(literal),
                    acceptor
            );
        } else if (identifier.isPresent()) {
            return module.get(SingleIdentifierExpressionSemantics.class).compilePatternMatchInternal(
                    input.replacePattern(VirtualIdentifier.virtualIdentifier(
                            identifier,
                            input.getPattern()
                    )),
                    acceptor
            );
        } else if (agent.isPresent() || message.isPresent() || percept.isPresent()
                || exception.isPresent() || behaviour.isPresent()) {
            return compileExpressionEqualityPatternMatch(input, acceptor);
        } else if (invoke.isPresent()) {
            return module.get(InvokeExpressionSemantics.class).compilePatternMatchInternal(
                    input.replacePattern(invoke),
                    acceptor
            );
        } else {
            return input.createEmptyCompileOutput();
        }
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<Primary> input) {
        final boolean isPlaceholder = input.__(Primary::isPlaceholder).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        final Maybe<TupledExpressions> tuple;
        if (exprs.isEmpty()) {
            parenthesizedExpression = nothing();
            tuple = nothing();
        } else if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
            tuple = nothing();
        } else {
            parenthesizedExpression = nothing();
            tuple = TupledExpressions.tupledExpressions(input);
        }
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<String> agent = input.__(Primary::getAgent);
        final Maybe<String> message = input.__(Primary::getMessage);
        final Maybe<String> percept = input.__(Primary::getPercept);
        final Maybe<String> exception = input.__(Primary::getException);
        final Maybe<String> behaviour = input.__(Primary::getBehaviour);
        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);
        if (isPlaceholder) {
            return module.get(PlaceholderExpressionSemantics.class).inferPatternTypeInternal(input);
        } else if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).inferPatternTypeInternal(parenthesizedExpression);
        } else if (tuple.isPresent()) {
            return module.get(TupleExpressionSemantics.class).inferPatternTypeInternal(tuple);
        } else if (literal.isPresent()) {
            return module.get(LiteralExpressionSemantics.class).inferPatternTypeInternal(literal);
        } else if (identifier.isPresent()) {
            return module.get(SingleIdentifierExpressionSemantics.class).inferPatternTypeInternal(
                    VirtualIdentifier.virtualIdentifier(identifier, input)
            );
        } else if (agent.isPresent() || message.isPresent() || percept.isPresent()
                || exception.isPresent() || behaviour.isPresent()) {
            return PatternType.simple(inferType(input));
        } else if (invoke.isPresent()) {
            return module.get(InvokeExpressionSemantics.class).inferPatternTypeInternal(
                    invoke
            );
        } else {

            return PatternType.empty(module);
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<Primary, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final boolean isPlaceholder = input.getPattern().__(Primary::isPlaceholder).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.getPattern()
                        .__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<RValueExpression> parenthesizedExpression;
        final Maybe<TupledExpressions> tuple;
        if (exprs.isEmpty()) {
            parenthesizedExpression = nothing();
            tuple = nothing();
        } else if (exprs.size() == 1) {
            parenthesizedExpression = exprs.get(0);
            tuple = nothing();
        } else {
            parenthesizedExpression = nothing();
            tuple = TupledExpressions.tupledExpressions(input.getPattern());
        }
        final Maybe<Literal> literal = input.getPattern().__(Primary::getLiteral);
        final Maybe<String> identifier = input.getPattern().__(Primary::getIdentifier);
        final Maybe<String> agent = input.getPattern().__(Primary::getAgent);
        final Maybe<String> message = input.getPattern().__(Primary::getMessage);
        final Maybe<String> percept = input.getPattern().__(Primary::getPercept);
        final Maybe<String> exception = input.getPattern().__(Primary::getException);
        final Maybe<String> behaviour = input.getPattern().__(Primary::getBehaviour);
        final Maybe<InvokeExpression> invoke = input.getPattern().__(Primary::getInvokeExpression);
        if (isPlaceholder) {
            return module.get(PlaceholderExpressionSemantics.class).validatePatternMatchInternal(
                    input.replacePattern(input.getPattern()), acceptor
            );
        } else if (parenthesizedExpression.isPresent()) {
            return module.get(RValueExpressionSemantics.class).validatePatternMatchInternal(
                    input.replacePattern(parenthesizedExpression), acceptor
            );
        } else if (tuple.isPresent()) {
            return module.get(TupleExpressionSemantics.class).validatePatternMatchInternal(
                    input.replacePattern(tuple), acceptor
            );
        } else if (literal.isPresent()) {
            return module.get(LiteralExpressionSemantics.class).validatePatternMatchInternal(
                    input.replacePattern(literal), acceptor
            );
        } else if (identifier.isPresent()) {
            return module.get(SingleIdentifierExpressionSemantics.class).validatePatternMatchInternal(
                    input.replacePattern(VirtualIdentifier.virtualIdentifier(
                            identifier,
                            input.getPattern()
                    )), acceptor
            );
        } else if (agent.isPresent() || message.isPresent() || percept.isPresent()
                || exception.isPresent() || behaviour.isPresent()) {
            return validateExpressionEqualityPatternMatch(input, acceptor);
        } else if (invoke.isPresent()) {
            return module.get(InvokeExpressionSemantics.class).validatePatternMatchInternal(
                    input.replacePattern(invoke), acceptor
            );
        } else {
            return input.createEmptyValidationOutput();
        }
    }
}
