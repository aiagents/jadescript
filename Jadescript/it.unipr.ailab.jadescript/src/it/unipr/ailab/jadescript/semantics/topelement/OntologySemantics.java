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
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.*;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsClassState;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import jade.core.AID;
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

import static it.unipr.ailab.maybe.Functional.filterAndCast;
import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 27/04/18.
 */
@Singleton
public class OntologySemantics extends FeatureContainerSemantics<Ontology> {

    private final SemanticsClassState<Ontology, HashMap<String, JvmDeclaredType>> declaredSchemaTypes
            = new SemanticsClassState<>(HashMap::new);
    private final SemanticsClassState<Ontology, HashMap<String, ExtendingFeature>> schemaFeatures
            = new SemanticsClassState<>(HashMap::new);

    public OntologySemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected void prepareAndEnterContext(Maybe<Ontology> input, JvmDeclaredType jvmDeclaredType) {
        module.get(ContextManager.class).enterTopLevelDeclaration((module, outer) ->
                new OntologyDeclarationContext(module, outer, jvmDeclaredType));
    }

    @Override
    protected void exitContext(Maybe<Ontology> input) {
        module.get(ContextManager.class).exit();//OntologyDeclarationContext
    }


    @SuppressWarnings("unchecked")
    @Override
    public void validate(Maybe<Ontology> input, ValidationMessageAcceptor acceptor) {
        super.validate(input, acceptor);
        if (input == null) return;

        Maybe<EList<JvmParameterizedTypeReference>> superTypes = input.__(FeatureContainer::getSuperTypes);//FUTURETODO multiple ontologies

        if (!superTypes
                .__(List::isEmpty)
                .extract(nullAsTrue)) {
            module.get(ValidationHelper.class).assertExpectedType(
                    jade.content.onto.Ontology.class,
                    superTypes//FUTURETODO multiple ontologies
                            .__(EList::get, 0)
                            .__(st -> module.get(TypeHelper.class).jtFromJvmTypeRef(st))
                            .orElse(module.get(TypeHelper.class).ANY),
                    "NotAValidOntologyTypeReference",
                    input,
                    JadescriptPackage.eINSTANCE.getFeatureContainer_SuperTypes(),
                    0,
                    acceptor
            );
        }

        module.get(ContextManager.class).enterTopLevelDeclaration((mod, out) ->
                new OntologyDeclarationSupportContext(mod, out, input));

        for (Maybe<? extends Feature> feature : iterate(input.__(FeatureContainer::getFeatures))) {
            InterceptAcceptor acceptor1 = new InterceptAcceptor(acceptor);

            //noinspection unchecked
            module.get(OntologyElementSemantics.class).validate((Maybe<ExtendingFeature>) feature, acceptor1);
        }

        module.get(ContextManager.class).exit();
    }


