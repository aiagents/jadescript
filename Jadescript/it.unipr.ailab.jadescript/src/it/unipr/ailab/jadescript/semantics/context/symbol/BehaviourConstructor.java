package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.CallSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalCallable;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agentenv.AgentEnvType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import org.eclipse.xtext.common.types.JvmConstructor;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;

public class BehaviourConstructor implements GlobalCallable {


    private final IJadescriptType returnType;
    private final String name;
    private final Map<String, IJadescriptType> parameterNamesToTypes;
    private final List<String> parameterNames;
    private final SearchLocation location;
    private final boolean withoutSideEffects;
    private final String fullyQualifiedName;


    private BehaviourConstructor(
        IJadescriptType returnType,
        String name,
        Map<String, IJadescriptType> parameterNamesToTypes,
        List<String> parameterNames,
        SearchLocation location,
        boolean withoutSideEffects,
        String fullyQualifiedName
    ) {
        this.returnType = returnType;
        this.name = name;
        this.parameterNamesToTypes = parameterNamesToTypes;
        this.parameterNames = parameterNames;
        this.location = location;
        this.withoutSideEffects = withoutSideEffects;
        this.fullyQualifiedName = fullyQualifiedName;
    }


    public static BehaviourConstructor fromJvmConstructor(
        SemanticsModule module,
        JvmTypeNamespace namespace,
        JvmConstructor constructor,
        JvmDeclaredType type
    ) {
        List<JvmFormalParameter> parameters = constructor.getParameters();
        if (parameters == null) {
            parameters = List.of();
        }

        List<String> paramNames = new ArrayList<>();
        Map<String, IJadescriptType> paramNamesToTypes = new HashMap<>();

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);

        final IJadescriptType anyAE = builtins.anyAgentEnv();

        boolean withoutSideEffects = false;

        for (JvmFormalParameter parameter : parameters) {
            if (parameter == null) {
                continue;
            }

            final String paramName = parameter.getName();
            final JvmTypeReference paramTypeRef =
                parameter.getParameterType();
            if (paramName == null || paramTypeRef == null) {
                continue;
            }

            if (paramName.equals(SemanticsConsts.AGENT_ENV)) {
                final IJadescriptType envType =
                    namespace.resolveType(paramTypeRef)
                        .ignoreBound();

                if (envType instanceof AgentEnvType) {
                    withoutSideEffects =
                        ((AgentEnvType) envType).isWithoutSideEffects();
                }

                continue;
            }

            final IJadescriptType solvedType =
                namespace.resolveType(paramTypeRef)
                    .ignoreBound();

            if (comparator.compare(anyAE, solvedType).is(superTypeOrEqual())) {
                continue;
            }

            paramNames.add(paramName);
            paramNamesToTypes.put(paramName, solvedType);
        }


        String fqn = constructor.getQualifiedName('.');

        return new BehaviourConstructor(
            namespace.resolveType(jvm.typeRef(type)).ignoreBound(),
            type.getSimpleName(),
            paramNamesToTypes,
            paramNames,
            namespace.currentLocation(),
            withoutSideEffects,
            fqn
        );
    }


    @Override
    public String name() {
        return name;
    }


    @Override
    public IJadescriptType returnType() {
        return returnType;
    }


    @Override
    public Map<String, IJadescriptType> parameterTypesByName() {
        return parameterNamesToTypes;
    }


    @Override
    public List<String> parameterNames() {
        return parameterNames;
    }


    @Override
    public List<IJadescriptType> parameterTypes() {
        final Map<String, IJadescriptType> map = parameterTypesByName();
        return parameterNames.stream()
            .filter(map::containsKey)
            .map(map::get)
            .collect(Collectors.toList());
    }


    @Override
    public boolean isWithoutSideEffects() {
        return withoutSideEffects;
    }


    @Override
    public String compileInvokeByArity(
        List<String> compiledRexprs,
        BlockElementAcceptor acceptor
    ) {
        return CompilationHelper.addEnvParameterByArity(
            (args) -> "new " + this.fullyQualifiedName + "(" +
                String.join(" ,", args) +
                ")"
        ).apply(compiledRexprs);
    }


    @Override
    public String compileInvokeByName(
        Map<String, String> compiledRexprs,
        BlockElementAcceptor acceptor
    ) {
        return CompilationHelper.addEnvParameterByName(
            (args) -> "new " + this.fullyQualifiedName + "(" +
                String.join(
                    " ,",
                    CallSemantics.sortToMatchParamNames(args, parameterNames)
                ) + ")"
        ).apply(compiledRexprs);
    }


    @Override
    public SearchLocation sourceLocation() {
        return location;
    }

}
