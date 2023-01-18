package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.BuiltinHierarchicType;
import it.unipr.ailab.jadescript.jadescript.CollectionTypeExpression;
import it.unipr.ailab.jadescript.jadescript.MessageType;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.Semantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.BaseBehaviourType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TupleType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeArgument;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import jadescript.content.JadescriptOntoElement;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 28/12/16.
 */
public final class TypeExpressionSemantics extends Semantics {

    public TypeExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }



    public boolean validate(
        Maybe<TypeExpression> input,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return VALID;
        }
        final Maybe<JvmTypeReference> jvmtype =
            input.__(TypeExpression::getJvmType);
        final Maybe<CollectionTypeExpression> collectionType =
            input.__(TypeExpression::getCollectionTypeExpression);
        final List<Maybe<TypeExpression>> subExprs =
            Maybe.toListOfMaybes(input.__(TypeExpression::getSubExprs)).stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());
        final Maybe<BuiltinHierarchicType> builtinHierarchicType =
            input.__(TypeExpression::getBuiltinHiearchic);

        final TypeHelper typeHelper = module.get(TypeHelper.class);
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        if (jvmtype.isPresent()) {
            return validateJVMType(
                input,
                acceptor,
                jvmtype,
                typeHelper,
                validationHelper
            );
        } else if (collectionType.isPresent()) {
            return validateCollectionType(
                acceptor,
                collectionType
            );
        } else if (!subExprs.isEmpty()) {
            return validateTupleType(
                input,
                acceptor,
                subExprs,
                validationHelper
            );
        } else if (builtinHierarchicType.isPresent()) {
            return validateBuiltinHierarchic(
                builtinHierarchicType,
                acceptor
            );
        } else {
            return VALID;
        }
    }


    private boolean validateTupleType(
        Maybe<TypeExpression> input,
        ValidationMessageAcceptor acceptor,
        List<Maybe<TypeExpression>> subExprs,
        ValidationHelper validationHelper
    ) {
        boolean result = validationHelper.asserting(
            subExprs.size() <= 20,
            "TooBigTuple",
            "Tuples with more than 20 elements are not supported.",
            input,
            acceptor
        );
        for (Maybe<TypeExpression> subExpr : subExprs) {
            final boolean subExprValidation = this.validate(subExpr, acceptor);
            result = result && subExprValidation;
        }
        return result;
    }


    private boolean validateCollectionType(
        ValidationMessageAcceptor acceptor,
        Maybe<CollectionTypeExpression> collectionType
    ) {
        boolean result = VALID;
        final String collectionTypeString =
            collectionType.__(CollectionTypeExpression::getCollectionType)
                .extract(nullAsEmptyString);
        switch (collectionTypeString) {
            case "map":
            case "list":
            case "set": {
                for (Maybe<TypeExpression> typeParameter :
                    iterate(collectionType
                        .__(CollectionTypeExpression::getTypeParameters))
                ) {
                    final boolean typeParameterValidation =
                        this.validate(typeParameter, acceptor);
                    result = result && typeParameterValidation;
                }
            }
        }
        return result;
    }


    private boolean validateJVMType(
        Maybe<TypeExpression> input,
        ValidationMessageAcceptor acceptor,
        Maybe<JvmTypeReference> jvmtype,
        TypeHelper typeHelper,
        ValidationHelper validationHelper
    ) {
        final IJadescriptType jdType =
            typeHelper.jtFromJvmTypeRef(jvmtype.toNullable());
        boolean result = validationHelper.assertTypeReferable(
            input,
            "Invalid type reference",
            jdType,
            acceptor
        );

        if (typeHelper.isAssignable(JadescriptOntoElement.class, jdType)) {
            Optional<IJadescriptType> declaringOntology =
                jdType.namespace()
                    // Search only local on purpose.
                    .searchCallable(
                        name -> name.startsWith("__metadata"),
                        null,
                        null,
                        null
                    ).findFirst()
                    .map(CallableSymbol::returnType);

            if (declaringOntology.isPresent()) {
                IJadescriptType jadescriptType = declaringOntology.get();
                boolean ontologyAccessible = validationHelper.asserting(
                    module.get(ContextManager.class).currentContext()
                        .actAs(OntologyAssociationComputer.class)
                        .findFirst()
                        .orElse(OntologyAssociationComputer
                            .EMPTY_ONTOLOGY_ASSOCIATIONS)
                        .computeAllOntologyAssociations()
                        .anyMatch(oa ->
                            oa.getOntology().typeEquals(jadescriptType)
                        ),
                    "NotUsedOntology",
                    "The type '" + jdType + "' is defined in" +
                        " an ontology which" +
                        " is not accessible in this context.",
                    input,
                    acceptor
                );
                result = result && ontologyAccessible;
            }
        }

        return result;
    }

    private boolean validateBuiltinHierarchic(
        Maybe<BuiltinHierarchicType> builtinHierarchicType,
        ValidationMessageAcceptor acceptor
    ) {

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        Function<List<TypeArgument>, BaseBehaviourType> behaviourTypeFunction =
            getBaseBehaviourTypeFunction(builtinHierarchicType);

        if (behaviourTypeFunction != null) {
            IJadescriptType agentType = getAgentArgumentType(
                builtinHierarchicType);
            final boolean expectedTypeValidation =
                module.get(ValidationHelper.class).assertExpectedType(
                    typeHelper.AGENT,
                    agentType,
                    "InvalidBehaviourTypeArgument",
                    builtinHierarchicType
                        .__(BuiltinHierarchicType::getArgumentAgentRef),
                    acceptor
                );
            final boolean agentTypeValidation = behaviourTypeFunction.apply(
                    Arrays.asList(agentType))
                .validateType(builtinHierarchicType, acceptor);
            return expectedTypeValidation && agentTypeValidation;
        } else {
            final Maybe<MessageType> messageTypeMaybe =
                builtinHierarchicType.__(
                    BuiltinHierarchicType::getMessageType);
            if (messageTypeMaybe.isPresent()) {
                return getMessageType(messageTypeMaybe).validateType(
                    messageTypeMaybe,
                    acceptor
                );
            }
        }
        return VALID;
    }


    private IJadescriptType getMessageType(
        Maybe<MessageType> messageTypeMaybe
    ) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        final Maybe<String> baseTypeName =
            messageTypeMaybe.__(MessageType::getBaseType);
        final IJadescriptType contentType =
            messageTypeMaybe.__(MessageType::getContentType)
                .extract(this::toJadescriptType);
        boolean isExplicitContentType =
            messageTypeMaybe.__(MessageType::isWithOf).extract(
                nullAsFalse);

        final List<TypeArgument> contentTypes;
        if (!isExplicitContentType) {
            contentTypes =
                typeHelper.getDefaultTypeArguments(baseTypeName.toNullable());
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


    private IJadescriptType getAgentArgumentType(
        Maybe<BuiltinHierarchicType> bhType
    ) {
        TypeHelper typeHelper = module.get(TypeHelper.class);
        IJadescriptType agentType = typeHelper.AGENT;
        if (bhType.__(BuiltinHierarchicType::isFor).extract(nullAsFalse)) {
            final Maybe<JvmTypeReference> agentArgumentRef =
                bhType.__(BuiltinHierarchicType::getArgumentAgentRef);
            if (agentArgumentRef.isPresent()) {
                agentType = typeHelper.jtFromJvmTypeRef(
                    agentArgumentRef.toNullable()
                );
            }
        }
        return agentType;
    }


    @Nullable
    private Function<List<TypeArgument>, BaseBehaviourType>
    getBaseBehaviourTypeFunction(
        Maybe<BuiltinHierarchicType> bhType
    ) {
        Function<List<TypeArgument>, BaseBehaviourType> behaviourTypeFunction
            = null;
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (
            bhType.__(BuiltinHierarchicType::isBaseBehaviour)
                .extract(nullAsFalse)
        ) {
            behaviourTypeFunction = typeHelper.BEHAVIOUR;
        } else if (
            bhType.__(BuiltinHierarchicType::isCyclicBehaviour)
                .extract(nullAsFalse)
        ) {
            behaviourTypeFunction = typeHelper.CYCLIC_BEHAVIOUR;
        } else if (
            bhType.__(BuiltinHierarchicType::isOneshotBehaviour)
                .extract(nullAsFalse)
        ) {
            behaviourTypeFunction = typeHelper.ONESHOT_BEHAVIOUR;
        }
        return behaviourTypeFunction;
    }


    public IJadescriptType toJadescriptType(Maybe<TypeExpression> input) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (input == null) {
            return typeHelper.NOTHING;
        }
        final Maybe<BuiltinHierarchicType> hierarchicType = input.__(
            TypeExpression::getBuiltinHiearchic);
        final Maybe<CollectionTypeExpression> collectionType = input.__(
            TypeExpression::getCollectionTypeExpression);
        final List<Maybe<TypeExpression>> subExprs =
            Maybe.toListOfMaybes(input.__(
                    TypeExpression::getSubExprs))
                .stream()
                .filter(Maybe::isPresent)
                .collect(Collectors.toList());

        final Function<List<TypeArgument>, BaseBehaviourType>
            baseBehaviourTypeFunction =
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
        } else if (input.__(TypeExpression::isPerformative)
            .extract(nullAsFalse)) {
            return typeHelper.PERFORMATIVE;
        } else if (subExprs.size() == 1) {
            return toJadescriptType(subExprs.get(0));
        } else if (subExprs.size() > 1) {
            List<TypeArgument> elementTypes = subExprs.stream()
                .map(this::toJadescriptType)
                .collect(Collectors.toList());
            return typeHelper.TUPLE.apply(elementTypes);
        }
        if (hierarchicType.__(BuiltinHierarchicType::isAgent).extract(
            nullAsFalse)) {
            return typeHelper.AGENT;
        } else if (hierarchicType.__(BuiltinHierarchicType::isOntology).extract(
            nullAsFalse)) {
            return typeHelper.ONTOLOGY;
        } else if (baseBehaviourTypeFunction != null) {
            return baseBehaviourTypeFunction.apply(Arrays.asList(
                getAgentArgumentType(hierarchicType)));
        } else if (hierarchicType.__(BuiltinHierarchicType::isConcept).extract(
            nullAsFalse)) {
            return typeHelper.CONCEPT;
        } else if (hierarchicType.__(BuiltinHierarchicType::isProposition)
            .extract(
            nullAsFalse)) {
            return typeHelper.PROPOSITION;
        } else if (hierarchicType.__(BuiltinHierarchicType::isPredicate)
            .extract(
            nullAsFalse)) {
            return typeHelper.PREDICATE;
        } else if (hierarchicType.__(BuiltinHierarchicType::isAtomicProposition)
            .extract(
            nullAsFalse)) {
            return typeHelper.ATOMIC_PROPOSITION;
        } else if (hierarchicType.__(BuiltinHierarchicType::isAction).extract(
            nullAsFalse)) {
            return typeHelper.ACTION;
        } else if (hierarchicType.__(BuiltinHierarchicType::getMessageType)
            .isPresent()) {
            return getMessageType(hierarchicType
                .__(BuiltinHierarchicType::getMessageType));
        } else if (collectionType.isPresent()) {
            List<TypeArgument> typeParameters = stream(
                collectionType.__(CollectionTypeExpression::getTypeParameters)
            ).map(this::toJadescriptType)
                .collect(Collectors.toList());

            switch (collectionType
                .__(CollectionTypeExpression::getCollectionType)
                .extract(nullAsEmptyString)) {
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
