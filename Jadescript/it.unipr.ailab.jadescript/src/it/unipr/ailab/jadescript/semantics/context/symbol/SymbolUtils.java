package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;


public class SymbolUtils {

    private SymbolUtils() {
    } // do not instantiate


    public static NamedSymbol setDereferenceByVariable(
        NamedSymbol namedElement,
        String var
    ) {
        return new DereferencedByVarNamedSymbol(
            namedElement, var
        );
    }


    public static CallableSymbol changeLocation(
        CallableSymbol input,
        SearchLocation location
    ) {
        return new ChangedLocationCallableSymbol(
            input,
            location
        );
    }


    public static CallableSymbol setDereferenceByVariable(
        CallableSymbol callableElement,
        String var
    ) {
        return new DereferencedByVarCallableSymbol(
            callableElement,
            var
        );
    }


    public static CallableSymbol setDereferenceByCtor(
        CallableSymbol input,
        IJadescriptType inModuleType
    ) {
        return new DereferencedByCtorCallableSymbol(
            input,
            inModuleType.compileToJavaTypeReference()
        );
    }


    public static CallableSymbol setDereferenceByExternalClass(
        CallableSymbol input,
        IJadescriptType inModuleType
    ) {
        return new DereferencedByExternalClassCallableSymbol(
            input,
            inModuleType.compileToJavaTypeReference()
        );
    }


}
