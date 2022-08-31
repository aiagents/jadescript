package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.Writer;
import it.unipr.ailab.sonneteer.comment.Commentable;

import java.util.function.Consumer;

public abstract class StatementWriter
        extends Commentable
        implements Writer, BlockWriterElement {

    /**
     * Returns (via the acceptor) all the sub-blocks directly connected to
     * this statement (e.g., the body of a while loop).
     * Bodies of class literals/lambdas/anonymous classes should not be considered.
     */
    public void getSubBlocks(Consumer<BlockWriter> statementAcceptor){
        // do nothing
    }

    public abstract StatementWriter bindLocalVarUsages(LocalVarBindingProvider bindingProvider);
}
