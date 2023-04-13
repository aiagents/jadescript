package it.unipr.ailab.jadescript.semantics.context.c0outer;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalName;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xtype.XImportDeclaration;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;
import java.util.stream.Stream;


public class FileContext
    extends OuterLevelAbstractContext
    implements GlobalCallable.Namespace, GlobalName.Namespace {


    private final String fileName;
    private final String fileURI;
    private final ModuleContext outer;

    private final MaybeList<XImportDeclaration> importDeclarations;


    public FileContext(
        SemanticsModule module,
        ModuleContext outer,
        String fileName,
        String fileURI,
        MaybeList<XImportDeclaration> importDeclarations
    ) {
        super(module);
        this.outer = outer;
        this.fileName = fileName;
        this.fileURI = fileURI;
        this.importDeclarations = importDeclarations;
    }


    @Override
    public Maybe<? extends Searcheable> superSearcheable() {
        return Maybe.some(outer);
    }


    public ModuleContext getOuterContextModule() {
        return outer;
    }


    @Override
    public Stream<? extends GlobalCallable> globalCallables(
        @Nullable String name
    ) {
        return getImportedJvmTypeDeclarations()
            .filter(i -> name == null || name.equals(i.getSimpleName()))
            .flatMap(imported -> getCallableStreamFromDeclaredType(
                module.get(JvmTypeHelper.class).typeRef(imported),
                imported
            ));
    }


    @Override
    public Stream<? extends GlobalName> globalNames(
        @Nullable String name
    ) {
        return getImportedJvmTypeDeclarations()
            .flatMap(imported -> getNamedReferencesFromDeclaredType(
                module.get(JvmTypeHelper.class).typeRef(imported),
                imported
            ))
            .filter(n -> name == null || n.name().equals(name));
    }


    public Stream<JvmDeclaredType> getImportedJvmTypeDeclarations() {
        return getImportDeclarations().stream()
            .filter(j -> j.__(id -> !id.isWildcard()
                && !id.isStatic()).orElse(false))
            .filter(Maybe::isPresent)
            .map(Maybe::toNullable)
            .flatMap(it -> it.getImportedType() != null
                ? Stream.of(it.getImportedType())
                : Stream.empty()
            );
    }


    public MaybeList<XImportDeclaration> getImportDeclarations() {
        return importDeclarations;
    }


    public String getFileName() {
        return fileName;
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        scb.open("--> is FileContext {");
        scb.line("fileName = " + getFileName());
        scb.line("fileURI = " + getFileURI());
        scb.open("importedJvmTypeDeclarations = [");
        getImportedJvmTypeDeclarations().forEach(gfopDecl ->
            scb.line(gfopDecl.getQualifiedName('.'))
        );
        scb.close("]");
        scb.close("}");
    }


    @Override
    public String getCurrentOperationLogName() {
        return "<executing code in " + fileName + ">";
    }


    public String getFileURI() {
        return fileURI;
    }


    @Override
    public Stream<JvmTypeReference> rawResolveTypeReference(
        String typeRefIdentifier
    ) {
        //it firstly tries to solve it by finding in import declarations
        return getImportDeclarations().stream()
            .filter(Maybe::isPresent)
            .map(Maybe::toNullable)
            .filter(xi -> !xi.isWildcard() && !xi.isExtension())
            .filter(xi -> {
                String importedName = xi.getImportedName().trim();
                String[] splits = importedName.split(Pattern.quote("."));
                if (splits.length == 0) {
                    return false;
                }
                return splits[splits.length - 1].equals(typeRefIdentifier);
            })
            .map(XImportDeclaration::getImportedName)
            .map(module.get(JvmTypeHelper.class)::typeRef);
    }

}