    @SuppressWarnings("unchecked")
    @Override
    public void populateMainMembers(Maybe<Ontology> input, EList<JvmMember> members, JvmDeclaredType itClass) {
        super.populateMainMembers(input, members, itClass);
        JvmTypeReference superOntologyType = input//FUTURETODO multiple ontologies
                .__(FeatureContainer::getSuperTypes)
                .nullIf(List::isEmpty)
                .__(List::get, 0)
                .__(t -> (JvmTypeReference) t)
                .orElse(module.get(TypeHelper.class).typeRef(jadescript.content.onto.Ontology.class));

        input.safeDo(inputsafe -> {


            members.add(module.get(JvmTypesBuilder.class).toField(inputsafe, SUPER_ONTOLOGY_VAR, //FUTURETODO multiple ontologies
                    superOntologyType, itField -> {
                        itField.setVisibility(JvmVisibility.PRIVATE);
                        itField.setStatic(true);
                        module.get(CompilationHelper.class).createAndSetInitializer(
                                itField,
                                scb -> scb.add("null")
                        );
                    }
            ));

            final String ontologyName = module.get(CompilationHelper.class)
                    .getFullyQualifiedName(inputsafe)
                    .toString("_");
            input.__(NamedElement::getName).safeDo(nameSafe -> {
                members.add(module.get(JvmTypesBuilder.class).toField(
                        inputsafe,
                        ONTOLOGY_STATIC_INSTANCE_NAME,
                        module.get(TypeHelper.class).typeRef(jadescript.content.onto.Ontology.class),
                        itField -> {
                            itField.setVisibility(JvmVisibility.PRIVATE);
                            itField.setStatic(true);
                            module.get(CompilationHelper.class).createAndSetInitializer(
                                    itField,
                                    scb -> scb.add("new " + nameSafe + "()")
                            );
                        }
                ));
                members.add(module.get(JvmTypesBuilder.class).toField(
                        inputsafe,
                        "__NAME",
                        module.get(TypeHelper.class).typeRef(String.class),
                        itField -> {
                            itField.setVisibility(JvmVisibility.PUBLIC);
                            itField.setStatic(true);
                            itField.setFinal(true);
                            module.get(CompilationHelper.class).createAndSetInitializer(
                                    itField,
                                    scb -> scb.add("\"" + ontologyName + "\"")
                            );
                        }
                ));
            });

            members.add(module.get(JvmTypesBuilder.class).toMethod(inputsafe, "getInstance",
                    module.get(TypeHelper.class).typeRef(jadescript.content.onto.Ontology.class), it -> {
                        it.setStatic(true);
                        module.get(CompilationHelper.class).createAndSetBody(
                                it,
                                scb -> scb.line("return " + ONTOLOGY_STATIC_INSTANCE_NAME + ";")
                        );
                    }
            ));


            HashMap<String, String> generatedMapOrSetClasses = new HashMap<>();
            List<StatementWriter> addAdHocSchemaWriters = new ArrayList<>();
            List<StatementWriter> descriptionAdHocSchemaWriters = new ArrayList<>();
            for (Maybe<? extends Feature> feature : iterate(input.__(FeatureContainer::getFeatures))) {
                addElementFactoryMethod(input, members, (Maybe<ExtendingFeature>) feature);
                addSpecificMapOrSetClasses(
                        members,
                        (Maybe<ExtendingFeature>) feature,
                        generatedMapOrSetClasses,
                        addAdHocSchemaWriters,
                        descriptionAdHocSchemaWriters
                );
            }


            final JvmTypeReference superType = superOntologyType;

            members.add(module.get(JvmTypesBuilder.class).toConstructor(
                    inputsafe,
                    it -> module.get(CompilationHelper.class).createAndSetBody(it, scb -> {

                        scb.line("super(__NAME, " + superType.getQualifiedName('.') +
                                ".getInstance(), new jade.content.onto.CFReflectiveIntrospector());");


                        if (!input
                                .__(FeatureContainer::getFeatures)
                                .__(List::isEmpty)
                                .extract(nullAsTrue)) {
                            scb.line("try {").indent();
                            boolean writingSomething = false;

                            for (StatementWriter addAdHocSchemaWriter : addAdHocSchemaWriters) {
                                addAdHocSchemaWriter.writeSonnet(scb);
                                writingSomething = true;
                            }

                            scb.line().line();
                            for (Maybe<? extends Feature> feature : iterate(input.__(FeatureContainer::getFeatures))) {
                                if (feature.isPresent() && isSchemaCompilable(feature.toNullable())) {
                                    scb.line(compileAddSchema((Maybe<ExtendingFeature>) feature));
                                    writingSomething = true;
                                }
                            }
                            scb.line().line();

                            for (StatementWriter sw : descriptionAdHocSchemaWriters) {
                                sw.writeSonnet(scb);
                                writingSomething = true;
                            }

                            scb.line().line();
                            for (Maybe<? extends Feature> feature : iterate(input.__(FeatureContainer::getFeatures))) {
                                if (feature.__(f -> f instanceof FeatureWithSlots).extract(nullAsFalse)
                                        && feature.isPresent()
                                        && isSchemaCompilable(feature.toNullable())) {
                                    scb.line(compileSchemaDescription((Maybe<FeatureWithSlots>) feature));
                                    writingSomething = true;
                                }
                            }

                            scb.dedent().line("} catch (" +
                                    (writingSomething ? "jade.content.onto.OntologyException" : "java.lang.RuntimeException") +
                                    " e) {").indent();
                            scb.line("e.printStackTrace();");
                            scb.dedent().line("}");
                        }
                    })
            ));
        });
    }

