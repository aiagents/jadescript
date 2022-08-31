package it.unipr.ailab.sonneteer.statement.controlflow;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.LocalVarBindingProvider;
import it.unipr.ailab.sonneteer.statement.StatementWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class IfStatementWriter extends StatementWriter {

    private final ExpressionWriter condition;
    private final BlockWriter ifBranch;
    private final List<ExpressionWriter> elseIfConditions = new ArrayList<>();
    private final List<BlockWriter> elseIfBranches = new ArrayList<>();
    private BlockWriter elseBranch = null;

    public IfStatementWriter(ExpressionWriter condition, BlockWriter ifBranch){
        this.condition = condition;
        this.ifBranch = ifBranch;
    }

    public IfStatementWriter addElseIfBranch(ExpressionWriter condition, BlockWriter elseIfBranch){
        elseIfConditions.add(condition);
        elseIfBranches.add(elseIfBranch);
        return this;
    }

    public IfStatementWriter setElseBranch(BlockWriter elseBranch){
        this.elseBranch = elseBranch;
        return this;
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        s.add("if(");
        condition.writeSonnet(s);
        s.spaced(")");
        ifBranch.writeSonnet(s);
        for(int i = 0; i < elseIfConditions.size(); i++){
            ExpressionWriter eic = elseIfConditions.get(i);
            BlockWriter eib = elseIfBranches.get(i);
            s.add("else if(");
            eic.writeSonnet(s);
            s.spaced(")");
            eib.writeSonnet(s);
        }
        if(elseBranch != null){
            s.spaced("else");
            elseBranch.writeSonnet(s);
        }
    }

    public ExpressionWriter getCondition() {
        return condition;
    }

    public BlockWriter getElseBranch() {
        return elseBranch;
    }

    public List<BlockWriter> getElseIfBranches() {
        return elseIfBranches;
    }

    public List<ExpressionWriter> getElseIfConditions() {
        return elseIfConditions;
    }

    public BlockWriter getIfBranch() {
        return ifBranch;
    }

    @Override
    public void getSubBlocks(Consumer<BlockWriter> statementAcceptor) {
        statementAcceptor.accept(ifBranch);
        elseIfBranches.forEach(statementAcceptor);
        if (elseBranch != null) {
            statementAcceptor.accept(elseBranch);
        }
    }

    @Override
    public StatementWriter bindLocalVarUsages(LocalVarBindingProvider bindingProvider) {
        var ifst = new IfStatementWriter(
                condition.bindVariableUsages(bindingProvider),
                ifBranch.bindLocalVarUsages(bindingProvider)
        );
        ifst.elseIfConditions.addAll(this.elseIfConditions.stream()
                .map(ew -> ew.bindVariableUsages(bindingProvider))
                .collect(Collectors.toList()));
        ifst.elseIfBranches.addAll(this.elseIfBranches.stream()
                .map(ew -> ew.bindLocalVarUsages(bindingProvider))
                .collect(Collectors.toList()));
        if(elseBranch !=null) {
        ifst.elseBranch = this.elseBranch.bindLocalVarUsages(bindingProvider);
        }
        return ifst;
    }
}
