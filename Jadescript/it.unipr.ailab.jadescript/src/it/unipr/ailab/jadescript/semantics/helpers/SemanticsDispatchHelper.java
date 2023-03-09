package it.unipr.ailab.jadescript.semantics.helpers;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.feature.*;
import it.unipr.ailab.jadescript.semantics.statement.*;
import it.unipr.ailab.maybe.Maybe;

import java.util.function.Consumer;

public class SemanticsDispatchHelper {

    private final SemanticsModule module;

    public SemanticsDispatchHelper(SemanticsModule module) {
        this.module = module;
    }

    public void dispachFeatureSemantics(
            Maybe<? extends Feature> f,
            Consumer<DeclarationMemberSemantics<? extends Feature>> action
    ) {
        Feature feature = f.toNullable();
        if (feature instanceof Field) {
            action.accept(module.get(FieldSemantics.class));
        } else if (feature instanceof FunctionOrProcedure) {
            action.accept(module.get(MemberOperationSemantics.class));
        } else if (feature instanceof OnMessageHandler) {
            action.accept(module.get(OnMessageHandlerSemantics.class));
        } else if (feature instanceof OnPerceptHandler) {
            action.accept(module.get(OnPerceptHandlerSemantics.class));
        } else if (feature instanceof MemberBehaviour) {
            action.accept(module.get(MemberBehaviourSemantics.class));
        } else if (feature instanceof OnExceptionHandler) {
            action.accept(module.get(OnExceptionHandlerSemantics.class));
        } else if (feature instanceof OnCreateHandler) {
            action.accept(module.get(OnCreateHandlerSemantics.class));
        } else if (feature instanceof OnDestroyHandler) {
            action.accept(module.get(OnDestroyHandlerSemantics.class));
        } else if (feature instanceof OnActivateHandler) {
            action.accept(module.get(OnActivateHandlerSemantics.class));
        } else if (feature instanceof OnDeactivateHandler) {
            action.accept(module.get(OnDeactivateHandlerSemantics.class));
        } else if (feature instanceof OnExecuteHandler) {
            action.accept(module.get(OnExecuteHandlerSemantics.class));
        } else if (feature instanceof OnBehaviourFailureHandler) {
            action.accept(module.get(OnBehaviourFailureHandlerSemantics.class));
        } else {
            //do nothing
        }
    }


    public void dispatchStatementSemantics(
            Maybe<? extends Statement> statement,
            Consumer<StatementSemantics<? extends Statement>> action
    ) {
        Statement input = statement.toNullable();


        if (input instanceof Assignment) {
            action.accept(module.get(AssignmentSemantics.class));
        } else if (input instanceof ProcedureCallStatement) {
            action.accept(module.get(ProcedureCallStatementSemantics.class));
        } else if (input instanceof IfStatement) {
            action.accept(module.get(IfStatementSemantics.class));
        } else if (input instanceof WhenMatchesStatement) {
            action.accept(module.get(WhenMatchesStatementSemantics.class));
        } else if (input instanceof WhileStatement) {
            action.accept(module.get(WhileStatementSemantics.class));
        } else if (input instanceof AtomExpr) {
            action.accept(module.get(AtomWithTrailersStatementSemantics.class));
        } else if (input instanceof ActivateStatement) {
            action.accept(module.get(ActivateStatementSemantics.class));
        } else if (input instanceof CreateAgentStatement) {
            action.accept(module.get(CreateAgentStatementSemantics.class));
        } else if (input instanceof DeactivateStatement) {
            action.accept(module.get(DeactivateStatementSemantics.class));
        } else if (input instanceof FailStatement) {
            action.accept(module.get(FailStatementSemantics.class));
        } else if (input instanceof DestroyStatement) {
            action.accept(module.get(DestroyStatementSemantics.class));
        } else if (input instanceof SendMessageStatement) {
            action.accept(module.get(SendMessageStatementSemantics.class));
        } else if (input instanceof ForStatement) {
            action.accept(module.get(ForStatementSemantics.class));
        } else if (input instanceof ReturnStatement) {
            action.accept(module.get(ReturnStatementSemantics.class));
        } else if (input instanceof BreakStatement) {
            action.accept(module.get(BreakStatementSemantics.class));
        } else if (input instanceof LogStatement) {
            action.accept(module.get(LogStatementSemantics.class));
        } else if (input instanceof AddStatement) {
            action.accept(module.get(AddStatementSemantics.class));
        } else if (input instanceof ThrowStatement) {
            action.accept(module.get(ThrowStatementSemantics.class));
        } else if (input instanceof RemoveStatement) {
            action.accept(module.get(RemoveStatementSemantics.class));
        } else if (input instanceof ClearStatement) {
            action.accept(module.get(ClearStatementSemantics.class));
        } else if (input instanceof PutbackStatement) {
            action.accept(module.get(PutBackStatementSemantics.class));
        } else if(input instanceof DebugTypeComparison) {
            action.accept(module.get(DebugTypeComparisonSemantics.class));
        }
        //else do nothing
    }
}
