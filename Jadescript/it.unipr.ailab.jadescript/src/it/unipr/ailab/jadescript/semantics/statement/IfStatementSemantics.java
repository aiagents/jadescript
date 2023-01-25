package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.IfStatement;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.OptionalBlock;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.PSR;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.controlflow.IfStatementWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;
import static it.unipr.ailab.maybe.Maybe.toListOfMaybes;

/**
 * Created on 26/04/18.
 */
@Singleton
public class IfStatementSemantics extends StatementSemantics<IfStatement> {


    public IfStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<IfStatement> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        Maybe<RValueExpression> condition = input.__(IfStatement::getCondition);
        Maybe<OptionalBlock> thenBranch = input.__(IfStatement::getThenBranch);

        List<Maybe<RValueExpression>> elseIfConditions = toListOfMaybes(
            input.__(IfStatement::getElseIfConditions)
        );
        List<Maybe<OptionalBlock>> elseIfBranches = toListOfMaybes(
            input.__(IfStatement::getElseIfBranches)
        );

        Maybe<OptionalBlock> elseBranch = input.__(IfStatement::getElseBranch);


        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final BlockSemantics blockSemantics = module.get(BlockSemantics.class);

        StaticState runningState = state;

        String conditionCompiled = rves.compile(
            condition,
            runningState,
            acceptor
        );
        List<Maybe<RValueExpression>> prevConditions = new ArrayList<>();

        prevConditions.add(condition);
        runningState = rves.advance(condition, runningState);

        StaticState inThenBranch = rves.assertReturnedTrue(
            condition,
            runningState
        );

        //Enter scope
        inThenBranch = inThenBranch.enterScope();

        PSR<BlockWriter> thenBranchPSR = blockSemantics.compileOptionalBlock(
            thenBranch,
            inThenBranch
        );


        BlockWriter thenBranchCompiled = thenBranchPSR.result();
        IfStatementWriter ifsp = w.ifStmnt(
            w.expr(conditionCompiled),
            thenBranchCompiled
        );

        StaticState endOfThenBranch = thenBranchPSR.state();

        StaticState afterThenBranch = endOfThenBranch.exitScope();

        List<StaticState> afterBranches = new ArrayList<>();

        afterBranches.add(afterThenBranch);

        final int assumedSize = Math.min(
            elseIfBranches.size(),
            elseIfConditions.size()
        );
        for (int i = 0; i < assumedSize; ++i) {

            final Maybe<RValueExpression> elseIfCond = elseIfConditions.get(i);
            String elseIfCondCompiled = rves.compile(
                elseIfCond,
                runningState,
                acceptor
            );

            runningState = rves.advance(elseIfCond, runningState);

            StaticState inElseIfBranch = runningState;


            for (Maybe<RValueExpression> prevCondition : prevConditions) {
                inElseIfBranch = rves.assertReturnedFalse(
                    prevCondition,
                    inElseIfBranch
                );
            }

            inElseIfBranch = rves.assertReturnedTrue(
                elseIfCond,
                inElseIfBranch
            );

            inElseIfBranch = inElseIfBranch.enterScope();

            PSR<BlockWriter> elseIfBranchPSR =
                blockSemantics.compileOptionalBlock(
                    elseIfBranches.get(i),
                    inElseIfBranch
                );

            final BlockWriter branchCompiled = elseIfBranchPSR.result();


            final StaticState endOfElseIfBranch = elseIfBranchPSR.state();

            final StaticState afterElseIfBranch = endOfElseIfBranch.exitScope();

            ifsp.addElseIfBranch(w.expr(elseIfCondCompiled), branchCompiled);

            prevConditions.add(elseIfCond);

            afterBranches.add(afterElseIfBranch);
        }


        final StaticState beforeTheBranches = runningState;

        // Creates the scope of the else branch regardless of its definition
        // (in case there is no 'else' branch, regarding state, acts as if
        // there is an empty else branch).
        StaticState inElseBranch = runningState;

        for (Maybe<RValueExpression> prevCondition : prevConditions) {
            inElseBranch = rves.assertReturnedFalse(
                prevCondition,
                inElseBranch
            );
        }


        if (input.__(IfStatement::isWithElseBranch).extract(nullAsFalse)) {
            inElseBranch = inElseBranch.enterScope();

            PSR<BlockWriter> elseBranchPSR =
                blockSemantics.compileOptionalBlock(
                    elseBranch,
                    inElseBranch
                );

            final BlockWriter branchCompiled = elseBranchPSR.result();

            final StaticState endOfElseBranch = elseBranchPSR.state();

            ifsp.setElseBranch(branchCompiled);

            final StaticState afterElseBranch = endOfElseBranch.exitScope();

            afterBranches.add(afterElseBranch);
        } else {
            afterBranches.add(inElseBranch);
        }

