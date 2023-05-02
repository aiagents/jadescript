package it.unipr.ailab.jadescript.semantics.jadescripttypes.collection;

import it.unipr.ailab.jadescript.jadescript.ExtendingFeature;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Operation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
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
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import jadescript.util.JadescriptMap;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;
import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;
import static it.unipr.ailab.maybe.utils.LazyInit.lazyInit;

public class MapType
    extends JadescriptType
    implements EmptyCreatable, DeclaresOntologyAdHocClass {

    public static final TypeCategory CATEGORY = new TypeCategoryAdapter() {
        @Override
        public boolean isMap() {
            return true;
        }


        @Override
        public boolean isCollection() {
            return true;
        }

    };
    private final TypeArgument keyType;
    private final TypeArgument valueType;


    public MapType(
        SemanticsModule module,
        TypeArgument keyType,
        TypeArgument valueType
    ) {
        super(
            module,
            builtinPrefix + "Map",
            "map",
            "MAP"
        );

        this.keyType = keyType;
        this.valueType = valueType;

    }


    public static String getAdHocMapClassName(
        IJadescriptType keyType,
        IJadescriptType elementType
    ) {
        return "__MapClass_" + keyType.compileToJavaTypeReference().replace(
            ".",
            "_"
        ) +
            "__to__" + elementType.compileToJavaTypeReference().replace(
            ".",
            "_"
        );
    }


    @Override
    public TypeCategory category() {
        return CATEGORY;
    }


    @Override
    public String getParametricListSeparator() {
        return ":";
    }


    @Override
    public Stream<IJadescriptType> declaredSupertypes() {
        return Stream.empty();
    }


    @Override
    public List<TypeArgument> typeArguments() {
        return List.of(this.getKeyType(), this.getValueType());
    }


    public IJadescriptType getKeyType() {
        return this.keyType.ignoreBound();
    }


    public IJadescriptType getValueType() {
        return this.valueType.ignoreBound();
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        return jvm.typeRef(
            JadescriptMap.class,
            getKeyType().asJvmTypeReference(),
            getValueType().asJvmTypeReference()
        );
    }


    @Override
    public Maybe<IJadescriptType> getElementTypeIfCollection() {
        return some(getValueType());
    }


    @Override
    public boolean isSlottable() {
        return getKeyType().ignoreBound().isSlottable()
            && getValueType().ignoreBound().isSlottable();
    }


    @Override
    public boolean isSendable() {
        return getKeyType().ignoreBound().isSendable()
            && getValueType().ignoreBound().isSendable();
    }


    @Override
    public boolean isReferrable() {
        return getKeyType().ignoreBound().isReferrable()
            && getValueType().ignoreBound().isReferrable();
    }


    @Override
    public boolean hasProperties() {
        return true;
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        final TypeLatticeComputer lattice =
            module.get(TypeLatticeComputer.class);
        return lattice.getOntologyGLB(
            getKeyType().getDeclaringOntology(),
            getValueType().getDeclaringOntology()
        );
    }


    @Override
    public String getSlotSchemaName() {
        return "\"" + MapType.getAdHocMapClassName(
            getKeyType(),
            getValueType()
        ) + "\"";
    }


    private final LazyInit<BuiltinOpsNamespace> namespace =
        lazyInit(() -> {

            final BuiltinTypeProvider builtins =
                module.get(BuiltinTypeProvider.class);

            List<MemberName> properties = new ArrayList<>();
            List<MemberCallable> operations = new ArrayList<>();
            properties.add(
                Property.readonlyProperty(
                    "size",
                    builtins.integer(),
                    getLocation(),
                    Property.compileGetWithCustomMethod("size")
                )
            );
            properties.add(
                Property.readonlyProperty(
                    "values",
                    builtins.set(getValueType()),
                    getLocation(),
                    Property.compileGetWithCustomMethod("values")
                )
            );
            properties.add(
                Property.readonlyProperty(
                    "keys",
                    builtins.set(getKeyType()),
                    getLocation(),
                    Property.compileGetWithCustomMethod("keySet")
                )
            );

            operations.add(Operation.operation(
                builtins.javaVoid(),
                "__addAt",

                Map.of(
                    "index", getKeyType(),
                    "element", getValueType()
                ),
                List.of("index", "element"),
                getLocation(),
                false,
                (receiver, args) -> {
                    final String e;
                    final String i;
                    if (args.size() >= 2) {
                        i = args.get(0);
                        e = args.get(1);
                    } else {
                        i = "/*internal error: missing arguments*/";
                        e = "/*internal error: missing arguments*/";
                    }
                    return receiver + ".put(" + i + ", " + e + ")";
                },
                (receiver, namedArgs) -> {
                    return receiver + ".put(" + namedArgs.get("index") + "," +
                        namedArgs.get("element") + ")";
                }
            ));
            operations.add(Operation.operation(
                getValueType(),
                "get",
                Map.of("key", getKeyType()),
                List.of("key"),
                getLocation(),
                true // assuming no exceptions are thrown
            ));
            operations.add(Operation.operation(
                getValueType(),
                "put",
                Map.of(
                    "key", getKeyType(),
                    "value", getValueType()
                ),
                List.of("key", "value"),
                getLocation(),
                false
            ));
            operations.add(Operation.operation(
                builtins.boolean_(),
                "containsKey",
                Map.of("k", getKeyType()),
                List.of("k"),
                getLocation(),
                true
            ));
            operations.add(Operation.operation(
                builtins.boolean_(),
                "containsValue",
                Map.of("v", getValueType()),
                List.of("v"),
                getLocation(),
                true
            ));
            operations.add(Operation.operation(
                builtins.boolean_(),
                "containsAll",
                Map.of("m", this),
                List.of("m"),
                getLocation(),
                true
            ));
            operations.add(Operation.operation(
                builtins.boolean_(),
                "containsAny",
                Map.of("m", this),
                List.of("m"),
                getLocation(),
                true
            ));
            operations.add(Operation.operation(
                builtins.javaVoid(),
                "clear",
                Map.of(),
                List.of(),
                getLocation(),
                false
            ));

            return new BuiltinOpsNamespace(
                MapType.this.module,
                nothing(),
                properties,
                operations,
                MapType.this.getLocation()
            );
        });


    @Override
    public TypeNamespace namespace() {
        return namespace.get();
    }


    @Override
    public String compileNewEmptyInstance() {
        return "new jadescript.util.JadescriptMap<" +
            getKeyType().compileToJavaTypeReference() + ", " +
            getValueType().compileToJavaTypeReference() + ">()";
    }


    @Override
    public boolean requiresAgentEnvParameter() {
        return false;
    }


    @Override
    public boolean isErroneous() {
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
        IJadescriptType keyType = this.getKeyType();
        IJadescriptType elementType = this.getValueType();
        String className = getAdHocMapClassName(keyType, elementType);

        if (generatedSpecificClasses.containsKey(className)) {
            return;
        }

        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);

        members.add(jvmTB.toClass(featureSafe, className, itClass -> {
            final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);

            itClass.setStatic(true);
            itClass.setVisibility(JvmVisibility.PUBLIC);
            itClass.getSuperTypes().add(asJvmTypeReference());


            itClass.getMembers().add(jvmTB.toMethod(
                featureSafe,
                "__fromMap",
                jvm.typeRef(className),
                itMeth -> {
                    itMeth.setVisibility(JvmVisibility.PUBLIC);
                    itMeth.setStatic(true);
                    itMeth.getParameters().add(jvmTB.toParameter(
                        featureSafe,
                        "map",
                        jvm.typeRef(
                            Map.class,
                            keyType.asJvmTypeReference(),
                            elementType.asJvmTypeReference()
                        )
                    ));

                    final CompilationHelper compilationHelper =
                        module.get(CompilationHelper.class);

                    compilationHelper.createAndSetBody(itMeth, scb -> {
                        scb.line(className + " result = " +
                            "new " + className + "();");

                        scb.line("java.util.List<" +
                            JvmTypeHelper.noGenericsTypeName(
                                keyType.compileToJavaTypeReference()) +
                            "> keys = new java.util.ArrayList<>();");

                        scb.line("java.util.List<" +
                            JvmTypeHelper.noGenericsTypeName(
                                elementType.compileToJavaTypeReference()) +
                            "> values = new java.util.ArrayList<>();");

                        scb.line(
                            "map.forEach((k,v)->{" +
                                "keys.add(k); values.add(v);});"
                        );

                        scb.line("result.setKeys(keys);");
                        scb.line("result.setValues(values);");
                        scb.line("return result;");
                    });
                }
            ));
        }));

        generatedSpecificClasses.put(className, getCategoryName());

        addSchemaWriters.add(SemanticsConsts.w.simpleStmt(
            "add(new jade.content.schema.ConceptSchema(\"" +
                className + "\"), " + className + ".class);"
        ));

        EList<TypeExpression> typeParameters =
            slotTypeExpression
                .getCollectionTypeExpression().getTypeParameters();

        String populateMapSchema;
        if (typeParameters != null && typeParameters.size() == 2) {
            populateMapSchema = "jadescript.content.onto.Ontology" +
                ".__populateMapSchema(" +
                "(jade.content.schema.TermSchema) " +
                "getSchema(" +
                schemaNameForSlotProvider.apply(
                    typeParameters.get(0)) + "), " +
                "(jade.content.schema.TermSchema) getSchema(" +
                schemaNameForSlotProvider.apply(
                    typeParameters.get(1)) + "), " +
                "(jade.content.schema.ConceptSchema) " +
                "getSchema(\"" + className + "\"));";
        } else {
            populateMapSchema = "";
        }

        describeSchemaWriters.add(
            SemanticsConsts.w.simpleStmt(populateMapSchema)
        );


    }


    @Override
    public String getAdHocClassName() {
        return getAdHocMapClassName(getKeyType(), getValueType());
    }


    @Override
    public String getConverterToAdHocClassMethodName() {
        return "__fromMap";
    }

}
