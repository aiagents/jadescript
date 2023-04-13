package it.unipr.ailab.jadescript.semantics.jadescripttypes.collection;

import it.unipr.ailab.jadescript.jadescript.ExtendingFeature;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.*;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.utils.LazyInit;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.utils.LazyInit.lazyInit;

public class TupleType
    extends JadescriptType
    implements EmptyCreatable, DeclaresOntologyAdHocClass {

    public static final TypeCategory CATEGORY = new TypeCategoryAdapter() {
        @Override
        public boolean isTuple() {
            return true;
        }
    };
    private final List<TypeArgument> elementTypes;


    public TupleType(
        SemanticsModule module,
        List<TypeArgument> elementTypes
    ) {
        super(
            module,
            TypeHelper.builtinPrefix + "Tuple" + elementTypes.size(),
            "tuple",
            "TUPLE"
        );
        this.elementTypes = elementTypes;
    }


    public static String compileNewInstance(
        List<String> listOfCompiledExpressions,
        List<TypeArgument> listOfTypes
    ) {
        if (listOfTypes.size() != listOfCompiledExpressions.size()) {
            return "";
        }

        return "new " + "jadescript.lang.Tuple.Tuple" +
            listOfCompiledExpressions.size() + "<" +
            listOfTypes.stream()
                .map(TypeArgument::compileToJavaTypeReference)
                .collect(Collectors.joining(", ")) +
            ">(" + String.join(", ", listOfCompiledExpressions) + ")";
    }


    public static String compileAddToTuple(
        String originalTuple,
        String compiledArgument,
        TypeArgument argumentType
    ) {
        return originalTuple + ".<" +
            argumentType.compileToJavaTypeReference() +
            ">add(" + compiledArgument + ")";
    }


    public static String compileStandardGet(
        String tupleExpressionCompiled,
        int elemNumber
    ) {
        return tupleExpressionCompiled + ".getElement" + elemNumber + "()";
    }


    public static String getAdHocTupleClassName(
        List<IJadescriptType> elementTypes
    ) {
        return "__TupleClass__" + elementTypes.stream()
            .map(IJadescriptType::compileToJavaTypeReference)
            .map(s -> s.replace(".", "_"))
            .collect(Collectors.joining("__"));
    }


    @Override
    public TypeCategory category() {
        return CATEGORY;
    }


    @Override
    public Stream<IJadescriptType> declaredSupertypes() {
        return Stream.empty();
    }


    @Override
    public List<TypeArgument> typeArguments() {
        return this.elementTypes;
    }


    @Override
    public boolean isErroneous() {
        return false;
    }


    public List<IJadescriptType> getElementTypes() {
        return elementTypes.stream()
            .map(TypeArgument::ignoreBound)
            .collect(Collectors.toList());
    }


    public String compileGet(String tupleExpressionCompiled, int elemNumber) {
        return compileStandardGet(tupleExpressionCompiled, elemNumber);
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        return jvm.typeRef(
            "jadescript.lang.Tuple$Tuple" + elementTypes.size(),
            elementTypes.stream()
                .map(TypeArgument::asJvmTypeReference)
                .toArray(JvmTypeReference[]::new)
        );
    }


    @Override
    public boolean isSlottable() { //TODO
        return elementTypes.stream()
            .map(TypeArgument::ignoreBound)
            .allMatch(IJadescriptType::isSlottable);
    }


    @Override
    public boolean isSendable() {
        return elementTypes.stream()
            .map(TypeArgument::ignoreBound)
            .allMatch(IJadescriptType::isSendable);
    }


    @Override
    public boolean isReferrable() {
        return elementTypes.stream()
            .map(TypeArgument::ignoreBound)
            .allMatch(IJadescriptType::isSlottable);
    }


    @Override
    public boolean hasProperties() { //TODO
        return elementTypes.stream()
            .map(TypeArgument::ignoreBound)
            .allMatch(IJadescriptType::hasProperties);
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        final int size = getElementTypes().size();
        if (size == 0) {
            return Maybe.nothing();
        } else if (size == 1) {
            return getElementTypes().get(0).getDeclaringOntology();
        } else {
            final TypeLatticeComputer lattice =
                module.get(TypeLatticeComputer.class);
            return lattice.getOntologyGLB(
                getElementTypes().get(0).getDeclaringOntology(),
                getElementTypes().get(1).getDeclaringOntology(),
                getElementTypes().subList(1, size).stream()
                    .map(IJadescriptType::getDeclaringOntology)
                    .collect(Collectors.toList())
            );
        }
    }


    @Override
    public String getSlotSchemaName() {
        return "\"" + TupleType.getAdHocTupleClassName(this.getElementTypes()) +
            "\"";
    }


    @Override
    public String compileNewEmptyInstance() {
        List<String> newEmptyInstances = new ArrayList<>();
        for (TypeArgument typeArgument : elementTypes) {
            IJadescriptType elementType = typeArgument.ignoreBound();
            newEmptyInstances.add(
                CompilationHelper.compileDefaultValueForType(elementType)
            );
        }
        return compileNewInstance(newEmptyInstances, elementTypes);
    }


    @Override
    public boolean requiresAgentEnvParameter() {
        for (TypeArgument type : elementTypes) {
            if (type instanceof EmptyCreatable) {
                if (((EmptyCreatable) type).requiresAgentEnvParameter()) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public void declareAdHocClass(
        EList<JvmMember> members,
        Maybe<ExtendingFeature> feature,
        HashMap<String, String> generatedSpecificClasses,
        List<StatementWriter> addSchemaWriters,
        List<StatementWriter> describeSchemaWriters,
        TypeExpression slotTypeExpression,
        Function<TypeExpression, String> schemaNameForSlotProvider,
        SemanticsModule module
    ) {
        if (feature.isNothing()) {
            return;
        }
        final ExtendingFeature featureSafe = feature.toNullable();

        List<IJadescriptType> elementTypes = this.getElementTypes();

        String className = getAdHocTupleClassName(elementTypes);

        if (generatedSpecificClasses.containsKey(className)) {
            return;
        }

        final JvmTypesBuilder jvmTB = module.get(JvmTypesBuilder.class);
        members.add(jvmTB.toClass(featureSafe, className, itClass -> {
            final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
            itClass.setStatic(true);
            itClass.setVisibility(JvmVisibility.PUBLIC);
            final JvmTypeReference asJvmTypeReference =
                asJvmTypeReference();
            itClass.getSuperTypes().add(asJvmTypeReference);
            itClass.getMembers().add(jvmTB.toMethod(
                featureSafe,
                "__fromTuple",
                jvm.typeRef(className),
                itMeth -> {
                    itMeth.setVisibility(JvmVisibility.PUBLIC);
                    itMeth.setStatic(true);
                    itMeth.getParameters().add(jvmTB.toParameter(
                        featureSafe,
                        "tuple",
                        asJvmTypeReference
                    ));
                    final CompilationHelper compilationHelper =
                        module.get(CompilationHelper.class);
                    compilationHelper.createAndSetBody(itMeth, scb -> {
                        scb.line(className + " result = " +
                            "new " + className + "();");
                        for (int i = 0; i < elementTypes.size(); i++) {
                            scb.line("result.setElement" + i +
                                "(tuple.getElement" + i + "());");
                        }
                        scb.line("return result;");
                    });
                }
            ));
        }));

        generatedSpecificClasses.put(className, getCategoryName());
        addSchemaWriters.add(SemanticsConsts.w.simpleStmt(
            "add(new jade.content.schema.AgentActionSchema(\"" + className +
                "\"), " +
                "" + className + ".class);"));
        describeSchemaWriters.add(new StatementWriter() {
            @Override
            public void writeSonnet(SourceCodeBuilder scb) {
                if (slotTypeExpression != null
                    && slotTypeExpression.getSubExprs() != null) {
                    EList<TypeExpression> typeParameters =
                        slotTypeExpression.getSubExprs();
                    for (int i = 0; i < typeParameters.size(); i++) {
                        scb.line(
                            "((jade.content.schema.AgentActionSchema)" +
                                " getSchema(\"" + className + "\"))" +
                                ".add(\"element" + i + "\", (jade" +
                                ".content.schema.TermSchema) " +
                                "getSchema(" +
                                schemaNameForSlotProvider.apply(
                                    typeParameters.get(i)) +
                                "));");
                    }
                }
            }
        });
    }


    @Override
    public String getAdHocClassName() {
        return getAdHocTupleClassName(getElementTypes());
    }


    @Override
    public String getConverterToAdHocClassMethodName() {
        return "__fromTuple";
    }


    private final LazyInit<BuiltinOpsNamespace> namespace =
        lazyInit(() -> {
            final BuiltinTypeProvider builtins =
                module.get(BuiltinTypeProvider.class);

            List<MemberName> properties = new ArrayList<>();

            final List<IJadescriptType> elementTypes = getElementTypes();

            properties.add(Property.readonlyProperty(
                    "length",
                    builtins.integer(),
                    getLocation(),
                    (o, a) -> "" + elementTypes.size()
                )
            );

            if (elementTypes.size() > 0) {
                properties.add(Property.readonlyProperty(
                    "head",
                    elementTypes.get(0),
                    getLocation(),
                    (o, a) -> compileGet(o, 0)
                ));
                properties.add(Property.readonlyProperty(
                    "tail",
                    builtins.tuple(
                        typeArguments().subList(1, typeArguments().size())
                    ),
                    getLocation(),
                    (o, a) -> {
                        List<TypeArgument> tailTypes = new ArrayList<>();
                        List<String> tailCompiles = new ArrayList<>();
                        for (int i = 1; i < elementTypes.size(); i++) {
                            tailTypes.add(elementTypes.get(i).ignoreBound());
                            tailCompiles.add(compileGet(o, i));
                        }
                        return compileNewInstance(tailCompiles, tailTypes);
                    }
                ));
                properties.add(Property.readonlyProperty(
                    "last",
                    elementTypes.get(elementTypes.size() - 1),
                    getLocation(),
                    (o, a) -> compileGet(o, elementTypes.size() - 1)
                ));
            }

            return new BuiltinOpsNamespace(
                module,
                Maybe.nothing(),
                properties,
                List.of(),
                getLocation()
            );
        });


    @Override
    public TypeNamespace namespace() {
        return namespace.get();
    }


}
