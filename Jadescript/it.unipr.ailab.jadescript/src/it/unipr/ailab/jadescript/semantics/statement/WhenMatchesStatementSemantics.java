package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.LValueExpression;
import it.unipr.ailab.jadescript.jadescript.OptionalBlock;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.WhenMatchesStatement;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.PSR;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.helpers.PatternMatchHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.controlflow.IfStatementWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
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

        final Maybe<ExpressionDescriptor> inputExprDescriptor =
            rves.describeExpression(inputExpr, state);

        StaticState afterInputExpr = rves.advance(inputExpr, state);

        final int assumedSize = Math.min(patterns.size(), branches.size());

        List<StaticState> afterBranches = new ArrayList<>(assumedSize);

        for (int i = 0; i < assumedSize; ++i) {
            final Maybe<LValueExpression> pattern = patterns.get(i);

            PatternMatcher output = patternMatchHelper
                .compileWhenMatchesStatementPatternMatching(
                    inputExprType,
                    pattern,
                    afterInputExpr,
                    acceptor
                );

            StaticState afterPattern = patternMatchHelper
                .advanceWhenMatchesStatementPatternMatching(
                    inputExprType,
                    pattern,
                    afterInputExpr
                );

            final IJadescriptType patternType = patternMatchHelper
                .inferMatchesExpressionPatternType(
                    inputExprType,
                    pattern,
                    afterInputExpr
                );

            StaticState inBranch = afterPattern.assertFlowTypingUpperBound(
                inputExprDescriptor,
                patternType
            );

            String condition =
                output.operationInvocationText(compiledInputExpr);

            final PSR<BlockWriter> blockPSR =
                blockSemantics.compileOptionalBlock(
                    branches.get(i),
                    inBranch
                );
            final BlockWriter blockCompiled = blockPSR.result();
            final StaticState afterBranch = blockPSR.state();

            if (ifsp == null) {
                ifsp = w.ifStmnt(w.expr(condition), blockCompiled);
            } else {
                ifsp.addElseIfBranch(w.expr(condition), blockCompiled);
            }

            afterBranches.add(afterBranch);
        }


        final boolean hasElseBranch =
            input.__(WhenMatchesStatement::isWithElseBranch)
                .extract(nullAsFalse);



        if (ifsp != null && hasElseBranch) {
            final PSR<BlockWriter> blockPSR =
                blockSemantics.compileOptionalBlock(
                elseBranch,
                afterInputExpr
            );
            final BlockWriter elseBranchCompiled = blockPSR.result();
            ifsp.setElseBranch(elseBranchCompiled);
            final StaticState afterElseBranch = blockPSR.state();
            afterBranches.add(afterElseBranch);
        }

        if(ifsp != null) {
            acceptor.accept(ifsp);
        }

        final StaticState beforeTheBranches = afterInputExpr;
        if (hasElseBranch) {
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
            return beforeTheBranches.intersectAll(afterBranches);
        }

    }


    @Override
    public Stream<SemanticsBoundToExpression<?>> includedExpressions(
        Maybe<WhenMatchesStatement> input
    ) {
        return Util.buildStream(() ->new SemanticsBoundToExpression<>(
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
        final List<Maybe<LValueExpression>> patterns = toListOfMaybes(input.__(
            WhenMatchesStatement::getPatterns))
            .stream()
            .map(maybe -> maybe.__(i -> (LValueExpression) i))
            .collect(Collectors.toList());
        ;
        final List<Maybe<OptionalBlock>> branches = toListOfMaybes(input.__(
            WhenMatchesStatement::getBranches));
        final Maybe<OptionalBlock> elseBranch =
            input.__(WhenMatchesStatement::getElseBranch);
        for (int i = 0; i < Math.min(branches.size(), patterns.size()); i++) {
            final Maybe<LValueExpression> pattern = patterns.get(i);
            module.get(ContextManager.class).pushScope();
            module.get(RValueExpressionSemantics.class).validate(inputExpr, ,
                acceptor);


            final PatternMatchOutput<
                ? extends PatternMatchSemanticsProcess.IsValidation
                > output =
                module.get(PatternMatchHelper.class).validateWhenMatchesStatementPatternMatching(
                    inputExpr,
                    pattern,
                    acceptor
                );

            //TODO handle narrowing
            //TODO handle unification correctly (see comments in compile...)


            module.get(BlockSemantics.class).validateOptionalBlock(branches.get(
                i), acceptor);
            module.get(ContextManager.class).popScope();
        }


        if (elseBranch.isPresent()) {
            module.get(BlockSemantics.class).validateOptionalBlock(
                elseBranch,
                acceptor
            );
        }
    }


}
