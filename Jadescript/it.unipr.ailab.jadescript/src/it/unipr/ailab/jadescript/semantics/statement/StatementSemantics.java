package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.semantics.Semantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.stream.Stream;

/**
 * Created on 26/04/18.
 */
@Singleton
public abstract class StatementSemantics<T>
    extends Semantics {


    public StatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    /**
     * Compiles the statement into {@link BlockWriterElement} instances,
     * which are used to generate the actual Java
     * code.
     *
     * @param input    the input statement
     * @param state    the static-state before evaluating the statement
     * @param acceptor acceptor used to collect the resulting
     *                 {@link BlockWriterElement}s
     * @return the static-state after the statement evaluation
     */
    public abstract StaticState compileStatement(
        Maybe<T> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    );


    public abstract Stream<ExpressionSemantics.SemanticsBoundToExpression<?>>
    includedExpressions(Maybe<T> input);


    public abstract StaticState validateStatement(
        Maybe<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    );

}
