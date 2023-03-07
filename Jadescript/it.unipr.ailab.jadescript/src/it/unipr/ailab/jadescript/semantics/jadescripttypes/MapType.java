package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.jadescript.ExtendingFeature;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Operation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.Util.Tuple2;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import jadescript.util.JadescriptMap;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtend2.lib.StringConcatenationClient;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;
import static it.unipr.ailab.maybe.Maybe.some;

public class MapType extends ParametricType implements EmptyCreatable,
    DeclaresOntologyAdHocClass {

    private final Map<String, Property> properties = new HashMap<>();
    private final List<Operation> operations = new ArrayList<>();
    private boolean initializedProperties = false;


    public MapType(
        SemanticsModule module,
        TypeArgument keyType,
        TypeArgument valueType
    ) {
        super(
            module,
            builtinPrefix + "Map",
            "Map",
            "MAP",
            "of",
            "",
            "",
            ":",
            Arrays.asList(keyType, valueType),
            Arrays.asList(
                module.get(TypeHelper.class).ANY,
                module.get(TypeHelper.class).ANY
            )
        );

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


    public IJadescriptType getKeyType() {
        return getTypeArguments().get(0).ignoreBound();
    }


    public IJadescriptType getValueType() {
        return getTypeArguments().get(1).ignoreBound();
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        return module.get(TypeHelper.class).typeRef(
            JadescriptMap.class,
            getTypeArguments().stream()
                .map(TypeArgument::asJvmTypeReference)
                .collect(Collectors.toList())
        );
    }


    @Override
    public Maybe<IJadescriptType> getElementTypeIfCollection() {
        return some(getValueType());
    }


    @Override
    public void addProperty(Property prop) {
        properties.put(prop.name(), prop);
    }


    private void initBuiltinProperties() {
        if (initializedProperties) {
            return;
        }

        this.addProperty(
            Property.readonlyProperty(
                "size",
                module.get(TypeHelper.class).INTEGER,
                getLocation(),
                Property.compileGetWithCustomMethod("size")
            )
        );
        this.addProperty(
            Property.readonlyProperty(
                "values",
                module.get(TypeHelper.class).SET.apply(
                    Arrays.asList(getValueType())
                ),
                getLocation(),
                Property.compileGetWithCustomMethod("values")
            )
        );
        this.addProperty(
            Property.readonlyProperty(
                "keys",
                module.get(TypeHelper.class).SET.apply(
                    Arrays.asList(getKeyType())
                ),
                getLocation(),
                Property.compileGetWithCustomMethod("keySet")
            )
        );

        operations.add(Operation.operation(
            module.get(TypeHelper.class).VOID,
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
            module.get(TypeHelper.class).BOOLEAN,
            "containsKey",
            Map.of("k", getKeyType()),
            List.of("k"),
            getLocation(),
            true
        ));
        operations.add(Operation.operation(
            module.get(TypeHelper.class).BOOLEAN,
            "containsValue",
            Map.of("v", getValueType()),
            List.of("v"),
            getLocation(),
            true
        ));
        operations.add(Operation.operation(
            module.get(TypeHelper.class).BOOLEAN,
            "containsAll",
            Map.of("m", this),
            List.of("m"),
            getLocation(),
            true
        ));
        operations.add(Operation.operation(
            module.get(TypeHelper.class).BOOLEAN,
            "containsAny",
            Map.of("m", this),
            List.of("m"),
            getLocation(),
            true
        ));
        operations.add(Operation.operation(
            module.get(TypeHelper.class).VOID,
            "clear",
            Map.of(),
            List.of(),
            getLocation(),
            false
        ));
        this.initializedProperties = true;
    }


    private Map<String, Property> getBuiltinProperties() {
        initBuiltinProperties();
        return properties;
    }


    @Override
    public boolean isSlottable() {
        return getTypeArguments().stream()
            .map(TypeArgument::ignoreBound)
            .allMatch(IJadescriptType::isSlottable);
    }


    @Override
    public boolean isSendable() {
        return getTypeArguments().stream().map(TypeArgument::ignoreBound)
            .allMatch(IJadescriptType::isSendable);
    }


    @Override
    public boolean isReferrable() {
        return true;
    }


    @Override
    public boolean hasProperties() {
        return true;
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return module.get(TypeHelper.class).getOntologyGLB(
            getKeyType().getDeclaringOntology(),
            getValueType().getDeclaringOntology()
        );
    }


    @Override
    public boolean isCollection() {
        return true;
    }


    @Override
    public String getSlotSchemaName() {
        return "\"" + MapType.getAdHocMapClassName(
            getKeyType(),
            getValueType()
        ) + "\"";
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
        return "new jadescript.util.JadescriptMap<" + getKeyType()
            .compileToJavaTypeReference()
            + ", " + getValueType().compileToJavaTypeReference() + ">()";
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
        IJadescriptType keyType = this.getKeyType();
        IJadescriptType elementType = this.getValueType();
        String className = getAdHocMapClassName(keyType, elementType);
        if (!generatedSpecificClasses.containsKey(className)) {
            final JvmTypesBuilder jvmTB =
                module.get(JvmTypesBuilder.class);
            members.add(jvmTB.toClass(featureSafe, className, itClass -> {
                itClass.setStatic(true);
                itClass.setVisibility(JvmVisibility.PUBLIC);
                itClass.getSuperTypes().add(asJvmTypeReference());
                final TypeHelper typeHelper = module.get(TypeHelper.class);
                itClass.getMembers().add(jvmTB.toMethod(
                    featureSafe,
                    "__fromMap",
                    typeHelper.typeRef(className),
                    itMeth -> {
                        itMeth.setVisibility(JvmVisibility.PUBLIC);
                        itMeth.setStatic(true);
                        itMeth.getParameters().add(jvmTB.toParameter(
                            featureSafe,
                            "map",
                            typeHelper.typeRef(
                                Map.class,
                                keyType.asJvmTypeReference(),
                                elementType.asJvmTypeReference()
                            )
                        ));
                        jvmTB.setBody(
                            itMeth,
                            new StringConcatenationClient() { //TODO use scb
                                @Override
                                protected void appendTo(
                                    TargetStringConcatenation target
                                ) {
                                    target.append(className + " " +
                                        "result = new " + className + "();\n" +
                                        "  java.util.List<" +
                                        typeHelper.noGenericsTypeName(
                                            keyType.compileToJavaTypeReference()
                                        ) +
                                        "> keys = new java.util" +
                                        ".ArrayList<>();\n" +
                                        "  java.util.List<" +
                                        typeHelper.noGenericsTypeName(
                                            elementType
                                                .compileToJavaTypeReference()) +
                                        "> values = new java.util" +
                                        ".ArrayList<>();\n" +
                                        "  map.forEach((k,v)->{\n" +
                                        "\t  keys.add(k);\n" +
                                        "\t  values.add(v);\n" +
                                        "  });\n" +
                                        "  result.setKeys(keys);\n" +
                                        "  result.setValues(values);" +
                                        "\n" +
                                        "  return result;");
                                }
                            }
                        );
                    }
                ));
            }));
            generatedSpecificClasses.put(className, getCategoryName());
            addSchemaWriters.add(SemanticsConsts.w.simpleStmt(
                "add(new jade.content.schema.ConceptSchema(\"" + className +
                    "\"), " +
                    "" + className + ".class);"));
            describeSchemaWriters.add(new StatementWriter() {
                @Override
                public void writeSonnet(SourceCodeBuilder scb) {
                    EList<TypeExpression> typeParameters =
                        slotTypeExpression
                            .getCollectionTypeExpression().getTypeParameters();
                    if (typeParameters != null && typeParameters.size() == 2) {
                        scb.add(
                            "jadescript.content.onto.Ontology" +
                                ".__populateMapSchema(" +
                                "(jade.content.schema.TermSchema) " +
                                "getSchema(" +
                                schemaNameForSlotProvider.apply(
                                    typeParameters.get(0)) + "), " +
                                "(jade.content.schema.TermSchema) getSchema(" +
                                schemaNameForSlotProvider.apply(
                                    typeParameters.get(1)) + "), " +
                                "(jade.content.schema.ConceptSchema) " +
                                "getSchema(\"" + className + "\"));");
                    }

                }
            });
        }

    }

}
