package it.unipr.ailab.jadescript.semantics.helpers;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.WriterFactory;
import it.unipr.ailab.sonneteer.classmember.MethodWriter;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;
import it.unipr.ailab.sonneteer.statement.ReturnStatementWriter;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import it.unipr.ailab.sonneteer.statement.VariableDeclarationWriter;
import it.unipr.ailab.sonneteer.statement.controlflow.TryCatchWriter;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class PatternMatchHelper {
    protected static final WriterFactory w = WriterFactory.getInstance();
    private final SemanticsModule module;

    public PatternMatchHelper(SemanticsModule module) {
        this.module = module;
    }


    public List<StatementWriter> compileAdaptType(String adaptType) {
        final ReturnStatementWriter returnFalse = w.returnStmnt(w.False);

        final VariableDeclarationWriter declareX = w.variable(adaptType, "__x");//initialized later
        final TryCatchWriter checkXType = w.tryCatch(w.block()
                        .addStatement(w.ifStmnt(
                                w.expr("__objx instanceof " + module.get(TypeHelper.class).noGenericsTypeName(adaptType)),
                                w.block().addStatement(w.assign("__x", w.expr("(" + adaptType + ") __objx")))
                        ).setElseBranch(w.block().addStatement(returnFalse))))
                .addCatchBranch("java.lang.ClassCastException", "ignored", w.block()
                        .addStatement(returnFalse));
        return Arrays.asList(declareX, checkXType);
    }

    public MethodWriter prepareMatcherMethod(String termID, IJadescriptType solvedPatternType){
        MethodWriter m = w.method(Visibility.PUBLIC, false, false, "boolean", termID)
                .addParameter(w.param("java.lang.Object", "__objx"));
        m.getBody().addStatements(compileAdaptType(solvedPatternType.compileToJavaTypeReference()));
        return m;
    }

    public MethodWriter prepareMatcherMethod(String termID, PatternType patternType, IJadescriptType providedInputType){
        MethodWriter m = w.method(Visibility.PUBLIC, false, false, "boolean", termID)
                .addParameter(w.param("java.lang.Object", "__objx"));
        m.getBody().addStatements(compileAdaptType(patternType.solve(providedInputType).compileToJavaTypeReference()));
        return m;
    }

    public <T> MethodWriter prepareMatcherMethod(
            PatternMatchInput<T, ?, ?> input,
            Function<PatternMatchInput<T,?,?>, PatternType> inferrer
    ){
        return prepareMatcherMethod(input.getTermID(), inferrer.apply(input), input.providedInputType());
    }

}
