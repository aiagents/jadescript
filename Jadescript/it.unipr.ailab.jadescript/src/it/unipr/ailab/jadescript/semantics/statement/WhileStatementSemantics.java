package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.OptionalBlock;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.WhileStatement;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ScopeType;
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
        final Maybe<RValueExpression> condition =
            input.__(WhileStatement::getCondition);
        final Maybe<OptionalBlock> whileBody
            = input.__(WhileStatement::getWhileBody);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final BlockSemantics blockSemantics = module.get(BlockSemantics.class);

        final String compiledCondition =
            rves.compile(condition, state, acceptor);

        final StaticState afterCondition =
            rves.advance(condition, state);


        StaticState inLoopBlock =
            rves.assertReturnedTrue(
                condition,
                afterCondition
            );

        inLoopBlock = inLoopBlock.enterLoopScope();

        final PSR<BlockWriter> blockPSR =
            blockSemantics.compileOptionalBlock(
                whileBody,
                inLoopBlock
            );

        final BlockWriter blockCompiled = blockPSR.result();
        final StaticState endOfBlock = blockPSR.state();

        acceptor.accept(w.whileStmnt(
            w.expr(compiledCondition),
            blockCompiled
        ));

        final StaticState afterBlock = endOfBlock.exitScope();

        final StaticState afterWhile = afterCondition.intersect(afterBlock);


        return rves.assertReturnedFalse(
            condition,
            afterWhile
        );
    }


    @Override
    public StaticState validateStatement(
        Maybe<WhileStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final Maybe<RValueExpression> condition =
            input.__(WhileStatement::getCondition);
        final Maybe<OptionalBlock> whileBody
            = input.__(WhileStatement::getWhileBody);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final BlockSemantics blockSemantics = module.get(BlockSemantics.class);


        boolean conditionCheck = rves.validate(condition, state, acceptor);

        StaticState inLoopBlock;
        StaticState afterCondition;

        if (conditionCheck == VALID) {
            afterCondition = rves.advance(condition, state);

            inLoopBlock = rves.assertReturnedTrue(
                condition,
                afterCondition
            );
        } else {
            inLoopBlock = afterCondition = state;
        }

        inLoopBlock = inLoopBlock.enterLoopScope();

        StaticState endOfBlock =
            blockSemantics.validateOptionalBlock(
                whileBody,
                inLoopBlock,
                acceptor
            );

        StaticState afterBlock = endOfBlock.exitScope();

        final StaticState afterWhile = afterCondition.intersect(afterBlock);

        return rves.assertReturnedFalse(
            condition,
            afterWhile
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
