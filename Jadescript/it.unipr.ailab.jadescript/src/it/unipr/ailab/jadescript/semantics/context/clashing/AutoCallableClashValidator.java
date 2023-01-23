package it.unipr.ailab.jadescript.semantics.context.clashing;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.FQNameLocation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import org.eclipse.xtext.util.Strings;

import java.util.List;
import java.util.stream.Stream;

public interface AutoCallableClashValidator extends CallableClashValidator {

    @Override
    default Stream<DefinitionClash> checkCallableClash(SemanticsModule module, CallableSymbol toBeAdded) {
        Stream<DefinitionClash> fromCallables;
        if (this instanceof CallableSymbol.Searcher) {
            fromCallables = ((CallableSymbol.Searcher) this).searchCallable(
                            toBeAdded.name(),
                            null,
                            (size, names) -> size == toBeAdded.parameterNames().size(),
                            (size, types) -> size == toBeAdded.parameterTypes().size()
                    )
                    .filter(alreadyPresent -> !isOverriding(module, alreadyPresent, toBeAdded))
                    .map(alreadyPresent -> new DefinitionClash(toBeAdded, alreadyPresent));
            if (toBeAdded.parameterTypes().size() == 0) {
                fromCallables = Streams.concat(
                        fromCallables,
                        ((CallableSymbol.Searcher) this).searchCallable(
                                "get" + Strings.toFirstUpper(toBeAdded.name()),
                                null,
                                (size, names) -> size == 0,
                                (size, types) -> size == 0
                        ).map(alreadyPresent -> new DefinitionClash(toBeAdded, alreadyPresent))
                );
            }
            if (toBeAdded.parameterTypes().size() == 1) {
                fromCallables = Streams.concat(
                        fromCallables,
                        ((CallableSymbol.Searcher) this).searchCallable(
                                "set" + Strings.toFirstUpper(toBeAdded.name()),
                                null,
                                (size, names) -> size == 1,
                                (size, types) -> size == 1
                        ).map(alreadyPresent -> new DefinitionClash(toBeAdded, alreadyPresent))
                );
            }
        } else {
            fromCallables = Stream.empty();
        }

        Stream<DefinitionClash> fromNameds;
        if (this instanceof NamedSymbol.Searcher && toBeAdded.parameterNames().size() == 0) {
            fromNameds = ((NamedSymbol.Searcher) this).searchName(
                    toBeAdded.name(),
                    null,
                    null
            ).map(alreadyPresent -> new DefinitionClash(toBeAdded, alreadyPresent));
        } else {
            fromNameds = Stream.empty();
        }

        return Streams.concat(
                fromNameds,
                fromCallables
        );
    }

    /**
     * Returns true if {@code toBeAdded} is a declaration that overrides {@code alreadyPresent}.
     */
    static boolean isOverriding(SemanticsModule module, CallableSymbol alreadyPresent, CallableSymbol toBeAdded) {
        final SearchLocation alreadyPresentLocation = alreadyPresent.sourceLocation();
        final SearchLocation toBeAddedLocation = toBeAdded.sourceLocation();
        if (alreadyPresentLocation instanceof FQNameLocation
                && toBeAddedLocation instanceof FQNameLocation) {
            final IJadescriptType alreadyPresentType = ((FQNameLocation) alreadyPresentLocation).extractType(module);
            final IJadescriptType toBeAddedType = ((FQNameLocation) toBeAddedLocation).extractType(module);
            // is overriding if...
            return alreadyPresentType.isSupEqualTo(toBeAddedType) // it is a supertype
                    && !alreadyPresentType.typeEquals(toBeAddedType) // it has to be strictly a supertype
                    && isSignatureCompatibleForOverriding(alreadyPresent, toBeAdded); // the signature is compatible (in the Java sense)
        }
        return false;
    }

    class X0{}
    class X1 extends X0{

    }
    class X2 extends X1{}

    class A{
        public X1 method(X1 helo){
            return null;
        }
    }

    class B extends A{
        @Override
        public X1 method(X1 helo) {
            return null;
        }
    }

    static boolean isSignatureCompatibleForOverriding(CallableSymbol alreadyPresent, CallableSymbol toBeAdded) {
        if (alreadyPresent.arity() != toBeAdded.arity()) {
            return false;
        }
        if(!alreadyPresent.returnType().isSupEqualTo(toBeAdded.returnType())){
            return false;
        }
        final List<IJadescriptType> apTypes = alreadyPresent.parameterTypes();
        final List<IJadescriptType> tbaTypes = toBeAdded.parameterTypes();
        for (int i = 0; i < apTypes.size(); i++) {
            if(!apTypes.get(i).typeEquals(tbaTypes.get(i))){
                return false;
            }
        }
        return true;
    }
}
