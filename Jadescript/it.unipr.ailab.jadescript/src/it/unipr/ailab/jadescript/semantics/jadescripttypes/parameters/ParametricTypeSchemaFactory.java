package it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public class ParametricTypeSchemaFactory {

    private final SemanticsModule factoryModule;


    public ParametricTypeSchemaFactory(SemanticsModule module) {
        this.factoryModule = module;
    }


    public <S extends IJadescriptType> ParametricTypeSchema<S>
    parametricType() {
        return new ParametricTypeSchema<>(this.factoryModule);
    }


    public MessageTypeSchema messageType() {
        return new MessageTypeSchema(this.factoryModule);
    }


}
