package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.semantics.Semantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.effectanalysis.EffectfulOperationSemantics;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import it.unipr.ailab.sonneteer.statement.StatementWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 26/04/18.
 */
@Singleton
public abstract class StatementSemantics<T>
        extends Semantics<T>
        implements EffectfulOperationSemantics {


    public StatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    /**
     * Compiles the statement into {@link BlockWriterElement} instances, which are used to generate the actual Java
     * code.
     *
     * @param input    the input statement
     * @param acceptor acceptor used to collect the resulting {@link BlockWriterElement}s
     */
    public abstract void compileStatement(
            Maybe<T> input,
            StatementCompilationOutputAcceptor acceptor
    );

    public abstract List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<T> input);

}
