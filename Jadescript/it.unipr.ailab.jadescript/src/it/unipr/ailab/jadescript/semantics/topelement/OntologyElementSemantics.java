package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.common.collect.HashMultimap;
import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.javaapi.NativeValueFactory;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.MethodInvocationSemantics;
import it.unipr.ailab.jadescript.semantics.Semantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.SyntheticExpression;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.OntoContentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ParametricType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.*;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 27/04/18.
 */
@Singleton
public class OntologyElementSemantics extends Semantics {


    public OntologyElementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);

    }


    public void validate(Maybe<ExtendingFeature> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        module.get(ContextManager.class).enterOntologyElementDeclaration();
        Maybe<String> ontoElementName = input.__(ExtendingFeature::getName);
        module.get(ValidationHelper.class).assertNotReservedName(
                ontoElementName,
                input,
                JadescriptPackage.eINSTANCE.getNamedFeature_Name(),
                acceptor
        );

        Maybe<? extends JvmParameterizedTypeReference> superType = input.__(ExtendingFeature::getSuperType);

        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);

        //if native, can only extend native
        //(isNative) ==> (supertype.isPresent) ==> (superType is content type) ==> (superType.isNative)
        if (input.__(ExtendingFeature::isNative).extract(nullAsFalse)
                && superType.isPresent()) {
            final IJadescriptType contentType = module.get(TypeHelper.class).jtFromJvmTypeRef(superType.toNullable());
            if (contentType instanceof OntoContentType) {
                module.get(ValidationHelper.class).assertion(
                        ((OntoContentType) contentType).isNativeOntoContentType(),
                        "InvalidExtendedType",
                        "A native type can only extend native types.",
                        superType,
                        acceptor
                );
            }
        }

        //if not native, can only extend not native
        //(!isNative) ==> (supertype.isPresent) ==> (superType is content type) ==> (!superType.isNative)
        if (!input.__(ExtendingFeature::isNative).extract(nullAsFalse)
                && superType.isPresent()) {
            final IJadescriptType contentType = module.get(TypeHelper.class).jtFromJvmTypeRef(superType.toNullable());
            if (contentType instanceof OntoContentType) {
                module.get(ValidationHelper.class).assertion(
                        !((OntoContentType) contentType).isNativeOntoContentType(),
                        "InvalidExtendedType",
                        "A non-native type can not extend native types.",
                        superType,
                        acceptor
                );
            }

        }

        if (input.isInstanceOf(it.unipr.ailab.jadescript.jadescript.Predicate.class)) {
            module.get(ValidationHelper.class).assertion(
                    Maybe.stream(input
                                    .__(i -> (Predicate) i)
                                    .__(FeatureWithSlots::getSlots))
                            .anyMatch(Maybe::isPresent),
                    "InvalidPredicateDeclaration",
                    "Predicates require at least a slot.",
                    input,
                    interceptAcceptor
            );
        }

        module.get(ValidationHelper.class).assertExpectedType(
                getBaseOntologyContentType(input),
                superType
                        .__((JvmParameterizedTypeReference st) -> (JvmTypeReference) st)
                        .__(st -> module.get(TypeHelper.class).jtFromJvmTypeRef(st))
                        .orElse(module.get(TypeHelper.class).ANY),
                "InvalidOntologyElementSupertype",
                superType,
                interceptAcceptor
        );

        if (!interceptAcceptor.thereAreErrors()) {
            //force Concepts to NOT extend Actions
            if (input.__(i -> i instanceof Concept).extract(nullAsFalse)) {
                superType.safeDo(superTypeSafe -> {
                    module.get(ValidationHelper.class).assertion(
                            !module.get(TypeHelper.class).isAssignable(jade.content.AgentAction.class, superTypeSafe),
                            "InvalidOntologyElementSupertype",
                            "concepts can not extend agent actions",
                            superType,
                            interceptAcceptor
                    );
                });
            }
        }


        final HashMap<String, IJadescriptType> slotTypeSet = new HashMap<>();
        final HashMap<String, Maybe<SlotDeclaration>> slotSet = new HashMap<>();
        InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);
        if (input.__(i -> i instanceof FeatureWithSlots).extract(nullAsFalse)) { //--> concepts, actions or predicates, NOT propositions
            Maybe<FeatureWithSlots> inputWithSlots = input.__(i -> (FeatureWithSlots) i);

            //Validation of each single slot, independently
            List<Maybe<SlotDeclaration>> slots = toListOfMaybes(inputWithSlots.__(FeatureWithSlots::getSlots));
            for (Maybe<SlotDeclaration> slot : slots) {
                validateSlotDeclaration(slot, subValidation);
            }

            if (!subValidation.thereAreErrors()) {

                //Validation of the set of declared slots
                checkDuplicateDeclaratedSlots(acceptor, inputWithSlots);


                for (Maybe<SlotDeclaration> slot : slots) {
                    Maybe<String> slotName = slot.__(SlotDeclaration::getName);
                    InterceptAcceptor typeValidation = new InterceptAcceptor(acceptor);
                    module.get(TypeExpressionSemantics.class).validate(slot.__(SlotDeclaration::getType), , typeValidation);
                    if (!typeValidation.thereAreErrors()) {
                        IJadescriptType slotType = slot.__(SlotDeclaration::getType)
                                .extract(module.get(TypeExpressionSemantics.class)::toJadescriptType);
                        slotName.safeDo(slotNameSafe -> {
                            slotTypeSet.put(slotNameSafe, slotType);
                            slotSet.put(slotNameSafe, slot);
                        });
                    }
                }
            }
        }


        //Validation of the compatibility of the set of the declared slots with the constraints
        //  imposed by the super schema
        if (!subValidation.thereAreErrors()) {
            Maybe<JvmType> superTypeDeclared = superType.__(JvmParameterizedTypeReference::getType);
            superTypeDeclared.safeDo(superTypeDeclaredSafe -> {
                if (superTypeDeclaredSafe instanceof JvmDeclaredType) {
                    JvmTypeNamespace superNamespace = new JvmTypeNamespace(module, (JvmDeclaredType) superTypeDeclaredSafe);
                    Map<String, IJadescriptType> superProperties = superNamespace.getPropertiesFromBiggestCtor();

                    HashMap<String, IJadescriptType> superSlotTypeSet = new HashMap<>();
                    HashMap<String, Integer> superSlotPositionSet = new HashMap<>();

                    boolean isWithSuperSlots = input.__(ExtendingFeature::isWithSuperSlots).extract(nullAsFalse);
                    if (isWithSuperSlots) {
                        Maybe<NamedArgumentList> superSlots = input.__(ExtendingFeature::getNamedSuperSlots);
                        List<Maybe<String>> argNames = toListOfMaybes(superSlots.__(NamedArgumentList::getParameterNames));
                        List<Maybe<RValueExpression>> args = toListOfMaybes(superSlots.__(NamedArgumentList::getParameterValues));

                        HashMap<String, IJadescriptType> superSlotsInitScope = new HashMap<>();
                        superSlotsInitScope.putAll(superProperties);
                        superSlotsInitScope.putAll(slotTypeSet); //matching names override super ones

                        for (int i = 0; i < Math.min(argNames.size(), args.size()); i++) {
                            int finalI = i;
                            Maybe<String> argName = argNames.get(i);


                            module.get(ValidationHelper.class).assertion(
                                    module.get(RValueExpressionSemantics.class).isAlwaysPure(args.get(i), ),
                                    "InvalidSuperSlotInitExpression",
                                    "Initialization expressions of super-slots must be pure (without side effects).",
                                    superSlots,
                                    JadescriptPackage.eINSTANCE.getNamedArgumentList_ParameterValues(),
                                    i,
                                    acceptor
                            );

                            //push a scope; validate the init expression, and compute the type of it
                            module.get(ContextManager.class).enterSuperSlotInitializer(superSlotsInitScope);
                            InterceptAcceptor subVal = new InterceptAcceptor(acceptor);

                            module.get(RValueExpressionSemantics.class).validate(args.get(i), , subVal);
                            IJadescriptType argType;
                            if (subVal.thereAreErrors()) {
                                argType = module.get(TypeHelper.class).ANY;
                            } else {
                                argType = args.get(i).extract(input1 -> module.get(RValueExpressionSemantics.class).inferType(input1, ));
                            }
                            module.get(ContextManager.class).exit();


                            //then populate the sets for later checks
                            argName.safeDo(argNameSafe -> {
                                superSlotTypeSet.put(argNameSafe, argType);
                                superSlotPositionSet.put(argNameSafe, finalI);
                            });

                        }
                    }


                    superProperties.forEach((propName, propType) -> {
                        boolean isInitialized = isWithSuperSlots && superSlotTypeSet.containsKey(propName);
                        boolean isRedeclared = slotTypeSet.containsKey(propName);

                        InterceptAcceptor consisentInheritedPropertyCheck = new InterceptAcceptor(acceptor);
                        //super-properties cannot be both initialized in the extends section
                        // and re-declared in the parameters section
                        module.get(ValidationHelper.class).assertion(
                                !(isInitialized && isRedeclared),
                                "InvalidSlotInheritance",
                                "Super-property '" + propName + "' cannot be initialized and redeclared at the same time",
                                slotSet.get(propName),
                                consisentInheritedPropertyCheck
                        );

                        if (superSlotPositionSet.containsKey(propName)) {
                            module.get(ValidationHelper.class).assertion(
                                    !(isInitialized && isRedeclared),
                                    "InvalidSlotInheritance",
                                    "Super-property '" + propName + "' cannot be initialized and redeclared at the same time",
                                    input.__(ExtendingFeature::getNamedSuperSlots),
                                    JadescriptPackage.eINSTANCE.getNamedArgumentList_ParameterNames(),
                                    superSlotPositionSet.get(propName),
                                    consisentInheritedPropertyCheck
                            );
                        }


                        if (!consisentInheritedPropertyCheck.thereAreErrors()) {
                            //super-properties must be either initialized in the extends section
                            // or re-declared in the parameters section
                            module.get(ValidationHelper.class).assertion(
                                    isInitialized || isRedeclared,
                                    "InvalidSlotInheritance",
                                    "Super-property '" + propName + "' must be either initialized or re-declared",
                                    input.__(ExtendingFeature::getSuperType),
                                    consisentInheritedPropertyCheck
                            );
                        }


                        if (!consisentInheritedPropertyCheck.thereAreErrors()) {
                            if (isInitialized) {
                                Maybe<NamedArgumentList> superSlots = input.__(ExtendingFeature::getNamedSuperSlots);
                                Maybe<RValueExpression> argExpression = toListOfMaybes(superSlots.__(NamedArgumentList::getParameterValues))
                                        .get(superSlotPositionSet.get(propName));
                                IJadescriptType argType = superSlotTypeSet.get(propName);

                                //initialization of super-properties in extends section must be type conformant
                                module.get(ValidationHelper.class).assertExpectedType(
                                        superProperties.getOrDefault(propName, module.get(TypeHelper.class).ANY),
                                        argType,
                                        "InvalidSlotInheritance",
                                        argExpression,
                                        consisentInheritedPropertyCheck
                                );

                            } else if (isRedeclared) {
                                //redeclaration of super-properties in parameters section must be type conformant
                                if (slotSet.containsKey(propName)) {
                                    module.get(ValidationHelper.class).assertExpectedType(
                                            superProperties.getOrDefault(propName, module.get(TypeHelper.class).ANY),
                                            slotTypeSet.get(propName),
                                            "InvalidSlotInheritance",
                                            slotSet.get(propName),
                                            consisentInheritedPropertyCheck
                                    );
                                }
                            }
                        }

                    });
                }
            });
        }
        module.get(ContextManager.class).exit();

    }

    public void validateSlotDeclaration(Maybe<SlotDeclaration> slotDeclaration, ValidationMessageAcceptor acceptor) {

        module.get(ValidationHelper.class).assertNotReservedName(
                slotDeclaration.__(SlotDeclaration::getName),
                slotDeclaration,
                JadescriptPackage.eINSTANCE.getSlotDeclaration_Name(),
                acceptor
        );

        // checks type expressions of slot declarations: they have to be valid for ontologies
        final Maybe<TypeExpression> slotTypeExpression = slotDeclaration.__(SlotDeclaration::getType);
        slotTypeExpression.safeDo(slotTypeExprSafe -> {

            final TypeExpressionSemantics typeExprSem = module.get(TypeExpressionSemantics.class);
            typeExprSem.validate(slotTypeExpression, , acceptor);
            IJadescriptType slotType = typeExprSem.toJadescriptType(slotTypeExpression);


            module.get(ValidationHelper.class).assertion(
                    !slotType.isCollection() ||
                            ((ParametricType) slotType).getTypeArguments().stream()
                                    .map(TypeArgument::ignoreBound)
                                    .noneMatch(IJadescriptType::isCollection),
                    "InvalidSlotType",
                    "Collections of collections are not supported as slot types.",
                    slotTypeExpression,
                    acceptor

            );
        });

    }


    private void checkDuplicateDeclaratedSlots(ValidationMessageAcceptor acceptor, Maybe<FeatureWithSlots> inputWithSlots) {
        //checks duplicates between slots
        HashMultimap<String, SlotDeclaration> multiMap = HashMultimap.create();
        for (Maybe<SlotDeclaration> slot : iterate(inputWithSlots.__(FeatureWithSlots::getSlots))) {
            safeDo(slot, slot.__(SlotDeclaration::getName),
                    /*NULLSAFE REGION*/(slotSafe, slotNameSafe) -> {
                        //this portion of code is done  only if slot and
                        // are != null (and everything in the dotchains that generated them is !=null too)

                        multiMap.put(slotNameSafe, slotSafe);

                    }/*END NULLSAFE REGION - (slotSafe, slotNameSafe)*/
            );
        }

        for (Map.Entry<String, Collection<SlotDeclaration>> entry : multiMap.asMap().entrySet()) {
            Collection<SlotDeclaration> duplicates = entry.getValue();
            if (duplicates.size() > 1) {
                for (SlotDeclaration d : duplicates) {
                    acceptor.acceptError(
                            "Duplicate slot '" + entry.getKey() + "' in ' "
                                    + inputWithSlots.__(ExtendingFeature::getName).extract(nullAsEmptyString) + "''",
                            d,
                            JadescriptPackage.eINSTANCE.getSlotDeclaration_Name(),
                            ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                            ISSUE_DUPLICATE_ELEMENT
                    );
                }
            }
        }
    }

    public List<JvmDeclaredType> declareTypes(
            Maybe<ExtendingFeature> input,
            Maybe<QualifiedName> ontoFullQualifiedName,
            boolean isPreIndexingPhase
    ) {
        return input.toList().stream().flatMap(inputSafe -> {
            if (input.__(ExtendingFeature::isNative).extract(nullAsFalse)) {
                return generateNativeTypes(input, ontoFullQualifiedName, isPreIndexingPhase, inputSafe);
            } else {
                return generateConcreteType(input, ontoFullQualifiedName, isPreIndexingPhase, inputSafe);
            }
        }).collect(Collectors.toList());
    }

    private Stream<JvmDeclaredType> generateNativeTypes(
            Maybe<ExtendingFeature> input,
            Maybe<QualifiedName> ontoFullQualifiedName,
            boolean isPreindexingPhase,
            ExtendingFeature inputSafe
    ) {
        final QualifiedName fqName = module.get(CompilationHelper.class).getFullyQualifiedName(inputSafe);
        return Stream.of(module.get(JvmTypesBuilder.class).toClass(
                inputSafe,
                fqName != null ? fqName.toString() : null,
                (JvmGenericType it) -> {
                    it.setAbstract(true);
                    module.get(ContextManager.class).enterOntologyElementDeclaration();
                    if (!isPreindexingPhase) {
                        List<Maybe<RValueExpression>> superArguments = new ArrayList<>();
                        List<IJadescriptType> superDestTypes = new ArrayList<>();

                        Maybe<? extends JvmParameterizedTypeReference> superType = input
                                .__(ExtendingFeature::getSuperType);

                        if (superType.isNothing()) {
                            it.getSuperTypes().add(module.get(TypeHelper.class)
                                    .typeRef(getBaseOntologyContentType(input)));
                        }

                        prepareSuperTypeInitialization(input, it, superArguments, superDestTypes, superType);

                        if (input.__(i -> i instanceof FeatureWithSlots).extract(nullAsFalse)) {

                            for (Maybe<SlotDeclaration> slot : iterate(input.__(i -> (FeatureWithSlots) i)
                                    .__(FeatureWithSlots::getSlots))) {
                                addNativeProperty(it.getMembers(), slot);
                            }
                        }

                        addToString(it.getMembers(), input);

                        addEquals(it.getMembers(), input);

                        addOntologyElementsConstructor(it.getMembers(), input, superArguments, superDestTypes);

                        it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
                                inputSafe,
                                "__isNative",
                                module.get(TypeHelper.class).typeRef(Void.TYPE),
                                itMethod -> {
                                    itMethod.setDefault(true);
                                    itMethod.setAbstract(false);
                                    module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                                        scb.line("// Method used as metadata flag by the Jadescript compiler.");
                                    });
                                }

                        ));

                        addGetDeclaringOntology(it.getMembers(), input, ontoFullQualifiedName, true);

                        addMetadataMethod(it.getMembers(), ontoFullQualifiedName, input, true);

                    }
                    module.get(ContextManager.class).exit();
                }
        ), module.get(JvmTypesBuilder.class).toInterface(
                inputSafe,
                fqName != null ? (fqName + "Factory") : null,
                it -> {
                    final JvmTypeReference conceptTypeRef = module.get(TypeHelper.class)
                            .typeRef(fqName != null ? fqName.toString() : null);
                    it.getSuperTypes().add(module.get(TypeHelper.class).typeRef(
                            NativeValueFactory.class
                    ));

                    it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
                            inputSafe,
                            "getImplementationClass",
                            module.get(TypeHelper.class).typeRef(
                                    "Class<? extends " + conceptTypeRef.getQualifiedName('.') + ">"
                            ),
                            itMethod -> {
                                itMethod.setDefault(false);
                                itMethod.setAbstract(true);

                            }
                    ));

                    it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
                            inputSafe,
                            "empty",
                            conceptTypeRef,
                            itMethod -> {
                                itMethod.setDefault(false);
                                itMethod.setAbstract(true);
                            }
                    ));


                    if (input.__(i -> i instanceof FeatureWithSlots).extract(nullAsFalse)) {
                        Maybe<FeatureWithSlots> inputWithSlots = input.__(i -> (FeatureWithSlots) i);
                        Maybe<EList<SlotDeclaration>> slots = inputWithSlots.__(FeatureWithSlots::getSlots);

                        it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
                                inputSafe,
                                "create",
                                conceptTypeRef,
                                itMethod -> {
                                    for (Maybe<SlotDeclaration> slot : iterate(slots)) {


                                        IJadescriptType slotType = module.get(TypeExpressionSemantics.class)
                                                .toJadescriptType(slot.__(SlotDeclaration::getType));

                                        Maybe<String> slotName = slot.__(SlotDeclaration::getName);
                                        safeDo(slot, slotName,
                                                /*NULLSAFE REGION*/(slotSafe, slotNameSafe) -> {
                                                    //this portion of code is done  only if slot and slotName
                                                    // are != null (and everything in the dotchains that generated them is !=null too)

                                                    itMethod.getParameters().add(module.get(JvmTypesBuilder.class)
                                                            .toParameter(
                                                                    slotSafe,
                                                                    slotNameSafe,
                                                                    slotType.asJvmTypeReference()
                                                            ));

                                                }/*END NULLSAFE REGION - (slotSafe, slotNameSafe)*/
                                        );


                                    }
                                    itMethod.setDefault(false);
                                    itMethod.setAbstract(true);
                                }
                        ));
                    }
                }
        ));
    }

    private Stream<JvmDeclaredType> generateConcreteType(
            Maybe<ExtendingFeature> input,
            Maybe<QualifiedName> ontoFullQualifiedName,
            boolean isPreIndexingPhase,
            ExtendingFeature inputSafe
    ) {
        return Stream.of(module.get(JvmTypesBuilder.class).toClass(
                inputSafe,
                module.get(CompilationHelper.class).getFullyQualifiedName(inputSafe),
                it -> {
                    module.get(ContextManager.class).enterOntologyElementDeclaration();
                    if (!isPreIndexingPhase) {
                        List<Maybe<RValueExpression>> superArguments = new ArrayList<>();
                        List<IJadescriptType> superDestTypes = new ArrayList<>();

                        Maybe<? extends JvmParameterizedTypeReference> superType =
                                input.__(ExtendingFeature::getSuperType);

                        if (superType.isNothing()) {
                            it.getSuperTypes().add(module.get(TypeHelper.class)
                                    .typeRef(getBaseOntologyContentType(input)));
                        }

                        prepareSuperTypeInitialization(input, it, superArguments, superDestTypes, superType);


                        if (input.__(i -> i instanceof FeatureWithSlots).extract(nullAsFalse)) {

                            for (Maybe<SlotDeclaration> slot : iterate(input.__(i -> (FeatureWithSlots) i)
                                    .__(FeatureWithSlots::getSlots))) {
                                addProperty(it.getMembers(), slot);
                            }
                        }


                        addToString(it.getMembers(), input);

                        addEquals(it.getMembers(), input);

                        addOntologyElementsConstructor(it.getMembers(), input, superArguments, superDestTypes);

                        addGetDeclaringOntology(it.getMembers(), input, ontoFullQualifiedName, false);

                        addMetadataMethod(it.getMembers(), ontoFullQualifiedName, input, false);
                    }
                    module.get(ContextManager.class).exit();
                }
        ));
    }

    private void prepareSuperTypeInitialization(
            Maybe<ExtendingFeature> input,
            JvmGenericType it,
            List<Maybe<RValueExpression>> superArguments,
            List<IJadescriptType> superDestTypes,
            Maybe<? extends JvmParameterizedTypeReference> superType
    ) {
        superType.safeDo(superTypeSafe -> {
            it.getSuperTypes().add(module.get(JvmTypesBuilder.class).cloneWithProxies(superTypeSafe));

            if (superTypeSafe.getType() instanceof JvmDeclaredType) {

                JvmDeclaredType superTypeDeclaredSafe = (JvmDeclaredType) superTypeSafe.getType();
                final HashMap<String, IJadescriptType> slotTypeSet = new HashMap<>();


                if (input.__(i -> i instanceof FeatureWithSlots).extract(nullAsFalse)) {
                    for (Maybe<SlotDeclaration> slot : iterate(input.__(i -> (FeatureWithSlots) i).__(FeatureWithSlots::getSlots))) {
                        Maybe<String> slotName = slot.__(SlotDeclaration::getName);
                        IJadescriptType slotType = slot.__(SlotDeclaration::getType).extract(module.get(TypeExpressionSemantics.class)::toJadescriptType);
                        slotName.safeDo(slotNameSafe -> {
                            slotTypeSet.put(slotNameSafe, slotType);
                        });
                    }
                }


                final JvmTypeNamespace superTypeNamespace = new JvmTypeNamespace(module, superTypeDeclaredSafe);
                Map<String, IJadescriptType> superProperties = superTypeNamespace
                        .getPropertiesFromBiggestCtor();

                HashMap<String, IJadescriptType> superSlotTypeSet = new HashMap<>();
                HashMap<String, Integer> superSlotPositionSet = new HashMap<>();

                boolean isWithSuperSlots = input.__(ExtendingFeature::isWithSuperSlots).extract(nullAsFalse);
                List<Maybe<RValueExpression>> args = new ArrayList<>();
                if (isWithSuperSlots) {
                    Maybe<NamedArgumentList> superSlots = input.__(ExtendingFeature::getNamedSuperSlots);
                    List<Maybe<String>> argNames = toListOfMaybes(superSlots.__(NamedArgumentList::getParameterNames));
                    args.addAll(toListOfMaybes(superSlots.__(NamedArgumentList::getParameterValues)));

                    HashMap<String, IJadescriptType> superSlotsInitScope = new HashMap<>();
                    superSlotsInitScope.putAll(superProperties);
                    superSlotsInitScope.putAll(slotTypeSet); //matching names override super ones

                    for (int i = 0; i < Math.min(argNames.size(), args.size()); i++) {
                        int finalI = i;
                        Maybe<String> argName = argNames.get(i);

                        //push a scope; compute the type of the init expression
                        module.get(ContextManager.class).enterSuperSlotInitializer(superSlotsInitScope);
                        IJadescriptType argType = args.get(i)
                                .extract(input1 -> module.get(RValueExpressionSemantics.class).inferType(input1, ));
                        module.get(ContextManager.class).exit();

                        //then populate the sets for later
                        argName.safeDo(argNameSafe -> {
                            superSlotTypeSet.put(argNameSafe, argType);
                            superSlotPositionSet.put(argNameSafe, finalI);
                        });

                    }


                }

                List<String> ctorArgNames = new ArrayList<>();
                List<Maybe<RValueExpression>> ctorArgs = new ArrayList<>();

                superProperties.forEach((propName, propType) -> {
                    //assuming isInitialized XOR isRedeclared (checked by validator):
                    ctorArgNames.add(propName);

                    if (slotTypeSet.containsKey(propName)) {//redeclared
                        ctorArgs.add(of(new SyntheticExpression(new SyntheticExpression.SemanticsMethods() {
                            @Override
                            public String compile() {
                                return propName;
                            }
                        })));
                    } else if (isWithSuperSlots && superSlotTypeSet.containsKey(propName)) {//initialized
                        ctorArgs.add(args.get(superSlotPositionSet.get(propName)));
                    }
                });


                superTypeNamespace.getBiggestCtor()
                        .ifPresent(c -> {
                            superDestTypes.addAll(c.parameterTypes());
                            superArguments.addAll(MethodInvocationSemantics.sortToMatchParamNames(
                                    ctorArgs,
                                    ctorArgNames,
                                    c.parameterNames()
                            ));
                        });

            }

        });
    }

    private void addMetadataMethod(
            EList<JvmMember> members,
            Maybe<QualifiedName> ontoFullQualifiedName,
            Maybe<ExtendingFeature> input,
            boolean asDefault
    ) {
        safeDo(input, ontoFullQualifiedName, (inputSafe, ontoFullQualifiedNameSafe) -> {
            String typeFullyQualifiedName = input
                    .__(module.get(CompilationHelper.class)::getFullyQualifiedName)
                    .__(fqn -> fqn.toString("."))
                    .or(input.__(NamedFeature::getName))
                    .orElse("")
                    .replaceAll("\\.", "_");
            members.add(module.get(JvmTypesBuilder.class).toMethod(
                    inputSafe,
                    "__metadata_" + typeFullyQualifiedName,
                    module.get(TypeHelper.class).typeRef(ontoFullQualifiedNameSafe.toString(".")),
                    itMethod -> {
                        itMethod.setDefault(asDefault);
                        if (!asDefault) {
                            itMethod.setVisibility(JvmVisibility.PRIVATE);
                        }
                        if (inputSafe instanceof FeatureWithSlots) {
                            for (Maybe<SlotDeclaration> slot : toListOfMaybes(of(((FeatureWithSlots) inputSafe).getSlots()))) {
                                slot.__(SlotDeclaration::getName).safeDo(slotNameSafe -> {
                                    final JvmTypeReference typeRef = module.get(TypeExpressionSemantics.class)
                                            .toJadescriptType(slot.__(SlotDeclaration::getType)).asJvmTypeReference();
                                    itMethod.getParameters().add(module.get(JvmTypesBuilder.class)
                                            .toParameter(inputSafe, slotNameSafe, typeRef));
                                });
                            }
                        }
                        module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                            scb.line("return null;");
                        });
                    }
            ));
        });
    }

    private void addEquals(EList<JvmMember> members, Maybe<ExtendingFeature> input) {
        input.safeDo(inputSafe -> {
            members.add(module.get(JvmTypesBuilder.class).toMethod(
                    inputSafe,
                    "equals",
                    module.get(TypeHelper.class).typeRef(Boolean.TYPE),
                    itMethod -> {
                        itMethod.setVisibility(JvmVisibility.PUBLIC);
                        itMethod.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                inputSafe,
                                "obj",
                                module.get(TypeHelper.class).typeRef(Object.class)
                        ));
                        module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                            scb.line("if(obj instanceof " + inputSafe.getName() + ") {");
                            scb.indent().line(inputSafe.getName() + " o = (" + inputSafe.getName() + ") obj;");
                            scb.add("return ");
                            if (input.__(ExtendingFeature::getSuperType).isPresent()) {
                                scb.add("super.equals(obj)");
                            } else {
                                scb.add("true");
                            }
                            if (inputSafe instanceof FeatureWithSlots) {
                                for (Maybe<SlotDeclaration> slot : iterate(of((FeatureWithSlots) inputSafe)
                                        .__(FeatureWithSlots::getSlots))) {
                                    scb.add(" && ");
                                    scb.add("java.util.Objects.equals(");
                                    final String s = Strings.toFirstUpper(slot.__(SlotDeclaration::getName).orElse(""));
                                    scb.add("this.get")
                                            .add(s).add("(), o.get")
                                            .add(s).add("())");
                                }
                            }
                            scb.line(";");
                            scb.dedent().line("} else {");
                            scb.indent().line("return super.equals(obj);");
                            scb.dedent().line("}");
                        });
                    }
            ));
        });
    }

    private void addGetDeclaringOntology(
            EList<JvmMember> members,
            Maybe<ExtendingFeature> input,
            Maybe<QualifiedName> ontoFQName,
            boolean asDefault
    ) {
        safeDo(input, ontoFQName,
                /*NULLSAFE REGION*/(inputSafe, ontoFQNameSafe) -> {
                    //this portion of code is done only if input and ontoFullQualifiedName
                    // are != null
                    final JvmTypeReference ontoTypeRef = module.get(TypeHelper.class).typeRef(ontoFQNameSafe.toString());

                    members.add(module.get(JvmTypesBuilder.class).toMethod(
                            inputSafe,
                            "__getDeclaringOntology",
                            module.get(TypeHelper.class).typeRef(jade.content.onto.Ontology.class),
                            it -> {
                                it.setDefault(asDefault);

                                module.get(CompilationHelper.class).createAndSetBody(it, scb -> {
                                    scb.add("return ").add(ontoTypeRef.getQualifiedName('.')).add(".getInstance();");
                                });
                            }
                    ));


                }/*END NULLSAFE REGION - (inputSafe, ontoFullQualifiedNameSafe)*/
        );

    }


    /**
     * Generates the ctor of the concept POJO class
     */
    private void addOntologyElementsConstructor(
            EList<JvmMember> members,
            Maybe<ExtendingFeature> input,
            List<Maybe<RValueExpression>> superArguments,
            List<IJadescriptType> superDestTypes
    ) {
        input.safeDo(inputSafe -> {

            // Constructor without parameters
            members.add(module.get(JvmTypesBuilder.class).toConstructor(inputSafe, it -> {

                module.get(CompilationHelper.class).createAndSetBody(it, scb -> {
                    BlockWriter bw = new BlockWriter();
                    if (input.__(i -> i instanceof FeatureWithSlots).extract(nullAsFalse)) {
                        Maybe<FeatureWithSlots> inputWithSlots = input.__(i -> (FeatureWithSlots) i);
                        for (Maybe<SlotDeclaration> slot : iterate(inputWithSlots.__(FeatureWithSlots::getSlots))) {
                            Maybe<String> slotName = slot.__(SlotDeclaration::getName);
                            Maybe<TypeExpression> slotType = slot.__(SlotDeclaration::getType);
                            slotName.safeDo(
                                    /*NULLSAFE REGION*/(slotNameSafe) -> {
                                        //this portion of code is done  only if slotName
                                        // is != null (and everything in the dotchains that generated it
                                        // is !=null too)

                                        bw.addStatement(
                                                w.callStmnt(
                                                        "this.set" + Strings.toFirstUpper(slotNameSafe),
                                                        w.expr(module.get(CompilationHelper.class)
                                                                .compileEmptyConstructorCall(slotType))
                                                )
                                        );

                                    }/*END NULLSAFE REGION - (slotNameSafe)*/
                            );
                        }
                    }
                    bw.writeSonnet(scb);
                });
            }));


            if (input.__(i -> i instanceof FeatureWithSlots).extract(nullAsFalse)) {
                Maybe<FeatureWithSlots> inputWithSlots = input.__(i -> (FeatureWithSlots) i);
                Maybe<EList<SlotDeclaration>> slots = inputWithSlots.__(FeatureWithSlots::getSlots);
                if (slots.__(List::isEmpty).__(not).extract(nullAsFalse)) {


                    // Constructor with parameters
                    members.add(module.get(JvmTypesBuilder.class).toConstructor(inputSafe, it -> {


                        for (Maybe<SlotDeclaration> slot : iterate(slots)) {


                            IJadescriptType slotType = module.get(TypeExpressionSemantics.class).toJadescriptType(
                                    slot.__(SlotDeclaration::getType));

                            Maybe<String> slotName = slot.__(SlotDeclaration::getName);
                            safeDo(slot, slotName,
                                    /*NULLSAFE REGION*/(slotSafe, slotNameSafe) -> {
                                        //this portion of code is done  only if slot and slotName
                                        // are != null (and everything in the dotchains that generated them is !=null too)

                                        it.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                                slotSafe,
                                                slotNameSafe,
                                                slotType.asJvmTypeReference()
                                        ));

                                    }/*END NULLSAFE REGION - (slotSafe, slotNameSafe)*/
                            );


                        }


                        module.get(CompilationHelper.class).createAndSetBody(it, scb -> {

                            StringBuilder superArgumentsCompiled = new StringBuilder();
                            for (int i = 0; i < superArguments.size(); i++) {
                                if (i != 0) {
                                    superArgumentsCompiled.append(", ");
                                }
                                Maybe<RValueExpression> superArgument = superArguments.get(i);
                                IJadescriptType nullable = null;
                                if (i < superDestTypes.size()) {
                                    nullable = superDestTypes.get(i);
                                }
                                String lambdaCompiled = module.get(CompilationHelper.class)
                                        .compileRValueAsLambdaSupplier(
                                                superArgument,
                                                nullable
                                        );
                                superArgumentsCompiled.append(lambdaCompiled);
                            }


                            w.simpleStmt("super(" + superArgumentsCompiled + ")").writeSonnet(scb);


                            scb.line();
                            for (Maybe<SlotDeclaration> slot : iterate(slots)) {
                                slot.__(SlotDeclaration::getName).safeDo(slotNameSafe -> {
                                    scb.line("this.set" + Strings.toFirstUpper(slotNameSafe) + "(" + slotNameSafe + ");");
                                });
                            }
                        });
                    }));
                }
            }
        });
    }

    /**
     * Generates a new property including a private field with corresponding accessors
     * in the concept POJO class
     */
    private void addProperty(EList<JvmMember> members, Maybe<SlotDeclaration> slot) {
        IJadescriptType slotType = module.get(TypeExpressionSemantics.class).toJadescriptType(slot.__(SlotDeclaration::getType));
        Maybe<String> slotName = slot.__(SlotDeclaration::getName);

        safeDo(slot, slotName,
                /*NULLSAFE REGION*/(slotSafe, slotNameSafe) -> {
                    //this portion of code is done only if slot and slotName
                    // are != null (and everything in the dotchains that generated them is !=null too)

                    members.add(module.get(JvmTypesBuilder.class).toField(
                            slotSafe,
                            slotNameSafe,
                            slotType.asJvmTypeReference(),
                            it -> {
                                it.setVisibility(JvmVisibility.PRIVATE);
                            }
                    ));

                    members.add(module.get(JvmTypesBuilder.class).toGetter(
                            slotSafe,
                            slotNameSafe,
                            slotType.asJvmTypeReference()
                    ));
                    members.add(module.get(JvmTypesBuilder.class).toSetter(
                            slotSafe,
                            slotNameSafe,
                            slotType.asJvmTypeReference()
                    ));

                }/*END NULLSAFE REGION - (slotSafe, slotNameSafe)*/
        );


    }

    /**
     * Generates a new property for a native type
     */
    private void addNativeProperty(EList<JvmMember> members, Maybe<SlotDeclaration> slot) {
        IJadescriptType slotType = module.get(TypeExpressionSemantics.class)
                .toJadescriptType(slot.__(SlotDeclaration::getType));
        Maybe<String> slotName = slot.__(SlotDeclaration::getName);

        safeDo(slot, slotName,
                /*NULLSAFE REGION*/(slotSafe, slotNameSafe) -> {
                    //this portion of code is done only if slot and slotName
                    // are != null (and everything in the dotchains that generated them is !=null too)

                    members.add(module.get(JvmTypesBuilder.class).toMethod(
                            slotSafe,
                            "get" + Strings.toFirstUpper(slotNameSafe),
                            slotType.asJvmTypeReference(),
                            it -> {
                                it.setDefault(false);
                                it.setAbstract(true);
                            }
                    ));
                    members.add(module.get(JvmTypesBuilder.class).toMethod(
                            slotSafe,
                            "set" + Strings.toFirstUpper(slotNameSafe),
                            module.get(TypeHelper.class).typeRef(Void.TYPE),
                            it -> {
                                it.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                        slotSafe,
                                        "_value",
                                        slotType.asJvmTypeReference()
                                ));
                                it.setDefault(false);
                                it.setAbstract(true);
                            }
                    ));


                }/*END NULLSAFE REGION - (slotSafe, slotNameSafe)*/
        );


    }

    private void addToString(EList<JvmMember> members, Maybe<ExtendingFeature> feature) {
        feature.safeDo(extendingFeature -> {
            members.add(module.get(JvmTypesBuilder.class).toMethod(
                    extendingFeature,
                    "toString",
                    module.get(TypeHelper.class).TEXT.asJvmTypeReference(),
                    it -> {
                        it.setVisibility(JvmVisibility.PUBLIC);

                        module.get(CompilationHelper.class).createAndSetBody(it, scb -> {
                            w.variable("java.lang.StringBuilder", "_sb", w.callExpr("new java.lang.StringBuilder"))
                                    .writeSonnet(scb);

                            w.callStmnt("_sb.append", w.stringLiteral(module.get(CompilationHelper.class).getFullyQualifiedName(extendingFeature).toString()))
                                    .writeSonnet(scb);

                            if (extendingFeature instanceof FeatureWithSlots) {
                                FeatureWithSlots featureWithSlots = (FeatureWithSlots) extendingFeature;
                                EList<SlotDeclaration> slots = featureWithSlots.getSlots();
                                if (!slots.isEmpty()) {
                                    w.callStmnt("_sb.append", w.stringLiteral("("))
                                            .writeSonnet(scb);

                                    for (int i = 0; i < slots.size(); i++) {
                                        SlotDeclaration slot = slots.get(i);
                                        if (slot != null && slot.getName() != null) {
                                            if (i != 0) {
                                                w.callStmnt("_sb.append", w.stringLiteral(", "))
                                                        .writeSonnet(scb);
                                            }

                                            IJadescriptType type = module.get(TypeExpressionSemantics.class).toJadescriptType(of(slot.getType()));
                                            final String getterCall = "get" + Strings.toFirstUpper(slot.getName()) + "()";
                                            if (module.get(TypeHelper.class).TEXT.isAssignableFrom(type)) {
                                                w.callStmnt(
                                                                "_sb.append",
                                                                w.expr("\"\\\"\" + java.lang.String.valueOf(" + getterCall + ") + \"\\\"\"")
                                                        )
                                                        .writeSonnet(scb);
                                            } else {
                                                w.callStmnt("_sb.append", w.expr("java.lang.String.valueOf(" + getterCall + ")"))
                                                        .writeSonnet(scb);
                                            }

                                        }
                                    }

                                    w.callStmnt("_sb.append", w.stringLiteral(")"))
                                            .writeSonnet(scb);
                                }
                            }

                            w.returnStmnt(w.callExpr("_sb.toString"))
                                    .writeSonnet(scb);
                        });
                    }
            ));
        });
    }

    private Class<?> getBaseOntologyContentType(Maybe<ExtendingFeature> feature) {
        return feature.__(extendingFeature -> {
            if (extendingFeature instanceof Concept) {
                return jadescript.content.JadescriptConcept.class;
            } else if (extendingFeature instanceof Predicate) {
                return jadescript.content.JadescriptPredicate.class;
            } else if (extendingFeature instanceof Proposition) {
                return jadescript.content.JadescriptAtomicProposition.class;
            } else if (extendingFeature instanceof OntologyAction) {
                return jadescript.content.JadescriptAction.class;
            } else {
                return jadescript.content.JadescriptProposition.class;
            }
        }).orElse(jadescript.content.JadescriptProposition.class); // is the most generic
    }
}
