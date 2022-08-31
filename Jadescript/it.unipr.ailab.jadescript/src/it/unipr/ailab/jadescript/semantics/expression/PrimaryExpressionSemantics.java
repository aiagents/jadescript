package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.InvokeExpression;
import it.unipr.ailab.jadescript.jadescript.Literal;
import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnBehaviourFailureHandledContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnExceptionHandledContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.MessageReceivedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.PerceptPerceivedContext;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TupleType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeArgument;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.VirtualIdentifier;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.of;

/**
 * Created on 28/12/16.
 *
 * @author Giuseppe Petrosino - giuseppe.petrosino@studenti.unipr.it
 */
@Singleton
public class PrimaryExpressionSemantics extends AssignableExpressionSemantics<Primary> {


    public PrimaryExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<Primary> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }

        final List<Maybe<RValueExpression>> expr = Maybe.toListOfMaybes(input.__(Primary::getExprs));
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);
        final List<SemanticsBoundToExpression<?>> result = new ArrayList<>();
        result.add(invoke.extract(x -> new SemanticsBoundToExpression<>(module.get(InvokeExpressionSemantics.class), x)));
        result.add(literal.extract(x -> new SemanticsBoundToExpression<>(module.get(LiteralExpressionSemantics.class), x)));
        expr.forEach(e ->
                result.add(e.extract(x -> new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), x)))
        );
        return result;
    }

    @Override
    public Maybe<String> compile(Maybe<Primary> input) {
        if (input == null) {
            return nothing();
        }
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<String> agent = input.__(Primary::getAgent);
        final Maybe<String> message = input.__(Primary::getMessage);
        final Maybe<String> percept = input.__(Primary::getPercept);
        final Maybe<String> exception = input.__(Primary::getException);
        final Maybe<String> behaviour = input.__(Primary::getBehaviour);

        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);
        final RValueExpressionSemantics rValueExpressionSemantics = module.get(RValueExpressionSemantics.class);
        if (exprs.size() > 1) {
            List<String> elements = new ArrayList<>();
            List<TypeArgument> types = new ArrayList<>();
            for (Maybe<RValueExpression> expr : exprs) {
                elements.add(rValueExpressionSemantics.compile(expr).orElse(""));
                types.add(rValueExpressionSemantics.inferType(expr));
            }
            return of(TupleType.compileNewInstance(elements, types));
        } else if (exprs.size() == 1) {
            return of("(" + rValueExpressionSemantics.compile(exprs.get(0)).orElse("") + ")");
        } else if (literal.isPresent()) {
            return of(module.get(LiteralExpressionSemantics.class).compile(literal).orElse(""));
        } else if (identifier.isPresent()) {
            return of(module.get(SingleIdentifierExpressionSemantics.class).compile(
                    VirtualIdentifier.virtualIdentifier(identifier, input)).orElse(""));
        } else if (agent.isPresent()) {
            return of(THE_AGENT + "()");
        } else if (message.isPresent()) {
            final IJadescriptType messageType = inferType(input);
            if (messageType.isErroneous()) {
                return of(MESSAGE_VAR_NAME);
            } else {
                return of(
                        "(" + messageType.compileAsJavaCast() + " " + MESSAGE_VAR_NAME + ")"
                );
            }
        } else if (exception.isPresent()) {
            final IJadescriptType reasonType = inferType(input);
            if (reasonType.isErroneous()) {
                return of(EXCEPTION_REASON_VAR_NAME);
            } else {
                return of(
                        "(" + reasonType.compileAsJavaCast() + " " + EXCEPTION_REASON_VAR_NAME + ")"
                );
            }
        } else if (behaviour.isPresent()) {
            final IJadescriptType behaviourType = inferType(input);
            if (behaviourType.isErroneous()) {
                return of(FAILED_BEHAVIOUR_VAR_NAME);
            } else {
                return of(
                        "(" + behaviourType.compileAsJavaCast() + " " + FAILED_BEHAVIOUR_VAR_NAME + ")"
                );
            }
        } else if (percept.isPresent()) {
            return module.get(ContextManager.class).currentContext()
                    .actAs(PerceptPerceivedContext.class)
                    .findFirst()
                    .flatMap(ppc ->
                            ppc.getPerceptContentStream(n -> n.equals(PERCEPT_CONTENT_VAR_NAME), null, null)
                                    .findAny()
                    )
                    .map(ns -> of(ns.compileRead("")))
                    .orElseGet(() -> of(PERCEPT_CONTENT_VAR_NAME));
        } else if (invoke.isPresent()) {
            return of(module.get(InvokeExpressionSemantics.class).compile(invoke).orElse(""));
        } else return nothing();
    }

    @Override
    public IJadescriptType inferType(Maybe<Primary> input) {
        if (input == null) {
            return module.get(TypeHelper.class).ANY;
        }
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<String> agent = input.__(Primary::getAgent);
        final Maybe<String> message = input.__(Primary::getMessage);
        final Maybe<String> percept = input.__(Primary::getPercept);
        final Maybe<String> exception = input.__(Primary::getException);
        final Maybe<String> behaviour = input.__(Primary::getBehaviour);

        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);
        final RValueExpressionSemantics rValueExpressionSemantics = module.get(RValueExpressionSemantics.class);
        if (exprs.size() > 1) {
            List<TypeArgument> types = new ArrayList<>();
            for (Maybe<RValueExpression> expr : exprs) {
                types.add(rValueExpressionSemantics.inferType(expr));
            }
            return module.get(TypeHelper.class).TUPLE.apply(types);
        } else if (exprs.size() == 1) {
            return rValueExpressionSemantics.inferType(exprs.get(0));
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
    public List<String> extractPropertyChain(Maybe<Primary> input) {
        final Maybe<String> agent = input.__(Primary::getAgent);
        final Maybe<String> message = input.__(Primary::getMessage);
        final Maybe<String> percept = input.__(Primary::getPercept);
        final Maybe<String> exception = input.__(Primary::getException);
        final Maybe<String> behaviour = input.__(Primary::getBehaviour);
        if (agent.isPresent()) {
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
            return super.extractPropertyChain(input);
        }
    }

    @Override
    public boolean mustTraverse(Maybe<Primary> input) {

        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);
        return exprs.size() == 1
                || literal.isPresent()
                || identifier.isPresent()
                || invoke.isPresent();
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<Primary> input) {
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);
        if (mustTraverse(input)) {
            if (exprs.size() == 1) {
                return Optional.of(new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), exprs.get(0)));
            } else if (literal.isPresent()) {
                return Optional.of(new SemanticsBoundToExpression<>(module.get(LiteralExpressionSemantics.class), literal));
            } else if (identifier.isPresent()) {
                return Optional.of(new SemanticsBoundToExpression<>(
                        module.get(SingleIdentifierExpressionSemantics.class),
                        VirtualIdentifier.virtualIdentifier(identifier, input)
                ));
            } else if (invoke.isPresent()) {
                return Optional.of(new SemanticsBoundToExpression<>(module.get(InvokeExpressionSemantics.class), invoke));
            }
        }
        return Optional.empty();
    }


    @Override
    public void validate(Maybe<Primary> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        final List<Maybe<RValueExpression>> exprs = Maybe.toListOfMaybes(input.__(Primary::getExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<String> agent = input.__(Primary::getAgent);
        final Maybe<String> message = input.__(Primary::getMessage);
        final Maybe<String> percept = input.__(Primary::getPercept);
        final Maybe<String> exception = input.__(Primary::getException);
        final Maybe<String> behaviour = input.__(Primary::getBehaviour);

        final Maybe<InvokeExpression> invoke = input.__(Primary::getInvokeExpression);
        if (!exprs.isEmpty()) {
            module.get(ValidationHelper.class).assertion(
                    exprs.size() <= 20,
                    "TooBigTuple",
                    "Tuples with more than 20 elements are not supported.",
                    input,
                    acceptor
            );
            for (Maybe<RValueExpression> expr : exprs) {
                module.get(RValueExpressionSemantics.class).validate(expr, acceptor);
            }
        } else if (literal.isPresent()) {
            module.get(LiteralExpressionSemantics.class).validate(literal, acceptor);
        } else if (identifier.isPresent()) {
            module.get(SingleIdentifierExpressionSemantics.class)
                    .validate(VirtualIdentifier.virtualIdentifier(
                            identifier,
                            input
                    ), acceptor);
        } else if (agent.isPresent()) {
            module.get(ValidationHelper.class).assertCanUseAgentReference(input, acceptor);
        } else if (message.isPresent()) {
            module.get(ValidationHelper.class).assertion(
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
            module.get(ValidationHelper.class).assertion(
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
            module.get(ValidationHelper.class).assertion(
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
            module.get(ValidationHelper.class).assertion(
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
            module.get(InvokeExpressionSemantics.class).validate(invoke, acceptor);
        }
    }

    @Override
    public Maybe<String> compileAssignment(
            Maybe<Primary> input,
            String compiledExpression,
            IJadescriptType exprType
    ) {
        if (input == null) {
            return nothing();
        }
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        if (identifier.isPresent()) {
            return module.get(SingleIdentifierExpressionSemantics.class).compileAssignment(
                    VirtualIdentifier.virtualIdentifier(identifier, input),
                    compiledExpression,
                    exprType
            );

        } else return nothing();
    }

    @Override
    public void validateAssignment(
            Maybe<Primary> input,
            String assignmentOperator,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return;
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        if (identifier.isPresent()) {
            module.get(SingleIdentifierExpressionSemantics.class).validateAssignment(
                    VirtualIdentifier.virtualIdentifier(identifier, input),
                    assignmentOperator,
                    expression,
                    acceptor
            );
        }
    }

    @Override
    public void syntacticValidateLValue(Maybe<Primary> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        if (identifier.isPresent()) {
            module.get(SingleIdentifierExpressionSemantics.class).syntacticValidateLValue(
                    VirtualIdentifier.virtualIdentifier(identifier, input),
                    acceptor
            );
        } else {
            errorNotLvalue(input, acceptor);
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


}
