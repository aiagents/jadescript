package it.unipr.ailab.sonneteer;

public interface Writer {

    WriterFactory w = WriterFactory.getInstance();

    void writeSonnet(SourceCodeBuilder s);

}
