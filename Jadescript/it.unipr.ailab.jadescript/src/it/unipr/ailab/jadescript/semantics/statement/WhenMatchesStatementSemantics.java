package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.OptionalBlock;
import it.unipr.ailab.jadescript.jadescript.Pattern;
import it.unipr.ailab.jadescript.jadescript.UnaryPrefix;
import it.unipr.ailab.jadescript.jadescript.WhenMatchesStatement;
import it.unipr.ailab.jadescript.semantics.PatternMatchingSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.UnaryPrefixExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.PatternMatchRequest;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import it.unipr.ailab.sonneteer.statement.controlflow.IfStatementWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    public List<StatementWriter> generateAuxiliaryStatements(Maybe<WhenMatchesStatement> input) {
        final Maybe<UnaryPrefix> unary = input.__(WhenMatchesStatement::getUnary);
        final List<Maybe<Pattern>> patterns = toListOfMaybes(input.__(WhenMatchesStatement::getPatterns));

        List<StatementWriter> statementWriters = new ArrayList<>();


        for (Maybe<Pattern> pattern : patterns) {
            module.get(ContextManager.class).pushScope();
            statementWriters.addAll(module.get(PatternMatchingSemantics.class).generateAuxiliaryStatements(
                    PatternMatchRequest.patternMatchRequest(
                            input,
                            pattern,
                            unary,
                            true
                    )
            ));
            module.get(ContextManager.class).popScope();
        }
        return statementWriters;

    }

    @Override
    public List<BlockWriterElement> compileStatement(Maybe<WhenMatchesStatement> input) {
        final Maybe<UnaryPrefix> unary = input.__(WhenMatchesStatement::getUnary);
        final List<Maybe<Pattern>> patterns = toListOfMaybes(input.__(WhenMatchesStatement::getPatterns));
        final List<Maybe<OptionalBlock>> branches = toListOfMaybes(input.__(WhenMatchesStatement::getBranches));
        final Maybe<OptionalBlock> elseBranch = input.__(WhenMatchesStatement::getElseBranch);
        List<BlockWriterElement> result = new ArrayList<>();

        IfStatementWriter ifsp = null;

        for (int i = 0; i < Math.min(patterns.size(), branches.size()); ++i) {
            module.get(ContextManager.class).pushScope();

            module.get(PatternMatchingSemantics.class).generateAuxiliaryStatements(
                    PatternMatchRequest.patternMatchRequest(
                            input,
                            patterns.get(i),
                            unary,
                            true
                    )
            );
            String condition = module.get(PatternMatchingSemantics.class).compileMatchesExpression(
                            input,
                            unary
                    )
                    .extract(nullAsEmptyString);
            BlockWriter branch = module.get(BlockSemantics.class).compileOptionalBlock(branches.get(i));
            if (ifsp == null) {
                ifsp = w.ifStmnt(
                        w.expr(condition),
                        branch
                );
            } else {
                ifsp.addElseIfBranch(w.expr(condition), branch);
            }
            module.get(ContextManager.class).popScope();
        }


        if (ifsp != null && input.__(WhenMatchesStatement::isWithElseBranch).extract(nullAsFalse)) {
            ifsp.setElseBranch(module.get(BlockSemantics.class).compileOptionalBlock(elseBranch));
        }


        result.add(ifsp);
        return result;
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<WhenMatchesStatement> input) {

        final Maybe<UnaryPrefix> unary = input.__(WhenMatchesStatement::getUnary);
        return Collections.singletonList(new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(UnaryPrefixExpressionSemantics.class),
                unary
        ));
    }

    @Override
    public void validate(Maybe<WhenMatchesStatement> input, ValidationMessageAcceptor acceptor) {
        final Maybe<UnaryPrefix> unary = input.__(WhenMatchesStatement::getUnary);
        final List<Maybe<Pattern>> patterns = toListOfMaybes(input.__(WhenMatchesStatement::getPatterns));
        final List<Maybe<OptionalBlock>> branches = toListOfMaybes(input.__(WhenMatchesStatement::getBranches));
        final Maybe<OptionalBlock> elseBranch = input.__(WhenMatchesStatement::getElseBranch);
        for (int i = 0; i < Math.min(branches.size(), patterns.size()); i++) {
            module.get(ContextManager.class).pushScope();
            module.get(PatternMatchingSemantics.class).validate(
                    PatternMatchRequest.patternMatchRequest(
                            input,
                            patterns.get(i),
                            unary,
                            true
                    ),
                    acceptor
            );
            module.get(BlockSemantics.class).validateOptionalBlock(branches.get(i), acceptor);
            module.get(ContextManager.class).popScope();
        }


        if (elseBranch.isPresent()) {
            module.get(BlockSemantics.class).validateOptionalBlock(elseBranch, acceptor);
        }
    }


}
