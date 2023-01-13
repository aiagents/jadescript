package it.unipr.ailab.jadescript.semantics.namespace.jvm;

import it.unipr.ailab.jadescript.semantics.MethodCallSemantics;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JvmOperationSymbol
    implements CallableSymbol, JvmSymbol {

    private final JvmOperation jvmOperation;
    private final Function<JvmTypeReference, IJadescriptType> typeResolver;
    private final boolean isStatic;
    private final Maybe<JvmType> declaringType;


    public JvmOperationSymbol(
        JvmOperation jvmOperation,
        Function<JvmTypeReference, IJadescriptType> typeResolver,
        boolean isStatic,
        Maybe<JvmType> declaringType
    ) {
        this.jvmOperation = jvmOperation;
        this.typeResolver = typeResolver;
        this.isStatic = isStatic;
        this.declaringType = declaringType;
    }


    @Override
    public String name() {
        return jvmOperation.getSimpleName();
    }


    @Override
    public IJadescriptType returnType() {
        return typeResolver.apply(jvmOperation.getReturnType());
    }


    @Override
    public Map<String, IJadescriptType> parameterTypesByName() {
        return jvmOperation.getParameters().stream()
            .collect(Collectors.toMap(
                JvmFormalParameter::getName,
                p -> typeResolver.apply(p.getParameterType())
            ));
    }


    @Override
    public List<String> parameterNames() {
        return jvmOperation.getParameters().stream()
            .map(JvmFormalParameter::getName)
            .collect(Collectors.toList());
    }


    @Override
    public String compileInvokeByName(
        String dereferencePrefix,
        Map<String, String> compiledRexprs
    ) {
        List<String> argNames = new ArrayList<>();
        List<String> args = new ArrayList<>();
        compiledRexprs.forEach((name, arg) -> {
            argNames.add(name);
            args.add(arg);
        });
        return dereferencePrefix + jvmOperation.getSimpleName() + "(" +
            String.join(
                ", ",
                MethodCallSemantics.sortToMatchParamNames(
                    args,
                    argNames,
                    parameterNames()
                )
            ) + ")";
    }


    @Override
    public boolean isPure() {
        return false;
    }


    @Override
    public StaticState advanceCall(StaticState state) {
        return state;
    }


    @Override
    public List<IJadescriptType> parameterTypes() {
        return jvmOperation.getParameters().stream()
            .map(p -> typeResolver.apply(p.getParameterType()))
            .collect(Collectors.toList());
    }


    @Override
    public String compileInvokeByArity(
        String dereferencePrefix,
        List<String> compiledRexprs
    ) {
        return dereferencePrefix + jvmOperation.getSimpleName() + "(" +
            String.join(", ", compiledRexprs) +
            ")";
    }


    @Override
    public boolean isStatic() {
        return this.isStatic;
    }


    @Override
    public Maybe<JvmType> declaringType() {
        return this.declaringType;
    }


    @Override
    public void debugDumpCallableSymbol(SourceCodeBuilder scb) {
        CallableSymbol.super.debugDumpCallableSymbol(scb);
        scb.indent().line("--> (" + name() + " is also JvmOperationSymbol; isStatic=" + isStatic() + ")").dedent();
    }


}
