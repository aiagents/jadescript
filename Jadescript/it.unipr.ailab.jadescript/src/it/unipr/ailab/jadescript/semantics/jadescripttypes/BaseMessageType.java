package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.*;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.of;


public class BaseMessageType extends ParametricType implements EmptyCreatable {

    private final Map<String, Property> properties = new HashMap<>();
    private boolean initializedProperties = false;
    private final TypeArgument contentType;
    private final Class<?> messageClass;

    protected BaseMessageType(
            SemanticsModule module,
            String typeName,
            Class<?> messageClass,
            List<TypeArgument> typeParameters,
            List<IJadescriptType> upperBounds
    ) {
        super(
                module,
                TypeHelper.builtinPrefix + typeName,
                typeName,
                "MESSAGE",
                "of",
                "(", ")", ",",
                typeParameters,
                upperBounds
        );
        this.contentType = (typeParameters.size() == 1)
                ? typeParameters.get(0).ignoreBound()
                : module.get(TypeHelper.class).TUPLE.apply(typeParameters);
        this.messageClass = messageClass;
    }

    public BaseMessageType(
            SemanticsModule module,
            TypeArgument contentType
    ) {
        super(
                module,
                TypeHelper.builtinPrefix + "Message",
                "Message",
                "MESSAGE",
                "of",
                "(", ")", ",", Arrays.asList(contentType),
                Arrays.asList(module.get(TypeHelper.class).ANY)
        );
        this.contentType = contentType;
        this.messageClass = jadescript.core.message.Message.class;
    }


    public IJadescriptType getContentType() {
        return contentType.ignoreBound();
    }

    private void initBuiltinProperties() {
        if (!initializedProperties) {
            addProperty(new Property("sender", module.get(TypeHelper.class).AID, true, getLocation())
                    .setCustomCompile(
                            (__) -> MESSAGE_VAR_NAME + ".getSender()",
                            (__, send) -> MESSAGE_VAR_NAME + ".setSender(" + send + ")"
                    )
            );
            addProperty(new Property("performative", module.get(TypeHelper.class).PERFORMATIVE, true, getLocation())
                    .setCustomCompile(
                            (__) -> MESSAGE_VAR_NAME + ".getJadescriptPerformative()",
                            (__, perf) -> MESSAGE_VAR_NAME + ".setJadescriptPerformative(" + perf + ")"
                    )
            );
            addProperty(new Property("content", contentType.ignoreBound(), true, getLocation())
                    .setCustomCompile(
                            (__) -> CONTENT_VAR_NAME,
                            (__, re) -> CONTENT_VAR_NAME + "=" + re)
            );
            addProperty(new Property("ontology", module.get(TypeHelper.class).ONTOLOGY, true, getLocation())
                    .setCustomCompile(
                            (__) -> MESSAGE_VAR_NAME + ".getOntology()",
                            (__, onto) -> MESSAGE_VAR_NAME + ".setOntology(" + onto + ")"
                    ));
        }
        this.initializedProperties = true;
    }

    protected Map<String, Property> getBuiltinProperties() {
        initBuiltinProperties();
        return properties;
    }

    @Override
    public void addProperty(Property prop) {
        properties.put(prop.name(), prop);
    }

    @Override
    public boolean isBasicType() {
        return false;
    }

    @Override
    public boolean isSlottable() {
        return false;
    }

    @Override
    public boolean isSendable() {
        return true;
    }

    @Override
    public boolean isReferrable() {
        return true;
    }

    @Override
    public boolean isManipulable() {
        return true;
    }

    @Override
    public boolean isErroneous() {
        return false;
    }

    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return of(module.get(TypeHelper.class).ONTOLOGY);
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public TypeNamespace namespace() {
        return new BuiltinOpsNamespace(
                module,
                nothing(),
                new ArrayList<>(getBuiltinProperties().values()),
                List.of(),
                getLocation()
        );
    }

    @Override
    public JvmTypeReference asJvmTypeReference() {
        return module.get(TypeHelper.class).typeRef(
                messageClass,
                getTypeArguments().stream().map(TypeArgument::asJvmTypeReference).collect(Collectors.toList())
        );
    }

    @Override
    public String compileNewEmptyInstance() {
        return "new jadescript.core.message.Message<>(jadescript.lang.Performative.UNKNOWN)";
    }
}
