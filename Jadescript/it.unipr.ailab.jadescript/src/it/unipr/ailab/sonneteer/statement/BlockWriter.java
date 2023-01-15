package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class BlockWriter extends StatementWriter {

    private final List<BlockWriterElement> elements = new ArrayList<>();
    private LocalVarBindingProvider bindingProvider =
        CompilationHelper.userBlockLocalVars;


    private static StatementWriter performBinding(
        LocalVarBindingProvider localVarBindingProvider,
        BlockWriterElement element
    ) {
        if (element instanceof StatementWriter) {
            return ((StatementWriter) element)
                .bindLocalVarUsages(localVarBindingProvider);
        } else if (element instanceof VariableDeclarationPlaceholder) {
            return localVarBindingProvider.bindDeclaration(
                ((VariableDeclarationPlaceholder) element).getChosenType(),
                ((VariableDeclarationPlaceholder) element).getChosenName(),
                ((VariableDeclarationPlaceholder) element)
                    .getExpressionWriter().bindVariableUsages(
                        localVarBindingProvider)
            );
        } else if (element instanceof VariableAssignmentPlaceholder) {
            return localVarBindingProvider.bindWrite(
                ((VariableAssignmentPlaceholder) element).getVarName(),
                ((VariableAssignmentPlaceholder) element).getExpression()
                    .bindVariableUsages(localVarBindingProvider)
            );
        }
        throw new RuntimeException("Invalid BlockWriterElement found: " +
            element.getClass().getName());
    }


    public BlockWriter addStatement(StatementWriter statement) {
        elements.add(statement);
        return this;
    }

    public BlockWriter inside(Consumer<SourceCodeBuilder> useSCB){
        elements.add(new StatementWriter() {
            @Override
            public StatementWriter bindLocalVarUsages(LocalVarBindingProvider bindingProvider) {
                return this;
            }


            @Override
            public void writeSonnet(SourceCodeBuilder s) {
                useSCB.accept(s);
            }
        });
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
                StatementWriter st = performBinding(
                    bindingProvider,
                    elements.get(i)
                );
                st.writeSonnet(s);
                if (i != elements.size() - 1) s.line();
            }
        }
        s.dedent().line("}");
    }


    public List<BlockWriterElement> getBlockElements() {
        return elements;
    }


    public void replaceElements(
        UnaryOperator<BlockWriterElement> replace,
        Predicate<StatementWriter> shouldDig
    ) {
        elements.replaceAll(replace);

        for (BlockWriterElement element : elements) {
            if (element instanceof StatementWriter) {
                final StatementWriter statement = (StatementWriter) element;
                if (shouldDig.test(statement)) {
                    statement.getSubBlocks(bw -> bw.replaceElements(
                        replace,
                        shouldDig
                    ));
                }
            }
        }
    }


    public BlockWriter bindLocalVarUsages(LocalVarBindingProvider bindingProvider) {
        BlockWriter newBlock = w.block();
        for (BlockWriterElement element : elements) {
            final StatementWriter sw = performBinding(bindingProvider, element);
            if (sw instanceof BlockWriter) {
                ((BlockWriter) sw).setBindingProvider(bindingProvider);
            }
            newBlock.add(sw);
        }
        return newBlock;
    }


    public BlockWriter addAll(List<BlockWriterElement> blockElements) {
        this.elements.addAll(blockElements);
        return this;
    }


    public LocalVarBindingProvider getBindingProvider() {
        return bindingProvider;
    }


    public void setBindingProvider(LocalVarBindingProvider bindingProvider) {
        this.bindingProvider = bindingProvider;
    }

}
