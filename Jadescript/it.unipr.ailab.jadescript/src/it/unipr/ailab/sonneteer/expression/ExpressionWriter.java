package it.unipr.ailab.sonneteer.expression;

import it.unipr.ailab.sonneteer.Writer;
import it.unipr.ailab.sonneteer.comment.Commentable;
import it.unipr.ailab.sonneteer.statement.LocalVarBindingProvider;


public abstract class ExpressionWriter extends Commentable implements Writer {

    public abstract ExpressionWriter bindVariableUsages(
        LocalVarBindingProvider varBindingProvider
    );

}
