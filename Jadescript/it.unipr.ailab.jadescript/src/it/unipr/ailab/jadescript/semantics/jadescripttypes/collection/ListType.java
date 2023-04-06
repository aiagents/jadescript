package it.unipr.ailab.jadescript.semantics.jadescripttypes.collection;

import it.unipr.ailab.jadescript.jadescript.ExtendingFeature;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Operation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.DeclaresOntologyAdHocClass;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.JadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import jadescript.util.JadescriptList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;
import static it.unipr.ailab.maybe.Maybe.some;

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
    private final Map<String, Property> properties = new HashMap<>();
    private final List<Operation> operations = new ArrayList<>();
    private final TypeArgument elementType;
    private boolean initializedProperties = false;


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


    private void initBuiltinProperties() {
        if (initializedProperties) {
            return;
        }
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        this.addBultinProperty(
            Property.readonlyProperty(
                "length",
                typeHelper.INTEGER,
                getLocation(),
                Property.compileGetWithCustomMethod("size")
            )
        );
        this.addBultinProperty(
            new Property(
                true,
                "head",
                getElementType(),
                getLocation(),
                (o, a) -> o + ".get(0)",
                (o, r, a) -> a.accept(
                    SemanticsConsts.w
                        .simpleStmt(o + ".set(0, " + r + ")")
                )
            )
        );
        this.addBultinProperty(
            new Property(
                false,
                "tail",
                this,
                getLocation(),
                (e, a) -> "jadescript.util.JadescriptCollections" +
                    ".getRest(" + e + ", 1)",
                (e, re, a) -> a.accept(
                    SemanticsConsts.w
                        .simpleStmt("jadescript.util.JadescriptCollections" +
                            ".getRest(" + e + ", 1)")
                )
            )
        );
        this.addBultinProperty(
            new Property(
                true,
                "last",
                this,
                getLocation(),
                (e, a) -> e + ".get(" + e + ".size()-1)",
                (e, re, a) -> a.accept(
                    SemanticsConsts.w
                        .simpleStmt(e + ".set(" + e + ".size()-1, " + re + ")")
                )
            )
        );
        operations.add(Operation.operation(
            typeHelper.VOID,
            "__add",
            Map.of("element", getElementType()),
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
            typeHelper.VOID,
            "__addAt",
            Map.of(
                "index", typeHelper.INTEGER,
                "element", getElementType()
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
            typeHelper.VOID,
            "__addAll",
            Map.of("elements", this),
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
                return receiver + ".addAll(" + namedArgs.get("elements") + ")";
            }
        ));
        operations.add(Operation.operation(
            typeHelper.VOID,
            "__addAllAt",
            Map.of(
                "index", typeHelper.INTEGER,
                "elements", this
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
                return receiver + ".addAll(" + namedArgs.get("index") + ", " +
                    namedArgs.get("elements") + ")";
            }
        ));
        operations.add(Operation.operation(
            getElementType(),
            "get",
            Map.of("index", typeHelper.INTEGER),
            List.of("index"),
            getLocation(),
            true
        ));
        operations.add(Operation.operation(
            typeHelper.VOID,
            "set",
            Map.of(
                "index", typeHelper.INTEGER,
                "element", getElementType()
            ),
            List.of("index", "element"),
            getLocation(),
            false
        ));
        operations.add(Operation.operation(
            typeHelper.BOOLEAN,
            "contains",
            Map.of("o", getElementType()),
            List.of("o"),
            getLocation(),
            true
        ));
        operations.add(Operation.operation(
            typeHelper.BOOLEAN,
            "containsAll",
            Map.of("o", this),
            List.of("o"),
            getLocation(),
            true
        ));
        operations.add(Operation.operation(
            typeHelper.BOOLEAN,
            "containsAll",
            Map.of(
                "o", typeHelper.SET.apply(Arrays.asList(getElementType()))
            ),
            List.of("o"),
            getLocation(),
            true
        ));
        operations.add(Operation.operation(
            typeHelper.BOOLEAN,
            "containsAny",
            Map.of("o", this),
            List.of("o"),
            getLocation(),
            true
        ));
        operations.add(Operation.operation(
            typeHelper.BOOLEAN,
            "containsAny",
            Map.of(
                "o",
                typeHelper.SET.apply(Arrays.asList(getElementType()))
            ),
            List.of("o"),
            getLocation(),
            true
        ));
        operations.add(Operation.operation(
            typeHelper.VOID,
            "clear",
            Map.of(),
            List.of(),
            getLocation(),
            false
        ));
        this.initializedProperties = true;
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
        return module.get(TypeHelper.class).typeRef(
            JadescriptList.class,
            this.elementType.asJvmTypeReference()
        );
    }


    @Override
    public void addBultinProperty(Property prop) {
        this.properties.put(prop.name(), prop);
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
            operations,
            getLocation()
        );
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
                final TypeHelper typeHelper =
                    module.get(TypeHelper.class);

                itClass.getSuperTypes().add(typeHelper.typeRef(
                    JadescriptList.class,
                    elementType.asJvmTypeReference()
                ));

                itClass.getMembers().add(jvmTB.toMethod(
                    featureSafe,
                    "__fromList",
                    typeHelper.typeRef(className),
                    itMeth -> {
                        itMeth.setVisibility(JvmVisibility.PUBLIC);
                        itMeth.setStatic(true);
                        itMeth.getParameters().add(jvmTB.toParameter(
                            featureSafe,
                            "list",
                            typeHelper.typeRef(
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