    private void addSpecificMapOrSetClasses(
            EList<JvmMember> members,
            Maybe<ExtendingFeature> feature,
            HashMap<String, String> generatedSpecificClasses,
            List<StatementWriter> addSchemaWriters,
            List<StatementWriter> descriptionSchemaWriters
    ) {
        Optional<FeatureWithSlots> featureWithSlots = feature.stream()
                .flatMap(filterAndCast(FeatureWithSlots.class))
                .findAny();

        featureWithSlots.ifPresent(featureSafe -> {
            for (SlotDeclaration slotDeclaration : featureSafe.getSlots()) {
                TypeExpression typeExpression = slotDeclaration.getType();
                IJadescriptType type = module.get(TypeExpressionSemantics.class).toJadescriptType(
                    some(typeExpression));

                if (type instanceof DeclaresOntologyAdHocClass) {
                    ((DeclaresOntologyAdHocClass) type).declareSpecificOntologyClass(
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
        StringBuilder sb = new StringBuilder();

        safeDo(
                feature,
                feature.__(FeatureWithSlots::getName),
                feature.__(this::synthesizeSchemaName),
                (featuresafe, vocabularyName, schemaPrefix) -> {


                    String schemaVarName = schemaPrefix + featuresafe.getName();
                    String schemaTypeName = getSchemaTypeName(featuresafe);
                    sb.append(schemaTypeName).append(" ").append(schemaVarName);
                    sb.append(" = (").append(schemaTypeName).append(") getSchema(").append(vocabularyName).append(");");
                    sb.append("\n");
                    for (Maybe<SlotDeclaration> slot : iterate(feature.__(FeatureWithSlots::getSlots))) {
                        sb.append(compileSlot(slot, vocabularyName, schemaVarName));
                        sb.append("\n");
                    }
                    final Maybe<JvmParameterizedTypeReference> superType = feature.__(ExtendingFeature::getSuperType);

                    if (superType.isPresent()) {
                        sb.append(schemaVarName).append(".addSuperSchema((")
                                .append(getSchemaTypeName(featuresafe))
                                .append(") getSchema(")
                                .append(superType.toNullable().getSimpleName())
                                .append("));");

                    }
                }
        );

        return sb.toString();
    }

    private String compileSlot(
            Maybe<SlotDeclaration> declaration, String fatherVocabularyName,
            String enclosingSchemaName
    ) {
        Maybe<String> slotName = declaration.__(SlotDeclaration::getName);
        Maybe<TypeExpression> slotType = declaration.__(SlotDeclaration::getType);

        return slotName.__(safeSlotName -> slotType.__(safeSlotType -> {
            String vocabularyName = fatherVocabularyName + "_" + safeSlotName;

            final TypeExpressionSemantics typeExpressionSemantics = module.get(TypeExpressionSemantics.class);
            if (safeSlotType.getSubExprs() != null && safeSlotType.getSubExprs().size() > 1) {
                //Tuples
                EList<TypeExpression> typeParameters = safeSlotType.getSubExprs();

                return enclosingSchemaName + ".add(" + vocabularyName + ", " +
                        "(jade.content.schema.AgentActionSchema) getSchema(\"" +
                        TupleType.getAdHocTupleClassName(typeParameters.stream()
                                .map(Maybe::some)
                                .map(typeExpressionSemantics::toJadescriptType)
                                .collect(Collectors.toList())) + "\"));";
            } else if (safeSlotType.getCollectionTypeExpression() != null) {
                EList<TypeExpression> typeParameters = safeSlotType.getCollectionTypeExpression().getTypeParameters();
                switch (safeSlotType.getCollectionTypeExpression().getCollectionType()) {
                    case "list": {
                        Maybe<String> elementType = getSchemaKindForSlot(typeParameters.get(0));
                        String elementSchema = getSchemaNameForSlot(typeParameters.get(0));
                        if (elementType.isPresent()) {
                            return enclosingSchemaName + ".add(" +
                                    vocabularyName + ", " +
                                    "(" + elementType.toNullable() + ") getSchema(" + elementSchema + ")," +
                                    "0, " +
                                    "jade.content.schema.ObjectSchema.UNLIMITED" +
                                    ");";
                        } else {
                            return null;
                        }


                    }
                    case "map":
                        IJadescriptType keyType = module.get(TypeHelper.class).ANY;
                        IJadescriptType valType = module.get(TypeHelper.class).ANY;
                        if (typeParameters.size() == 2) {
                            keyType = typeExpressionSemantics.toJadescriptType(
                                some(typeParameters.get(0)));
                            valType = typeExpressionSemantics.toJadescriptType(
                                some(typeParameters.get(1)));
                        }

                        return enclosingSchemaName + ".add(" +
                                vocabularyName + ", " +
                                "(jade.content.schema.ConceptSchema) getSchema(\"" +
                                MapType.getAdHocMapClassName(keyType, valType) +
                                "\"));";
                    case "set":
                        IJadescriptType elemType = module.get(TypeHelper.class).ANY;
                        if (typeParameters.size() == 1) {
                            elemType = typeExpressionSemantics.toJadescriptType(
                                some(typeParameters.get(0)));
                        }

                        return enclosingSchemaName + ".add(" +
                                vocabularyName + ", " +
                                "(jade.content.schema.ConceptSchema) getSchema(\"" +
                                SetType.getAdHocSetClassName(elemType) +
                                "\"));";
                    default:
                        throw new RuntimeException("unsupported collection type: " + safeSlotType.getCollectionTypeExpression().getCollectionType());
                }
            }

            Maybe<String> schemaType = getSchemaKindForSlot(safeSlotType);
            String schemaExpression = getGetSchemaExpressionForSlot(safeSlotType);

            if (schemaType.isPresent()) {
                return enclosingSchemaName + ".add(" +
                        vocabularyName + ", " +
                        "(" + schemaType.toNullable() + ") " + schemaExpression + ");";
            } else {
                return null;
            }
        }).extract(nullAsEmptyString)).extract(nullAsEmptyString);


    }

    private boolean isSchemaCompilable(Feature feature) {
        if (feature instanceof FeatureWithSlots) {
            for (SlotDeclaration slotDeclaration : ((FeatureWithSlots) feature).getSlots()) {
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
        IJadescriptType type = module.get(TypeExpressionSemantics.class).toJadescriptType(
            some(slotType));
        if (type instanceof MapType || type instanceof SetType) {
            return some("jade.content.schema.ConceptSchema");
        } else if (type instanceof TupleType) {
            return some("jade.content.schema.AgentActionSchema");
        } else if (type instanceof ListType) {
            return some("jade.content.schema.TermSchema");
        } else if (module.get(TypeHelper.class).CONCEPT.isAssignableFrom(type)
                || module.get(TypeHelper.class).AID.isAssignableFrom(type)
                || module.get(TypeHelper.class).TIMESTAMP.isAssignableFrom(type)
                || module.get(TypeHelper.class).DURATION.isAssignableFrom(type)) {
            return some("jade.content.schema.ConceptSchema");
        } else if (module.get(TypeHelper.class).PROPOSITION.isAssignableFrom(type)
                || module.get(TypeHelper.class).PREDICATE.isAssignableFrom(type)
                || module.get(TypeHelper.class).ATOMIC_PROPOSITION.isAssignableFrom(type)) {
            return some("jade.content.schema.PredicateSchema");
        } else if (module.get(TypeHelper.class).ACTION.isAssignableFrom(type)) {
            return some("jade.content.schema.AgentActionSchema");
        } else if (module.get(TypeHelper.class).isTypeWithPrimitiveOntologySchema(type)) {
            return some("jade.content.schema.PrimitiveSchema");
        } else {
            return nothing();
        }
    }

    private String getSchemaNameForSlot(TypeExpression slotTypeExpression) {
        if (slotTypeExpression == null) {
            return "";
        }
        return module.get(TypeExpressionSemantics.class).toJadescriptType(some(slotTypeExpression))
                .getSlotSchemaName();
    }

    private String getGetSchemaExpressionForSlot(TypeExpression slotTypeExpression){
        if (slotTypeExpression == null) {
            return "";
        }
        return module.get(TypeExpressionSemantics.class).toJadescriptType(some(slotTypeExpression))
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
        JvmTypeReference type = feature.__(featureSafe -> {
            if (featureSafe instanceof Concept || featureSafe instanceof AID) {
                return module.get(TypeHelper.class).typeRef(jade.content.schema.ConceptSchema.class);
            } else if (featureSafe instanceof OntologyAction) {
                return module.get(TypeHelper.class).typeRef(jade.content.schema.AgentActionSchema.class);
            } else if (featureSafe instanceof Predicate || featureSafe instanceof Proposition) {
                return module.get(TypeHelper.class).typeRef(jade.content.schema.PredicateSchema.class);
            }
            return module.get(TypeHelper.class).typeRef(Object.class);
        }).orElse(module.get(TypeHelper.class).typeRef(Object.class));
        String featureName = feature.__(ExtendingFeature::getName).extract(nullAsEmptyString);
        final String obtainClass;
        if (feature.__(ExtendingFeature::isNative).extract(nullAsFalse)) {
            obtainClass = retrieveNativeTypeFactory(
                    //feature in this branch is safe to extract:
                    feature.toNullable()
            ) + ".getImplementationClass()";
        } else {
            obtainClass = featureName + ".class";
        }
        return "add(new " + type.getQualifiedName('.') + "(" + featureName + "), " + obtainClass + ");";
    }


    @SuppressWarnings("unchecked")
    private void addVocabulary(
            EList<JvmMember> members,
            Maybe<? extends ExtendingFeature> feature
    ) {
        Maybe<String> vocabularyName = feature.__(ExtendingFeature::getName);
        addVocabularyElement(members, feature, vocabularyName, vocabularyName);

        if (feature.__(f -> f instanceof FeatureWithSlots).extract(nullAsFalse)) {
            for (Maybe<SlotDeclaration> slot : iterate(((Maybe<FeatureWithSlots>) feature).__(FeatureWithSlots::getSlots))) {
                Maybe<String> slotName = slot.__(SlotDeclaration::getName);
                Maybe<String> name = vocabularyName.__(v -> v + "_" + slotName.extract(nullAsEmptyString));
                addVocabularyElement(members, feature, name, slotName);
            }
        }
    }


    private void addVocabularyElement(
            EList<JvmMember> members, Maybe<? extends ExtendingFeature> ontologyElement,
            Maybe<String> name, Maybe<String> init
    ) {
        if (members == null) return;
        safeDo(ontologyElement, name, init, (ontologyElementSafe, nameSafe, initSafe) -> {
                    members.add(module.get(JvmTypesBuilder.class).toField(ontologyElementSafe, nameSafe,
                            module.get(TypeHelper.class).typeRef(String.class), it -> {
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
        );
    }


    private void addElementFactoryMethod(
            Maybe<Ontology> input,
            EList<JvmMember> members,
            Maybe<ExtendingFeature> ontologyElement
    ) {
        Maybe<String> ontologyElementName = ontologyElement.__(ExtendingFeature::getName);
        safeDo(ontologyElement, ontologyElementName, (ontologyElementSafe, ontologyElementNameSafe) -> {
            JvmTypeReference returnType;
            if (declaredSchemaTypes.getOrNew(input).containsKey(ontologyElementNameSafe)) {
                returnType = module.get(TypeHelper.class).typeRef(
                        declaredSchemaTypes.getOrNew(input).get(ontologyElementNameSafe)
                );
            } else {
                returnType = module.get(TypeHelper.class).typeRef(ontologyElementNameSafe);
            }

            members.add(module.get(JvmTypesBuilder.class).toMethod(
                    ontologyElementSafe,
                    ontologyElementNameSafe,
                    returnType,
                    it -> {
                        it.setStatic(true);
                        it.setVisibility(JvmVisibility.PUBLIC);

                        boolean isWithSlots = ontologyElementSafe instanceof FeatureWithSlots;

                        final TypeExpressionSemantics typeExpressionSemantics = module.get(TypeExpressionSemantics.class);
                        if (isWithSlots) {
                            for (SlotDeclaration slot : ((FeatureWithSlots) ontologyElementSafe).getSlots()) {
                                if (slot == null || slot.getName() == null || slot.getType() == null)
                                    continue;
                                IJadescriptType type = typeExpressionSemantics.toJadescriptType(
                                    some(slot.getType()));

                                it.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                        ontologyElementSafe,
                                        slot.getName(),
                                        type.asJvmTypeReference()
                                ));
                            }
                        }


                        module.get(CompilationHelper.class).createAndSetBody(it, scb -> {
                            final String methodName;
                            if (ontologyElement.__(ExtendingFeature::isNative).extract(nullAsFalse)) {
                                final String methodNamePrefix = retrieveNativeTypeFactory(ontologyElementSafe);
                                if (isWithSlots) {
                                    methodName = methodNamePrefix + ".create";
                                } else {
                                    methodName = methodNamePrefix + ".empty";
                                }
                            } else {
                                methodName = "new " + ontologyElementNameSafe;
                            }
                            StringBuilder line = new StringBuilder("return " +
                                    methodName + "(");
                            if (isWithSlots) {
                                FeatureWithSlots withSlots = (FeatureWithSlots) ontologyElementSafe;
                                for (int i = 0; i < withSlots.getSlots().size(); i++) {
                                    SlotDeclaration slot = withSlots.getSlots().get(i);
                                    if (slot.getType().getSubExprs() != null && slot.getType().getSubExprs().size() > 1) {
                                        List<IJadescriptType> elementTypes = slot.getType().getSubExprs().stream()
                                                .map(Maybe::some)
                                                .map(typeExpressionSemantics::toJadescriptType)
                                                .collect(Collectors.toList());
                                        String className = TupleType.getAdHocTupleClassName(elementTypes);
                                        line.append(className)
                                                .append(".__fromTuple(")
                                                .append(withSlots.getSlots().get(i).getName())
                                                .append(")");

                                    } else if (slot.getType().getCollectionTypeExpression() != null) {
                                        EList<TypeExpression> typeParameters = slot.getType()
                                                .getCollectionTypeExpression().getTypeParameters();
                                        if (typeParameters != null
                                                && typeParameters.size() == 2
                                                && "map".equals(slot.getType().getCollectionTypeExpression().getCollectionType())) {
                                            IJadescriptType keyType = typeExpressionSemantics.toJadescriptType(
                                                some(typeParameters.get(0)));
                                            IJadescriptType valType = typeExpressionSemantics.toJadescriptType(
                                                some(typeParameters.get(1)));
                                            String className = MapType.getAdHocMapClassName(keyType, valType);
                                            line.append(className)
                                                    .append(".__fromMap(")
                                                    .append(withSlots.getSlots().get(i).getName())
                                                    .append(")");

                                        } else if (typeParameters != null
                                                && typeParameters.size() == 1
                                                && "set".equals(slot.getType().getCollectionTypeExpression().getCollectionType())) {
                                            IJadescriptType elemType = typeExpressionSemantics.toJadescriptType(
                                                some(typeParameters.get(0)));
                                            String className = SetType.getAdHocSetClassName(elemType);
                                            line.append(className)
                                                    .append(".__fromSet(")
                                                    .append(withSlots.getSlots().get(i).getName())
                                                    .append(")");

                                        } else if (typeParameters != null
                                                && typeParameters.size() == 1
                                                && "list".equals(slot.getType().getCollectionTypeExpression().getCollectionType())) {
                                            line.append(withSlots.getSlots().get(i).getName());
                                        }
                                    } else {
                                        line.append(withSlots.getSlots().get(i).getName());
                                    }
                                    if (i < withSlots.getSlots().size() - 1) {
                                        line.append(", ");
                                    }

                                }
                            }
                            line.append(");");
                            scb.line(line.toString());
                        });
                    }
            ));
        });

    }

    @NotNull
    private String retrieveNativeTypeFactory(ExtendingFeature ontologyElementSafe) {
        final String ontoElementFqName = module.get(CompilationHelper.class)
                .getFullyQualifiedName(ontologyElementSafe)
                .toString(".");

        final String methodNamePrefix = "(" +
                "(" + ontoElementFqName + "Factory) " +
                "(it.unipr.ailab.jadescript.javaapi.Jadescript." +
                "getNativeFactory(" +
                ontoElementFqName
                + ".class)))";
        return methodNamePrefix;
    }


    @Override
    public void generateDeclaredTypes(Maybe<Ontology> input, IJvmDeclaredTypeAcceptor acceptor, boolean isPreIndexingPhase) {
        //creates all the classes of the elements of the ontology:
        module.get(ContextManager.class).enterTopLevelDeclaration((module, outer) ->
                new OntologyDeclarationSupportContext(module, outer, input)
        );
        for (Maybe<? extends Feature> feature : iterate(input.__(FeatureContainer::getFeatures))) {
            safeDo(input, feature, (inputSafe, featureSafe) -> {
                if (featureSafe instanceof ExtendingFeature) {
                    Maybe<QualifiedName> ontoFullQualifiedName =
                            input.__(module.get(CompilationHelper.class)::getFullyQualifiedName);
                    List<JvmDeclaredType> pojoTypes = module.get(OntologyElementSemantics.class)
                            .declareTypes(
                                    feature.__(f -> (ExtendingFeature) f),
                                    ontoFullQualifiedName,
                                    isPreIndexingPhase
                            );
                    for (JvmDeclaredType pojoTypeSafe : pojoTypes) {
                        acceptor.accept(pojoTypeSafe);
                    }
                    if (!pojoTypes.isEmpty()) {
                        declaredSchemaTypes.getOrNew(input).put(((ExtendingFeature) featureSafe).getName(), pojoTypes.get(0));
                        schemaFeatures.getOrNew(input).put(((ExtendingFeature) featureSafe).getName(), (ExtendingFeature) featureSafe);
                    }
                }
            });
        }
        module.get(ContextManager.class).exit();

        //creates the vocabulary interface:
        generateVocabularyInterface(input, acceptor, isPreIndexingPhase);

        //then creates the main ontology class:
        super.generateDeclaredTypes(input, acceptor, isPreIndexingPhase);
    }

    @SuppressWarnings("unchecked")
    private void generateVocabularyInterface(Maybe<Ontology> input, IJvmDeclaredTypeAcceptor acceptor, boolean isPreIndexingPhase) {
        input.safeDo(inputSafe -> {
            Maybe<String> name = input.__(NamedElement::getName);
            if (isNameAlwaysRequired() && name.isNothing()) {
                return;
            }
            input.__(module.get(CompilationHelper.class)::getFullyQualifiedName).safeDo(fullyQualifiedNameSafe -> {
                acceptor.accept(module.get(JvmTypesBuilder.class).toInterface(
                                inputSafe,
                                fullyQualifiedNameSafe + "_Vocabulary",
                                itInterf -> {
                                    if (!isPreIndexingPhase) {
                                        JvmTypeReference superOntologyType = input
                                                .__(FeatureContainer::getSuperTypes)
                                                .nullIf(List::isEmpty)
                                                .__(List::get, 0)
                                                .__(t -> (JvmTypeReference) t)
                                                .orElse(module.get(TypeHelper.class).typeRef(jadescript.content.onto.Ontology.class));
                                        itInterf.getSuperTypes().add(
                                                module.get(TypeHelper.class).typeRef(superOntologyType.getQualifiedName('.') + "_Vocabulary")
                                        );
                                        for (Maybe<? extends Feature> feature : iterate(input.__(FeatureContainer::getFeatures))) {
                                            //noinspection unchecked
                                            addVocabulary(itInterf.getMembers(), (Maybe<ExtendingFeature>) feature);
                                        }

                                    }
                                }
                        )
                );
            });
        });
    }


    @Override
    public void populateMainSuperTypes(Maybe<Ontology> input, EList<JvmTypeReference> superTypes) {
        superTypes.add(module.get(TypeHelper.class).ONTOLOGY.asJvmTypeReference());
        Optional<QualifiedName> fullyQualifiedName = input.__(module.get(CompilationHelper.class)::getFullyQualifiedName).toOpt();
        fullyQualifiedName.ifPresent(fullyQualifiedNameSafe -> {
            superTypes.add(module.get(TypeHelper.class).typeRef(fullyQualifiedNameSafe + "_Vocabulary"));
        });
        super.populateMainSuperTypes(input, superTypes);

    }

}
