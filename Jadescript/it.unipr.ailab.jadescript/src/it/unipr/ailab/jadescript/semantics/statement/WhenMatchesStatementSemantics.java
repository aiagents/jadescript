package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.LValueExpression;
import it.unipr.ailab.jadescript.jadescript.OptionalBlock;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.WhenMatchesStatement;
import it.unipr.ailab.jadescript.semantics.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.LValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.PSR;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.helpers.PatternMatchHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.LocalClassStatementWriter;
import it.unipr.ailab.sonneteer.statement.controlflow.IfStatementWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;
import static it.unipr.ailab.maybe.Maybe.toListOfMaybes;

/**
 * Created on 22/08/2021.
 */
@Singleton
public class WhenMatchesStatementSemantics
    extends StatementSemantics<WhenMatchesStatement> {


    public WhenMatchesStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<WhenMatchesStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        final Maybe<RValueExpression> inputExpr =
            input.__(WhenMatchesStatement::getInputExpr);

        final List<Maybe<LValueExpression>> patterns =
            toListOfMaybes(input.__(WhenMatchesStatement::getPatterns))
                .stream()
                .map(maybe -> maybe.__(i -> (LValueExpression) i))
                .collect(Collectors.toList());

        final List<Maybe<OptionalBlock>> branches =
            toListOfMaybes(input.__(WhenMatchesStatement::getBranches));

        final Maybe<OptionalBlock> elseBranch =
            input.__(WhenMatchesStatement::getElseBranch);

        IfStatementWriter ifsp = null;

        final PatternMatchHelper patternMatchHelper =
            module.get(PatternMatchHelper.class);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final LValueExpressionSemantics lves =
            module.get(LValueExpressionSemantics.class);

        final BlockSemantics blockSemantics = module.get(BlockSemantics.class);

        final IJadescriptType inputExprType = rves.inferType(
            inputExpr,
            state
        );

        final String compiledInputExpr = acceptor.auxiliaryVariable(
            inputExpr,
            inputExprType.compileToJavaTypeReference(),
            "inputExpr",
            rves.compile(inputExpr, state, acceptor)
        );


        StaticState afterInputExpr = rves.advance(inputExpr, state);

        final int assumedSize = Math.min(patterns.size(), branches.size());

        List<StaticState> afterBranches = new ArrayList<>(assumedSize);

        StaticState runningState = afterInputExpr;
        for (int i = 0; i < assumedSize; ++i) {
            final Maybe<LValueExpression> pattern = patterns.get(i);

            String localClassName =
                patternMatchHelper.getPatternMatcherClassName(pattern);

            final String variableName =
                patternMatchHelper.getPatternMatcherVariableName(pattern);

            final PatternMatchInput.WhenMatchesStatement<LValueExpression>
                pmi = new PatternMatchInput.WhenMatchesStatement<>(
                module,
                inputExprType,
                pattern,
                "__",
                variableName
            );

            final PatternMatcher output =
                lves.compilePatternMatch(
                    pmi,
                    runningState,
                    acceptor
                );

            final LocalClassStatementWriter localClass =
                PatternMatchHelper.w.localClass(localClassName);

            output.getWriters().forEach(localClass::addMember);

            acceptor.accept(localClass);
            acceptor.accept(PatternMatchHelper.w.variable(
                localClassName,
                variableName,
                PatternMatchHelper.w.expr("new "
                    + localClassName + "()")
            ));

            StaticState afterPattern = lves.advancePattern(pmi, runningState);

            StaticState inBranch = lves.assertDidMatch(pmi, runningState);

            String condition =
                output.operationInvocationText(compiledInputExpr);

            inBranch = inBranch.enterScope();

            final PSR<BlockWriter> blockPSR =
                blockSemantics.compileOptionalBlock(
                    branches.get(i),
                    inBranch
                );

            final BlockWriter blockCompiled = blockPSR.result();
            final StaticState endOfBranch = blockPSR.state();

            if (ifsp == null) {
                ifsp = w.ifStmnt(w.expr(condition), blockCompiled);
            } else {
                ifsp.addElseIfBranch(w.expr(condition), blockCompiled);
            }

            final StaticState afterBranch = endOfBranch.exitScope();

            afterBranches.add(afterBranch);
            runningState = afterPattern;
        }


        StaticState inElseBranch = runningState;




        if (ifsp != null &&
            input.__(WhenMatchesStatement::isWithElseBranch)
                .extract(nullAsFalse)) {

            inElseBranch = inElseBranch.enterScope();

            final PSR<BlockWriter> blockPSR =
                blockSemantics.compileOptionalBlock(
                    elseBranch,
                    inElseBranch
                );
            final BlockWriter elseBranchCompiled = blockPSR.result();
            ifsp.setElseBranch(elseBranchCompiled);
            final StaticState endOfElseBranch = blockPSR.state();

            final StaticState afterElseBranch = endOfElseBranch.exitScope();

            afterBranches.add(afterElseBranch);
        }else{
            afterBranches.add(inElseBranch);
        }

        if (ifsp != null) {
            acceptor.accept(ifsp);
        }


        StaticState beforeBranches = runningState;


        return StaticState.intersectAllAlternatives(
            afterBranches,
            () -> beforeBranches
        );

    }


    @Override
    public Stream<SemanticsBoundToExpression<?>> includedExpressions(
        Maybe<WhenMatchesStatement> input
    ) {
        return Util.buildStream(() -> new SemanticsBoundToExpression<>(
            module.get(RValueExpressionSemantics.class),
            input.__(WhenMatchesStatement::getInputExpr)
        ));
    }


    @Override
    public StaticState validateStatement(
        Maybe<WhenMatchesStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final Maybe<RValueExpression> inputExpr =
            input.__(WhenMatchesStatement::getInputExpr);

        final List<Maybe<LValueExpression>> patterns =
            toListOfMaybes(input.__(WhenMatchesStatement::getPatterns))
                .stream()
                .map(maybe -> maybe.__(i -> (LValueExpression) i))
                .collect(Collectors.toList());

        final List<Maybe<OptionalBlock>> branches =
            toListOfMaybes(input.__(WhenMatchesStatement::getBranches));

        final Maybe<OptionalBlock> elseBranch =
            input.__(WhenMatchesStatement::getElseBranch);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final LValueExpressionSemantics lves =
            module.get(LValueExpressionSemantics.class);

        final PatternMatchHelper patternMatchHelper =
            module.get(PatternMatchHelper.class);

        final BlockSemantics blockSemantics =
            module.get(BlockSemantics.class);


        rves.validate(inputExpr, state, acceptor);

        final IJadescriptType inputExprType =
            rves.inferType(inputExpr, state);

        StaticState afterInputExpr = rves.advance(inputExpr, state);

        final int assumedSize = Math.min(patterns.size(), branches.size());

        List<StaticState> afterBranches = new ArrayList<>(assumedSize);

        StaticState runningState = afterInputExpr;
        boolean allPatternsChecks = VALID;
        for (int i = 0; i < Math.min(branches.size(), patterns.size()); i++) {
            final Maybe<LValueExpression> pattern = patterns.get(i);

            final PatternMatchInput.WhenMatchesStatement<LValueExpression> pmi =
                patternMatchHelper.whenMatchesStatement(
                    inputExprType,
                    pattern
                );

            boolean patternCheck = lves.validatePatternMatch(
                pmi,
                runningState,
                acceptor
            );

            allPatternsChecks = allPatternsChecks && patternCheck;


            StaticState afterPattern = lves.advancePattern(pmi, runningState);

            StaticState inBranch = lves.assertDidMatch(pmi, runningState);

            inBranch = inBranch.enterScope();

            final StaticState endOfBranch =
                blockSemantics.validateOptionalBlock(
                    branches.get(i),
                    inBranch,
                    acceptor
                );

            final StaticState afterBranch = endOfBranch.exitScope();

            afterBranches.add(afterBranch);

            runningState = afterPattern;
        }


        StaticState inElseBranch = runningState;


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


}
