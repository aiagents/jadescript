package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.search.AutoCompiled;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class ContextGeneratedOperation extends Operation {

    public ContextGeneratedOperation(
        boolean withoutSideEffects,
        String name,
        IJadescriptType returnType,
        List<Util.Tuple2<String, IJadescriptType>> params
    ) {
        super(
            withoutSideEffects,
            name,
            returnType,
            params,
            AutoCompiled.getInstance()
        );
    }


    public ContextGeneratedOperation(
        boolean withoutSideEffects,
        String name,
        IJadescriptType returnType,
        List<Util.Tuple2<String, IJadescriptType>> params,
        BiFunction<String, Map<String, String>, String> invokeByNameCustom,
        BiFunction<String, List<String>, String> invokeByArityCustom
    ) {
        super(
            withoutSideEffects,
            name,
            returnType,
            params,
            AutoCompiled.getInstance(),
            invokeByNameCustom,
            invokeByArityCustom
        );
    }


}
