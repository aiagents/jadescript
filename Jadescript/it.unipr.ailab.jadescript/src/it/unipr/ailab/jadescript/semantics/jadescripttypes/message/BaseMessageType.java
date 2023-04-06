package it.unipr.ailab.jadescript.semantics.jadescripttypes.message;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.JadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.MessageTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.List;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.some;


public final class BaseMessageType
    extends JadescriptType
    implements MessageType, EmptyCreatable {

    private final TypeArgument contentType;


    public BaseMessageType(
        SemanticsModule module,
        TypeArgument contentType
    ) {
        super(
            module,
            TypeHelper.builtinPrefix + "Message",
            "Message",
            "MESSAGE"
        );
        this.contentType = contentType;
    }


    @Override
    public IJadescriptType getContentType() {
        return contentType.ignoreBound();
    }


    @Override
    public void addBultinProperty(Property prop) {

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
    public MessageTypeNamespace namespace() {
        return MessageTypeNamespace.messageTypeNamespace(
            module,
            getContentType(),
            getLocation()
        );
    }


    @Override
    public Stream<IJadescriptType> declaredSupertypes() {
        return Stream.empty();
    }


    @Override
    public List<TypeArgument> typeArguments() {
        return List.of(this.contentType);
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        return module.get(TypeHelper.class).typeRef(
            jadescript.core.message.Message.class,
            contentType.asJvmTypeReference()
        );
    }


    @Override
    public String compileNewEmptyInstance() {
        return "new jadescript.core.message.Message<>(" +
            "jadescript.lang.Performative.UNKNOWN)";
    }


    @Override
    public boolean requiresAgentEnvParameter() {
        return false;
    }




}
