package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.List;
import java.util.Map;


public class SymbolUtils {
    private SymbolUtils(){} // do not instantiate



    public static NamedSymbol setDereferenceByVariable(NamedSymbol namedElement, String var) {
        return new NamedSymbol() {
            @Override
            public SearchLocation sourceLocation() {
                return namedElement.sourceLocation();
            }

            @Override
            public String name() {
                return namedElement.name();
            }

            @Override
            public String compileRead(String ignored) {
                return namedElement.compileRead(var+".");
            }

            @Override
            public IJadescriptType readingType() {
                return namedElement.readingType();
            }

            @Override
            public boolean canWrite() {
                return namedElement.canWrite();
            }

            @Override
            public String compileWrite(String dereferencePrefix, String rexpr) {
                return namedElement.compileWrite(var+".", rexpr);
            }
        };
    }

    public static CallableSymbol changeLocation(CallableSymbol input, SearchLocation location){
        return new CallableSymbol() {
            @Override
            public String name() {
                return input.name();
            }

            @Override
            public IJadescriptType returnType() {
                return input.returnType();
            }

            @Override
            public Map<String, IJadescriptType> parameterTypesByName() {
                return input.parameterTypesByName();
            }

            @Override
            public List<String> parameterNames() {
                return input.parameterNames();
            }

            @Override
            public List<IJadescriptType> parameterTypes() {
                return input.parameterTypes();
            }

            @Override
            public String compileInvokeByArity(String dereferencePrefix, List<String> compiledRexprs) {
                return input.compileInvokeByArity(dereferencePrefix, compiledRexprs);
            }

            @Override
            public String compileInvokeByName(String dereferencePrefix, Map<String, String> compiledRexprs) {
                return input.compileInvokeByName(dereferencePrefix, compiledRexprs);
            }

            @Override
            public SearchLocation sourceLocation() {
                return location;
            }

        };
    }



    public static CallableSymbol setDereferenceByVariable(CallableSymbol callableElement, String var){
        return new CallableSymbol() {
            @Override
            public SearchLocation sourceLocation() {
                return callableElement.sourceLocation();
            }

            @Override
            public String name() {
                return callableElement.name();
            }

            @Override
            public IJadescriptType returnType() {
                return callableElement.returnType();
            }

            @Override
            public Map<String, IJadescriptType> parameterTypesByName() {
                return callableElement.parameterTypesByName();
            }

            @Override
            public List<String> parameterNames() {
                return callableElement.parameterNames();
            }

            @Override
            public List<IJadescriptType> parameterTypes() {
                return callableElement.parameterTypes();
            }

            @Override
            public String compileInvokeByName(String dereferencePrefix, Map<String, String> compiledRexprs) {
                return callableElement.compileInvokeByName(var+".", compiledRexprs);
            }

            @Override
            public String compileInvokeByArity(String dereferencePrefix, List<String> compiledRexprs) {
                return callableElement.compileInvokeByArity(var+".", compiledRexprs);
            }
        };
    }

    public static CallableSymbol setDereferenceByCtor(CallableSymbol input, IJadescriptType inModuleType){
        return new CallableSymbol() {
            @Override
            public SearchLocation sourceLocation() {
                return input.sourceLocation();
            }

            @Override
            public String name() {
                return input.name();
            }

            @Override
            public IJadescriptType returnType() {
                return input.returnType();
            }

            @Override
            public Map<String, IJadescriptType> parameterTypesByName() {
                return input.parameterTypesByName();
            }

            @Override
            public List<String> parameterNames() {
                return input.parameterNames();
            }

            @Override
            public List<IJadescriptType> parameterTypes() {
                return input.parameterTypes();
            }

            @Override
            public String compileInvokeByName(String dereferencePrefix, Map<String, String> compiledRexprs) {
                return input.compileInvokeByName(
                        "new " + inModuleType.compileToJavaTypeReference() + "().",
                        compiledRexprs
                );
            }

            @Override
            public String compileInvokeByArity(String dereferencePrefix, List<String> compiledRexprs) {
                return input.compileInvokeByArity(
                        "new " + inModuleType.compileToJavaTypeReference() + "().",
                        compiledRexprs
                );
            }
        };
    }

    public static CallableSymbol setDereferenceByExternalClass(CallableSymbol input, IJadescriptType inModuleType){
        return new CallableSymbol() {
            @Override
            public SearchLocation sourceLocation() {
                return input.sourceLocation();
            }

            @Override
            public String name() {
                return input.name();
            }

            @Override
            public IJadescriptType returnType() {
                return input.returnType();
            }

            @Override
            public Map<String, IJadescriptType> parameterTypesByName() {
                return input.parameterTypesByName();
            }

            @Override
            public List<String> parameterNames() {
                return input.parameterNames();
            }

            @Override
            public List<IJadescriptType> parameterTypes() {
                return input.parameterTypes();
            }

            @Override
            public String compileInvokeByName(String dereferencePrefix, Map<String, String> compiledRexprs) {
                return input.compileInvokeByName(
                        inModuleType.compileToJavaTypeReference()+".",
                        compiledRexprs
                );
            }

            @Override
            public String compileInvokeByArity(String dereferencePrefix, List<String> compiledRexprs) {
                return input.compileInvokeByArity(
                        inModuleType.compileToJavaTypeReference()+".",
                        compiledRexprs
                );
            }
        };
    }
}
