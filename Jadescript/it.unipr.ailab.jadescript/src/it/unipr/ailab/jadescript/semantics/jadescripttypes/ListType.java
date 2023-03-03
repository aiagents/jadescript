package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Operation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.*;
import java.util.stream.Collectors;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;
import static it.unipr.ailab.maybe.Maybe.some;

public class ListType extends ParametricType implements EmptyCreatable {


    private final Map<String, Property> properties = new HashMap<>();
    private final List<Operation> operations = new ArrayList<>();
    private boolean initializedProperties = false;


    public ListType(
        SemanticsModule module,
        TypeArgument elementType
    ) {
        super(
            module,
            builtinPrefix + "List",
            "List",
            "LIST",
            "of",
            "",
            "",
            "",
            Collections.singletonList(elementType),
            Collections.singletonList(module.get(TypeHelper.class).ANY
            )
        );

    }


    private void initBuiltinProperties() {
        if (initializedProperties) {
            return;
        }
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        this.addProperty(
            Property.readonlyProperty(
                "length",
                typeHelper.INTEGER,
                getLocation(),
                Property.compileGetWithCustomMethod("size")
            )
        );
        this.addProperty(
            new Property(
                true,
                "head",
                getElementType(),
                getLocation(),
                (o, a) -> o + ".get(0)",
                (o, r, a) -> a.accept(
                    w.simpleStmt(o + ".set(0, " + r + ")")
                )
            )
        );
        this.addProperty(
            new Property(
                false,
                "tail",
                this,
                getLocation(),
                (e, a) -> "jadescript.util.JadescriptCollections" +
                    ".getRest(" + e + ", 1)",
                (e, re, a) -> a.accept(
                    w.simpleStmt("jadescript.util.JadescriptCollections" +
                    ".getRest(" + e + ", 1)")
                )
            )
        );
        this.addProperty(
            new Property(
                true,
                "last",
                this,
                getLocation(),
                (e, a) -> e + ".get(" + e + ".size()-1)",
                (e, re, a) -> a.accept(
                    w.simpleStmt(e + ".set(" + e + ".size()-1, " + re + ")")
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
        return getTypeArguments().get(0).ignoreBound();
    }


    @Override
    public Maybe<IJadescriptType> getElementTypeIfCollection() {
        return some(getElementType());
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        return module.get(TypeHelper.class).typeRef(
            List.class,
            getTypeArguments().stream()
                .map(TypeArgument::asJvmTypeReference)
                .collect(Collectors.toList())
        );
    }


    @Override
    public void addProperty(Property prop) {
        properties.put(prop.name(), prop);
    }


    @Override
    public boolean isSlottable() {
        return getTypeArguments().stream().map(TypeArgument::ignoreBound).allMatch(
            IJadescriptType::isSlottable);
    }


    @Override
    public boolean isSendable() {
        return getTypeArguments().stream().map(TypeArgument::ignoreBound).allMatch(
            IJadescriptType::isSendable);
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
        return "jade.content.schema.ContentElementListSchema.BASE_NAME";
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
        return "new java.util.ArrayList<" +
            getElementType().compileToJavaTypeReference() + ">()";
    }

}
