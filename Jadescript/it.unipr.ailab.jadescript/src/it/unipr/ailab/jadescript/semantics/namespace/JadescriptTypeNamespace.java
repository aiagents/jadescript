package it.unipr.ailab.jadescript.semantics.namespace;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.Operation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmModelBasedNamespace;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import org.eclipse.xtext.util.Strings;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class JadescriptTypeNamespace extends TypeNamespace {


    public JadescriptTypeNamespace(SemanticsModule module) {
        super(module);
    }


    protected UserDefinedSearchers computeUserDefinedSymbols(
        JvmModelBasedNamespace namespace
    ) {
        return new UserDefinedSearchers(namespace);
    }

    protected class UserDefinedSearchers implements
        CallableSymbol.Searcher, NamedSymbol.Searcher {

        private final JvmModelBasedNamespace namespace;


        public UserDefinedSearchers(JvmModelBasedNamespace namespace) {
            this.namespace = namespace;
        }


        @Override
        public Stream<? extends CallableSymbol> searchCallable(
            String name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>>
                parameterTypes
        ) {
            return namespace.searchAs(
                CallableSymbol.Searcher.class,
                s -> s.searchCallable(
                    name,
                    returnType,
                    parameterNames,
                    parameterTypes
                )
            ).map(cs -> new Operation(
                false,
                cs.name(),
                cs.returnType(),
                Streams.zip(
                    cs.parameterNames().stream(),
                    cs.parameterTypes().stream(),
                    Util.Tuple2::new
                ).collect(Collectors.toList()),
                cs.sourceLocation(),
                cs::compileInvokeByName,
                cs::compileInvokeByArity
            ));
        }


        @Override
        public Stream<? extends CallableSymbol> searchCallable(
            Predicate<String> name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>>
                parameterTypes
        ) {
            return namespace.searchAs(
                CallableSymbol.Searcher.class,
                s -> s.searchCallable(
                    name,
                    returnType,
                    parameterNames,
                    parameterTypes
                )
            ).map(cs -> new Operation(
                false,
                cs.name(),
                cs.returnType(),
                Streams.zip(
                    cs.parameterNames().stream(),
                    cs.parameterTypes().stream(),
                    Util.Tuple2::new
                ).collect(Collectors.toList()),
                cs.sourceLocation(),
                cs::compileInvokeByName,
                cs::compileInvokeByArity
            ));
        }


        @Override
        public Stream<? extends NamedSymbol> searchName(
            Predicate<String> name,
            Predicate<IJadescriptType> readingType,
            Predicate<Boolean> canWrite
        ) {
            return namespace.searchName(name, readingType, canWrite)
                .filter(ns -> namespace.searchCallable(
                        "get" + Strings.toFirstUpper(ns.name()),
                        rt -> ns.readingType().typeEquals(rt),
                        (size, names) -> size == 0,
                        (size, types) -> size == 0
                    ).anyMatch(cs -> cs.sourceLocation()
                    .equals(ns.sourceLocation()))
                ).map(ns -> {
                    boolean isWriteable = namespace.searchCallable(
                        "set" + Strings.toFirstUpper(ns.name()),
                        rt -> module.get(TypeHelper.class).VOID.typeEquals(rt),
                        (size, names) -> size == 1,
                        (size, types) -> size == 1 && types.apply(0).typeEquals(
                            ns.writingType())
                    ).anyMatch(cs -> cs.sourceLocation()
                        .equals(ns.sourceLocation()));
                    return new Property(
                        ns.name(),
                        ns.readingType(),
                        !isWriteable,
                        ns.sourceLocation()
                    ).setCompileByJVMAccessors();
                });
        }

    }

}
