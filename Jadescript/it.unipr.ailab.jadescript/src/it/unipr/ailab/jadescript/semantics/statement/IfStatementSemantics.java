package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.IfStatement;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.OptionalBlock;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.staticstate.EvaluationResult;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.PSR;
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
import java.util.stream.Stream;

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
        CompilationOutputAcceptor acceptor
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

        boolean isExhaustive;

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final BlockSemantics blockSemantics = module.get(BlockSemantics.class);

        StaticState runningState = state;

        String conditionCompiled = rves.compile(
            condition,
            runningState,
            acceptor
        );
        List<Maybe<ExpressionDescriptor>> prevConditions = new ArrayList<>();

        Maybe<ExpressionDescriptor> conditionDescriptor =
            rves.describeExpression(condition, runningState);
        runningState = rves.advance(condition, runningState);
        StaticState inThenBranch;
        inThenBranch = runningState.assertEvaluation(
            conditionDescriptor,
            EvaluationResult.ReturnedTrue.INSTANCE
        );
        prevConditions.add(conditionDescriptor);


        PSR<BlockWriter> thenBranchPSR = blockSemantics.compileOptionalBlock(
            thenBranch,
            inThenBranch
        );
        BlockWriter thenBranchCompiled = thenBranchPSR.result();
        IfStatementWriter ifsp = w.ifStmnt(
            w.expr(conditionCompiled),
            thenBranchCompiled
        );

        //Exit scope
        StaticState afterThenBranch = thenBranchPSR.state();

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
            Maybe<ExpressionDescriptor> elseIfCondDescriptor =
                rves.describeExpression(elseIfCond, runningState);

            runningState = rves.advance(elseIfCond, runningState);

            StaticState inElseIfBranch = runningState.assertEvaluation(
                elseIfCondDescriptor,
                EvaluationResult.ReturnedTrue.INSTANCE
            );


            for (Maybe<ExpressionDescriptor> prevCondition : prevConditions) {
                inElseIfBranch = inElseIfBranch.assertEvaluation(
                    prevCondition,
                    EvaluationResult.ReturnedFalse.INSTANCE
                );
            }


            PSR<BlockWriter> elseIfBranchPSR =
                blockSemantics.compileOptionalBlock(
                    elseIfBranches.get(i),
                    inElseIfBranch
                );

            final BlockWriter branchCompiled = elseIfBranchPSR.result();

            //Exit scope
            final StaticState afterBranch = elseIfBranchPSR.state();

            ifsp.addElseIfBranch(w.expr(elseIfCondCompiled), branchCompiled);

            prevConditions.add(elseIfCondDescriptor);

            afterBranches.add(afterBranch);
        }


        if (input.__(IfStatement::isWithElseBranch).extract(nullAsFalse)) {
            isExhaustive = true;
            StaticState inElseBranch = runningState;
            for (Maybe<ExpressionDescriptor> prevCondition : prevConditions) {
                inElseBranch = inElseBranch.assertEvaluation(
                    prevCondition,
                    EvaluationResult.ReturnedFalse.INSTANCE
                );
            }


            PSR<BlockWriter> elseBranchPSR =
                blockSemantics.compileOptionalBlock(
                    elseBranch,
                    inElseBranch
                );

            final BlockWriter branchCompiled = elseBranchPSR.result();

            //Exit scope
            final StaticState afterBranch = elseBranchPSR.state();

            ifsp.setElseBranch(branchCompiled);
            afterBranches.add(afterBranch);
        } else {
            isExhaustive = false;
        }

        acceptor.accept(ifsp);

        final StaticState beforeTheBranches = runningState;
        if (isExhaustive) {
            // If exhaustive, the state "before the branches" does not directly
            // contribute to the intersection, because if it is exhaustive,
            // exactly one branch is executed for sure and it changes the state.
            return StaticState.intersectAll(
                afterBranches,
                () -> beforeTheBranches
            );
        } else {
            // If not exhaustive, the state "before the branches" might survive
            // unchanged the if statement. Therefore, it contributes directly
            // to the intersection.
            //TODO: If not exhaustive and all the afterBranches states are
            // invalidated, all the conditions can be asserted false on
            // the resulting state
            return beforeTheBranches.intersectAll(afterBranches);
        }
    }


    @Override
    public Stream<SemanticsBoundToExpression<?>> includedExpressions(
        Maybe<IfStatement> input
    ) {
        Maybe<RValueExpression> condition = input.__(IfStatement::getCondition);
        List<Maybe<RValueExpression>> elseIfConditions =
            toListOfMaybes(input.__(
                IfStatement::getElseIfConditions));
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        return Stream.concat(Stream.of(condition), elseIfConditions.stream())
            .filter(Maybe::isPresent)
            .map(i -> new SemanticsBoundToExpression<>(rves, i));
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
        List<Maybe<ExpressionDescriptor>> prevConditions = new ArrayList<>();

        StaticState inThenBranch;
        if (conditionCheck == VALID) {
            Maybe<ExpressionDescriptor> condDescriptor =
                rves.describeExpression(
                    condition,
                    state
                );

            runningState = rves.advance(condition, state);


            inThenBranch = runningState.assertEvaluation(
                condDescriptor,
                EvaluationResult.ReturnedTrue.INSTANCE
            );

            prevConditions.add(condDescriptor);
        } else {
            inThenBranch = runningState = state;

        }

        final StaticState afterThenBranch =
            blockSemantics.validateOptionalBlock(
                thenBranch,
                inThenBranch,
                acceptor
            );

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

            StaticState inElseIfBranch;
            if (elseIfCondCheck == VALID) {
                Maybe<ExpressionDescriptor> elseIfCondDescr =
                    rves.describeExpression(elseIfCond, runningState);

                runningState = rves.advance(condition, state);


                inElseIfBranch = runningState.assertEvaluation(
                    elseIfCondDescr,
                    EvaluationResult.ReturnedTrue.INSTANCE
                );

                for (Maybe<ExpressionDescriptor> prevExpression :
                    prevConditions) {
                    inElseIfBranch = inElseIfBranch.assertEvaluation(
                        prevExpression,
                        EvaluationResult.ReturnedFalse.INSTANCE
                    );
                }

                prevConditions.add(elseIfCondDescr);
            } else {
                inElseIfBranch = runningState;
            }


            final StaticState afterElseIfBranch =
                blockSemantics.validateOptionalBlock(
                    elseIfBranches.get(i),
                    inElseIfBranch,
                    acceptor
                );

            afterBranches.add(afterElseIfBranch);

        }


        boolean isExhaustive;


        if (elseBranch.isPresent()) {
            StaticState inElseBranch = runningState;
            for (Maybe<ExpressionDescriptor> prevCondition : prevConditions) {
                inElseBranch = inElseBranch.assertEvaluation(
                    prevCondition,
                    EvaluationResult.ReturnedFalse.INSTANCE
                );
            }

            final StaticState afterElseBranch =
                blockSemantics.validateOptionalBlock(
                    elseBranch,
                    inElseBranch,
                    acceptor
                );
            afterBranches.add(afterElseBranch);

            isExhaustive = true;
        } else {
            isExhaustive = false;
        }


        final StaticState beforeTheBranches = runningState;
        if (isExhaustive) {
            // If exhaustive, the state "before the branches" does not directly
            // contribute to the intersection, because if it is exhaustive,
            // exactly one branch is executed for sure and it changes the state.
            return StaticState.intersectAll(
                afterBranches,
                () -> beforeTheBranches
            );
        } else {
            // If not exhaustive, the state "before the branches" might survive
            // unchanged the if statement. Therefore, it contributes directly
            // to the intersection.
            //TODO: If not exhaustive and all the afterBranches states are
            // invalidated, all the conditions can be asserted false on the
            // resulting state
            return beforeTheBranches.intersectAll(afterBranches);
        }
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
