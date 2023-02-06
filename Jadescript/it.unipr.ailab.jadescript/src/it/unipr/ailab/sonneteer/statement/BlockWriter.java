package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class BlockWriter extends StatementWriter {

    private final List<BlockWriterElement> elements = new ArrayList<>();


    public BlockWriter inside(Consumer<SourceCodeBuilder> useSCB){
        elements.add(useSCB::accept);
        return this;
    }


    public BlockWriter addStatement(StatementWriter statement) {
        elements.add(statement);
        return this;
    }


    public BlockWriter addStatement(
        int index,
        StatementWriter statementWriter
    ) {
        elements.add(index, statementWriter);
        return this;
    }


    public BlockWriter addStatements(StatementWriter... statementWriters) {
        return addStatements(Arrays.asList(statementWriters));
    }


    public BlockWriter addStatements(
        List<StatementWriter> statementWriterList
    ) {
        elements.addAll(statementWriterList);
        return this;
    }


    public BlockWriter add(BlockWriterElement element) {
        elements.add(element);
        return this;
    }


    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        s.line("{").indent();
        {
            for (int i = 0; i < elements.size(); i++) {
                elements.get(i).writeSonnet(s);
                if (i != elements.size() - 1) s.line();
            }
        }
        s.dedent().line("}");
    }


    public List<BlockWriterElement> getBlockElements() {
        return elements;
    }


    public BlockWriter addAll(List<BlockWriterElement> blockElements) {
        this.elements.addAll(blockElements);
        return this;
    }

}
