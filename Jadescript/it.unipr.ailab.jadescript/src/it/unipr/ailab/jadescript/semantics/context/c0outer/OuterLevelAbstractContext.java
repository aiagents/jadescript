package it.unipr.ailab.jadescript.semantics.context.c0outer;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.Context;
import it.unipr.ailab.jadescript.semantics.context.search.ModuleGlobalLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.BehaviourConstructor;
import it.unipr.ailab.jadescript.semantics.context.symbol.GlobalFunctionOrProcedure;
import it.unipr.ailab.jadescript.semantics.context.symbol.OntologyNamedReference;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalName;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import jadescript.lang.JadescriptGlobalFunction;
import jadescript.lang.JadescriptGlobalProcedure;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;

import java.util.stream.Stream;

/**
 * Base class for File and Module contexts.
 */
public abstract class OuterLevelAbstractContext extends Context
    implements RawTypeReferenceSolverContext {

    public OuterLevelAbstractContext(SemanticsModule module) {
        super(module);
    }


    protected Stream<? extends GlobalName>
    getNamedReferencesFromDeclaredType(
        JvmTypeReference jvmClassRef,
        JvmDeclaredType type
    ) {
        final TypeSolver typeSolver = module.get(TypeSolver.class);
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        IJadescriptType jadescriptType = typeSolver
            .fromJvmTypeReference(jvmClassRef);
        Stream<? extends GlobalName> result;
        if (jvm.isAssignable(
            jade.content.onto.Ontology.class,
            jvmClassRef
        )) {
            String name = jvmClassRef.getSimpleName();

            result = Stream.of((GlobalName) new OntologyNamedReference(
                jadescriptType,
                name
            ));
        } else {
            result = Stream.of();
        }
        return result;
    }


    protected Stream<? extends GlobalCallable>
    getCallableStreamFromDeclaredType(
        JvmTypeReference methodJVMClassRef,
        JvmDeclaredType type
    ) {
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        Stream<? extends GlobalCallable> result;
        if (jvm.isAssignable(
            jade.core.behaviours.Behaviour.class,
            methodJVMClassRef
        )) {
            JvmTypeNamespace namespace =
                JvmTypeNamespace.resolved(module, type);

            result = searchBehaviourCtorFunction(type, namespace);
        } else if (jvm.isAssignable(
            JadescriptGlobalFunction.class,
            methodJVMClassRef
        ) || jvm.isAssignable(
            JadescriptGlobalProcedure.class,
            methodJVMClassRef
        )) {

            JvmTypeNamespace namespace =
                JvmTypeNamespace.resolved(module, type);
            result = namespace.searchJvmOperation()
                .filter(jvmop -> jvmop.getVisibility() == JvmVisibility.PUBLIC)
                .filter(JvmOperation::isStatic)
                .map(jvmop -> GlobalFunctionOrProcedure.fromJvmStaticOperation(
                        module,
                        namespace,
                        jvmop,
                        new ModuleGlobalLocation(
                            type.getPackageName() != null ?
                                type.getPackageName() : ""
                        )
                    )
                );
        } else {
            result = Stream.empty();
        }

        return result;
    }


    protected Stream<? extends GlobalName> getGlobalNamedCellsFromFQName(
        String fqName
    ) {
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        JvmTypeReference jvmClassRef = jvm.typeRef(fqName);
        if (jvmClassRef.getType() instanceof JvmDeclaredType) {
            final JvmDeclaredType type =
                (JvmDeclaredType) jvmClassRef.getType();
            return getNamedReferencesFromDeclaredType(jvmClassRef, type);
        }
        return Stream.empty();
    }


    protected Stream<? extends GlobalCallable> getGlobalCallablesFromFQName(
        String fqName
    ) {
        JvmTypeReference methodJVMClassRef =
            module.get(JvmTypeHelper.class).typeRef(fqName);
        if (methodJVMClassRef.getType() instanceof JvmDeclaredType) {
            final JvmDeclaredType type =
                (JvmDeclaredType) methodJVMClassRef.getType();
            return getCallableStreamFromDeclaredType(methodJVMClassRef, type);
        }
        return Stream.empty();
    }


    private Stream<? extends GlobalCallable> searchBehaviourCtorFunction(
        JvmDeclaredType type,
        JvmTypeNamespace namespace
    ) {
        return namespace.searchJvmConstructor()
            .map(ctor -> BehaviourConstructor.fromJvmConstructor(
                module,
                namespace,
                ctor,
                type
            ));
    }

}
