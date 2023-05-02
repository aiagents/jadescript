package it.unipr.ailab.sonneteer.file;

import java.util.ArrayList;
import java.util.List;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.Writer;
import it.unipr.ailab.sonneteer.comment.Commentable;
import it.unipr.ailab.sonneteer.type.ITypeDeclarationWriter;

/**
 * Writer for Java source files
 */
public class FileWriter extends Commentable implements Writer {
    private final String fileName;
    private final String packageName;
    private final List<ImportWriter> importWriters = new ArrayList<>();
    private final ITypeDeclarationWriter typeDeclarationPoet;

    public FileWriter(
        String fileName,
        String packageName,
        ITypeDeclarationWriter typeDeclarationPoet
    ) {
        this.fileName = fileName;
        this.packageName = packageName;
        this.typeDeclarationPoet = typeDeclarationPoet;
    }

    public FileWriter addImport(ImportWriter importd){
        importWriters.add(importd);
        return this;
    }

    public FileWriter addImport(String importd){
        importWriters.add(new ImportWriter(importd));
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPackageName() {
        return packageName;
    }

    public ITypeDeclarationWriter getTypeDeclarationPoet() {
        return typeDeclarationPoet;
    }



    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        s.line();
        s.spaced("package").add(packageName).line(";");
        s.line();
        importWriters.forEach(it -> it.writeSonnet(s));
        s.line();
        typeDeclarationPoet.writeSonnet(s);
    }
}
