package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.helpers.PatternMatchHelper;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.controlflow.IfStatementWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 22/08/2021.
 */
@Singleton
public class WhenMatchesStatementSemantics extends StatementSemantics<WhenMatchesStatement> {


    public WhenMatchesStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void compileStatement(Maybe<WhenMatchesStatement> input, CompilationOutputAcceptor acceptor) {
        final Maybe<RValueExpression> inputExpr = input.__(WhenMatchesStatement::getInputExpr);
        final List<Maybe<LValueExpression>> patterns = toListOfMaybes(input.__(WhenMatchesStatement::getPatterns))
                .stream()
                .map(maybe -> maybe.__(i -> (LValueExpression) i))
                .collect(Collectors.toList());
        final List<Maybe<OptionalBlock>> branches = toListOfMaybes(input.__(WhenMatchesStatement::getBranches));
        final Maybe<OptionalBlock> elseBranch = input.__(WhenMatchesStatement::getElseBranch);

        IfStatementWriter ifsp = null;

        for (int i = 0; i < Math.min(patterns.size(), branches.size()); ++i) {
            module.get(ContextManager.class).pushScope();


            final Maybe<LValueExpression> pattern = patterns.get(i);
            final String compiledInputExpr = module.get(RValueExpressionSemantics.class).compile(inputExpr, , acceptor)
                    .toString();

            PatternMatchOutput<
                    ? extends PatternMatcher
                    > output =
                    module.get(PatternMatchHelper.class)
                            .compileWhenMatchesStatementPatternMatching(inputExpr, pattern, acceptor);

            //TODO handle narrowing
            //TODO handle unification correctly (see next TODO comment...)

            //TODO this approach uses a shared scope between the pattern matching and the block execution.
            //  However, the two scopes should be separated, and the results of the unification should be injected
            //  into the block's evaluation context

            String condition = output.getProcessInfo().operationInvocationText(compiledInputExpr);
            BlockWriter branch = module.get(BlockSemantics.class).compileOptionalBlock(branches.get(i));
            if (ifsp == null) {
                ifsp = w.ifStmnt(w.expr(condition), branch);
            } else {
                ifsp.addElseIfBranch(w.expr(condition), branch);
            }
            //TODO see previous TODO comment ^^^
            module.get(ContextManager.class).popScope();
        }


        if (ifsp != null && input.__(WhenMatchesStatement::isWithElseBranch).extract(nullAsFalse)) {
            ifsp.setElseBranch(module.get(BlockSemantics.class).compileOptionalBlock(elseBranch));
        }


        acceptor.accept(ifsp);

    }


    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<WhenMatchesStatement> input) {

        final Maybe<RValueExpression> inputExpr = input.__(WhenMatchesStatement::getInputExpr);
        return Collections.singletonList(new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                inputExpr
        ));
    }

    @Override
    public void validate(Maybe<WhenMatchesStatement> input, ValidationMessageAcceptor acceptor) {
        final Maybe<RValueExpression> inputExpr = input.__(WhenMatchesStatement::getInputExpr);
        final List<Maybe<LValueExpression>> patterns = toListOfMaybes(input.__(WhenMatchesStatement::getPatterns))
                .stream()
                .map(maybe -> maybe.__(i -> (LValueExpression) i))
                .collect(Collectors.toList());
        ;
        final List<Maybe<OptionalBlock>> branches = toListOfMaybes(input.__(WhenMatchesStatement::getBranches));
        final Maybe<OptionalBlock> elseBranch = input.__(WhenMatchesStatement::getElseBranch);
        for (int i = 0; i < Math.min(branches.size(), patterns.size()); i++) {
            final Maybe<LValueExpression> pattern = patterns.get(i);
            module.get(ContextManager.class).pushScope();
            module.get(RValueExpressionSemantics.class).validate(inputExpr, , acceptor);


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


            module.get(BlockSemantics.class).validateOptionalBlock(branches.get(i), acceptor);
            module.get(ContextManager.class).popScope();
        }


        if (elseBranch.isPresent()) {
            module.get(BlockSemantics.class).validateOptionalBlock(elseBranch, acceptor);
        }
    }


}