        acceptor.accept(ifsp);

        return StaticState.intersectAllAlternatives(
            afterBranches,
            () -> beforeTheBranches
        );
    }


    @Override
    public StaticState validateStatement(
        Maybe<IfStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        Maybe<RValueExpression> condition = input.__(IfStatement::getCondition);
        Maybe<OptionalBlock> thenBranch = input.__(IfStatement::getThenBranch);

        List<Maybe<RValueExpression>> elseIfConditions =
            toListOfMaybes(input.__(IfStatement::getElseIfConditions));
        List<Maybe<OptionalBlock>> elseIfBranches =
            toListOfMaybes(input.__(IfStatement::getElseIfBranches));

        Maybe<OptionalBlock> elseBranch = input.__(IfStatement::getElseBranch);

        boolean conditionCheck = validateCondition(
            input,
            condition,
            -1,
            state,
            acceptor
        );
        final BlockSemantics blockSemantics = module.get(BlockSemantics.class);

        StaticState runningState;
        List<StaticState> afterBranches = new ArrayList<>();
        List<Maybe<RValueExpression>> prevConditions =
            new ArrayList<>();

        StaticState inThenBranch;
        if (conditionCheck == VALID) {
            runningState = rves.advance(condition, state);


            inThenBranch = rves.assertReturnedTrue(
                condition,
                runningState
            );

            prevConditions.add(condition);
        } else {
            inThenBranch = runningState = state;

        }

        inThenBranch = inThenBranch.enterScope();

        final StaticState endOfThenBranch =
            blockSemantics.validateOptionalBlock(
                thenBranch,
                inThenBranch,
                acceptor
            );

        final StaticState afterThenBranch = endOfThenBranch.exitScope();

        afterBranches.add(afterThenBranch);


        for (int i = 0; i < elseIfConditions.size(); i++) {

            final Maybe<RValueExpression> elseIfCond = elseIfConditions.get(i);
            boolean elseIfCondCheck = validateCondition(
                input,
                elseIfCond,
                i,
                runningState,
                acceptor
            );

            StaticState inElseIfBranch = runningState;

            if (elseIfCondCheck == VALID) {
                runningState = rves.advance(condition, state);

                for (Maybe<RValueExpression> prevCondition : prevConditions) {
                    inElseIfBranch = rves.assertReturnedFalse(
                        prevCondition,
                        inElseIfBranch
                    );
                }

                inElseIfBranch = rves.assertReturnedTrue(
                    elseIfCond,
                    inElseIfBranch
                );

                prevConditions.add(elseIfCond);
            }

            inElseIfBranch = inElseIfBranch.enterScope();


            final StaticState endOfElseIfBranch =
                blockSemantics.validateOptionalBlock(
                    elseIfBranches.get(i),
                    inElseIfBranch,
                    acceptor
                );

            final StaticState afterElseIfBranch = endOfElseIfBranch.exitScope();

            afterBranches.add(afterElseIfBranch);

        }


        StaticState inElseBranch = runningState;
        for (Maybe<RValueExpression> prevCondition : prevConditions) {
            inElseBranch = rves.assertReturnedFalse(
                prevCondition,
                inElseBranch
            );
        }


        if (elseBranch.isPresent()) {
            inElseBranch = inElseBranch.enterScope();

            final StaticState endOfElseBranch =
                blockSemantics.validateOptionalBlock(
                    elseBranch,
                    inElseBranch,
                    acceptor
                );

            final StaticState afterElseBranch = endOfElseBranch.exitScope();
            afterBranches.add(afterElseBranch);

        } else {

            afterBranches.add(inElseBranch);
        }

        final StaticState beforeTheBranches = runningState;

        return StaticState.intersectAllAlternatives(
            afterBranches,
            () -> beforeTheBranches
        );
    }


    private boolean validateCondition(
        Maybe<IfStatement> input,
        Maybe<RValueExpression> condition,
        int index,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        boolean conditionCheck = rves.validate(
            condition,
            state,
            acceptor
        );

        if (conditionCheck == INVALID) {
            return INVALID;
        }


        IJadescriptType condType =
            rves.inferType(condition, state);


        conditionCheck = module.get(ValidationHelper.class).assertExpectedType(
            module.get(TypeHelper.class).BOOLEAN,
            condType,
            "InvalidCondition",
            input,
            index == -1
                ? JadescriptPackage.eINSTANCE.getIfStatement_Condition()
                : JadescriptPackage.eINSTANCE.getIfStatement_ElseIfConditions(),
            index == -1
                ? ValidationMessageAcceptor.INSIGNIFICANT_INDEX
                : index,
            acceptor
        );

        return conditionCheck;
    }

}
