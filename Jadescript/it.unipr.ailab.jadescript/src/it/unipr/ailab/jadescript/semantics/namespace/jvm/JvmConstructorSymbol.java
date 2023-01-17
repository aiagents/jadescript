package it.unipr.ailab.jadescript.semantics.namespace.jvm;

import it.unipr.ailab.jadescript.semantics.CallSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.common.types.JvmConstructor;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JvmConstructorSymbol
    implements CallableSymbol, JvmSymbol {

    private final SemanticsModule module;
    private final JvmConstructor jvmConstructor;
    private final Function<JvmTypeReference, IJadescriptType> typeResolver;
    private final Maybe<JvmType> declaringType;


    public JvmConstructorSymbol(
        SemanticsModule module,
        JvmConstructor jvmConstructor,
        Function<JvmTypeReference, IJadescriptType> typeResolver,
        Maybe<JvmType> declaringType
    ) {
        this.module = module;
        this.jvmConstructor = jvmConstructor;
        this.typeResolver = typeResolver;
        this.declaringType = declaringType;
    }


    @Override
    public String name() {
        return JvmModelBasedNamespace.CTOR_INTERNAL_NAME;
    }


    @Override
    public IJadescriptType returnType() {
        return typeResolver.apply(module.get(TypeHelper.class).typeRef(
            jvmConstructor.getDeclaringType()));
    }


    @Override
    public Map<String, IJadescriptType> parameterTypesByName() {
        return jvmConstructor.getParameters().stream()
            .collect(Collectors.toMap(
                JvmFormalParameter::getName,
                p -> typeResolver.apply(p.getParameterType())
            ));
    }


    @Override
    public List<String> parameterNames() {
        return jvmConstructor.getParameters().stream()
            .map(JvmFormalParameter::getName)
            .collect(Collectors.toList());
    }


    @Override
    public String compileInvokeByName(
        String dereferencePrefix_ignored,
        Map<String, String> compiledRexprs
    ) {
        List<String> argNames = new ArrayList<>();
        List<String> args = new ArrayList<>();
        compiledRexprs.forEach((name, arg) -> {
            argNames.add(name);
            args.add(arg);
        });
        return "new " + jvmConstructor.getQualifiedName('.') + "(" +
            String.join(
                ", ",
                CallSemantics.sortToMatchParamNames(
                    args,
                    argNames,
                    parameterNames()
                )
            ) + ")";
    }


    @Override
    public boolean isWithoutSideEffects() {
        return false;
    }


    @Override
    public List<IJadescriptType> parameterTypes() {
        return jvmConstructor.getParameters().stream()
            .map(p -> typeResolver.apply(p.getParameterType()))
            .collect(Collectors.toList());
    }


    @Override
    public String compileInvokeByArity(
        String dereferencePrefix_ignored,
        List<String> compiledRexprs
    ) {
        return "new " + jvmConstructor.getQualifiedName('.') + "(" +
            String.join(", ", compiledRexprs) +
            ")";
    }


    @Override
    public void debugDumpCallableSymbol(SourceCodeBuilder scb) {
        CallableSymbol.super.debugDumpCallableSymbol(scb);
        scb.indent()
            .line("--> (" + name() + " is also JvmConstructorSymbol)")
            .dedent();
    }


    @Override
    public boolean isStatic() {
        return true;
    }


    @Override
    public Maybe<JvmType> declaringType() {
        return declaringType;
    }

}
