package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;

import java.util.List;

public class MessageSubType extends BaseMessageType{
    private final Class<?> messageClass;

    public MessageSubType(
            SemanticsModule module,
            Class<?> messageClass,
            List<TypeArgument> typeArguments,
            List<IJadescriptType> upperBounds
    ) {
        super(
                module,
                messageClass.getSimpleName(),
                messageClass,
                typeArguments,
                upperBounds
        );
        this.messageClass = messageClass;
    }

    @Override
    public String compileNewEmptyInstance() {
        return "new "+messageClass.getName()+"<>()";
    }

    @Override
    public MessageTypeNamespace namespace() {
        return MessageTypeNamespace.messageTypeNamespace(
            module,
            getContentType(),
            getLocation()
        );
    }

}
