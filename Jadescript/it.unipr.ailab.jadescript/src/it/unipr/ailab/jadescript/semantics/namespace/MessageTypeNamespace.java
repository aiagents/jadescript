package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.message.MessageType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;

import java.util.List;

import static it.unipr.ailab.maybe.Maybe.nothing;

public class MessageTypeNamespace extends BuiltinOpsNamespace {


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
