package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.jadescript.ExtendingFeature;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Operation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.Util.Tuple2;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import jadescript.util.JadescriptSet;
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

public class SetType extends ParametricType implements EmptyCreatable,
    DeclaresOntologyAdHocClass {

    private final Map<String, Property> properties = new HashMap<>();
    private final List<Operation> operations = new ArrayList<>();
    private boolean initializedProperties = false;


    public SetType(
        SemanticsModule module,
        TypeArgument elementType
    ) {
        super(
            module,
            builtinPrefix + "Set",
            "Set",
            "SET",
            "of",
            "",
            "",
            "",
            Collections.singletonList(elementType),
            Collections.singletonList(module.get(TypeHelper.class).ANY)
        );

    }


    public static String getAdHocSetClassName(IJadescriptType elementType) {
        return "__SetClass_" + elementType.compileToJavaTypeReference().replace(
            ".",
            "_"
        );
    }


    public IJadescriptType getElementType() {
        return getTypeArguments().get(0).ignoreBound();
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        return module.get(TypeHelper.class).typeRef(
            JadescriptSet.class,
            getTypeArguments().stream()
                .map(TypeArgument::asJvmTypeReference)
                .collect(Collectors.toList())
        );
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
            new Property(
                "size",
                module.get(TypeHelper.class).INTEGER,
                true,
                getLocation()
            ).setCompileByCustomJVMMethod("size", "size")
        );
        operations.add(new Operation(
            false,
            "__add",
            module.get(TypeHelper.class).VOID,
            List.of(new Tuple2<>("element", getElementType())),
            getLocation(),
            (receiver, namedArgs) -> {
                return receiver + ".add(" + namedArgs.get("element") + ")";
            },
            (receiver, args) -> {
                final String s;
                if (args.size() >= 1) {
                    s = args.get(0);
                } else {
                    s = "/*internal error: missing arguments*/";
                }
                return receiver + ".add(" + s + ")";
            }
        ));

        operations.add(new Operation(
            true,
            "contains",
            module.get(TypeHelper.class).BOOLEAN,
            List.of(new Tuple2<>("o", getElementType())),
            getLocation()
        ));
        operations.add(new Operation(
            true,
            "containsAll",
            module.get(TypeHelper.class).BOOLEAN,
            List.of(new Tuple2<>("o", this)),
            getLocation()
        ));
        operations.add(new Operation(
            true,
            "containsAll",
            module.get(TypeHelper.class).BOOLEAN,
            List.of(new Tuple2<>(
                "o",
                module.get(TypeHelper.class).LIST.apply(Arrays.asList(
                    getElementType()))
            )),
            getLocation()
        ));
        operations.add(new Operation(
            true,
            "containsAny",
            module.get(TypeHelper.class).BOOLEAN,
            List.of(new Tuple2<>("o", this)),
            getLocation()
        ));
        operations.add(new Operation(
            true,
            "containsAny",
            module.get(TypeHelper.class).BOOLEAN,
            List.of(new Tuple2<>(
                "o",
                module.get(TypeHelper.class).LIST.apply(Arrays.asList(
                    getElementType()))
            )),
            getLocation()
        ));
        operations.add(new Operation(
            false,
            "clear",
            module.get(TypeHelper.class).VOID,
            List.of(),
            getLocation()
        ));
        this.initializedProperties = true;
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
        return getElementType().getDeclaringOntology();
    }


    @Override
    public boolean isCollection() {
        return true;
    }


    @Override
    public String getSlotSchemaName() {
        return "\"" + SetType.getAdHocSetClassName(getElementType()) + "\"";
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
        return "new jadescript.util.JadescriptSet<" +
            getElementType().compileToJavaTypeReference() + ">()";
    }


    @Override
    public Maybe<IJadescriptType> getElementTypeIfCollection() {
        return some(getElementType());
    }


    private Map<String, Property> getBuiltinProperties() {
        initBuiltinProperties();
        return properties;
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
            IJadescriptType elementType = this.getElementType();
            String className = getAdHocSetClassName(elementType);
            if (!generatedSpecificClasses.containsKey(className)) {
                members.add(module.get(JvmTypesBuilder.class).toClass(
                    featureSafe,
                    className,
                    itClass -> {
                        itClass.setStatic(true);
                        itClass.setVisibility(JvmVisibility.PUBLIC);
                        itClass.getSuperTypes()
                            .add(module.get(TypeHelper.class).typeRef(
                                jadescript.util.JadescriptSet.class,
                                elementType.asJvmTypeReference()
                            ));
                        itClass.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(
                            featureSafe,
                            "__fromSet",
                            module.get(TypeHelper.class).typeRef(className),
                            itMeth -> {
                                itMeth.setVisibility(JvmVisibility.PUBLIC);
                                itMeth.setStatic(true);
                                itMeth.getParameters().add(module.get(
                                    JvmTypesBuilder.class).toParameter(featureSafe,
                                    "set",
                                    module.get(TypeHelper.class).typeRef(
                                        java.util.Set.class,
                                        elementType.asJvmTypeReference()
                                    )
                                ));
                                module.get(JvmTypesBuilder.class).setBody(
                                    itMeth,
                                    new StringConcatenationClient() {//TODO use scb
                                        @Override
                                        protected void appendTo(
                                            TargetStringConcatenation target
                                        ) {
                                            target.append(className + " " +
                                                "result = new " + className + "();\n" +
                                                "  java.util.List<" +
                                                module.get(TypeHelper.class).noGenericsTypeName(
                                                    elementType.compileToJavaTypeReference()) +
                                                "> elements = new java.util" +
                                                ".ArrayList<>();\n" +
                                                "  set.forEach(elements::add)" +
                                                ";\n" +
                                                "  result.setElements" +
                                                "(elements);\n" +
                                                "  return result;");
                                        }
                                    }
                                );
                            }
                        ));
                    }
                ));
                generatedSpecificClasses.put(className, getCategoryName());
                addSchemaWriters.add(w.simpleStmt(
                    "add(new jade.content.schema.ConceptSchema(\"" + className + "\"), " +
                        "" + className + ".class);"));
                describeSchemaWriters.add(new StatementWriter() {
                    @Override
                    public void writeSonnet(SourceCodeBuilder scb) {
                        EList<TypeExpression> typeParameters =
                            slotTypeExpression
                            .getCollectionTypeExpression().getTypeParameters();
                        if (typeParameters != null && typeParameters.size() == 1) {

                            scb.add(
                                "jadescript.content.onto.Ontology" +
                                    ".__populateSetSchema(" +
                                    "(jade.content.schema.TermSchema) " +
                                    "getSchema(" +
                                    schemaNameForSlotProvider.apply(
                                        typeParameters.get(0)) + "), " +
                                    "(jade.content.schema.ConceptSchema) getSchema(\"" + className + "\"));");
                        }

                    }
                });
            }

        });
    }

}
