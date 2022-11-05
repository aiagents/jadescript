package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.jadescript.ExtendingFeature;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.LocalVarBindingProvider;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TupleType extends ParametricType implements EmptyCreatable, DeclaresOntologyAdHocClass {
    private final List<TypeArgument> elementTypes;

    private final Map<String, Property> properties = new HashMap<>();
    private boolean initializedProperties = false;

    public TupleType(SemanticsModule module, List<TypeArgument> elementTypes) {
        super(
                module,
                TypeHelper.builtinPrefix + "Tuple" + elementTypes.size(),
                "tuple",
                "TUPLE",
                "of",
                "(",
                ")",
                ",",
                elementTypes,
                null
        );
        this.elementTypes = elementTypes;
    }

    public TupleType(SemanticsModule module, IJadescriptType... elementTypes) {
        this(module, Arrays.asList(elementTypes));
    }

    public List<IJadescriptType> getElementTypes() {
        return elementTypes.stream()
                .map(TypeArgument::ignoreBound)
                .collect(Collectors.toList());
    }


    public static String compileNewInstance(
            List<String> listOfCompiledExpressions,
            List<TypeArgument> listOfTypes
    ) {
        if (listOfTypes.size() != listOfCompiledExpressions.size()) {
            return "";
        }

        return "new " + "jadescript.lang.Tuple.Tuple" + listOfCompiledExpressions.size() + "<" + listOfTypes.stream()
                .map(TypeArgument::compileToJavaTypeReference)
                .collect(Collectors.joining(", ")) +
                ">(" + String.join(", ", listOfCompiledExpressions) + ")";
    }

    public static String compileAddToTuple(
            String originalTuple,
            String compiledArgument,
            TypeArgument argumentType
    ) {
        return originalTuple + ".<" + argumentType.compileToJavaTypeReference() + ">add(" + compiledArgument + ")";
    }


    @Override
    public boolean typeEquals(IJadescriptType other) {
        if (other instanceof TupleType) {
            if (this.getElementTypes().size() == ((TupleType) other).getElementTypes().size()) {
                for (int i = 0; i < this.getElementTypes().size(); i++) {
                    if (!this.getElementTypes().get(i).typeEquals(((TupleType) other).getElementTypes().get(i))) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        return super.typeEquals(other);
    }

    public static String compileStandardGet(String tupleExpressionCompiled, int elemNumber) {
        return tupleExpressionCompiled + ".getElement" + elemNumber + "()";
    }

    public String compileGet(String tupleExpressionCompiled, int elemNumber) {
        return compileStandardGet(tupleExpressionCompiled, elemNumber);
    }

    private void initBuiltinProperties() {
        if (!initializedProperties) {
            this.addProperty(new Property("length", module.get(TypeHelper.class).INTEGER, true, getLocation())
                    .setCustomCompile((__) -> "" + elementTypes.size(), (__, ___) -> "")
            );

            if (getElementTypes().size() > 0) {
                this.addProperty(new Property("head", getElementTypes().get(0), true, getLocation())
                        .setCustomCompile(
                                (e) -> compileGet(e, 0),
                                (e, re) -> "/* readOnlyProperty */"
                        ));
                this.addProperty(new Property("tail", new TupleType(module, elementTypes.subList(1, elementTypes.size())), true, getLocation())
                        .setCustomCompile(
                                (e) -> {
                                    List<TypeArgument> tailTypes = new ArrayList<>();
                                    List<String> tailCompiles = new ArrayList<>();
                                    for (int i = 1; i < elementTypes.size(); i++) {
                                        tailTypes.add(elementTypes.get(i).ignoreBound());
                                        tailCompiles.add(compileGet(e, i));
                                    }
                                    return compileNewInstance(tailCompiles, tailTypes);
                                },
                                (e, re) -> "/* readOnlyProperty */"
                        )
                );
                this.addProperty(new Property("last", getElementTypes().get(elementTypes.size() - 1), true, getLocation())
                        .setCustomCompile(
                                (e) -> compileGet(e, elementTypes.size() - 1),
                                (e, re) -> "/* readOnlyProperty */"
                        )
                );
            }
        }
        this.initializedProperties = true;
    }

    @Override
    public JvmTypeReference asJvmTypeReference() {
        return module.get(TypeHelper.class).typeRef(
                "jadescript.lang.Tuple$Tuple" + elementTypes.size(),
                elementTypes.stream()
                        .map(TypeArgument::asJvmTypeReference)
                        .toArray(JvmTypeReference[]::new)
        );
    }

    @Override
    public void addProperty(Property prop) {
        properties.put(prop.name(), prop);
    }


    @Override
    public boolean isSlottable() {
        return getTypeArguments().stream()
                .map(TypeArgument::ignoreBound)
                .allMatch(IJadescriptType::isSlottable);
    }

    @Override
    public boolean isSendable() {
        return getTypeArguments().stream().map(TypeArgument::ignoreBound).allMatch(IJadescriptType::isSendable);
    }


    @Override
    public boolean isReferrable() {
        return getTypeArguments().stream()
                .map(TypeArgument::ignoreBound)
                .allMatch(IJadescriptType::isSlottable);
    }

    @Override
    public boolean isManipulable() {
        return getTypeArguments().stream()
                .map(TypeArgument::ignoreBound)
                .allMatch(IJadescriptType::isManipulable);
    }

    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        final int size = getElementTypes().size();
        if (size == 0) {
            return Maybe.nothing();
        } else if (size == 1) {
            return getElementTypes().get(0).getDeclaringOntology();
        } else {
            return module.get(TypeHelper.class).getOntologyGLB(
                    getElementTypes().get(0).getDeclaringOntology(),
                    getElementTypes().get(1).getDeclaringOntology(),
                    getElementTypes().subList(1, size).stream()
                            .map(IJadescriptType::getDeclaringOntology)
                            .collect(Collectors.toList())
            );
        }
    }


    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public String getSlotSchemaName() {
        return "\"" + TupleType.getAdHocTupleClassName(this.getElementTypes()) + "\"";
    }

    @Override
    public String compileNewEmptyInstance() {
        List<String> newEmptyInstances = new ArrayList<>();
        for (TypeArgument typeArgument : elementTypes) {
            IJadescriptType elementType = typeArgument.ignoreBound();
            newEmptyInstances.add(CompilationHelper.compileEmptyConstructorCall(elementType));
        }
        return compileNewInstance(newEmptyInstances, elementTypes);
    }

    @Override
    public void declareSpecificOntologyClass(
            EList<JvmMember> members,
            Maybe<ExtendingFeature> feature,
            HashMap<String, String> generatedSpecificClasses,
            List<StatementWriter> addSchemaWriters,
            List<StatementWriter> describeSchemaWriters,
            TypeExpression slotTypeExpression,
            Function<TypeExpression, String> schemaNameForSlotProvider,
            SemanticsModule module
    ) {
        feature.safeDo(featureSafe -> {
            List<IJadescriptType> elementTypes = this.getElementTypes();
            String className = getAdHocTupleClassName(elementTypes);
            if (!generatedSpecificClasses.containsKey(className)) {
                members.add(module.get(JvmTypesBuilder.class).toClass(featureSafe, className, itClass -> {
                    itClass.setStatic(true);
                    itClass.setVisibility(JvmVisibility.PUBLIC);
                    final JvmTypeReference asJvmTypeReference = asJvmTypeReference();
                    itClass.getSuperTypes().add(asJvmTypeReference);
                    itClass.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
                            featureSafe,
                            "__fromTuple",
                            module.get(TypeHelper.class).typeRef(className),
                            itMeth -> {
                                itMeth.setVisibility(JvmVisibility.PUBLIC);
                                itMeth.setStatic(true);
                                itMeth.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                        featureSafe,
                                        "tuple",
                                        asJvmTypeReference
                                ));
                                module.get(CompilationHelper.class).createAndSetBody(itMeth, scb -> {
                                    scb.line(className + " result = new " + className + "();");
                                    for (int i = 0; i < elementTypes.size(); i++) {
                                        scb.line("result.setElement" + i + "(tuple.getElement" + i + "());");
                                    }
                                    scb.line("return result;");
                                });
                            }
                    ));
                }));
                generatedSpecificClasses.put(className, getCategoryName());
                addSchemaWriters.add(w.simplStmt("add(new jade.content.schema.AgentActionSchema(\"" + className + "\"), " +
                        "" + className + ".class);"));
                describeSchemaWriters.add(new StatementWriter() {
                    @Override
                    public StatementWriter bindLocalVarUsages(LocalVarBindingProvider bindingProvider) {
                        return this;
                    }

                    @Override
                    public void writeSonnet(SourceCodeBuilder scb) {
                        if (slotTypeExpression != null && slotTypeExpression.getSubExprs() != null) {
                            EList<TypeExpression> typeParameters = slotTypeExpression.getSubExprs();
                            for (int i = 0; i < typeParameters.size(); i++) {
                                scb.line("((jade.content.schema.AgentActionSchema) getSchema(\"" + className + "\"))" +
                                        ".add(\"element" + i + "\", (jade.content.schema.TermSchema) getSchema(" +
                                        schemaNameForSlotProvider.apply(typeParameters.get(i)) +
                                        "));");
                            }
                        }
                    }
                });
            }
        });
    }

    public static String getAdHocTupleClassName(List<IJadescriptType> elementTypes) {
        return "__TupleClass__" + elementTypes.stream()
                .map(IJadescriptType::compileToJavaTypeReference)
                .map(s -> s.replace(".", "_"))
                .collect(Collectors.joining("__"));
    }
    private Map<String, Property> getBuiltinProperties() {
        initBuiltinProperties();
        return properties;
    }

    @Override
    public TypeNamespace namespace() {
        return new BuiltinOpsNamespace(
                module,
                Maybe.nothing(),
                new ArrayList<>(getBuiltinProperties().values()),
                List.of(),
                getLocation()
        );
    }
}
