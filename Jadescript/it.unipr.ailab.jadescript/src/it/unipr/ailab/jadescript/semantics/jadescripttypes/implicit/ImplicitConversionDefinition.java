package it.unipr.ailab.jadescript.semantics.jadescripttypes.implicit;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.function.Function;

class ImplicitConversionDefinition {

    private final IJadescriptType from;
    private final IJadescriptType to;
    private final Function<String, String> conversionCompilation;


    public ImplicitConversionDefinition(
        IJadescriptType from,
        IJadescriptType to,
        Function<String, String> conversionCompilation
    ) {
        this.from = from;
        this.to = to;
        this.conversionCompilation = conversionCompilation;
    }


    public IJadescriptType getFrom() {
        return from;
    }


    public IJadescriptType getTo() {
        return to;
    }


    public String compileConversion(String compiledRExpr) {
        return conversionCompilation.apply(compiledRExpr);
    }

}
