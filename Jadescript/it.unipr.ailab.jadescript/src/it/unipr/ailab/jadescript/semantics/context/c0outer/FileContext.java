package it.unipr.ailab.jadescript.semantics.context.c0outer;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xtype.XImportDeclaration;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

public class FileContext
        extends OuterLevelAbstractContext
        implements CallableSymbol.Searcher{


    private final String fileName;
    private final String fileURI;
    private final ModuleContext outer;

    private final List<Maybe<XImportDeclaration>> importDeclarations;

    public FileContext(
            SemanticsModule module,
            ModuleContext outer,
            String fileName,
            String fileURI,
            List<Maybe<XImportDeclaration>> importDeclarations
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

    public ModuleContext getOuterContextModule(){
        return outer;
    }

    @Override
    public Stream<? extends CallableSymbol> searchCallable(
            String name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>>
                parameterTypes
    ) {
        return getImportedJvmTypeDeclarations()
                .flatMap(imported -> getCallableStreamFromDeclaredType(
                        module.get(TypeHelper.class).typeRef(imported),
                        imported,
                        name,
                        returnType,
                        parameterNames,
                        parameterTypes
                ));
    }

    public Stream<JvmDeclaredType> getImportedJvmTypeDeclarations() {
        return importDeclarations.stream()
                .filter(j -> j.__(id -> !id.isWildcard()
                    && !id.isStatic()).extract(nullAsFalse))
                .filter(Maybe::isPresent)
                .map(Maybe::toNullable)
                .flatMap(it -> it.getImportedType() != null
                    ? Stream.of(it.getImportedType())
                    : Stream.empty()
                );
    }

    @Override
    public Stream<? extends CallableSymbol> searchCallable(
            Predicate<String> name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>>
                parameterTypes
    ) {
        return getImportedJvmTypeDeclarations()
                .flatMap(imported -> getCallableStreamFromDeclaredType(
                        module.get(TypeHelper.class).typeRef(imported),
                        imported,
                        name,
                        returnType,
                        parameterNames,
                        parameterTypes
                ));
    }

    public List<Maybe<XImportDeclaration>> getImportDeclarations() {
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
        return "<executing "+fileName+">";
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
                    if(splits.length == 0){
                        return false;
                    }
                    return splits[splits.length - 1].equals(typeRefIdentifier);
                })
                .map(XImportDeclaration::getImportedName)
                .map(module.get(TypeHelper.class)::typeRef);
    }
}
