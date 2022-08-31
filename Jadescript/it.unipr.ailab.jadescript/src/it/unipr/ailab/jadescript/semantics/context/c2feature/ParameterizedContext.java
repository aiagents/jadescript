package it.unipr.ailab.jadescript.semantics.context.c2feature;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.context.symbol.ActualParameter;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public interface ParameterizedContext extends NamedSymbol.Searcher {
    List<ActualParameter> getParameters();

    default Stream<? extends NamedSymbol> searchName(
            Predicate<String> name,
            Predicate<IJadescriptType> readingType,
            Predicate<Boolean> canWrite
    ) {
        Stream<ActualParameter> stream = getParameters().stream();
        stream = safeFilter(stream, NamedSymbol::name, name);
        stream = safeFilter(stream, NamedSymbol::readingType, readingType);
        stream = safeFilter(stream, NamedSymbol::canWrite, canWrite);
        return stream;
    }

    static List<ActualParameter> zipArguments(
            List<String> paramNames,
            List<IJadescriptType> paramTypes
    ) {
        return Streams.zip(paramNames.stream(), paramTypes.stream(), ActualParameter::new)
                .collect(Collectors.toList());
    }




    default void debugDumpParameters(SourceCodeBuilder scb){
        scb.open("--> is ParameterizedContext {");
        scb.open("parameters = [");
        for (NamedSymbol parameter : getParameters()) {
            parameter.debugDumpNamedSymbol(scb);
        }
        scb.close("]");
        scb.close("}");
    }

}
