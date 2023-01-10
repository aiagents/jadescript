package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.WhileStatement;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.staticstate.EvaluationResult;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.PSR;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.stream.Stream;

/**
 * Created on 26/04/18.
 */
@Singleton
public class WhileStatementSemantics
    extends StatementSemantics<WhileStatement> {


    public WhileStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<WhileStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final Maybe<RValueExpression> condition =
            input.__(WhileStatement::getCondition);
        final String compiledCondition =
            rves.compile(condition, state, acceptor);

        final StaticState afterCondition =
            rves.advance(condition, state);


        final Maybe<ExpressionDescriptor> conditionDescriptor =
            rves.describeExpression(condition, state);

        final StaticState inLoopBlock =
            afterCondition.assertEvaluation(
                conditionDescriptor,
                EvaluationResult.ReturnedTrue.INSTANCE
            );


        final PSR<BlockWriter> blockPSR =
            module.get(BlockSemantics.class).compileOptionalBlock(
                input.__(WhileStatement::getWhileBody),
                inLoopBlock
            );

        final BlockWriter blockCompiled = blockPSR.result();
        final StaticState afterBlock = blockPSR.state();

        acceptor.accept(w.whileStmnt(
            w.expr(compiledCondition),
            blockCompiled
        ));

        return afterCondition.intersect(afterBlock)
            .assertEvaluation(
                conditionDescriptor,
                EvaluationResult.ReturnedFalse.INSTANCE
            );
    }


    @Override
    public StaticState validateStatement(
        Maybe<WhileStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final Maybe<RValueExpression> condition =
            input.__(WhileStatement::getCondition);
        boolean conditionCheck = rves.validate(condition, state, acceptor);

        StaticState inLoopBlock;
        StaticState afterCondition;
        final Maybe<ExpressionDescriptor> conditionDescriptor =
            rves.describeExpression(
                condition,
                state
            );

        if (conditionCheck == VALID) {
            afterCondition = rves.advance(condition, state);
            inLoopBlock = afterCondition.assertEvaluation(
                conditionDescriptor,
                EvaluationResult.ReturnedTrue.INSTANCE
            );
        } else {
            inLoopBlock = afterCondition = state;
        }


        final StaticState afterBlock = module.get(BlockSemantics.class)
            .validateOptionalBlock(
                input.__(WhileStatement::getWhileBody),
                inLoopBlock,
                acceptor
            );

        return afterCondition.intersect(afterBlock)
            .assertEvaluation(
                conditionDescriptor,
                EvaluationResult.ReturnedFalse.INSTANCE
            );
    }


    @Override
    public Stream<SemanticsBoundToExpression<?>>
    includedExpressions(Maybe<WhileStatement> input) {
        return Util.buildStream(() -> new SemanticsBoundToExpression<>(
            module.get(RValueExpressionSemantics.class),
            input.__(WhileStatement::getCondition)
        ));
    }


}
