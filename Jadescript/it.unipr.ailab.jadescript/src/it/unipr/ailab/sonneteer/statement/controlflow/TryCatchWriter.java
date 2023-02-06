package it.unipr.ailab.sonneteer.statement.controlflow;

import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.StatementWriter;

import java.util.ArrayList;
import java.util.List;

public class TryCatchWriter extends StatementWriter {

    private final BlockWriter tryBranch;
    private final List<String> exceptionTypes = new ArrayList<>();
    private final List<String> varNames = new ArrayList<>();
    private final List<BlockWriter> catchBranches = new ArrayList<>();
    private Maybe<BlockWriter> finallyBranch = Maybe.nothing();

    public TryCatchWriter(BlockWriter tryBranch){
        this.tryBranch = tryBranch;

    }

    public TryCatchWriter addCatchBranch(String exceptionType, String varName, BlockWriter body){
        exceptionTypes.add(exceptionType);
        varNames.add(varName);
        catchBranches.add(body);
        return this;
    }

    public TryCatchWriter setFinallyBranch(BlockWriter finallyBranch){
        this.finallyBranch = Maybe.some(finallyBranch);
        return this;
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        if(catchBranches.isEmpty()) throw new InvalidStatementException(
                "Attempted to create a try/catch statement without any catch branches");
        s.spaced("try");
        tryBranch.writeSonnet(s);
        for(int i = 0; i < varNames.size(); i++){
            s.add("catch(").spaced(exceptionTypes.get(i)).add(varNames.get(i)).spaced(")");
            catchBranches.get(i).writeSonnet(s);
        }
        finallyBranch.safeDo(fbSafe -> {
            s.spaced("finally");
            fbSafe.writeSonnet(s);
        });
    }
}
