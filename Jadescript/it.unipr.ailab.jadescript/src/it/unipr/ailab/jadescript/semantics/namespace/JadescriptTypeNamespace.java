package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.GlobalFunctionOrProcedure;
import it.unipr.ailab.jadescript.semantics.context.symbol.Operation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmTypeNamespace;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.util.Strings;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.buildStream;

public abstract class JadescriptTypeNamespace extends TypeNamespace {


    public JadescriptTypeNamespace(SemanticsModule module) {
        super(module);
    }


    protected MemberCallable.Namespace callablesFromJvm(
        JvmTypeNamespace jvmTypeNamespace
    ) {
        return (name) -> {
            final TypeHelper typeHelper = module.get(TypeHelper.class);
            return jvmTypeNamespace.searchJvmOperation()
                .filter(jvmop -> jvmop.getSimpleName() != null
                    && !jvmop.getSimpleName().startsWith("_"))
                .filter(jvmop -> {
                    final EList<JvmFormalParameter> parameters =
                        jvmop.getParameters();

                    if (parameters.size() < 1) {
                        return false;
                    }

                    if (parameters.get(0) == null
                        || parameters.get(0).getParameterType() == null) {
                        return false;
                    }

                    final IJadescriptType firstParamType =
                        jvmTypeNamespace.resolveType(
                            parameters.get(0).getParameterType()
                        );
                    return typeHelper.ANYAGENTENV.isSupEqualTo(firstParamType);
                })
                .map((JvmOperation operation) -> Operation
                    .fromJvmOperation(module, jvmTypeNamespace, operation))
                .filter(o -> name == null || o.name().equals(name));
        };
    }


    protected GlobalCallable.Namespace staticCallablesFromJvm(
        JvmTypeNamespace jvmTypeNamespace
    ) {
        return (@Nullable String name) -> {
            final TypeHelper typeHelper = module.get(TypeHelper.class);
            return jvmTypeNamespace.searchJvmOperation()
                .filter(JvmOperation::isStatic)
                .filter(jvmop -> jvmop.getSimpleName() != null
                    && !jvmop.getSimpleName().startsWith("_"))
                .filter(jvmop -> {
                    final EList<JvmFormalParameter> parameters =
                        jvmop.getParameters();

                    if (parameters.size() < 1) {
                        return false;
                    }

                    if (parameters.get(0) == null
                        || parameters.get(0).getParameterType() == null) {
                        return false;
                    }

                    final IJadescriptType firstParamType =
                        jvmTypeNamespace.resolveType(
                            parameters.get(0).getParameterType()
                        );
                    return typeHelper.ANYAGENTENV.isSupEqualTo(firstParamType);
                }).map((JvmOperation operation) -> GlobalFunctionOrProcedure
                    .fromJvmStaticOperation(
                        module,
                        jvmTypeNamespace,
                        operation
                    )
                );
        };
    }


    protected MemberName.Namespace namesFromJvm(
        JvmTypeNamespace jvmTypeNamespace
    ) {
        return (searchedName) -> {
            return jvmTypeNamespace.searchJvmField()
                .flatMap(f -> {
                    if (f.getType() == null || f.getSimpleName() == null) {
                        return Stream.empty();
                    }

                    final IJadescriptType resolvedType =
                        jvmTypeNamespace.resolveType(f.getType());
                    String name = f.getSimpleName();

                    if(searchedName != null && !searchedName.equals(name)){
                        return Stream.empty();
                    }

                    boolean hasGetter = jvmTypeNamespace.searchJvmOperation()
                        .anyMatch(o -> o.getSimpleName().equals(
                            "get" + Strings.toFirstUpper(name))
                            && jvmTypeNamespace.resolveType(o.getReturnType())
                            .typeEquals(resolvedType)
                        );

                    if (!hasGetter) {
                        return Stream.empty();
                    }

                    boolean hasSetter = jvmTypeNamespace.searchJvmOperation()
                        .anyMatch(o -> {
                            boolean nameCheck = o.getSimpleName().equals(
                                "set" + Strings.toFirstUpper(name));
                            if (!nameCheck) {
                                return false;
                            }

                            if (o.getParameters() == null) {
                                return false;
                            }

                            if (o.getParameters().size() != 1) {
                                return false;
                            }

                            final JvmFormalParameter param =
                                o.getParameters().get(0);

                            if (param == null
                                || param.getParameterType() == null) {
                                return false;
                            }

                            return jvmTypeNamespace.resolveType(
                                param.getParameterType()
                            ).typeEquals(resolvedType);

                        });

                    return buildStream(
                        () -> new Property(
                            hasSetter,
                            name,
                            resolvedType,
                            jvmTypeNamespace.currentLocation(),
                            Property.compileWithJVMGetter(name),
                            Property.compileWithJVMSetter(name)
                        )
                    );
                });
        };
    }


}
