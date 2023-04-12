package it.unipr.ailab.jadescript.semantics.context.c0outer;

import it.unipr.ailab.jadescript.jadescript.Model;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalName;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;


public class ModuleContext
    extends OuterLevelAbstractContext
    implements GlobalCallable.Namespace, GlobalName.Namespace {

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
    public Stream<? extends GlobalCallable> globalCallables(
        @Nullable String name
    ) {
        if(name == null){
            return Stream.empty();
        }
        final String fqName = getModuleNameAsPrefix() + name;
        return getGlobalCallablesFromFQName(fqName);
    }


    @Override
    public Stream<? extends GlobalName> globalNames(
        @Nullable String name
    ) {
        if(name == null){
            return Stream.empty();
        }
        final String fqName = getModuleNameAsPrefix() + name;
        return getGlobalNamedCellsFromFQName(fqName);
    }



    @NotNull
    public String getModuleNameAsPrefix() {
        return getModuleName().isEmpty() ? "" : (getModuleName() + ".");
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
        return "<module declarations>";
    }


    @Override
    public Stream<JvmTypeReference> rawResolveTypeReference(
        String typeRefIdentifier
    ) {

        return Stream.of(module.get(JvmTypeHelper.class).typeRef(
            getModuleNameAsPrefix() + typeRefIdentifier
        ));
    }

}
