package it.unipr.ailab.sonneteer.file;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.Writer;
import it.unipr.ailab.sonneteer.comment.Commentable;

public class ImportWriter extends Commentable implements Writer {

    private final String qualifiedName;


    public ImportWriter(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }


    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        s.spaced("import").add(qualifiedName).line(";");
    }


    public String getQualifiedName() {
        return qualifiedName;
    }

}
