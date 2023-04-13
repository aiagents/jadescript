package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c1toplevel.OntologyDeclarationContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OntologyDeclarationSupportContext;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.DeclaresOntologyAdHocClass;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.ListType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.MapType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.SetType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.TupleType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsClassState;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.*;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;
import static it.unipr.ailab.maybe.Functional.filterAndCast;
import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 27/04/18.
 */
@Singleton
public class OntologyDeclarationSemantics extends
    MemberContainerTopLevelDeclarationSemantics<Ontology> {

    private final SemanticsClassState<Ontology, HashMap<String,
        JvmDeclaredType>> declaredSchemaTypes
        = new SemanticsClassState<>(HashMap::new);
    private final SemanticsClassState<Ontology, HashMap<String,
        ExtendingFeature>> schemaFeatures
        = new SemanticsClassState<>(HashMap::new);


    public OntologyDeclarationSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected void prepareAndEnterContext(
        Maybe<Ontology> input,
        JvmDeclaredType jvmDeclaredType
    ) {
        module.get(ContextManager.class).enterTopLevelDeclaration((
            module,
            outer
        ) -> new OntologyDeclarationContext(module, outer, jvmDeclaredType));
    }


    @Override
    protected void exitContext(Maybe<Ontology> input) {
        module.get(ContextManager.class).exit();//OntologyDeclarationContext
    }


    @SuppressWarnings("unchecked")
    @Override
    public void validateOnEdit(
        Maybe<Ontology> input,
        ValidationMessageAcceptor acceptor
    ) {
        super.validateOnEdit(input, acceptor);

        if (input == null) {
            return;
        }


        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        final Maybe<String> ontoFqName = input
            .__(compilationHelper::getFullyQualifiedName)
            .__(qn -> qn.toString("."));

        module.get(ContextManager.class).enterTopLevelDeclaration((mod, out) ->
            new OntologyDeclarationSupportContext(mod, out, input, ontoFqName));

        final Iterable<Maybe<Feature>> features =
            iterate(input.__(FeatureContainer::getFeatures));

        for (Maybe<? extends Feature> feature : features) {
            InterceptAcceptor acceptor1 = new InterceptAcceptor(acceptor);

            //noinspection unchecked
            module.get(OntologyElementSemantics.class).validate(
                (Maybe<ExtendingFeature>) feature,
                acceptor1
            );
        }

        module.get(ContextManager.class).exit();
    }


    @Override
    public void validateOnSave(
        Maybe<Ontology> input,
        ValidationMessageAcceptor acceptor
    ) {
        super.validateOnSave(input, acceptor);
        Maybe<EList<JvmParameterizedTypeReference>> superTypes =
            input.__(FeatureContainer::getSuperTypes);//TODO multiple ontologies

        if (!superTypes.__(List::isEmpty).orElse(true)) {
            final ValidationHelper validationHelper =
                module.get(ValidationHelper.class);
            final BuiltinTypeProvider builtins = module.get(
                BuiltinTypeProvider.class);
            validationHelper.assertExpectedType(
                builtins.ontology(),
                superTypes//TODO multiple ontologies
                    .__partial2(EList::get, 0)
                    .__(st -> module.get(TypeSolver.class)
                        .fromJvmTypeReference(st))
                    .orElse(builtins.any(
                        "Could not resolve ontology supertype."
                    )),
                "NotAValidOntologyTypeReference",
                input,
                JadescriptPackage.eINSTANCE.getFeatureContainer_SuperTypes(),
                0,
                acceptor
            );
        }
    }


    @Override
    public void populateMainMembers(
        Maybe<Ontology> input,
        EList<JvmMember> members,
        JvmDeclaredType itClass
    ) {
        super.populateMainMembers(input, members, itClass);
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        JvmTypeReference superOntologyType = input//TODO multiple ontologies
            .__(FeatureContainer::getSuperTypes)
            .nullIf(List::isEmpty)
            .__partial2(List::get, 0)
            .__(t -> (JvmTypeReference) t)
            .orElse(jvm.typeRef(jadescript.content.onto.Ontology.class));

        if (input.isNothing()) {
            return;
        }

        final Ontology inputsafe = input.toNullable();

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        final JvmTypesBuilder jvmTB = module.get(JvmTypesBuilder.class);

        members.add(jvmTB.toField(
            inputsafe,
            SUPER_ONTOLOGY_VAR,
            //TODO multiple ontologies
            superOntologyType,
            itField -> {
                itField.setVisibility(JvmVisibility.PRIVATE);
                itField.setStatic(true);
                compilationHelper.createAndSetInitializer(
                    itField,
                    scb -> scb.add("null")
                );
            }
        ));
        final String ontologyNameString = input
            .__(compilationHelper::getFullyQualifiedName)
            .__(qn -> qn.toString("_"))
            .orElse("");


        input.__(NamedElement::getName).safeDo(nameSafe -> {
            members.add(jvmTB.toField(
                inputsafe,
                ONTOLOGY_STATIC_INSTANCE_NAME,
                jvm.typeRef(jadescript.content.onto.Ontology.class),
                itField -> {
                    itField.setVisibility(JvmVisibility.PRIVATE);
                    itField.setStatic(true);
                    compilationHelper.createAndSetInitializer(
                        itField,
                        scb -> scb.add("new " + nameSafe + "()")
                    );
                }
            ));
            members.add(jvmTB.toField(
                inputsafe,
                "__NAME",
                jvm.typeRef(String.class),
                itField -> {
                    itField.setVisibility(JvmVisibility.PUBLIC);
                    itField.setStatic(true);
                    itField.setFinal(true);
                    compilationHelper.createAndSetInitializer(
                        itField,
                        scb -> scb.add("\"" + ontologyNameString + "\"")
                    );
                }
            ));
        });

        members.add(jvmTB.toMethod(
            inputsafe,
            "getInstance",
            jvm.typeRef(jadescript.content.onto.Ontology.class),
            it -> {
                it.setStatic(true);
                compilationHelper.createAndSetBody(
                    it,
                    scb -> scb.line("return " +
                        ONTOLOGY_STATIC_INSTANCE_NAME + ";")
                );
            }
        ));


        HashMap<String, String> generatedMapOrSetClasses = new HashMap<>();
        List<StatementWriter> addAdHocSchemaWriters = new ArrayList<>();
        List<StatementWriter> descriptionAdHocSchemaWriters = new ArrayList<>();
        for (Maybe<? extends Feature> feature : iterate(
            input.__(FeatureContainer::getFeatures))
        ) {
            addElementFactoryMethod(
                input,
                members,
                feature.__(f -> (ExtendingFeature) f)
            );
            addAdHocMapOrSetClasses(
                members,
                feature.__(f -> (ExtendingFeature) f),
                generatedMapOrSetClasses,
                addAdHocSchemaWriters,
                descriptionAdHocSchemaWriters
            );
        }


        final JvmTypeReference superType = superOntologyType;

        members.add(jvmTB.toConstructor(
            inputsafe,
            it -> compilationHelper.createAndSetBody(it, scb -> {

                scb.line("super(__NAME, " + superType.getQualifiedName(
                    '.') +
                    ".getInstance(), new jade.content.onto" +
                    ".CFReflectiveIntrospector());");


                if (input
                    .__(FeatureContainer::getFeatures)
                    .__(List::isEmpty)
                    .orElse(true)) {
                    return;
                }

                scb.line("try {").indent();
                boolean writingSomething = false;

                for (StatementWriter addAdHocSchemaWriter :
                    addAdHocSchemaWriters) {
                    addAdHocSchemaWriter.writeSonnet(scb);
                    writingSomething = true;
                }

                scb.line().line();
                for (Maybe<? extends Feature> feature : iterate(
                    input.__(FeatureContainer::getFeatures))) {
                    if (feature.isPresent()
                        && isSchemaCompilable(feature.toNullable())) {
                        scb.line(compileAddSchema(
                            feature.__(f -> (ExtendingFeature) f)
                        ));
                        writingSomething = true;
                    }
                }
                scb.line().line();

                for (StatementWriter sw :
                    descriptionAdHocSchemaWriters) {
                    sw.writeSonnet(scb);
                    scb.line();
                    writingSomething = true;
                }

                scb.line().line();
                for (Maybe<? extends Feature> feature :
                    iterate(input.__(FeatureContainer::getFeatures))) {
                    if (feature.__(f -> f instanceof FeatureWithSlots)
                        .orElse(false)
                        && feature.isPresent()
                        && isSchemaCompilable(feature.toNullable())) {
                        scb.line(compileSchemaDescription(
                            feature.__(f -> (FeatureWithSlots) f)
                        ));
                        writingSomething = true;
                    }
                }

                scb.dedent().line("} catch (" +
                    (writingSomething ? "jade.content.onto" +
                        ".OntologyException" : "java.lang" +
                        ".RuntimeException") +
                    " e) {").indent();
                scb.line("e.printStackTrace();");
                scb.dedent().line("}");
            })
        ));
    }


    private void addAdHocMapOrSetClasses(
        EList<JvmMember> members,
        Maybe<ExtendingFeature> feature,
        HashMap<String, String> generatedSpecificClasses,
        List<StatementWriter> addSchemaWriters,
        List<StatementWriter> descriptionSchemaWriters
    ) {
        Optional<FeatureWithSlots> featureWithSlots = feature.someStream()
            .flatMap(filterAndCast(FeatureWithSlots.class))
            .findAny();

        featureWithSlots.ifPresent(featureSafe -> {
            for (SlotDeclaration slotDeclaration : featureSafe.getSlots()) {
                TypeExpression typeExpression = slotDeclaration.getType();
                final TypeExpressionSemantics tes =
                    module.get(TypeExpressionSemantics.class);
                IJadescriptType type =
                    tes.toJadescriptType(some(typeExpression));

                if (type instanceof DeclaresOntologyAdHocClass) {
                    ((DeclaresOntologyAdHocClass) type).declareAdHocClass(
                        members,
                        feature,
                        generatedSpecificClasses,
                        addSchemaWriters,
                        descriptionSchemaWriters,
                        typeExpression,
                        this::getSchemaNameForSlot,
                        module
                    );
                }
            }
        });
    }


    private String compileSchemaDescription(Maybe<FeatureWithSlots> feature) {
        final Maybe<String> featureName = feature.__(FeatureWithSlots::getName);
        final Maybe<String> synthSchemaName =
            feature.__(this::synthesizeSchemaName);
        if (feature.isNothing()
            || featureName.isNothing()
            || synthSchemaName.isNothing()) {
            return "";
        }

        final FeatureWithSlots featuresafe = feature.toNullable();
        final String vocabularyName = featureName.toNullable();
        final String schemaPrefix = synthSchemaName.toNullable();

        StringBuilder sb = new StringBuilder();

        String schemaVarName = schemaPrefix + featuresafe.getName();
        String schemaTypeName = getSchemaTypeName(featuresafe);
        sb.append(schemaTypeName).append(" ").append(schemaVarName);
        sb.append(" = (").append(schemaTypeName)
            .append(") getSchema(").append(vocabularyName).append(");");

        sb.append("\n");
        for (Maybe<SlotDeclaration> slot :
            iterate(feature.__(FeatureWithSlots::getSlots))) {
            sb.append(compileSlot(slot, vocabularyName, schemaVarName));
            sb.append("\n");
        }

        final Maybe<JvmParameterizedTypeReference> superType =
            feature.__(ExtendingFeature::getSuperType);

        if (superType.isPresent()) {
            sb.append(schemaVarName).append(".addSuperSchema((")
                .append(getSchemaTypeName(featuresafe))
                .append(") getSchema(")
                .append(superType.toNullable().getSimpleName())
                .append("));");

        }

        return sb.toString();
    }


    private @NotNull String compileSlot(
        Maybe<SlotDeclaration> declaration, String fatherVocabularyName,
        String enclosingSchemaName
    ) {
        Maybe<String> slotName = declaration.__(SlotDeclaration::getName);
        Maybe<TypeExpression> slotType =
            declaration.__(SlotDeclaration::getType);

        if (slotName.isNothing()) {
            return "";
        }

        if (slotType.isNothing()) {
            return "";
        }

        final String safeSlotName = slotName.toNullable();
        final TypeExpression safeSlotType = slotType.toNullable();

        String vocabularyName = fatherVocabularyName + "_" + safeSlotName;

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);
        if (safeSlotType.getSubExprs() != null
            && safeSlotType.getSubExprs().size() > 1) {
            //Tuples
            EList<TypeExpression> typeParameters = safeSlotType.getSubExprs();

            return enclosingSchemaName + ".add(" + vocabularyName + ", " +
                "(jade.content.schema.AgentActionSchema) getSchema(\"" +
                TupleType.getAdHocTupleClassName(typeParameters.stream()
                    .map(Maybe::some)
                    .map(tes::toJadescriptType)
                    .collect(Collectors.toList())) + "\"));";

        } else if (safeSlotType.getCollectionTypeExpression() != null) {
            EList<TypeExpression> typeParameters =
                safeSlotType.getCollectionTypeExpression().getTypeParameters();
            final String collectionType =
                safeSlotType.getCollectionTypeExpression().getCollectionType();

            final BuiltinTypeProvider builtins =
                module.get(BuiltinTypeProvider.class);

            if (collectionType.equals("list")) {
                IJadescriptType elemType =
                    builtins.any("No element type specified.");
                if (typeParameters.size() == 1) {
                    elemType = tes.toJadescriptType(
                        some(typeParameters.get(0))
                    );
                }

                return enclosingSchemaName + ".add("
                    + vocabularyName + ", " +
                    "(jade.content.schema.ConceptSchema) getSchema(\"" +
                    ListType.getAdHocListClassName(elemType) +
                    "\"));";
            }

            if (collectionType.equals("map")) {
                IJadescriptType keyType =
                    builtins.any("No key type specified.");
                IJadescriptType valType =
                    builtins.any("No value type specified.");
                if (typeParameters.size() == 2) {
                    keyType = tes.toJadescriptType(
                        some(typeParameters.get(0)));
                    valType = tes.toJadescriptType(
                        some(typeParameters.get(1)));
                }

                return enclosingSchemaName + ".add(" +
                    vocabularyName + ", " +
                    "(jade.content.schema.ConceptSchema) getSchema(\"" +
                    MapType.getAdHocMapClassName(keyType, valType) +
                    "\"));";
            }

            if (collectionType.equals("set")) {
                IJadescriptType elemType =
                    builtins.any("No element type specified.");
                if (typeParameters.size() == 1) {
                    elemType = tes.toJadescriptType(
                        some(typeParameters.get(0)));
                }

                return enclosingSchemaName + ".add(" +
                    vocabularyName + ", " +
                    "(jade.content.schema.ConceptSchema) getSchema(\"" +
                    SetType.getAdHocSetClassName(elemType) +
                    "\"));";
            }

            throw new RuntimeException(
                "unsupported collection type: " +
                    collectionType
            );
        }

        Maybe<String> schemaType = getSchemaKindForSlot(safeSlotType);
        String schemaExpression =
            getGetSchemaExpressionForSlot(safeSlotType);

        if (schemaType.isPresent()) {
            return enclosingSchemaName + ".add(" +
                vocabularyName + ", " +
                "(" + schemaType.toNullable() + ") " + schemaExpression + ");";
        } else {
            return "";
        }


    }


    private boolean isSchemaCompilable(Feature feature) {
        if (feature instanceof FeatureWithSlots) {
            for (SlotDeclaration slotDeclaration :
                ((FeatureWithSlots) feature).getSlots()) {
                TypeExpression type = slotDeclaration.getType();
                Maybe<String> schemaKindForSlot = getSchemaKindForSlot(type);
                if (!schemaKindForSlot.isPresent()) {
                    return false;
                }
            }
        }
        return true;
    }


    private Maybe<String> getSchemaKindForSlot(TypeExpression slotType) {
        IJadescriptType type = module.get(TypeExpressionSemantics.class)
            .toJadescriptType(some(slotType));
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);


        if (type.category().isMap()
            || type.category().isSet()
            || type.category().isList()) {
            return some("jade.content.schema.ConceptSchema");
        }
        if (type.category().isTuple()) {
            return some("jade.content.schema.AgentActionSchema");
        }
        if (Stream.of(
                builtins.concept(),
                builtins.aid(),
                builtins.timestamp(),
                builtins.duration()
            ).map(t -> comparator.compare(t, type))
            .anyMatch(r -> r.is(superTypeOrEqual()))) {
            return some("jade.content.schema.ConceptSchema");
        }
        if (Stream.of(
                builtins.proposition(),
                builtins.predicate(),
                builtins.atomicProposition()
            ).map(t -> comparator.compare(t, type))
            .anyMatch(r -> r.is(superTypeOrEqual()))) {
            return some("jade.content.schema.PredicateSchema");
        }

        if (comparator.compare(builtins.action(), type)
            .is(superTypeOrEqual())) {
            return some("jade.content.schema.AgentActionSchema");
        }
        if (typeHelper.isTypeWithPrimitiveOntologySchema(type)) {
            return some("jade.content.schema.PrimitiveSchema");
        }

        return nothing();
    }


    private String getSchemaNameForSlot(TypeExpression slotTypeExpression) {
        if (slotTypeExpression == null) {
            return "";
        }
        return module.get(TypeExpressionSemantics.class)
            .toJadescriptType(some(slotTypeExpression))
            .getSlotSchemaName();
    }


    private String getGetSchemaExpressionForSlot(
        TypeExpression slotTypeExpression
    ) {
        if (slotTypeExpression == null) {
            return "";
        }
        return module.get(TypeExpressionSemantics.class)
            .toJadescriptType(some(slotTypeExpression))
            .getGetSlotSchemaExpression();
    }


    private String getSchemaTypeName(FeatureWithSlots schema) {
        if (schema instanceof OntologyAction) {
            return "jade.content.schema.AgentActionSchema";
        } else if (schema instanceof Concept) {
            return "jade.content.schema.ConceptSchema";
        } else { //predicate or proposition
            return "jade.content.schema.PredicateSchema";
        }
    }


    private String compileAddSchema(Maybe<ExtendingFeature> feature) {
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        JvmTypeReference type = feature.__(featureSafe -> {
            if (featureSafe instanceof Concept) {
                return jvm.typeRef(
                    jade.content.schema.ConceptSchema.class
                );
            } else if (featureSafe instanceof OntologyAction) {
                return jvm.typeRef(
                    jade.content.schema.AgentActionSchema.class
                );
            } else if (featureSafe instanceof Predicate
                || featureSafe instanceof Proposition) {
                return jvm.typeRef(
                    jade.content.schema.PredicateSchema.class
                );
            }
            return jvm.objectTypeRef();
        }).orElse(jvm.objectTypeRef());

        String featureName = feature.__(ExtendingFeature::getName)
            .orElse("");

        final String obtainClass;

        if (feature.__(ExtendingFeature::isNative).orElse(false)) {
            obtainClass = retrieveNativeTypeFactory(
                //feature in this branch is safe to extract:
                feature.toNullable()
            ) + ".getImplementationClass()";
        } else {
            obtainClass = featureName + ".class";
        }
        return "add(new " + type.getQualifiedName('.') + "(" +
            featureName + "), " + obtainClass + ");";
    }


    @SuppressWarnings("unchecked")
    private void addVocabulary(
        EList<JvmMember> members,
        Maybe<? extends ExtendingFeature> feature
    ) {
        Maybe<String> vocabularyName = feature.__(ExtendingFeature::getName);
        addVocabularyElement(members, feature, vocabularyName, vocabularyName);

        if (feature.__(f -> f instanceof FeatureWithSlots)
            .orElse(false)) {
            for (Maybe<SlotDeclaration> slot :
                iterate(((Maybe<FeatureWithSlots>) feature).__(
                    FeatureWithSlots::getSlots))) {
                Maybe<String> slotName = slot.__(SlotDeclaration::getName);
                Maybe<String> name =
                    vocabularyName.__(v -> v + "_" + slotName.orElse(""));
                addVocabularyElement(members, feature, name, slotName);
            }
        }
    }


    private void addVocabularyElement(
        EList<JvmMember> members,
        Maybe<? extends ExtendingFeature> ontologyElement,
        Maybe<String> name,
        Maybe<String> init
    ) {
        if (members == null) {
            return;
        }
        if (ontologyElement.isNothing()) {
            return;
        }
        if (name.isNothing()) {
            return;
        }
        if (init.isNothing()) {
            return;
        }

        final ExtendingFeature ontologyElementSafe =
            ontologyElement.toNullable();
        final String nameSafe = name.toNullable();
        final String initSafe = init.toNullable();

        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        members.add(module.get(JvmTypesBuilder.class).toField(
            ontologyElementSafe,
            nameSafe,
            jvm.typeRef(String.class),
            it -> {
                it.setVisibility(JvmVisibility.PUBLIC);
                it.setStatic(true);
                it.setFinal(true);
                module.get(CompilationHelper.class).createAndSetInitializer(
                    it,
                    scb -> scb.add("\"" + initSafe + "\"")
                );
            }
        ));
    }


    private void addElementFactoryMethod(
        Maybe<Ontology> input,
        EList<JvmMember> members,
        Maybe<ExtendingFeature> ontoElement
    ) {
        if (ontoElement.isNothing()) {
            return;
        }

        Maybe<String> ontoElementName =
            ontoElement.__(ExtendingFeature::getName);

        if (ontoElementName.isNothing()) {
            return;
        }

        final String ontoElementNameSafe = ontoElementName.toNullable();

        final ExtendingFeature ontoElementSafe = ontoElement.toNullable();


        JvmTypeReference returnType;
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);

        if (declaredSchemaTypes.getOrNew(input)
            .containsKey(ontoElementNameSafe)) {

            returnType = jvm.typeRef(
                declaredSchemaTypes.getOrNew(input)
                    .get(ontoElementNameSafe)
            );
        } else {
            returnType = jvm.typeRef(ontoElementNameSafe);
        }

        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);

        members.add(jvmTB.toMethod(
            ontoElementSafe,
            ontoElementNameSafe,
            returnType,
            it -> {
                it.setStatic(true);
                it.setVisibility(JvmVisibility.PUBLIC);

                boolean isWithSlots =
                    ontoElementSafe instanceof FeatureWithSlots;

                final TypeExpressionSemantics tes =
                    module.get(TypeExpressionSemantics.class);

                if (isWithSlots) {
                    populateParametersFromSlots(
                        ontoElementSafe,
                        jvmTB,
                        it,
                        tes
                    );
                }

                final String methodName;

                if (ontoElement
                    .__(ExtendingFeature::isNative)
                    .orElse(false)) {

                    final String methodNamePrefix =
                        retrieveNativeTypeFactory(ontoElementSafe);

                    if (isWithSlots) {
                        methodName = methodNamePrefix + ".create";
                    } else {
                        methodName = methodNamePrefix + ".empty";
                    }

                } else {
                    methodName = "new " + ontoElementNameSafe;
                }

                final CompilationHelper compilationHelper =
                    module.get(CompilationHelper.class);

                compilationHelper.createAndSetBody(it, scb -> {

                    StringBuilder line =
                        new StringBuilder("return " + methodName + "(");

                    if (isWithSlots) {
                        populateArgumentsFromSlots(
                            (FeatureWithSlots) ontoElementSafe,
                            tes,
                            line
                        );
                    }
                    line.append(");");
                    scb.line(line.toString());
                });
            }
        ));

    }


    private void populateArgumentsFromSlots(
        FeatureWithSlots ontoElementSafe,
        TypeExpressionSemantics tes,
        StringBuilder line
    ) {

        for (int i = 0; i < ontoElementSafe.getSlots().size(); i++) {
            SlotDeclaration slot = ontoElementSafe.getSlots().get(i);

            final IJadescriptType slotType =
                tes.toJadescriptType(
                    some(slot).__(SlotDeclaration::getType));

            if (slotType
                instanceof DeclaresOntologyAdHocClass) {
                DeclaresOntologyAdHocClass adHocType =
                    (DeclaresOntologyAdHocClass) slotType;

                final String adHocClassName =
                    adHocType.getAdHocClassName();

                final String converterName = adHocType
                    .getConverterToAdHocClassMethodName();

                line.append(adHocClassName)
                    .append(".")
                    .append(converterName)
                    .append("(")
                    .append(slot.getName())
                    .append(")");
            } else {
                line.append(slot.getName());
            }

            if (i < ontoElementSafe.getSlots().size() - 1) {
                line.append(", ");
            }

        }
    }


    private void populateParametersFromSlots(
        ExtendingFeature ontoElementSafe,
        JvmTypesBuilder jvmTB,
        JvmOperation it,
        TypeExpressionSemantics tes
    ) {
        final EList<SlotDeclaration> slots =
            ((FeatureWithSlots) ontoElementSafe).getSlots();

        if (slots != null) {
            for (SlotDeclaration slot : slots) {
                if (slot == null
                    || slot.getName() == null
                    || slot.getType() == null) {
                    continue;
                }

                IJadescriptType type =
                    tes.toJadescriptType(some(slot.getType()));

                it.getParameters().add(jvmTB.toParameter(
                    ontoElementSafe,
                    slot.getName(),
                    type.asJvmTypeReference()
                ));
            }
        }
    }


    @NotNull
    private String retrieveNativeTypeFactory(
        ExtendingFeature ontologyElementSafe
    ) {
        final String ontoElementFqName =
            some(module.get(CompilationHelper.class)
                .getFullyQualifiedName(ontologyElementSafe))
                .__(fqn -> fqn.toString("."))
                .orElse("");


        return "((" + ontoElementFqName + "Factory) " +
            "(jadescript.java.Jadescript." +
            "getNativeFactory(" + ontoElementFqName + ".class)))";
    }


    @Override
    public void generateDeclaredTypes(
        Maybe<Ontology> input,
        IJvmDeclaredTypeAcceptor acceptor,
        boolean isPreIndexingPhase
    ) {

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        final Maybe<String> ontoFqName = input
            .__(compilationHelper::getFullyQualifiedName)
            .__(qn -> qn.toString("."));

        //creates all the classes of the elements of the ontology:
        module.get(ContextManager.class).enterTopLevelDeclaration(
            (module, outer) -> new OntologyDeclarationSupportContext(
                module,
                outer,
                input,
                ontoFqName
            )
        );

        final OntologyElementSemantics oes =
            module.get(OntologyElementSemantics.class);

        for (Maybe<? extends Feature> feature :
            iterate(input.__(FeatureContainer::getFeatures))) {

            if (input.isNothing()) {
                continue;
            }

            if (feature.isNothing()) {
                continue;
            }

            final Feature featureSafe = feature.toNullable();

            if (!(featureSafe instanceof ExtendingFeature)) {
                continue;
            }

            Maybe<QualifiedName> ontoFullQualifiedName =
                input.__(compilationHelper::getFullyQualifiedName);

            List<JvmDeclaredType> pojoTypes = oes.declareTypes(
                feature.__(f -> (ExtendingFeature) f),
                ontoFullQualifiedName,
                isPreIndexingPhase
            );

            for (JvmDeclaredType pojoTypeSafe : pojoTypes) {
                acceptor.accept(pojoTypeSafe);
            }

            if (!pojoTypes.isEmpty()) {
                declaredSchemaTypes.getOrNew(input).put(
                    ((ExtendingFeature) featureSafe).getName(),
                    pojoTypes.get(0)
                );
                schemaFeatures.getOrNew(input).put(
                    ((ExtendingFeature) featureSafe).getName(),
                    (ExtendingFeature) featureSafe
                );
            }
        }
        module.get(ContextManager.class).exit();

        //creates the vocabulary interface:
        generateVocabularyInterface(input, acceptor, isPreIndexingPhase);

        //then creates the main ontology class:
        super.generateDeclaredTypes(input, acceptor, isPreIndexingPhase);
    }


    private void generateVocabularyInterface(
        Maybe<Ontology> input,
        IJvmDeclaredTypeAcceptor acceptor,
        boolean isPreIndexingPhase
    ) {

        if (input.isNothing()) {
            return;
        }

        final Ontology inputSafe = input.toNullable();

        Maybe<String> name = input.__(NamedElement::getName);
        if (isNameAlwaysRequired() && name.isNothing()) {
            return;
        }
        final CompilationHelper compilationHelper = module.get(
            CompilationHelper.class);
        final Maybe<QualifiedName> fqn =
            input.__(compilationHelper::getFullyQualifiedName);

        if (fqn.isNothing()) {
            return;
        }

        final QualifiedName fqnSafe =
            fqn.toNullable();

        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);

        final String vocabularyName = fqnSafe + "_Vocabulary";
        acceptor.accept(jvmTB.toInterface(inputSafe, vocabularyName, it -> {
            if (!isPreIndexingPhase) {
                final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);

                JvmTypeReference superOntologyType = input
                    .__(FeatureContainer::getSuperTypes)
                    .nullIf(List::isEmpty)
                    .__partial2(List::get, 0)
                    .__(t -> (JvmTypeReference) t)
                    .orElse(jvm.typeRef(
                        jadescript.content.onto.Ontology.class
                    ));

                it.getSuperTypes().add(
                    jvm.typeRef(
                        superOntologyType.getQualifiedName('.') +
                            "_Vocabulary")
                );

                for (Maybe<? extends Feature> feature :
                    iterate(input.__(FeatureContainer::getFeatures))) {
                    addVocabulary(
                        it.getMembers(),
                        feature.__(f -> (ExtendingFeature) f)
                    );
                }

            }
        }));
    }


    @Override
    public void populateMainSuperTypes(
        Maybe<Ontology> input,
        EList<JvmTypeReference> superTypes
    ) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        superTypes.add(builtins.ontology().asJvmTypeReference());
        final CompilationHelper compilationHelper = module.get(
            CompilationHelper.class);
        Maybe<QualifiedName> fullyQualifiedName =
            input.__(compilationHelper::getFullyQualifiedName);
        fullyQualifiedName.safeDo(fullyQualifiedNameSafe -> {
            superTypes.add(
                jvm.typeRef(fullyQualifiedNameSafe + "_Vocabulary")
            );
        });
        super.populateMainSuperTypes(input, superTypes);

    }

}
