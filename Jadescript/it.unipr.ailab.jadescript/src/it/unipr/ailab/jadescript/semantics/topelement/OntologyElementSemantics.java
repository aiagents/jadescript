package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.common.collect.HashMultimap;
import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.CallSemantics;
import it.unipr.ailab.jadescript.semantics.Semantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
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
import it.unipr.ailab.maybe.MaybeList;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import jadescript.java.NativeValueFactory;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.*;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.jetbrains.annotations.Nullable;

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


    public void validate(
        Maybe<ExtendingFeature> input,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return;
        if (input.isNothing()) {
            return;
        }


        module.get(ContextManager.class).enterOntologyElementDeclaration();

        Maybe<String> ontoElementName = input.__(ExtendingFeature::getName);

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        validationHelper.assertNotReservedName(
            ontoElementName,
            input,
            JadescriptPackage.eINSTANCE.getNamedFeature_Name(),
            acceptor
        );

        Maybe<? extends JvmParameterizedTypeReference> superTypeExpr =
            input.__(ExtendingFeature::getSuperType);

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        //if it is native...
        if (input.__(ExtendingFeature::isNative).extract(nullAsFalse)
            // ... and it has an explicit supertype ...
            && superTypeExpr.isPresent()) {
            final IJadescriptType superType =
                typeHelper.jtFromJvmTypeRef(
                    superTypeExpr.toNullable());

            // ... and the supertype is a OntoContentType
            //     (added just for cast safety) ...
            if (superType instanceof OntoContentType) {

                // ... then the super type has to be native
                validationHelper.asserting(
                    ((OntoContentType) superType).isNativeOntoContentType(),
                    "InvalidExtendedType",
                    "A native type can only extend native types.",
                    superTypeExpr,
                    acceptor
                );
            }
        }

        //if it is NOT native ...
        if (!input.__(ExtendingFeature::isNative).extract(nullAsFalse)
            // ... and it has an explicit supertype ...
            && superTypeExpr.isPresent()) {
            final IJadescriptType superType =
                typeHelper.jtFromJvmTypeRef(
                    superTypeExpr.toNullable());

            // ... and the supertype is a OntoContentType
            //     (added just for cast safety) ...
            if (superType instanceof OntoContentType) {

                // ... then the super type must not be native
                validationHelper.asserting(
                    !((OntoContentType) superType).isNativeOntoContentType(),
                    "InvalidExtendedType",
                    "A non-native type can not extend native types.",
                    superTypeExpr,
                    acceptor
                );
            }

        }


        // if it is a 'predicate' declaration ...
        if (input.isInstanceOf(
            it.unipr.ailab.jadescript.jadescript.Predicate.class
        )) {
            // then at least a slot is required
            validationHelper.asserting(
                Maybe.someStream(input
                        .__(i -> (Predicate) i)
                        .__(FeatureWithSlots::getSlots))
                    .anyMatch(Maybe::isPresent),
                "InvalidPredicateDeclaration",
                "Predicates require at least a slot.",
                input,
                acceptor
            );
        }

        // ensures that the supertype extends the basic type for that
        // kind of declaration ('concept', 'action' etc...)
        boolean superTypeCheck = validationHelper.assertExpectedType(
            getBaseOntologyContentType(input),
            superTypeExpr
                .__(st -> (JvmTypeReference) st)
                .__(typeHelper::jtFromJvmTypeRef)
                .orElse(typeHelper.ANY),
            "InvalidOntologyElementSupertype",
            superTypeExpr,
            acceptor
        );

        if (superTypeCheck == VALID) {
            // if it is a concept ...
            if (input.__(i -> i instanceof Concept).extract(nullAsFalse)
                && superTypeExpr.isPresent()) {
                final JvmParameterizedTypeReference superTypeSafe =
                    superTypeExpr.toNullable();
                // ... it cannot extend an action
                //     (the previous check fails to detect this because in JADE
                //      AgentAction extends Concept)
                validationHelper.asserting(
                    !typeHelper.isAssignable(
                        jade.content.AgentAction.class,
                        superTypeSafe
                    ),
                    "InvalidOntologyElementSupertype",
                    "concepts can not extend agent actions",
                    superTypeExpr,
                    acceptor
                );
            }
        }


        final HashMap<String, IJadescriptType> slotTypeSet = new HashMap<>();
        final HashMap<String, Maybe<SlotDeclaration>> slotSet = new HashMap<>();


        final Boolean hasSlots = input.__(i -> i instanceof FeatureWithSlots)
            .extract(nullAsFalse);

        boolean allSlotsCheck = VALID;
        if (hasSlots) {
            Maybe<FeatureWithSlots> inputWithSlots =
                input.__(i -> (FeatureWithSlots) i);

            //Validation of each single slot, independently
            MaybeList<SlotDeclaration> slots =
                inputWithSlots.__toList(FeatureWithSlots::getSlots);

            TypeExpressionSemantics tes =
                module.get(TypeExpressionSemantics.class);

            for (Maybe<SlotDeclaration> slot : slots) {
                boolean slotCheck = validateSlotDeclaration(
                    slot,
                    tes,
                    acceptor
                );
                allSlotsCheck = allSlotsCheck && slotCheck;
            }

            if (allSlotsCheck == VALID) {
                //Validation of the set of declared slots
                checkDuplicateDeclaredSlots(acceptor, inputWithSlots);

                for (Maybe<SlotDeclaration> slot : slots) {
                    Maybe<String> slotName =
                        slot.__(SlotDeclaration::getName);
                    boolean slotTypeCheck = tes.validate(
                        slot.__(SlotDeclaration::getType),
                        acceptor
                    );
                    if (slotTypeCheck == VALID) {
                        IJadescriptType slotType = tes.toJadescriptType(
                            slot.__(SlotDeclaration::getType)
                        );

                        slotName.safeDo(slotNameSafe -> {
                            slotTypeSet.put(slotNameSafe, slotType);
                            slotSet.put(slotNameSafe, slot);
                        });
                    }
                }
            }
        }


        //Validation of the compatibility of the set of the declared slots
        // with the constraints imposed by the super schema
        if (allSlotsCheck == VALID) {
            validateSlotsCompatibility(
                input,
                superTypeExpr, slotTypeSet, slotSet, acceptor
            );
        }

        module.get(ContextManager.class).exit();

    }


    private void validateSlotsCompatibility(
        Maybe<ExtendingFeature> input,
        Maybe<? extends JvmParameterizedTypeReference> superTypeExpr,
        HashMap<String, IJadescriptType> slotTypeSet,
        HashMap<String, Maybe<SlotDeclaration>> slotSet,
        ValidationMessageAcceptor acceptor
    ) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);


        Maybe<JvmType> superTypeDeclared = superTypeExpr
            .__(JvmParameterizedTypeReference::getType);


        if (superTypeDeclared.isNothing()) {
            return;
        }

        final JvmType superTypeDeclaredSafe = superTypeDeclared.toNullable();

        if (!(superTypeDeclaredSafe instanceof JvmDeclaredType)) {
            return;
        }


        JvmTypeNamespace superNamespace = JvmTypeNamespace.resolved(
            module,
            (JvmDeclaredType) superTypeDeclaredSafe
        );

        Map<String, IJadescriptType> superProperties =
            superNamespace.getPropertiesFromBiggestCtor();

        HashMap<String, IJadescriptType> superSlotTypeSet =
            new HashMap<>();
        HashMap<String, Integer> superSlotPositionSet =
            new HashMap<>();


        boolean isWithSuperSlots =
            input.__(ExtendingFeature::isWithSuperSlots)
                .extract(nullAsFalse);

        if (isWithSuperSlots) {
            Maybe<NamedArgumentList> superSlots =
                input.__(ExtendingFeature::getNamedSuperSlots);

            MaybeList<String> argNames =
                superSlots.__toList(NamedArgumentList::getParameterNames);
            MaybeList<RValueExpression> args =
                superSlots.__toList(NamedArgumentList::getParameterValues);

            HashMap<String, IJadescriptType> superSlotsInitScope
                = new HashMap<>();

            superSlotsInitScope.putAll(superProperties);
            //matching names override super ones:
            superSlotsInitScope.putAll(slotTypeSet);

            final RValueExpressionSemantics rves =
                module.get(RValueExpressionSemantics.class);

            for (int i = 0; i < Math.min(argNames.size(), args.size()); i++) {
                int finalI = i;
                Maybe<String> argName = argNames.get(i);
                final Maybe<RValueExpression> arg = args.get(i);

                module.get(ContextManager.class)
                    .enterSuperSlotInitializer(superSlotsInitScope);

                StaticState beforeInitExpr =
                    StaticState.beginningOfOperation(module);

                validationHelper.asserting(
                    rves.isWithoutSideEffects(arg, beforeInitExpr),
                    "InvalidSuperSlotInitExpression",
                    "Initialization expressions of super-slots " +
                        "must be pure (without side effects).",
                    superSlots,
                    JadescriptPackage.eINSTANCE
                        .getNamedArgumentList_ParameterValues(),
                    i,
                    acceptor
                );

                boolean argCheck = rves.validate(arg, beforeInitExpr, acceptor);

                IJadescriptType argType;
                if (argCheck == INVALID) {
                    argType = typeHelper.ANY;
                } else {
                    argType = rves.inferType(arg, beforeInitExpr);
                }

                module.get(ContextManager.class).exit();


                //then populate the sets for later checks
                argName.safeDo(argNameSafe -> {
                    superSlotTypeSet.put(argNameSafe, argType);
                    superSlotPositionSet.put(argNameSafe, finalI);
                });

            }
        }

        Maybe<NamedArgumentList> superSlots =
            input.__(ExtendingFeature::getNamedSuperSlots);

        MaybeList<RValueExpression> superSlotsNamedArgumentValues =
            superSlots.__toList(NamedArgumentList::getParameterValues);

        superProperties.forEach((propName, propType) -> {
            boolean isInitialized =
                isWithSuperSlots && superSlotTypeSet.containsKey(
                    propName);

            boolean isRedeclared =
                slotTypeSet.containsKey(propName);


            boolean consisentInheritedPropertyCheck = VALID;
            //super-properties cannot be both initialized in the
            // extends section and re-declared in the parameters section:
            if (isInitialized && isRedeclared) {
                validationHelper.emitError(
                    "InvalidSlotInheritance",
                    "Super-property '" + propName + "' cannot be " +
                        "initialized and redeclared at the same time",
                    slotSet.get(propName),
                    acceptor
                );
                validationHelper.emitError(
                    "InvalidSlotInheritance",
                    "Super-property '" + propName + "' cannot be " +
                        "initialized and redeclared at the same " +
                        "time",
                    input.__(ExtendingFeature::getNamedSuperSlots),
                    JadescriptPackage.eINSTANCE
                        .getNamedArgumentList_ParameterNames(),
                    superSlotPositionSet.get(propName),
                    acceptor
                );

                consisentInheritedPropertyCheck = INVALID;
            }


            if (consisentInheritedPropertyCheck == VALID
                //super-properties must be either initialized in the extends
                // section or re-declared in the parameters section:
                && !isInitialized && !isRedeclared) {
                consisentInheritedPropertyCheck = validationHelper.emitError(
                    "InvalidSlotInheritance",
                    "Super-property '" + propName + "' must be " +
                        "either initialized or re-declared",
                    input.__(ExtendingFeature::getSuperType),
                    acceptor
                );
            }


            if (consisentInheritedPropertyCheck == VALID) {
                // we can now assume isInitialized XOR isRedeclared
                if (isInitialized) {


                    Maybe<RValueExpression> argExpression =
                        superSlotsNamedArgumentValues.get(
                            superSlotPositionSet.get(propName)
                        );

                    IJadescriptType argType = superSlotTypeSet.get(propName);

                    //initialization of super-properties in
                    // extends section must be type conformant
                    validationHelper.assertExpectedType(
                        superProperties.getOrDefault(
                            propName,
                            typeHelper.ANY
                        ),
                        argType,
                        "InvalidSlotInheritance",
                        argExpression,
                        acceptor
                    );

                } else if (isRedeclared) {
                    //redeclaration of super-properties in
                    // parameters section must be type conformant
                    if (slotSet.containsKey(propName)) {
                        validationHelper.assertExpectedType(
                            superProperties.getOrDefault(
                                propName,
                                typeHelper.ANY
                            ),
                            slotTypeSet.get(propName),
                            "InvalidSlotInheritance",
                            slotSet.get(propName),
                            acceptor
                        );
                    }
                }
            }

        });
    }


    public boolean validateSlotDeclaration(
        Maybe<SlotDeclaration> slotDeclaration,
        TypeExpressionSemantics tes,
        ValidationMessageAcceptor acceptor
    ) {

        module.get(ValidationHelper.class).assertNotReservedName(
            slotDeclaration.__(SlotDeclaration::getName),
            slotDeclaration,
            JadescriptPackage.eINSTANCE.getSlotDeclaration_Name(),
            acceptor
        );

        // checks type expressions of slot declarations: they have to be
        // valid for ontologies
        final Maybe<TypeExpression> slotTypeExpression =
            slotDeclaration.__(SlotDeclaration::getType);
        if (slotTypeExpression.isNothing()) {
            return VALID;
        }


        final boolean slotTypeExprCheck = tes.validate(
            slotTypeExpression,
            acceptor
        );

        if (slotTypeExprCheck == INVALID) {
            return INVALID;
        }

        IJadescriptType slotType = tes.toJadescriptType(slotTypeExpression);

        return module.get(ValidationHelper.class).asserting(
            !slotType.isCollection() ||
                ((ParametricType) slotType).getTypeArguments().stream()
                    .map(TypeArgument::ignoreBound)
                    .noneMatch(IJadescriptType::isCollection),
            "InvalidSlotType",
            "Collections of collections are not supported as slot types.",
            slotTypeExpression,
            acceptor
        );

    }


    private void checkDuplicateDeclaredSlots(
        ValidationMessageAcceptor acceptor,
        Maybe<FeatureWithSlots> inputWithSlots
    ) {
        //checks duplicates between slots
        HashMultimap<String, SlotDeclaration> multiMap = HashMultimap.create();
        final Maybe<EList<SlotDeclaration>> slots =
            inputWithSlots.__(FeatureWithSlots::getSlots);
        for (Maybe<SlotDeclaration> slot : iterate(slots)) {
            final Maybe<String> slotName = slot.__(SlotDeclaration::getName);
            if (slot.isNothing() || slotName.isNothing()) {
                continue;
            }

            multiMap.put(slotName.toNullable(), slot.toNullable());
        }

        for (Map.Entry<String, Collection<SlotDeclaration>> entry :
            multiMap.asMap().entrySet()) {
            Collection<SlotDeclaration> duplicates = entry.getValue();
            if (duplicates.size() > 1) {
                for (SlotDeclaration d : duplicates) {
                    acceptor.acceptError(
                        "Duplicate slot '" + entry.getKey() +
                            "' in ' " + inputWithSlots
                            .__(ExtendingFeature::getName)
                            .extract(nullAsEmptyString) + "''",
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
        return input.toSingleList().stream().flatMap(inputSafe -> {
            if (input.__(ExtendingFeature::isNative).extract(nullAsFalse)) {
                return generateNativeTypes(
                    input,
                    ontoFullQualifiedName,
                    isPreIndexingPhase,
                    inputSafe
                );
            } else {
                return generateConcreteType(
                    input,
                    ontoFullQualifiedName,
                    isPreIndexingPhase,
                    inputSafe
                );
            }
        }).collect(Collectors.toList());
    }


    private Stream<JvmDeclaredType> generateConcreteType(
        Maybe<ExtendingFeature> input,
        Maybe<QualifiedName> ontoFullQualifiedName,
        boolean isPreIndexingPhase,
        ExtendingFeature inputSafe
    ) {
        final JvmTypesBuilder jvmTB = module.get(JvmTypesBuilder.class);

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        final QualifiedName fullyQualifiedName =
            compilationHelper.getFullyQualifiedName(inputSafe);


        if (fullyQualifiedName == null || fullyQualifiedName.isEmpty()) {
            return Stream.empty();
        }

        return Stream.of(jvmTB.toClass(
            inputSafe,
            fullyQualifiedName,
            itClass -> {
                module.get(ContextManager.class)
                    .enterOntologyElementDeclaration();

                fillConcreteType(
                    input,
                    ontoFullQualifiedName,
                    isPreIndexingPhase,
                    itClass
                );

                module.get(ContextManager.class).exit();
            }
        ));
    }


    private void fillConcreteType(
        Maybe<ExtendingFeature> input,
        Maybe<QualifiedName> ontoFullQualifiedName,
        boolean isPreIndexingPhase,
        JvmGenericType itClass
    ) {
        if (isPreIndexingPhase) {
            return;
        }
        List<String> compiledSuperArguments = new ArrayList<>();

        Maybe<? extends JvmParameterizedTypeReference> superType =
            input.__(ExtendingFeature::getSuperType);

        if (superType.isNothing()) {
            final TypeHelper typeHelper = module.get(TypeHelper.class);
            itClass.getSuperTypes().add(typeHelper.typeRef(
                getBaseOntologyContentType(input)
            ));
        }

        prepareSuperTypeInitialization(
            input,
            itClass,
            superType,
            compiledSuperArguments // filled here
        );


        final Boolean hasSlots =
            input.__(i -> i instanceof FeatureWithSlots)
                .orElse(false);

        if (hasSlots) {
            final Maybe<EList<SlotDeclaration>> slots = input
                .__(i -> (FeatureWithSlots) i)
                .__(FeatureWithSlots::getSlots);

            for (Maybe<SlotDeclaration> slot : iterate(slots)) {
                addProperty(itClass.getMembers(), slot);
            }
        }


        addToString(itClass.getMembers(), input);

        addEquals(itClass.getMembers(), input);

        addOntologyElementsConstructor(
            itClass.getMembers(),
            input,
            compiledSuperArguments // consumed here
        );

        addGetDeclaringOntology(
            itClass.getMembers(),
            input,
            ontoFullQualifiedName,
            false
        );

        addMetadataMethod(
            itClass.getMembers(),
            ontoFullQualifiedName,
            input,
            false
        );

    }


    private Stream<JvmDeclaredType> generateNativeTypes(
        Maybe<ExtendingFeature> input,
        Maybe<QualifiedName> ontoFullQualifiedName,
        boolean isPreindexingPhase,
        ExtendingFeature inputSafe
    ) {
        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);
        final QualifiedName fqName =
            compilationHelper.getFullyQualifiedName(inputSafe);

        final JvmTypesBuilder jvmTB = module.get(JvmTypesBuilder.class);

        if (fqName == null) {
            return Stream.empty();
        }

        return Stream.of(jvmTB.toClass(
            inputSafe,
            fqName,
            (JvmGenericType itClass) -> {
                itClass.setAbstract(true);
                module.get(ContextManager.class)
                    .enterOntologyElementDeclaration();

                fillNativeType(
                    input,
                    ontoFullQualifiedName,
                    isPreindexingPhase,
                    inputSafe,
                    compilationHelper,
                    jvmTB,
                    itClass
                );

                module.get(ContextManager.class).exit();
            }
        ), jvmTB.toInterface(
            inputSafe,
            fqName + "Factory",
            itClass -> fillNativeInterface(
                input,
                inputSafe,
                fqName,
                jvmTB,
                itClass
            )
        ));
    }


    private void fillNativeType(
        Maybe<ExtendingFeature> input,
        Maybe<QualifiedName> ontoFullQualifiedName,
        boolean isPreindexingPhase,
        ExtendingFeature inputSafe,
        CompilationHelper compilationHelper,
        JvmTypesBuilder jvmTB,
        JvmGenericType itClass
    ) {
        if (isPreindexingPhase) {
            return;
        }

        List<String> compiledSuperArguments = new ArrayList<>();

        Maybe<? extends JvmParameterizedTypeReference> superType =
            input.__(ExtendingFeature::getSuperType);

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        if (superType.isNothing()) {
            itClass.getSuperTypes().add(
                typeHelper.typeRef(getBaseOntologyContentType(input))
            );
        }

        prepareSuperTypeInitialization(
            input,
            itClass,
            superType,
            compiledSuperArguments // filled here
        );

        final Boolean hasSlots =
            input.__(i -> i instanceof FeatureWithSlots).extract(
                nullAsFalse);
        if (hasSlots) {

            final Maybe<EList<SlotDeclaration>> slots = input
                .__(i -> (FeatureWithSlots) i)
                .__(FeatureWithSlots::getSlots);

            for (Maybe<SlotDeclaration> slot : iterate(slots)) {
                addNativeProperty(itClass.getMembers(), slot);
            }
        }

        addToString(itClass.getMembers(), input);

        addEquals(itClass.getMembers(), input);

        addOntologyElementsConstructor(
            itClass.getMembers(),
            input,
            compiledSuperArguments // consumed here
        );

        itClass.getMembers().add(jvmTB.toMethod(
            inputSafe,
            "__isNative",
            typeHelper.typeRef(Void.TYPE),
            itMethod -> {
                itMethod.setDefault(true);
                itMethod.setAbstract(false);
                compilationHelper.createAndSetBody(
                    itMethod,
                    scb -> {
                        scb.line(
                            "// Method used as metadata flag by " +
                                "the Jadescript compiler.");
                    }
                );
            }

        ));

        addGetDeclaringOntology(
            itClass.getMembers(),
            input,
            ontoFullQualifiedName,
            true
        );

        addMetadataMethod(
            itClass.getMembers(),
            ontoFullQualifiedName,
            input,
            true
        );

    }


    private void fillNativeInterface(
        Maybe<ExtendingFeature> input,
        ExtendingFeature inputSafe,
        QualifiedName fqName,
        JvmTypesBuilder jvmTB,
        JvmGenericType itClass
    ) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);

        final JvmTypeReference conceptTypeRef =
            typeHelper.typeRef(fqName != null ? fqName.toString() : null);

        itClass.getSuperTypes().add(typeHelper.typeRef(
            NativeValueFactory.class
        ));

        itClass.getMembers().add(jvmTB.toMethod(
            inputSafe,
            "getImplementationClass",
            typeHelper.typeRef(
                "Class<? extends " + conceptTypeRef.getQualifiedName('.') + ">"
            ),
            itMethod -> {
                itMethod.setDefault(false);
                itMethod.setAbstract(true);

            }
        ));

        itClass.getMembers().add(jvmTB.toMethod(
            inputSafe,
            "empty",
            conceptTypeRef,
            itMethod -> {
                itMethod.setDefault(false);
                itMethod.setAbstract(true);
            }
        ));


        final Boolean hasSlots = input
            .__(i -> i instanceof FeatureWithSlots)
            .extract(nullAsFalse);

        if (hasSlots) {
            Maybe<FeatureWithSlots> inputWithSlots =
                input.__(i -> (FeatureWithSlots) i);

            Maybe<EList<SlotDeclaration>> slots = inputWithSlots
                .__(FeatureWithSlots::getSlots);

            final TypeExpressionSemantics tes =
                module.get(TypeExpressionSemantics.class);

            itClass.getMembers().add(jvmTB.toMethod(
                inputSafe,
                "create",
                conceptTypeRef,
                itMethod -> {
                    itMethod.setDefault(false);
                    itMethod.setAbstract(true);
                    for (Maybe<SlotDeclaration> slot : iterate(slots)) {

                        final Maybe<TypeExpression> slotTypeExpr =
                            slot.__(SlotDeclaration::getType);

                        IJadescriptType slotType =
                            tes.toJadescriptType(slotTypeExpr);

                        Maybe<String> slotName =
                            slot.__(SlotDeclaration::getName);


                        if (slot.isNothing() || slotName.isNothing()) {
                            continue;
                        }

                        final SlotDeclaration slotSafe =
                            slot.toNullable();

                        final String slotNameSafe = slotName.toNullable();

                        itMethod.getParameters().add(
                            jvmTB
                                .toParameter(
                                    slotSafe,
                                    slotNameSafe,
                                    slotType.asJvmTypeReference()
                                ));
                    }
                }
            ));
        }
    }


    private void prepareSuperTypeInitialization(
        Maybe<ExtendingFeature> input,
        JvmGenericType itClass,
        Maybe<? extends JvmParameterizedTypeReference> superType,
        List<String> compiledSuperArguments // filled with results
    ) {
        if (superType.isNothing()) {
            return;
        }
        final JvmParameterizedTypeReference superTypeSafe =
            superType.toNullable();

        final JvmTypesBuilder jvmTB = module.get(JvmTypesBuilder.class);

        itClass.getSuperTypes().add(jvmTB.cloneWithProxies(superTypeSafe));

        if (!(superTypeSafe.getType() instanceof JvmDeclaredType)) {
            return;
        }

        JvmDeclaredType superTypeDeclaredSafe =
            (JvmDeclaredType) superTypeSafe.getType();

        final HashMap<String, IJadescriptType> slotTypeSet = new HashMap<>();


        final boolean hasSlots =
            input.__(i -> i instanceof FeatureWithSlots).orElse(false);

        if (hasSlots) {
            final Maybe<EList<SlotDeclaration>> slots = input
                .__(i -> (FeatureWithSlots) i)
                .__(FeatureWithSlots::getSlots);

            final TypeExpressionSemantics tes =
                module.get(TypeExpressionSemantics.class);

            for (Maybe<SlotDeclaration> slot : iterate(slots)) {
                Maybe<String> slotName =
                    slot.__(SlotDeclaration::getName);
                IJadescriptType slotType =
                    slot.__(SlotDeclaration::getType).extract(
                        tes::toJadescriptType);
                slotName.safeDo(slotNameSafe -> {
                    slotTypeSet.put(slotNameSafe, slotType);
                });
            }
        }

        final JvmTypeNamespace superTypeNamespace = JvmTypeNamespace.resolved(
            module,
            superTypeDeclaredSafe
        );

        Map<String, IJadescriptType> superProperties =
            superTypeNamespace.getPropertiesFromBiggestCtor();

        HashMap<String, String> superSlotCompilationMap = new HashMap<>();

        boolean isWithSuperSlots =
            input.__(ExtendingFeature::isWithSuperSlots).orElse(false);

        if (isWithSuperSlots) {
            Maybe<NamedArgumentList> superSlots =
                input.__(ExtendingFeature::getNamedSuperSlots);

            MaybeList<String> argNames =
                superSlots.__toList(NamedArgumentList::getParameterNames);

            MaybeList<RValueExpression> args =
                superSlots.__toListCopy(NamedArgumentList::getParameterValues);

            HashMap<String, IJadescriptType> superSlotsInitPairs =
                new HashMap<>();

            superSlotsInitPairs.putAll(superProperties);

            //matching names override super ones
            superSlotsInitPairs.putAll(slotTypeSet);

            final RValueExpressionSemantics rves =
                module.get(RValueExpressionSemantics.class);

            final CompilationHelper compilationHelper =
                module.get(CompilationHelper.class);


            for (int i = 0; i < Math.min(argNames.size(), args.size()); i++) {

                Maybe<String> argName = argNames.get(i);
                final Maybe<RValueExpression> arg = args.get(i);

                if (argName.isNothing()) {
                    continue;
                }

                final String argNameSafe = argName.toNullable();

                if (argNameSafe.isBlank()) {
                    continue;
                }

                module.get(ContextManager.class)
                    .enterSuperSlotInitializer(superSlotsInitPairs);

                StaticState state = StaticState.beginningOfOperation(module);

                IJadescriptType argType = rves.inferType(arg, state);


                @Nullable
                IJadescriptType superSlotType =
                    superSlotsInitPairs.get(argNameSafe);

                String argCompiled = compilationHelper
                    .compileRValueAsLambdaSupplier(
                        arg,
                        state,
                        argType,
                        superSlotType
                    );

                module.get(ContextManager.class).exit();

                // populate the map for later
                superSlotCompilationMap.put(argNameSafe, argCompiled);
            }


        }

        List<String> ctorArgNames = new ArrayList<>();
        List<String> superArgs = new ArrayList<>();

        superProperties.forEach((propName, propType) -> {
            //assuming is initialized XOR is redeclared (checked by validator):
            ctorArgNames.add(propName);

            if (slotTypeSet.containsKey(propName)) {
                // The property is redeclared: refer to the property in the
                // constructor of the sub-type
                superArgs.add(propName);
            } else if (isWithSuperSlots
                && superSlotCompilationMap.containsKey(propName)) {
                // The super-property is initialized with a specific expression:
                // use the compiled expression
                superArgs.add(superSlotCompilationMap.get(propName));
            }
        });


        superTypeNamespace.getBiggestCtor().ifPresent((JvmConstructor c) -> {
            final EList<JvmFormalParameter> parameters = c.getParameters();
            if (parameters == null) {
                return;
            }


            compiledSuperArguments.addAll(
                CallSemantics.sortToMatchParamNames(
                    superArgs,
                    ctorArgNames,
                    parameters.stream()
                        .map(JvmFormalParameter::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                ));
        });

    }


    private void addMetadataMethod(
        EList<JvmMember> members,
        Maybe<QualifiedName> ontoFullQualifiedName,
        Maybe<ExtendingFeature> input,
        boolean inInterface
    ) {
        if (input.isNothing() || ontoFullQualifiedName.isNothing()) {
            return;
        }
        final ExtendingFeature inputSafe = input.toNullable();
        final QualifiedName ontoFullQualifiedNameSafe =
            ontoFullQualifiedName.toNullable();

        String typeFullyQualifiedName = input
            .__(module.get(CompilationHelper.class)::getFullyQualifiedName)
            .__(fqn -> fqn.toString("."))
            .or(input.__(NamedFeature::getName))
            .orElse("")
            .replaceAll("\\.", "_");


        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);

        final TypeHelper typeHelper = module.get(TypeHelper.class);
        members.add(jvmTB.toMethod(
            inputSafe,
            "__metadata_" + typeFullyQualifiedName,
            typeHelper.typeRef(ontoFullQualifiedNameSafe.toString(".")),
            itMethod -> {

                itMethod.setDefault(inInterface);
                if (!inInterface) {
                    itMethod.setVisibility(JvmVisibility.PRIVATE);
                }

                // if is with slots, add a parameter for each slot
                if (inputSafe instanceof FeatureWithSlots) {
                    final Maybe<EList<SlotDeclaration>> slots =
                        some(((FeatureWithSlots) inputSafe).getSlots());

                    final TypeExpressionSemantics typeExpressionSemantics =
                        module.get(TypeExpressionSemantics.class);

                    for (Maybe<SlotDeclaration> slot : iterate(slots)) {
                        final Maybe<String> slotName =
                            slot.__(SlotDeclaration::getName);

                        if (slotName.isNothing()) {
                            continue;
                        }
                        final String slotNameSafe = slotName.toNullable();

                        final Maybe<TypeExpression> slotType =
                            slot.__(SlotDeclaration::getType);

                        final JvmTypeReference typeRef =
                            typeExpressionSemantics.toJadescriptType(slotType)
                                .asJvmTypeReference();

                        itMethod.getParameters().add(jvmTB.toParameter(
                            inputSafe,
                            slotNameSafe,
                            typeRef
                        ));
                    }
                }

                // just a metadata method, returning null since the important
                // part is its signature
                module.get(CompilationHelper.class).createAndSetBody(
                    itMethod,
                    scb -> scb.line("return null;")
                );
            }
        ));
    }


    private void addEquals(
        EList<JvmMember> members,
        Maybe<ExtendingFeature> input
    ) {
        if (input.isNothing()) {
            return;
        }

        final ExtendingFeature inputSafe = input.toNullable();

        final String typeName = inputSafe.getName();
        if (typeName == null || typeName.isBlank()) {
            return;
        }

        final JvmTypesBuilder jvmTB = module.get(JvmTypesBuilder.class);

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        members.add(jvmTB.toMethod(
            inputSafe,
            "equals",
            typeHelper.typeRef(Boolean.TYPE),
            itMethod -> {
                itMethod.setVisibility(JvmVisibility.PUBLIC);

                itMethod.getParameters().add(jvmTB.toParameter(
                    inputSafe,
                    "obj",
                    typeHelper.typeRef(Object.class)
                ));

                final CompilationHelper compilationHelper =
                    module.get(CompilationHelper.class);

                compilationHelper.createAndSetBody(itMethod, scb -> {

                    scb.open("if(obj instanceof " + typeName + ") {");
                    scb.line(typeName + " o = (" + typeName + ") obj;");
                    scb.add("return ");
                    if (input
                        .__(ExtendingFeature::getSuperType)
                        .isPresent()) {
                        scb.add("super.equals(obj)");
                    } else {
                        scb.add("true");
                    }
                    if (inputSafe instanceof FeatureWithSlots) {
                        final Maybe<EList<SlotDeclaration>> slots =
                            some((FeatureWithSlots) inputSafe)
                                .__(FeatureWithSlots::getSlots);
                        for (Maybe<SlotDeclaration> slot : iterate(slots)) {
                            final Maybe<String> slotName =
                                slot.__(SlotDeclaration::getName);

                            if (slotName.isNothing()) {
                                continue;
                            }

                            String slotNameSafe =
                                Strings.toFirstUpper(slotName.toNullable());

                            if (slotNameSafe.isBlank()) {
                                continue;
                            }

                            scb.add(" && ");
                            scb.add("java.util.Objects.equals(");
                            scb.add("this.get")
                                .add(slotNameSafe).add("(), o.get")
                                .add(slotNameSafe).add("())");
                        }
                    }
                    scb.line(";");
                    scb.closeAndOpen("} else {");
                    scb.line("return super.equals(obj);");
                    scb.close("}");
                });
            }
        ));
    }


    private void addGetDeclaringOntology(
        EList<JvmMember> members,
        Maybe<ExtendingFeature> input,
        Maybe<QualifiedName> ontoFQName,
        boolean asDefault
    ) {

        if (input.isNothing() || ontoFQName.isNothing()) {
            return;
        }
        final ExtendingFeature inputSafe = input.toNullable();
        final QualifiedName ontoFQNameSafe = ontoFQName.toNullable();

        final JvmTypeReference ontoTypeRef =
            module.get(TypeHelper.class).typeRef(ontoFQNameSafe.toString());

        members.add(module.get(JvmTypesBuilder.class).toMethod(
            inputSafe,
            "__getDeclaringOntology",
            module.get(TypeHelper.class)
                .typeRef(jade.content.onto.Ontology.class),
            it -> {
                it.setDefault(asDefault);

                module.get(CompilationHelper.class).createAndSetBody(
                    it,
                    scb -> scb.add("return ")
                        .add(ontoTypeRef.getQualifiedName('.'))
                        .add(".getInstance();")
                );
            }
        ));

    }


    /**
     * Generates the ctor of the concept POJO class
     */
    private void addOntologyElementsConstructor(
        EList<JvmMember> members,
        Maybe<ExtendingFeature> input,
        List<String> compiledSuperArguments
    ) {
        if (input.isNothing()) {
            return;
        }
        final ExtendingFeature inputSafe = input.toNullable();

        // Constructor without parameters
        final JvmTypesBuilder jvmTypesBuilder =
            module.get(JvmTypesBuilder.class);

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        final boolean hasSlots = input
            .__(i -> i instanceof FeatureWithSlots)
            .extract(nullAsFalse);

        // Add empty constructor, for default value initialization and for
        //  deserialization from messages
        members.add(jvmTypesBuilder.toConstructor(inputSafe, itCtor ->
            compilationHelper.createAndSetBody(itCtor, scb -> {

                if (!hasSlots) {
                    w.block().writeSonnet(scb);
                    return; //from lambda
                }

                BlockWriter block = w.block();

                Maybe<FeatureWithSlots> inputWithSlots =
                    input.__(i -> (FeatureWithSlots) i);

                final Maybe<EList<SlotDeclaration>> slots =
                    inputWithSlots.__(FeatureWithSlots::getSlots);

                for (Maybe<SlotDeclaration> slot : iterate(slots)) {
                    Maybe<String> slotName =
                        slot.__(SlotDeclaration::getName);
                    Maybe<TypeExpression> slotType =
                        slot.__(SlotDeclaration::getType);

                    if (slotName.isNothing()) {
                        continue;
                    }

                    String slotNameSafe = slotName.toNullable();

                    if (slotNameSafe.isBlank()) {
                        continue;
                    }

                    block.addStatement(w.callStmnt(
                        "this.set" + Strings.toFirstUpper(slotNameSafe),
                        w.expr(compilationHelper
                            .compileDefaultValueForType(slotType))
                    ));
                }

                block.writeSonnet(scb);
            })
        ));


        if (!hasSlots) {
            return;
        }

        Maybe<FeatureWithSlots> inputWithSlots =
            input.__(i -> (FeatureWithSlots) i);
        MaybeList<SlotDeclaration> slots =
            inputWithSlots.__toList(FeatureWithSlots::getSlots);

        if (slots.isEmpty()) {
            return;
        }

        // Constructor with parameters
        final TypeExpressionSemantics typeExpressionSemantics =
            module.get(TypeExpressionSemantics.class);

        members.add(jvmTypesBuilder.toConstructor(inputSafe, itCtor -> {
            for (Maybe<SlotDeclaration> slot : slots) {

                IJadescriptType slotType = typeExpressionSemantics.
                    toJadescriptType(slot.__(SlotDeclaration::getType));

                Maybe<String> slotName =
                    slot.__(SlotDeclaration::getName);

                if (slot.isNothing() || slotName.isNothing()) {
                    continue;
                }

                final SlotDeclaration slotSafe = slot.toNullable();
                final String slotNameSafe = slotName.toNullable();

                if (slotNameSafe.isBlank()) {
                    continue;
                }

                itCtor.getParameters().add(jvmTypesBuilder.toParameter(
                    slotSafe,
                    slotNameSafe,
                    slotType.asJvmTypeReference()
                ));
            }


            compilationHelper.createAndSetBody(itCtor, scb -> {
                StringBuilder superArgumentsList = new StringBuilder();
                for (int i = 0; i < compiledSuperArguments.size(); i++) {
                    if (i != 0) {
                        superArgumentsList.append(", ");
                    }

                    final String compiledSuperArgument =
                        compiledSuperArguments.get(i);

                    superArgumentsList.append(compiledSuperArgument);
                }


                w.simpleStmt("super(" + superArgumentsList + ")")
                    .writeSonnet(scb);


                scb.line();

                for (Maybe<SlotDeclaration> slot : slots) {
                    final Maybe<String> slotName =
                        slot.__(SlotDeclaration::getName);

                    if (slotName.isNothing()) {
                        continue;
                    }

                    final String slotNameSafe = slotName.toNullable();

                    if (slotNameSafe.isBlank()) {
                        continue;
                    }

                    scb.line("this.set" + Strings.toFirstUpper(slotNameSafe) +
                        "(" + slotNameSafe + ");");
                }
            });
        }));
    }


    /**
     * Generates a new property including a private field with corresponding
     * accessors in the ontology element class
     */
    private void addProperty(
        EList<JvmMember> members,
        Maybe<SlotDeclaration> slot
    ) {
        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);
        IJadescriptType slotType = tes.toJadescriptType(
            slot.__(SlotDeclaration::getType)
        );

        Maybe<String> slotName = slot.__(SlotDeclaration::getName);

        if (slot.isNothing() || slotName.isNothing()) {
            return;
        }

        final SlotDeclaration slotSafe = slot.toNullable();
        final String slotNameSafe = slotName.toNullable();

        final JvmTypesBuilder jvmTB = module.get(JvmTypesBuilder.class);

        members.add(jvmTB.toField(
            slotSafe,
            slotNameSafe,
            slotType.asJvmTypeReference(),
            it -> it.setVisibility(JvmVisibility.PRIVATE)
        ));

        members.add(jvmTB.toGetter(
            slotSafe,
            slotNameSafe,
            slotType.asJvmTypeReference()
        ));

        members.add(jvmTB.toSetter(
            slotSafe,
            slotNameSafe,
            slotType.asJvmTypeReference()
        ));
    }


    /**
     * Generates a new property for a native type
     */
    private void addNativeProperty(
        EList<JvmMember> members,
        Maybe<SlotDeclaration> slot
    ) {
        IJadescriptType slotType = module.get(TypeExpressionSemantics.class)
            .toJadescriptType(slot.__(SlotDeclaration::getType));
        Maybe<String> slotName = slot.__(SlotDeclaration::getName);

        if (slot.isNothing() || slotName.isNothing()) {
            return;
        }

        final SlotDeclaration slotSafe = slot.toNullable();
        final String slotNameSafe = slotName.toNullable();


        final JvmTypesBuilder jvmTB = module.get(JvmTypesBuilder.class);

        members.add(jvmTB.toMethod(
            slotSafe,
            "get" + Strings.toFirstUpper(slotNameSafe),
            slotType.asJvmTypeReference(),
            it -> {
                it.setDefault(false);
                it.setAbstract(true);
            }
        ));

        members.add(jvmTB.toMethod(
            slotSafe,
            "set" + Strings.toFirstUpper(slotNameSafe),
            module.get(TypeHelper.class).typeRef(Void.TYPE),
            it -> {
                it.getParameters().add(jvmTB.toParameter(
                    slotSafe,
                    "_value",
                    slotType.asJvmTypeReference()
                ));
                it.setDefault(false);
                it.setAbstract(true);
            }
        ));

    }


    private void addToString(
        EList<JvmMember> members,
        Maybe<ExtendingFeature> input
    ) {
        if (input.isNothing()) {
            return;
        }

        final ExtendingFeature inputSafe = input.toNullable();

        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        members.add(jvmTB.toMethod(
            inputSafe,
            "toString",
            typeHelper.TEXT.asJvmTypeReference(),
            it -> {
                it.setVisibility(JvmVisibility.PUBLIC);
                final CompilationHelper compilationHelper =
                    module.get(CompilationHelper.class);

                compilationHelper.createAndSetBody(it, scb -> {
                    fillToStringMethod(
                        inputSafe,
                        typeHelper,
                        compilationHelper,
                        scb
                    );
                });
            }
        ));
    }


    private void fillToStringMethod(
        ExtendingFeature inputSafe,
        TypeHelper typeHelper,
        CompilationHelper compilationHelper,
        SourceCodeBuilder scb
    ) {
        w.variable(
            "java.lang.StringBuilder",
            "_sb",
            w.callExpr("new java.lang.StringBuilder")
        ).writeSonnet(scb);

        w.callStmnt(
            "_sb.append",
            w.stringLiteral(
                some(inputSafe)
                    .__(compilationHelper::getFullyQualifiedName)
                    .__(QualifiedName::toString)
                    .nullIf(String::isBlank)
                    .orElse("")
            )
        ).writeSonnet(scb);

        if (inputSafe instanceof FeatureWithSlots) {

            FeatureWithSlots featureWithSlots = (FeatureWithSlots) inputSafe;
            EList<SlotDeclaration> slots = featureWithSlots.getSlots();

            if (!slots.isEmpty()) {
                w.callStmnt("_sb.append", w.stringLiteral("("))
                    .writeSonnet(scb);

                final TypeExpressionSemantics tes =
                    module.get(TypeExpressionSemantics.class);

                for (int i = 0; i < slots.size(); i++) {
                    SlotDeclaration slot = slots.get(i);

                    if (slot == null || slot.getName() == null) {
                        continue;
                    }


                    if (i != 0) {
                        w.callStmnt("_sb.append", w.stringLiteral(", "))
                            .writeSonnet(scb);
                    }

                    IJadescriptType type =
                        tes.toJadescriptType(some(slot.getType()));

                    final String getterCall = "get" + Strings.toFirstUpper(
                        slot.getName()
                    ) + "()";


                    if (typeHelper.TEXT.isSupEqualTo(type)) {
                        final String quoteLiteral = "\"\\\"\"";
                        w.callStmnt(
                            "_sb.append",
                            w.expr(quoteLiteral +
                                " + java.lang.String.valueOf(" + getterCall +
                                ") + " + quoteLiteral
                            )
                        ).writeSonnet(scb);
                    } else {
                        w.callStmnt(
                            "_sb.append",
                            w.expr("java.lang.String.valueOf("
                                + getterCall + ")")
                        ).writeSonnet(scb);
                    }

                }

                w.callStmnt(
                    "_sb.append",
                    w.stringLiteral(")")
                ).writeSonnet(scb);
            }
        }

        w.returnStmnt(w.callExpr("_sb.toString"))
            .writeSonnet(scb);
    }


    private Class<?> getBaseOntologyContentType(
        Maybe<ExtendingFeature> feature
    ) {
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
        }).orElse(
            // is the most generic:
            jadescript.content.JadescriptProposition.class
        );
    }

}
