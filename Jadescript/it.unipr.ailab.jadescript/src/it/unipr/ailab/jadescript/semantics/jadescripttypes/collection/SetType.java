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
import jadescript.util.JadescriptSet;
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

public class SetType
    extends JadescriptType
    implements EmptyCreatable, DeclaresOntologyAdHocClass {

    public static final TypeCategory CATEGORY = new TypeCategoryAdapter() {
        @Override
        public boolean isSet() {
            return true;
        }


        @Override
        public boolean isCollection() {
            return true;
        }
    };
    private final TypeArgument elementType;


    public SetType(
        SemanticsModule module,
        TypeArgument elementType
    ) {
        super(
            module,
            builtinPrefix + "Set",
            "set",
            "SET"
        );
        this.elementType = elementType;
    }


    public static String getAdHocSetClassName(IJadescriptType elementType) {
        return "__SetClass_" + elementType.compileToJavaTypeReference().replace(
            ".",
            "_"
        );
    }


    @Override
    public TypeCategory category() {
        return CATEGORY;
    }


    public IJadescriptType getElementType() {
        return this.elementType.ignoreBound();
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        return jvm.typeRef(
            JadescriptSet.class,
            elementType.asJvmTypeReference()
        );
    }


    @Override
    public boolean isSlottable() {
        return this.getElementType().isSlottable();
    }


    @Override
    public boolean isSendable() {
        return this.getElementType().isSendable();
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
        return getElementType().getDeclaringOntology();
    }


    @Override
    public String getSlotSchemaName() {
        return "\"" + SetType.getAdHocSetClassName(getElementType()) + "\"";
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
            operations.add(Operation.operation(
                builtins.javaVoid(),
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
                builtins.boolean_(),
                "contains",
                Map.of("o", getElementType()),
                List.of("o"),
                getLocation(),
                true
            ));
            operations.add(Operation.operation(
                builtins.boolean_(),
                "containsAll",
                Map.of("o", this),
                List.of("o"),
                getLocation(),
                true
            ));
            operations.add(Operation.operation(
                builtins.boolean_(),
                "containsAll",
                Map.of(
                    "o",
                    builtins.list(getElementType())
                ),
                List.of("o"),
                getLocation(),
                true
            ));
            operations.add(Operation.operation(
                builtins.boolean_(),
                "containsAny",
                Map.of("o", this),
                List.of("o"),
                getLocation(),
                true
            ));
            operations.add(Operation.operation(
                builtins.boolean_(),
                "containsAny",
                Map.of(
                    "o",
                    builtins.list(getElementType())
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
                module,
                Maybe.nothing(),
                properties,
                operations,
                getLocation()
            );
        });


    @Override
    public TypeNamespace namespace() {
        return namespace.get();
    }


    @Override
    public String compileNewEmptyInstance() {
        return "new jadescript.util.JadescriptSet<" +
            getElementType().compileToJavaTypeReference() + ">()";
    }


    @Override
    public boolean requiresAgentEnvParameter() {
        return false;
    }


    @Override
    public Maybe<IJadescriptType> getElementTypeIfCollection() {
        return some(getElementType());
    }


    @Override
    public Stream<IJadescriptType> declaredSupertypes() {
        return Stream.empty();
    }


    @Override
    public List<TypeArgument> typeArguments() {
        return List.of(this.getElementType());
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

        IJadescriptType elementType = this.getElementType();
        String className = getAdHocSetClassName(elementType);

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
                    JadescriptSet.class,
                    elementType.asJvmTypeReference()
                ));


                itClass.getMembers().add(jvmTB.toMethod(
                    featureSafe,
                    "__fromSet",
                    jvm.typeRef(className),
                    itMeth -> {
                        itMeth.setVisibility(JvmVisibility.PUBLIC);
                        itMeth.setStatic(true);
                        itMeth.getParameters().add(jvmTB.toParameter(
                            featureSafe,
                            "set",
                            jvm.typeRef(
                                JadescriptSet.class,
                                elementType.asJvmTypeReference()
                            )
                        ));

                        module.get(CompilationHelper.class).createAndSetBody(
                            itMeth,
                            scb -> {
                                final String typeName =
                                    JvmTypeHelper.noGenericsTypeName(
                                        elementType.compileToJavaTypeReference()
                                    );
                                scb.line(className + " result = " +
                                    "new " + className + "();");
                                scb.line(
                                    "java.util.List<" + typeName +
                                        "> elements = new java.util" +
                                        ".ArrayList<>();"
                                );
                                scb.line("set.forEach(elements::add);");
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
                        ".__populateSetSchema(" +
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
        return getAdHocSetClassName(getElementType());
    }


    @Override
    public String getConverterToAdHocClassMethodName() {
        return "__fromSet";
    }

}
