package it.unipr.ailab.jadescript.semantics.jadescripttypes.collection;

import it.unipr.ailab.jadescript.jadescript.ExtendingFeature;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Operation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.DeclaresOntologyAdHocClass;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.JadescriptType;
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
import jadescript.util.JadescriptList;
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
import static it.unipr.ailab.maybe.Maybe.some;
import static it.unipr.ailab.maybe.utils.LazyInit.lazyInit;

public class ListType
    extends JadescriptType
    implements EmptyCreatable, DeclaresOntologyAdHocClass {


    public static final TypeCategory CATEGORY = new TypeCategoryAdapter() {
        @Override
        public boolean isList() {
            return true;
        }


        @Override
        public boolean isCollection() {
            return true;
        }
    };

    private final TypeArgument elementType;


    public ListType(
        SemanticsModule module,
        TypeArgument elementType
    ) {
        super(
            module,
            builtinPrefix + "List",
            "List",
            "LIST"
        );

        this.elementType = elementType;
    }


    public static String getAdHocListClassName(IJadescriptType elementType) {
        return "__ListClass_" + elementType.compileToJavaTypeReference()
            .replace(".", "_");
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
        return List.of(this.elementType);
    }



    public IJadescriptType getElementType() {
        return this.elementType.ignoreBound();
    }


    @Override
    public Maybe<IJadescriptType> getElementTypeIfCollection() {
        return some(getElementType());
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        return jvm.typeRef(
            JadescriptList.class,
            this.elementType.asJvmTypeReference()
        );
    }


    @Override
    public boolean isSlottable() {
        return this.elementType.ignoreBound().isSlottable();
    }


    @Override
    public boolean isSendable() {
        return this.elementType.ignoreBound().isSendable();
    }


    @Override
    public boolean isReferrable() {
        return this.elementType.ignoreBound().isReferrable();
    }


    @Override
    public boolean hasProperties() {
        return true;
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return getElementType().getDeclaringOntology();
    }


    @Override
    public String getSlotSchemaName() {
        return "\"" + ListType.getAdHocListClassName(getElementType()) + "\"";
    }




    private final LazyInit<BuiltinOpsNamespace> namespace =
        lazyInit(() -> {

            final BuiltinTypeProvider builtins =
                module.get(BuiltinTypeProvider.class);

            List<MemberName> properties = new ArrayList<>();
            properties.add(
                Property.readonlyProperty(
                    "length",
                    builtins.integer(),
                    getLocation(),
                    Property.compileGetWithCustomMethod("size")
                )
            );

            properties.add(
                new Property(
                    true,
                    "head",
                    ListType.this.getElementType(),
                    getLocation(),
                    (o, a) -> o + ".get(0)",
                    (o, r, a) -> a.accept(
                        SemanticsConsts.w
                            .simpleStmt(o + ".set(0, " + r + ")")
                    )
                )
            );
            properties.add(
                new Property(
                    false,
                    "tail",
                    ListType.this,
                    getLocation(),
                    (e, a) -> "jadescript.util.JadescriptCollections" +
                        ".getRest(" + e + ", 1)",
                    (e, re, a) -> a.accept(
                        SemanticsConsts.w
                            .simpleStmt("jadescript.util" +
                                ".JadescriptCollections" +
                                ".getRest(" + e + ", 1)")
                    )
                )
            );
            properties.add(
                new Property(
                    true,
                    "last",
                    ListType.this.getElementType(),
                    getLocation(),
                    (e, a) -> e + ".get(" + e + ".size()-1)",
                    (e, re, a) -> a.accept(
                        SemanticsConsts.w
                            .simpleStmt(e + ".set(" + e + ".size()-1, "
                                + re + ")")
                    )
                )
            );

            List<Operation> operations = new ArrayList<>();

            operations.add(Operation.operation(
                builtins.javaVoid(),
                "__add",
                Map.of("element", ListType.this.getElementType()),
                List.of("element"),
                getLocation(),
                false,
                (receiver, args) -> {
                    final String s;
                    if (args.size() >= 1) {
                        s = args.get(0);
                    } else {
                        s = "/*internal error: missing arguments*/";
                    }
                    return receiver + ".add(" + s + ")";
                },
                (receiver, namedArgs) -> {
                    return receiver + ".add(" + namedArgs.get("element") + ")";
                }
            ));
            operations.add(Operation.operation(
                builtins.javaVoid(),
                "__addAt",
                Map.of(
                    "index", builtins.integer(),
                    "element", ListType.this.getElementType()
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
                    return receiver + ".add(" + i + ", " + e + ")";
                },
                (receiver, namedArgs) -> {
                    return receiver + ".add(" + namedArgs.get("index") + ", " +
                        namedArgs.get("element") + ")";
                }
            ));
            operations.add(Operation.operation(
                builtins.javaVoid(),
                "__addAll",
                Map.of("elements", ListType.this),
                List.of("elements"),
                getLocation(),
                false,
                (receiver, args) -> {
                    final String e;
                    if (args.size() >= 1) {
                        e = args.get(1);
                    } else {
                        e = "/*internal error: missing arguments*/";
                    }
                    return receiver + ".addAll(" + e + ")";
                },
                (receiver, namedArgs) -> {
                    return receiver + ".addAll(" +
                        namedArgs.get("elements") +
                        ")";
                }
            ));
            operations.add(Operation.operation(
                builtins.javaVoid(),
                "__addAllAt",
                Map.of(
                    "index", builtins.integer(),
                    "elements", ListType.this
                ),
                List.of("index", "elements"),
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
                    return receiver + ".addAll(" + i + ", " + e + ")";
                },
                (receiver, namedArgs) -> {
                    return receiver + ".addAll(" + namedArgs.get("index") +
                        ", " +
                        namedArgs.get("elements") + ")";
                }
            ));
            operations.add(Operation.operation(
                ListType.this.getElementType(),
                "get",
                Map.of("index", builtins.integer()),
                List.of("index"),
                getLocation(),
                true
            ));
            operations.add(Operation.operation(
                builtins.javaVoid(),
                "set",
                Map.of(
                    "index", builtins.integer(),
                    "element", ListType.this.getElementType()
                ),
                List.of("index", "element"),
                getLocation(),
                false
            ));
            operations.add(Operation.operation(
                builtins.boolean_(),
                "contains",
                Map.of("o", ListType.this.getElementType()),
                List.of("o"),
                getLocation(),
                true
            ));
            operations.add(Operation.operation(
                builtins.boolean_(),
                "containsAll",
                Map.of("o", ListType.this),
                List.of("o"),
                getLocation(),
                true
            ));
            operations.add(Operation.operation(
                builtins.boolean_(),
                "containsAll",
                Map.of(
                    "o", builtins.set(getElementType())
                ),
                List.of("o"),
                getLocation(),
                true
            ));
            operations.add(Operation.operation(
                builtins.boolean_(),
                "containsAny",
                Map.of("o", ListType.this),
                List.of("o"),
                getLocation(),
                true
            ));
            operations.add(Operation.operation(
                builtins.boolean_(),
                "containsAny",
                Map.of(
                    "o",
                    builtins.set(getElementType())
                ),
                List.of("o"),
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
                ListType.this.module,
                Maybe.nothing(),
                properties,
                operations,
                ListType.this.getLocation()
            );
        });


    @Override
    public TypeNamespace namespace() {
        return namespace.get();
    }


    @Override
    public String compileNewEmptyInstance() {
        return "new jadescript.util.JadescriptList<" +
            getElementType().compileToJavaTypeReference()
            + ">()";
    }


    @Override
    public boolean requiresAgentEnvParameter() {
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

        IJadescriptType elementType = this.getElementType();
        String className = getAdHocListClassName(elementType);

        if (generatedSpecificClasses.containsKey(className)) {
            return;
        }

        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);


        members.add(jvmTB.toClass(
            featureSafe,
            className,
            itClass -> {
                itClass.setStatic(true);
                itClass.setVisibility(JvmVisibility.PUBLIC);
                final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);

                itClass.getSuperTypes().add(jvm.typeRef(
                    JadescriptList.class,
                    elementType.asJvmTypeReference()
                ));

                itClass.getMembers().add(jvmTB.toMethod(
                    featureSafe,
                    "__fromList",
                    jvm.typeRef(className),
                    itMeth -> {
                        itMeth.setVisibility(JvmVisibility.PUBLIC);
                        itMeth.setStatic(true);
                        itMeth.getParameters().add(jvmTB.toParameter(
                            featureSafe,
                            "list",
                            jvm.typeRef(
                                JadescriptList.class,
                                elementType.asJvmTypeReference()
                            )
                        ));

                        module.get(CompilationHelper.class).createAndSetBody(
                            itMeth,
                            scb -> {
                                final String typeName = JvmTypeHelper
                                    .noGenericsTypeName(
                                        elementType.compileToJavaTypeReference()
                                    );
                                scb.line(className + " result = " +
                                    "new " + className + "();");
                                scb.line(
                                    "java.util.List<" + typeName +
                                        "> elements = new java.util" +
                                        ".ArrayList<>();"
                                );
                                scb.line("list.forEach(elements::add);");
                                scb.line("result.setElements(elements);");
                                scb.line("return result;");
                            }
                        );
                    }


                ));

            }
        ));

        generatedSpecificClasses.put(className, getCategoryName());

        addSchemaWriters.add(SemanticsConsts.w.simpleStmt(
            "add(new jade.content.schema.ConceptSchema(\"" +
                className + "\"), " + className + ".class);"));

        describeSchemaWriters.add(new StatementWriter() {
            @Override
            public void writeSonnet(SourceCodeBuilder scb) {
                EList<TypeExpression> typeParameters = slotTypeExpression
                    .getCollectionTypeExpression().getTypeParameters();

                if (typeParameters == null || typeParameters.size() != 1) {
                    return;
                }

                scb.add(
                    "jadescript.content.onto.Ontology" +
                        ".__populateListSchema(" +
                        "(jade.content.schema.TermSchema) " +
                        "getSchema(" + schemaNameForSlotProvider
                        .apply(typeParameters.get(0)) + "), " +
                        "(jade.content.schema.ConceptSchema) " +
                        "getSchema(\"" + className + "\"));");

            }
        });
    }


    @Override
    public String getAdHocClassName() {
        return getAdHocListClassName(getElementType());
    }


    @Override
    public String getConverterToAdHocClassMethodName() {
        return "__fromList";
    }


    @Override
    public boolean isErroneous() {
        return false;
    }


}
