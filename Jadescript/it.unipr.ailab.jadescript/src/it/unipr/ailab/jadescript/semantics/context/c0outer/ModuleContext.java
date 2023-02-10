package it.unipr.ailab.jadescript.semantics.context.c0outer;

import it.unipr.ailab.jadescript.jadescript.Model;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.CallableMember;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;


public class ModuleContext
        extends OuterLevelAbstractContext
        implements CallableMember.Namespace {
    private final String moduleName;

    private final Maybe<Model> sourceModule;

    public ModuleContext(
        SemanticsModule module,
        String moduleName,
        Maybe<Model> sourceModule
    ) {
        super(module);
        this.moduleName = moduleName;
        this.sourceModule = sourceModule;
    }

    public String getModuleName() {
        return moduleName;
    }


    @Override
    public Maybe<? extends Searcheable> superSearcheable() {
        return Maybe.nothing();
    }


    @Override
    public Stream<? extends CallableMember> searchCallable(
            String name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>>
                parameterTypes
    ) {
        final String fqName = getModuleNameAsPrefix() + name;
        return getCallableStreamFromFQName(
            fqName,
            name,
            returnType,
            parameterNames,
            parameterTypes
        );
    }

    @NotNull
    public String getModuleNameAsPrefix() {
        return getModuleName().isEmpty() ? "" : (getModuleName() + ".");
    }


    @Override
    public Stream<? extends CallableMember> searchCallable(
            Predicate<String> name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>>
                parameterTypes
    ) {
        return Stream.empty();
    }

    public Maybe<Model> getSourceModule() {
        return sourceModule;
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        scb.open("--> is ModuleContext {");
        scb.line("moduleName = " + moduleName);
        scb.close("}");
    }

    @Override
    public String getCurrentOperationLogName() {
        return null;
    }


    @Override
    public Stream<JvmTypeReference> rawResolveTypeReference(
        String typeRefIdentifier
    ) {

        return Stream.of(module.get(TypeHelper.class).typeRef(
                getModuleNameAsPrefix() + typeRefIdentifier
        ));
    }

}
