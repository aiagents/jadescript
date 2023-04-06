package it.unipr.ailab.jadescript.semantics.jadescripttypes.message;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.JadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.MessageTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.utils.LazyInit;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.some;

public class MessageSubType
    extends JadescriptType
    implements MessageType {

    private final Class<?> messageClass;
    private final List<TypeArgument> typeArguments;
    private final LazyInit<IJadescriptType> contentType;


    public MessageSubType(
        SemanticsModule module,
        Class<?> messageClass,
        TypeArgument... typeArguments
    ) {
        super(
            module,
            TypeHelper.builtinPrefix + messageClass.getSimpleName(),
            messageClass.getSimpleName(),
            "MESSAGE"
        );
        this.messageClass = messageClass;
        this.typeArguments = Arrays.asList(typeArguments);
        this.contentType = new LazyInit<>(this::computeRootContentType);
    }


    private IJadescriptType computeRootContentType() {
        final TypeHelper typeHelper = module.get(TypeHelper.class);

        if (this.typeArguments().isEmpty()) {
            return typeHelper.ANY;
        }

        if (this.typeArguments().size() == 1) {
            return typeArguments().get(0).ignoreBound();
        }

        return typeHelper.TUPLE.apply(this.typeArguments());
    }


    @Override
    public String compileNewEmptyInstance() {
        return "new " + messageClass.getName() + "<>()";
    }


    @Override
    public boolean requiresAgentEnvParameter() {
        return false;
    }


    @Override
    public IJadescriptType getContentType() {
        return this.contentType.get();
    }


    @Override
    public Stream<IJadescriptType> declaredSupertypes() {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        return Stream.of(typeHelper.MESSAGE.apply(List.of(getContentType())));
    }


    @Override
    public List<TypeArgument> typeArguments() {
        return this.typeArguments;
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        return module.get(TypeHelper.class).typeRef(
            this.messageClass,
            getContentType().asJvmTypeReference()
        );
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
    public void addBultinProperty(Property prop) {

    }

}
