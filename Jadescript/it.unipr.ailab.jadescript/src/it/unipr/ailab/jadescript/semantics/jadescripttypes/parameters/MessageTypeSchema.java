package it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.message.MessageSubType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class MessageTypeSchema
    extends ParametricTypeSchema<MessageSubType> {

    /*package-private*/ MessageTypeSchema(
        SemanticsModule factoryModule
    ) {
        super(factoryModule);
    }


    @Override
    @Contract("_ -> this")
    public MessageTypeSchema add(
        @NotNull FormalTypeParameter parameter
    ) {
        super.add(parameter);
        return this;
    }


    @Override
    @Contract("_ -> this")
    public MessageTypeSchema add(
        @NotNull VariadicTypeParameter parameter
    ) {
        super.add(parameter);
        return this;
    }


    @Override
    @Contract("_ -> this")
    public MessageTypeSchema seal(
        ParametricTypeBuilder<? extends MessageSubType> builder
    ) {
        super.seal(builder);
        return this;
    }


    public String adaptContentExpression(
        int argumentIndex,
        IJadescriptType inputContentType,
        String inputExpression
    ) {
        if (argumentIndex >= formalTypeParameters.size()) {
            return inputExpression;
        }

        final FormalTypeParameter formalTypeParameter =
            formalTypeParameters.get(argumentIndex);

        if (!(formalTypeParameter instanceof MessageTypeParameter)) {
            return inputExpression;
        }

        MessageTypeParameter typeParameter =
            (MessageTypeParameter) formalTypeParameter;

        return typeParameter.promoter.apply(
            inputContentType,
            inputExpression
        );
    }

}
