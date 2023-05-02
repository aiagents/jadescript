package it.unipr.ailab.jadescript.semantics.context.c2feature;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.context.symbol.ActualParameter;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.LocalName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ParameterizedContext
    extends LocalName.Namespace {

    List<ActualParameter> getParameters();

    @Override
    default Stream<? extends LocalName> localNames(@Nullable String name) {
        return getParameters().stream().filter(
            p -> name == null || p.name().equals(name)
        );
    }


    static List<ActualParameter> zipArguments(
        List<String> paramNames,
        List<IJadescriptType> paramTypes
    ) {
        return Streams.zip(
            paramNames.stream(),
            paramTypes.stream(),
            ActualParameter::actualParameter
        ).collect(Collectors.toList());
    }


    default void debugDumpParameters(SourceCodeBuilder scb) {
        scb.open("--> is ParameterizedContext {");
        scb.open("parameters = [");
        for (LocalName parameter : getParameters()) {
            parameter.debugDumpName(scb);
        }
        scb.close("]");
        scb.close("}");
    }

}
