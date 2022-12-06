package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.BuiltinHierarchicType;
import it.unipr.ailab.jadescript.jadescript.CollectionTypeExpression;
import it.unipr.ailab.jadescript.jadescript.MessageType;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.*;
import it.unipr.ailab.maybe.Maybe;
import jadescript.content.JadescriptOntoElement;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 28/12/16.
 *
 * 
 */
@Singleton
public class TypeExpressionSemantics extends ExpressionSemantics<TypeExpression> {

    public TypeExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<TypeExpression> input) {
        return Collections.emptyList();
    }

    @Override
    public Maybe<String> compile(Maybe<TypeExpression> input) {
        if (input == null) return nothing();
        return of(toJadescriptType(input).compileToJavaTypeReference());
    }


    @Override
    public IJadescriptType inferType(Maybe<TypeExpression> input) {
        if (input == null)
            return module.get(TypeHelper.class).ANY;
        return module.get(TypeHelper.class).jtFromClass(Class.class);
    }

    @Override
    public boolean mustTraverse(Maybe<TypeExpression> input) {
        return false;
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<TypeExpression> input) {
        return Optional.empty();
    }

    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<TypeExpression, ?, ?> input) {
        return input.createEmptyCompileOutput();
    }

    @Override
    protected PatternType inferPatternTypeInternal(PatternMatchInput<TypeExpression, ?, ?> input) {
        return PatternType.empty(module);
    }

    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<TypeExpression, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        return input.createEmptyValidationOutput();
    }


    @Override
    public void validate(Maybe<TypeExpression> input, ValidationMessageAcceptor acceptor) {
        if (input == null) {
            return;
        }
        final Maybe<JvmTypeReference> jvmtype = input.__(TypeExpression::getJvmType);
        final Maybe<CollectionTypeExpression> collectionType = input.__(TypeExpression::getCollectionTypeExpression);
        final List<Maybe<TypeExpression>> subExprs = Maybe.toListOfMaybes(input.__(TypeExpression::getSubExprs))
                .stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<BuiltinHierarchicType> builtinHierarchicType = input.__(TypeExpression::getBuiltinHiearchic);

        //it is always valid (by grammar constraints) except not valid JvmTypeReferences
        if (jvmtype.isPresent()) {
            final IJadescriptType correspondingJDtype = module.get(TypeHelper.class).jtFromJvmTypeRef(jvmtype.toNullable());
            module.get(ValidationHelper.class).assertTypeReferable(
                    input,
                    "Invalid type reference",
                    correspondingJDtype,
                    acceptor
            );

            if (module.get(TypeHelper.class).isAssignable(
                    JadescriptOntoElement.class,
                    correspondingJDtype
            )) {
                Optional<IJadescriptType> declaringOntology = correspondingJDtype.namespace()
                        //local search:
                        .searchCallable(
                                name -> name.startsWith("__metadata"),
                                null,
                                null,
                                null
                        ).findFirst()
                        .map(CallableSymbol::returnType);

                declaringOntology.ifPresent(jadescriptType -> {
                    module.get(ValidationHelper.class).assertion(
                            module.get(ContextManager.class).currentContext()
                                    .actAs(OntologyAssociationComputer.class)
                                    .findFirst().orElse(OntologyAssociationComputer.EMPTY_ONTOLOGY_ASSOCIATIONS)
                                    .computeAllOntologyAssociations()
                                    .anyMatch(oa -> oa.getOntology().typeEquals(jadescriptType)),
                            "NotUsedOntology",
                            "The type '" + correspondingJDtype + "' is defined in an ontology which" +
                                    " is not accessible in this context.",
                            input,
                            acceptor
                    );
                });
            }


        } else if (collectionType.isPresent()) {
            switch (collectionType.__(CollectionTypeExpression::getCollectionType).extract(nullAsEmptyString)) {
                case "map":
                case "list":
                case "set": {
                    for (Maybe<TypeExpression> typeParameter : iterate(
                            collectionType.__(CollectionTypeExpression::getTypeParameters)
                    )) {
                        validate(typeParameter, acceptor);
                    }
                    break;
                }
            }
        } else if (!subExprs.isEmpty()) {
            module.get(ValidationHelper.class).assertion(
                    subExprs.size() <= 20,
                    "TooBigTuple",
                    "Tuples with more than 20 elements are not supported.",
                    input,
                    acceptor
            );
            for (Maybe<TypeExpression> subExpr : subExprs) {
                validate(subExpr, acceptor);
            }
        } else if (builtinHierarchicType.isPresent()) {
            validateBuiltinHierarchic(builtinHierarchicType, acceptor);
        }
    }

    private void validateBuiltinHierarchic(
            Maybe<BuiltinHierarchicType> builtinHierarchicType,
            ValidationMessageAcceptor acceptor
    ) {

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        Function<List<TypeArgument>, BaseBehaviourType> behaviourTypeFunction =
                getBaseBehaviourTypeFunction(builtinHierarchicType);

        if (behaviourTypeFunction != null) {
            IJadescriptType agentType = getAgentArgumentType(builtinHierarchicType);
            module.get(ValidationHelper.class).assertExpectedType(
                    typeHelper.AGENT,
                    agentType,
                    "InvalidBehaviourTypeArgument",
                    builtinHierarchicType.__(BuiltinHierarchicType::getArgumentAgentRef),
                    acceptor
            );
            behaviourTypeFunction.apply(Arrays.asList(agentType)).validateType(builtinHierarchicType, acceptor);
        } else {
            final Maybe<MessageType> messageTypeMaybe = builtinHierarchicType.__(BuiltinHierarchicType::getMessageType);
            if (messageTypeMaybe.isPresent()) {
                getMessageType(messageTypeMaybe).validateType(messageTypeMaybe, acceptor);
            }
        }
    }

    private IJadescriptType getMessageType(Maybe<MessageType> messageTypeMaybe) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        final Maybe<String> baseTypeName = messageTypeMaybe.__(MessageType::getBaseType);
        final IJadescriptType contentType = messageTypeMaybe.__(MessageType::getContentType)
                .extract(this::toJadescriptType);
        boolean isExplicitContentType = messageTypeMaybe.__(MessageType::isWithOf).extract(nullAsFalse);

        final List<TypeArgument> contentTypes;
        if (!isExplicitContentType) {
            contentTypes = typeHelper.getDefaultTypeArguments(baseTypeName.toNullable());
        } else {
            if (contentType instanceof TupleType) {
                contentTypes = ((TupleType) contentType).getTypeArguments();
            } else {
                contentTypes = Collections.singletonList(contentType);
            }
        }
        return baseTypeName
                .__(typeHelper::getMessageType)
                .<IJadescriptType>__(f -> f.apply(contentTypes))
                .orElse(typeHelper.ANYMESSAGE);
    }

    private IJadescriptType getAgentArgumentType(Maybe<BuiltinHierarchicType> builtinHierarchicType) {
        TypeHelper typeHelper = module.get(TypeHelper.class);
        IJadescriptType agentType = typeHelper.AGENT;
        if (builtinHierarchicType.__(BuiltinHierarchicType::isFor).extract(nullAsFalse)) {
            if (builtinHierarchicType.__(BuiltinHierarchicType::getArgumentAgentRef).isPresent()) {
                agentType = typeHelper.jtFromJvmTypeRef(
                        builtinHierarchicType.__(BuiltinHierarchicType::getArgumentAgentRef).toNullable()
                );
            }
        }
        return agentType;
    }

    private Function<List<TypeArgument>, BaseBehaviourType> getBaseBehaviourTypeFunction(
            Maybe<BuiltinHierarchicType> builtinHierarchicType
    ) {
        Function<List<TypeArgument>, BaseBehaviourType> behaviourTypeFunction = null;
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (builtinHierarchicType.__(BuiltinHierarchicType::isBaseBehaviour).extract(nullAsFalse)) {
            behaviourTypeFunction = typeHelper.BEHAVIOUR;
        } else if (builtinHierarchicType.__(BuiltinHierarchicType::isCyclicBehaviour).extract(nullAsFalse)) {
            behaviourTypeFunction = typeHelper.CYCLIC_BEHAVIOUR;
        } else if (builtinHierarchicType.__(BuiltinHierarchicType::isOneshotBehaviour).extract(nullAsFalse)) {
            behaviourTypeFunction = typeHelper.ONESHOT_BEHAVIOUR;
        }
        return behaviourTypeFunction;
    }

    public IJadescriptType toJadescriptType(Maybe<TypeExpression> input) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (input == null) {
            return typeHelper.NOTHING;
        }
        final Maybe<BuiltinHierarchicType> hierarchicType = input.__(TypeExpression::getBuiltinHiearchic);
        final Maybe<CollectionTypeExpression> collectionType = input.__(TypeExpression::getCollectionTypeExpression);
        final List<Maybe<TypeExpression>> subExprs = Maybe.toListOfMaybes(input.__(TypeExpression::getSubExprs))
                .stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());

        final Function<List<TypeArgument>, BaseBehaviourType> baseBehaviourTypeFunction =
                getBaseBehaviourTypeFunction(hierarchicType);

        if (input.__(TypeExpression::isAid).extract(nullAsFalse)) {
            return typeHelper.AID;
        } else if (input.__(TypeExpression::isBoolean).extract(nullAsFalse)) {
            return typeHelper.BOOLEAN;
        } else if (input.__(TypeExpression::isReal).extract(nullAsFalse)) {
            return typeHelper.REAL;
        } else if (input.__(TypeExpression::isInteger).extract(nullAsFalse)) {
            return typeHelper.INTEGER;
        } else if (input.__(TypeExpression::isDuration).extract(nullAsFalse)) {
            return typeHelper.DURATION;
        } else if (input.__(TypeExpression::isTimestamp).extract(nullAsFalse)) {
            return typeHelper.TIMESTAMP;
        } else if (input.__(TypeExpression::isText).extract(nullAsFalse)) {
            return typeHelper.TEXT;
        } else if (input.__(TypeExpression::isPerformative).extract(nullAsFalse)) {
            return typeHelper.PERFORMATIVE;
        } else if (subExprs.size() == 1) {
            return toJadescriptType(subExprs.get(0));
        } else if (subExprs.size() > 1) {
            List<TypeArgument> elementTypes = subExprs.stream()
                    .map(this::toJadescriptType)
                    .collect(Collectors.toList());
            return typeHelper.TUPLE.apply(elementTypes);
        }
        if (hierarchicType.__(BuiltinHierarchicType::isAgent).extract(nullAsFalse)) {
            return typeHelper.AGENT;
        } else if (hierarchicType.__(BuiltinHierarchicType::isOntology).extract(nullAsFalse)) {
            return typeHelper.ONTOLOGY;
        } else if (baseBehaviourTypeFunction != null) {
            return baseBehaviourTypeFunction.apply(Arrays.asList(getAgentArgumentType(hierarchicType)));
        } else if (hierarchicType.__(BuiltinHierarchicType::isConcept).extract(nullAsFalse)) {
            return typeHelper.CONCEPT;
        } else if (hierarchicType.__(BuiltinHierarchicType::isProposition).extract(nullAsFalse)) {
            return typeHelper.PROPOSITION;
        } else if (hierarchicType.__(BuiltinHierarchicType::isPredicate).extract(nullAsFalse)) {
            return typeHelper.PREDICATE;
        } else if (hierarchicType.__(BuiltinHierarchicType::isAtomicProposition).extract(nullAsFalse)) {
            return typeHelper.ATOMIC_PROPOSITION;
        } else if (hierarchicType.__(BuiltinHierarchicType::isAction).extract(nullAsFalse)) {
            return typeHelper.ACTION;
        } else if (hierarchicType.__(BuiltinHierarchicType::getMessageType).isPresent()) {
            return getMessageType(hierarchicType.__(BuiltinHierarchicType::getMessageType));
        } else if (collectionType.isPresent()) {
            List<TypeArgument> typeParameters = stream(
                    collectionType.__(CollectionTypeExpression::getTypeParameters)
            ).map(this::toJadescriptType)
                    .collect(Collectors.toList());

            switch (collectionType.__(CollectionTypeExpression::getCollectionType).extract(nullAsEmptyString)) {
                case "list": {
                    return typeHelper.LIST.apply(typeParameters);
                }
                case "map": {
                    return typeHelper.MAP.apply(typeParameters);
                }
                case "set": {
                    return typeHelper.SET.apply(typeParameters);
                }
            }
        }

        

        return input
                .__(TypeExpression::getJvmType)
                .__(typeHelper::jtFromJvmTypeRef)
                .orElse(typeHelper.ANY);
    }


}
