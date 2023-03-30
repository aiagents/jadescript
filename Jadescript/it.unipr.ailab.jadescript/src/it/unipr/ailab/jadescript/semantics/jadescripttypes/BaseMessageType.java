package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;


public class BaseMessageType extends ParametricType implements EmptyCreatable {

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
            "(",
            ")",
            ",",
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


    @Override
    public void addProperty(Property prop) {

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
    public boolean hasProperties() {
        return true;
    }


    @Override
    public boolean isErroneous() {
        return false;
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return some(module.get(TypeHelper.class).ONTOLOGY);
    }


    @Override
    public boolean isCollection() {
        return false;
    }


    @Override
    public MessageTypeNamespace namespace() {
        return MessageTypeNamespace.messageTypeNamespace(
            module,
            getContentType(),
            getLocation()
        );
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        return module.get(TypeHelper.class).typeRef(
            messageClass,
            getTypeArguments().stream()
                .map(TypeArgument::asJvmTypeReference)
                .collect(Collectors.toList())
        );
    }


    @Override
    public String compileNewEmptyInstance() {
        return "new jadescript.core.message.Message<>(jadescript.lang" +
            ".Performative.UNKNOWN)";
    }


    @Override
    public boolean requiresAgentEnvParameter() {
        return false;
    }


    public static class MessageTypeNamespace extends BuiltinOpsNamespace {


        private final Property senderProperty;
        private final Property performativeProperty;
        private final Property contentProperty;
        private final Property ontologyProperty;


        private MessageTypeNamespace(
            SemanticsModule module,
            Property senderProperty,
            Property performativeProperty,
            Property contentProperty,
            Property ontologyProperty,
            SearchLocation location
        ) {
            super(
                module,
                nothing(),
                List.of(
                    senderProperty,
                    performativeProperty,
                    contentProperty,
                    ontologyProperty
                ),
                List.of(),
                location
            );
            this.senderProperty = senderProperty;
            this.performativeProperty = performativeProperty;
            this.contentProperty = contentProperty;
            this.ontologyProperty = ontologyProperty;
        }


        public static MessageTypeNamespace messageTypeNamespace(
            SemanticsModule module,
            TypeArgument contentType,
            SearchLocation location
        ) {
            final TypeHelper typeHelper = module.get(TypeHelper.class);
            return new MessageTypeNamespace(
                module,
                Property.readonlyProperty(
                    "sender",
                    typeHelper.AID,
                    location,
                    Property.compileWithJVMGetter("sender")
                ),
                Property.readonlyProperty(
                    "performative",
                    typeHelper.PERFORMATIVE,
                    location,
                    Property.compileGetWithCustomMethod(
                        "getJadescriptPerformative"
                    )
                ),
                Property.readonlyProperty(
                    "content",
                    contentType.ignoreBound(),
                    location,
                    (o, a) -> o + "." + "getContent(" +
                        CompilationHelper.compileAgentReference() +
                        ".getContentManager())"
                ),
                Property.readonlyProperty(
                    "ontology",
                    typeHelper.ONTOLOGY,
                    location,
                    Property.compileWithJVMGetter("ontology")
                ),
                location
            );
        }


        public Property getSenderProperty() {
            return senderProperty;
        }


        public Property getPerformativeProperty() {
            return performativeProperty;
        }


        public Property getContentProperty() {
            return contentProperty;
        }


        public Property getOntologyProperty() {
            return ontologyProperty;
        }

    }

}
