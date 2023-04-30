package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.BuiltinHierarchicType;
import it.unipr.ailab.jadescript.jadescript.CollectionTypeExpression;
import it.unipr.ailab.jadescript.jadescript.MessageType;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.Semantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OntologyDeclarationSupportContext;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour.BaseBehaviourType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import jadescript.content.JadescriptOntoElement;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.equal;
import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;
import static it.unipr.ailab.maybe.Maybe.iterate;
import static it.unipr.ailab.maybe.Maybe.someStream;

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
        final MaybeList<TypeExpression> subExprs =
            input.__toListNullsRemoved(TypeExpression::getSubExprs);

        final Maybe<BuiltinHierarchicType> builtinHierarchicType =
            input.__(TypeExpression::getBuiltinHiearchic);

        if (jvmtype.isPresent()) {
            return validateJVMType(
                input,
                acceptor,
                jvmtype,
                module
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
                module
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
        MaybeList<TypeExpression> subExprs,
        SemanticsModule module
    ) {
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

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
                .orElse("");
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
        SemanticsModule module
    ) {
        final TypeSolver typeSolver = module.get(TypeSolver.class);
        final TypeComparator comparator = module.get(TypeComparator.class);
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);
        final IJadescriptType jdType =
            typeSolver.fromJvmTypeReference(jvmtype.toNullable())
                .ignoreBound();
        boolean result = validationHelper.assertTypeReferable(
            input,
            "Invalid type reference",
            jdType,
            acceptor
        );

        if (comparator.compare(JadescriptOntoElement.class, jdType)
            .is(superTypeOrEqual())) {
            final JvmTypeNamespace jvmNamespace = jdType.jvmNamespace();
            Optional<IJadescriptType> declaringOntology =
                jvmNamespace
                    // Search only local on purpose.
                    .getMetadataMethod()
                    .map(JvmOperation::getReturnType)
                    .map(jvmNamespace::resolveType)
                    .map(TypeArgument::ignoreBound);

            if (declaringOntology.isPresent()) {
                IJadescriptType ontoType = declaringOntology.get();
                final boolean hasMatchingOntoAssociations =
                    module.get(ContextManager.class).currentContext()
                        .actAs(OntologyAssociationComputer.class)
                        .findFirst()
                        .orElse(OntologyAssociationComputer
                            .EMPTY_ONTOLOGY_ASSOCIATIONS)
                        .computeAllOntologyAssociations()
                        .anyMatch(oa ->
                            comparator.compare(oa.getOntology(), ontoType)
                                .is(equal())
                        );


                boolean ontologyAccessible;
                if (!hasMatchingOntoAssociations) {
                    Optional<OntologyDeclarationSupportContext> supportContext =
                        module.get(ContextManager.class)
                            .currentContext()
                            .actAs(OntologyDeclarationSupportContext.class)
                            .findFirst();

                    final boolean isInDeclaration = supportContext.map(
                        context -> context.isDeclarationOrExtensionOfOntology(
                            ontoType.compileToJavaTypeReference()
                        )).orElse(false);
                    // The ontology containing the ontology element declaration
                    // has to be used in some way (e.g., direcly
                    // used, used by supertypes, used by the agent...)
                    ontologyAccessible = validationHelper.asserting(
                        isInDeclaration,
                        "OntologyNotUsed",
                        "The type " + jdType
                            + "is defined in the ontology " +
                            ontoType.getFullJadescriptName() + " which is not" +
                            " accessible in this context.",
                        input,
                        acceptor
                    );
                } else {
                    ontologyAccessible = VALID;
                }

                result = result && ontologyAccessible;
            }
        }

        return result;
    }


    private boolean validateBuiltinHierarchic(
        Maybe<BuiltinHierarchicType> builtinHierarchicType,
        ValidationMessageAcceptor acceptor
    ) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final ValidationHelper validationHelper = module.get(
            ValidationHelper.class);


        if (builtinHierarchicType
            .__(BuiltinHierarchicType::isBaseBehaviour).orElse(false)
            || builtinHierarchicType
            .__(BuiltinHierarchicType::isCyclicBehaviour).orElse(false)
            || builtinHierarchicType
            .__(BuiltinHierarchicType::isOneshotBehaviour).orElse(false)
        ) {
            TypeArgument agentType =
                getAgentArgumentType(builtinHierarchicType);
            final boolean expectedTypeValidation;
            if (agentType == null) {
                expectedTypeValidation = VALID;
            } else {
                expectedTypeValidation = validationHelper.assertExpectedType(
                    builtins.agent(),
                    agentType,
                    "InvalidBehaviourTypeArgument",
                    builtinHierarchicType
                        .__(BuiltinHierarchicType::getArgumentAgentRef),
                    acceptor
                );
            }
            final BaseBehaviourType type;
            if (builtinHierarchicType
                .__(BuiltinHierarchicType::isCyclicBehaviour)
                .orElse(false)) {
                if(agentType == null){
                    type = builtins.cyclicBehaviour();
                }else{
                    type = builtins.cyclicBehaviour(agentType);
                }
            }else if(builtinHierarchicType
            .__(BuiltinHierarchicType::isOneshotBehaviour)
                .orElse(false)){
                if(agentType == null){
                    type = builtins.oneshotBehaviour();
                }else{
                    type = builtins.oneshotBehaviour(agentType);
                }
            }else{
                if(agentType == null){
                    type = builtins.behaviour();
                }else{
                    type = builtins.behaviour(agentType);
                }
            }

            final boolean validation = type
                .validateType(builtinHierarchicType, acceptor);

            return expectedTypeValidation && validation;
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
        final TypeSolver typeSolver = module.get(TypeSolver.class);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        final Maybe<String> baseTypeName =
            messageTypeMaybe.__(MessageType::getBaseType);
        final IJadescriptType contentType =
            messageTypeMaybe.__(MessageType::getContentType)
                .extract(this::toJadescriptType);
        boolean isExplicitContentType =
            messageTypeMaybe.__(MessageType::isWithOf).orElse(false);

        final List<? extends TypeArgument> contentTypes;

        if (!isExplicitContentType) {

            if (baseTypeName.wrappedEquals("Message")) { //TODO
                return builtins.anyMessage();
            }

            contentTypes = typeSolver.getDefaultTypeArguments(
                baseTypeName.toNullable()
            );
        } else {
            contentTypes = typeHelper.unpackTuple(contentType);
        }

        return baseTypeName
            .__(typeSolver::getMessageTypeSchemaForTypeName)
            .<IJadescriptType>__(f -> f.create(contentTypes))
            .orElse(builtins.any("Could not resolve message type " +
                "for performative " + baseTypeName));
    }


    private @Nullable TypeArgument getAgentArgumentType(
        @NotNull Maybe<BuiltinHierarchicType> bhType
    ) {
        final TypeSolver typeSolver = module.get(TypeSolver.class);
        TypeArgument agentType = null;

        if (bhType.__(BuiltinHierarchicType::isFor).orElse(false)) {
            final Maybe<JvmTypeReference> agentArgumentRef =
                bhType.__(BuiltinHierarchicType::getArgumentAgentRef);
            if (agentArgumentRef.isPresent()) {
                agentType = typeSolver.fromJvmTypeReference(
                    agentArgumentRef.toNullable()
                );
            }
        }
        return agentType;
    }


    public IJadescriptType toJadescriptType(Maybe<TypeExpression> input) {
        final TypeSolver typeSolver = module.get(TypeSolver.class);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        if (input == null) {
            return builtins.nothing(
                "Input type expression was empty."
            );
        }
        if (input.__(TypeExpression::isAid).orElse(false)) {
            return builtins.aid();
        }
        if (input.__(TypeExpression::isBoolean).orElse(false)) {
            return builtins.boolean_();
        }
        if (input.__(TypeExpression::isReal).orElse(false)) {
            return builtins.real();
        }
        if (input.__(TypeExpression::isInteger).orElse(false)) {
            return builtins.integer();
        }
        if (input.__(TypeExpression::isDuration).orElse(false)) {
            return builtins.duration();
        }
        if (input.__(TypeExpression::isTimestamp).orElse(false)) {
            return builtins.timestamp();
        }
        if (input.__(TypeExpression::isText).orElse(false)) {
            return builtins.text();
        }
        if (input.__(TypeExpression::isPerformative)
            .orElse(false)) {
            return builtins.performative();
        }

        final MaybeList<TypeExpression> subExprs =
            input.__toListNullsRemoved(TypeExpression::getSubExprs);

        if (subExprs.size() == 1) {
            return toJadescriptType(subExprs.get(0));
        }
        if (subExprs.size() > 1) {
            List<TypeArgument> elementTypes = subExprs.stream()
                .map(this::toJadescriptType)
                .collect(Collectors.toList());
            return builtins.tuple(elementTypes);
        }
        final Maybe<BuiltinHierarchicType> hierarchicType =
            input.__(TypeExpression::getBuiltinHiearchic);

        if (hierarchicType.__(BuiltinHierarchicType::isAgent)
            .orElse(false)) {
            return builtins.agent();
        }
        if (hierarchicType.__(BuiltinHierarchicType::isOntology)
            .orElse(false)) {
            return builtins.ontology();
        }


        if (
            hierarchicType.__(BuiltinHierarchicType::isBaseBehaviour)
                .orElse(false)
        ) {
            final @Nullable TypeArgument agentArgumentType =
                getAgentArgumentType(hierarchicType);
            if (agentArgumentType == null) {
                return builtins.behaviour();
            } else {
                return builtins.behaviour(agentArgumentType);
            }
        } else if (
            hierarchicType.__(BuiltinHierarchicType::isCyclicBehaviour)
                .orElse(false)
        ) {
            final @Nullable TypeArgument agentArgumentType =
                getAgentArgumentType(hierarchicType);
            if (agentArgumentType == null) {
                return builtins.cyclicBehaviour();
            } else {
                return builtins.cyclicBehaviour(agentArgumentType);
            }
        } else if (
            hierarchicType.__(BuiltinHierarchicType::isOneshotBehaviour)
                .orElse(false)
        ) {
            final @Nullable TypeArgument agentArgumentType =
                getAgentArgumentType(hierarchicType);
            if (agentArgumentType == null) {
                return builtins.oneshotBehaviour();
            } else {
                return builtins.oneshotBehaviour(agentArgumentType);
            }
        }


        if (hierarchicType.__(BuiltinHierarchicType::isConcept)
            .orElse(false)) {
            return builtins.concept();
        }
        if (hierarchicType.__(BuiltinHierarchicType::isProposition)
            .orElse(false)) {
            return builtins.proposition();
        }
        if (hierarchicType.__(BuiltinHierarchicType::isPredicate)
            .orElse(false)) {
            return builtins.predicate();
        }
        if (hierarchicType.__(BuiltinHierarchicType::isAtomicProposition)
            .orElse(false)) {
            return builtins.atomicProposition();
        }
        if (hierarchicType.__(BuiltinHierarchicType::isAction)
            .orElse(false)) {
            return builtins.action();
        }
        if (hierarchicType.__(BuiltinHierarchicType::getMessageType)
            .isPresent()) {
            return getMessageType(hierarchicType
                .__(BuiltinHierarchicType::getMessageType));
        }

        final Maybe<CollectionTypeExpression> collectionType =
            input.__(TypeExpression::getCollectionTypeExpression);
        if (collectionType.isPresent()) {
            return getCollectionType(
                input,
                collectionType
            );

        }

        return input
            .__(TypeExpression::getJvmType)
            .__(typeSolver::fromJvmTypeReference)
            .__(TypeArgument::ignoreBound)
            .orElse(builtins.any(
                "Could not resolve type from type expression."));
    }


    private IJadescriptType getCollectionType(
        Maybe<TypeExpression> input,
        Maybe<CollectionTypeExpression> collectionType
    ) {
        BuiltinTypeProvider builtins = module.get(BuiltinTypeProvider.class);
        List<TypeArgument> typeParameters = someStream(
            collectionType.__(CollectionTypeExpression::getTypeParameters)
        ).map(this::toJadescriptType)
            .collect(Collectors.toList());

        String extract = collectionType
            .__(CollectionTypeExpression::getCollectionType)
            .orElse("");
        if (extract.equals("list")) {
            if (typeParameters.isEmpty()) {
                return builtins.list(builtins.any(
                    "Missing element type specification."
                ));
            }
            return builtins.list(typeParameters.get(0));
        }
        if (extract.equals("map")) {
            if (typeParameters.isEmpty()) {
                return builtins.map(
                    builtins.any("Missing key type specification."),
                    builtins.any("Missing value type specification.")
                );
            }
            if (typeParameters.size() < 2) {
                return builtins.map(
                    typeParameters.get(0),
                    builtins.any("Missing value type specification.")
                );
            }
            return builtins.map(
                typeParameters.get(0),
                typeParameters.get(1)
            );
        }
        if (extract.equals("set")) {
            if (typeParameters.isEmpty()) {
                return builtins.set(
                    builtins.any("Missing element type specification.")
                );
            }
            return builtins.set(typeParameters.get(0));
        }

        final TypeSolver typeSolver = module.get(TypeSolver.class);

        return input
            .__(TypeExpression::getJvmType)
            .__(typeSolver::fromJvmTypeReference)
            .__(TypeArgument::ignoreBound)
            .orElse(builtins.any(
                "Could not resolve type from type expression."));
    }


}
