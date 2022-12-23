package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.semantics.Semantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.effectanalysis.EffectfulOperationSemantics;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;

/**
 * Created on 26/04/18.
 */
@Singleton
public abstract class StatementSemantics<T>
        extends Semantics
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
            CompilationOutputAcceptor acceptor
    );

    public abstract List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<T> input);

    public abstract void validate(Maybe<T> input, ValidationMessageAcceptor acceptor);

}
