package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Operation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.Util.Tuple2;
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
            new Property(
                "length",
                typeHelper.INTEGER,
                true,
                getLocation()
            ).setCompileByCustomJVMMethod("size", "size")
        );
        this.addProperty(
            new Property(
                "head",
                getElementType(),
                false,
                getLocation()
            ).setCustomCompile(
                (e) -> e + ".get(0)",
                (e, re) -> e + ".set(0, " + re + ")"
            )
        );
        this.addProperty(
            new Property("tail", this, true, getLocation()).setCustomCompile(
                (e) -> "jadescript.util.JadescriptCollections" +
                    ".getRest(" + e + ", 1)",
                (e, re) -> "jadescript.util.JadescriptCollections" +
                    ".getRest(" + e + ", 1)"
            )
        );
        this.addProperty(
            new Property("last", this, true, getLocation()).setCustomCompile(
                (e) -> e + ".get(" + e + ".size()-1)",
                (e, re) -> e + ".set(" + e + ".size()-1, " + re + ")"
            )
        );
        operations.add(new Operation(
            false,
            "__add",
            typeHelper.VOID,
            List.of(
                new Tuple2<>("element", getElementType())
            ),
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
            false,
            "__addAt",
            typeHelper.VOID,
            List.of(
                new Tuple2<>("index", typeHelper.INTEGER),
                new Tuple2<>("element", getElementType())
            ),
            getLocation(),
            (receiver, namedArgs) -> {
                return receiver + ".add(" + namedArgs.get("index") + ", " +
                    namedArgs.get("element") + ")";
            },
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
            }
        ));
        operations.add(new Operation(
            false,
            "__addAll",
            typeHelper.VOID,
            List.of(new Tuple2<>("elements", this)),
            getLocation(),
            (receiver, namedArgs) -> {
                return receiver + ".addAll(" + namedArgs.get("elements") + ")";
            },
            (receiver, args) -> {
                final String e;
                if (args.size() >= 1) {
                    e = args.get(1);
                } else {
                    e = "/*internal error: missing arguments*/";
                }
                return receiver + ".addAll(" + e + ")";
            }
        ));
        operations.add(new Operation(
            false,
            "__addAllAt",
            typeHelper.VOID,
            List.of(
                new Tuple2<>("index", typeHelper.INTEGER),
                new Tuple2<>("elements", this)
            ),
            getLocation(),
            (receiver, namedArgs) -> {
                return receiver + ".addAll(" + namedArgs.get("index") + ", " +
                    namedArgs.get("elements") + ")";
            },
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
            }
        ));
        operations.add(new Operation(
            true,
            "get",
            getElementType(),
            List.of(
                new Tuple2<>("index", typeHelper.INTEGER)
            ),
            getLocation()
        ));
        operations.add(new Operation(
            false,
            "set",
            typeHelper.VOID,
            List.of(
                new Tuple2<>("index", typeHelper.INTEGER),
                new Tuple2<>("element", getElementType())
            ),
            getLocation()
        ));
        operations.add(new Operation(
            true,
            "contains",
            typeHelper.BOOLEAN,
            List.of(new Tuple2<>("o", getElementType())),
            getLocation()
        ));
        operations.add(new Operation(
            true,
            "containsAll",
            typeHelper.BOOLEAN,
            List.of(new Tuple2<>("o", this)),
            getLocation()
        ));
        operations.add(new Operation(
            true,
            "containsAll",
            typeHelper.BOOLEAN,
            List.of(new Tuple2<>(
                "o",
                typeHelper.SET.apply(Arrays.asList(getElementType()))
            )),
            getLocation()
        ));
        operations.add(new Operation(
            true,
            "containsAny",
            typeHelper.BOOLEAN,
            List.of(new Tuple2<>("o", this)),
            getLocation()
        ));
        operations.add(new Operation(
            true,
            "containsAny",
            typeHelper.BOOLEAN,
            List.of(new Tuple2<>(
                "o",
                typeHelper.SET.apply(Arrays.asList(getElementType()))
            )),
            getLocation()
        ));
        operations.add(new Operation(
            false,
            "clear",
            typeHelper.VOID,
            List.of(),
            getLocation()
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
