package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.IfStatement;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.OptionalBlock;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.controlflow.IfStatementWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;
import java.util.stream.Collectors;
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
    public void compileStatement(Maybe<IfStatement> input, CompilationOutputAcceptor acceptor) {
        Maybe<RValueExpression> condition = input.__(IfStatement::getCondition);
        Maybe<OptionalBlock> thenBranch = input.__(IfStatement::getThenBranch);

        List<Maybe<RValueExpression>> elseIfConditions = toListOfMaybes(input.__(IfStatement::getElseIfConditions));
        List<Maybe<OptionalBlock>> elseIfBranches = toListOfMaybes(input.__(IfStatement::getElseIfBranches));

        Maybe<OptionalBlock> elseBranch = input.__(IfStatement::getElseBranch);

        module.get(ContextManager.class).pushScope();
        String conditionCompiled = module.get(RValueExpressionSemantics.class)
                .compile(condition, acceptor).getGeneratedText();
        BlockWriter thenBranchCompiled = module.get(BlockSemantics.class).compileOptionalBlock(thenBranch);
        IfStatementWriter ifsp = w.ifStmnt(w.expr(conditionCompiled), thenBranchCompiled);
        module.get(ContextManager.class).popScope();

        for (int i = 0; i < elseIfBranches.size(); ++i) {
            module.get(ContextManager.class).pushScope();
            String elseIfCond = module.get(RValueExpressionSemantics.class)
                    .compile(elseIfConditions.get(i), acceptor).getGeneratedText();
            BlockWriter elseIfBranch = module.get(BlockSemantics.class).compileOptionalBlock(elseIfBranches.get(i));
            ifsp.addElseIfBranch(w.expr(elseIfCond), elseIfBranch);
            module.get(ContextManager.class).popScope();
        }


        if (input.__(IfStatement::isWithElseBranch).extract(nullAsFalse)) {
            ifsp.setElseBranch(module.get(BlockSemantics.class).compileOptionalBlock(elseBranch));
        }


        acceptor.accept(ifsp);
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<IfStatement> input) {
        Maybe<RValueExpression> condition = input.__(IfStatement::getCondition);
        List<Maybe<RValueExpression>> elseIfConditions = toListOfMaybes(input.__(IfStatement::getElseIfConditions));
        return Stream.concat(Stream.of(condition), elseIfConditions.stream())
                .filter(Maybe::isPresent)
                .map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), x))
                .collect(Collectors.toList());
    }

    @Override
    public void validate(Maybe<IfStatement> input, ValidationMessageAcceptor acceptor) {
        Maybe<RValueExpression> condition = input.__(IfStatement::getCondition);
        Maybe<OptionalBlock> thenBranch = input.__(IfStatement::getThenBranch);

        List<Maybe<RValueExpression>> elseIfConditions = toListOfMaybes(input.__(IfStatement::getElseIfConditions));
        List<Maybe<OptionalBlock>> elseIfBranches = toListOfMaybes(input.__(IfStatement::getElseIfBranches));

        Maybe<OptionalBlock> elseBranch = input.__(IfStatement::getElseBranch);

        module.get(ContextManager.class).pushScope();
        validateCondition(input, condition, -1, acceptor);
        module.get(BlockSemantics.class).validateOptionalBlock(thenBranch, acceptor);
        module.get(ContextManager.class).popScope();

        for (int i = 0; i < elseIfConditions.size(); i++) {
            module.get(ContextManager.class).pushScope();
            validateCondition(input, elseIfConditions.get(i), i, acceptor);
            module.get(BlockSemantics.class).validateOptionalBlock(elseIfBranches.get(i), acceptor);
            module.get(ContextManager.class).popScope();
        }


        if (elseBranch.isPresent()) {
            module.get(BlockSemantics.class).validateOptionalBlock(elseBranch, acceptor);
        }


    }

    private void validateCondition(Maybe<IfStatement> input, Maybe<RValueExpression> condition, int index, ValidationMessageAcceptor acceptor) {
        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);

        module.get(RValueExpressionSemantics.class).validate(condition, interceptAcceptor);

        if (!interceptAcceptor.thereAreErrors()) {

            IJadescriptType condType = module.get(RValueExpressionSemantics.class).inferType(condition);
            module.get(ValidationHelper.class).assertExpectedType(module.get(TypeHelper.class).BOOLEAN, condType,
                    "InvalidCondition",
                    input,
                    index == -1 ? JadescriptPackage.eINSTANCE.getIfStatement_Condition()
                            : JadescriptPackage.eINSTANCE.getIfStatement_ElseIfConditions(),
                    index,
                    acceptor
            );
        }
    }
}
