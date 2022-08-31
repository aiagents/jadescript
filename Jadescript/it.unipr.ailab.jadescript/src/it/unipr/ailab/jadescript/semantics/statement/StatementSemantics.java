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
 *
 */
@Singleton
public abstract class StatementSemantics<T>
        extends Semantics<T>
        implements EffectfulOperationSemantics {


    public StatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    public abstract List<BlockWriterElement> compileStatement(Maybe<T> input);


    @SuppressWarnings({ "rawtypes", "unchecked" })
	public List<StatementWriter> generateAuxiliaryStatements(Maybe<T> input) {
        List<StatementWriter> result = new ArrayList<>();
        for (ExpressionSemantics.SemanticsBoundToExpression<?> includedExpression : includedExpressions(input)) {
            result.addAll(includedExpression.getSemantics()
                    .generateAuxiliaryStatements((Maybe)includedExpression.getInput()));

        }
        return result;
    }

    public abstract List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<T> input);

}
