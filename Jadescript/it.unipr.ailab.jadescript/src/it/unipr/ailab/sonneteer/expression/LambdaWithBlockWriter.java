package it.unipr.ailab.sonneteer.expression;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.classmember.ParameterWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.LocalVarBindingProvider;

import java.util.ArrayList;
import java.util.List;

public class LambdaWithBlockWriter extends ExpressionWriter{
    private final List<ParameterWriter> parameters = new ArrayList<>();
    private BlockWriter body = new BlockWriter();

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        s.add("(");
        for (int i = 0; i < parameters.size(); i++) {
            ParameterWriter p = parameters.get(i);
            p.writeSonnet(s);
            if (i != parameters.size() - 1) {
                s.spaced(",");
            }
        }
        s.spaced(")").spaced("->");
        body.writeSonnet(s);
    }


    public LambdaWithBlockWriter addParameter(ParameterWriter parameterPoet) {
        this.parameters.add(parameterPoet);
        return this;
    }

    public LambdaWithBlockWriter setBody(BlockWriter body) {
        this.body = body;
        return this;
    }

    public BlockWriter getBody() {
        return body;
    }

    public List<ParameterWriter> getParameters() {
        return parameters;
    }

    public LambdaWithBlockWriter setParameters(Iterable<ParameterWriter> parameters){
        this.parameters.clear();
        for (ParameterWriter parameter : parameters) {
            this.parameters.add(parameter);
        }
        return this;
    }

    @Override
    public ExpressionWriter bindVariableUsages(
        LocalVarBindingProvider varBindingProvider
    ) {
        var l = w.blockLambda().setParameters(
                this.parameters
        );

        final BlockWriter block = w.block().addAll(
            this.body.getBlockElements()
        );

        return l.setBody(block);
    }
}
