package it.unipr.ailab.sonneteer;

import it.unipr.ailab.sonneteer.classmember.FieldWriter;
import it.unipr.ailab.sonneteer.classmember.MethodSignatureWriter;
import it.unipr.ailab.sonneteer.classmember.MethodWriter;
import it.unipr.ailab.sonneteer.classmember.ParameterWriter;
import it.unipr.ailab.sonneteer.comment.DocumentationCommentWriter;
import it.unipr.ailab.sonneteer.comment.MultilineCommentWriter;
import it.unipr.ailab.sonneteer.comment.SingleLineCommentWriter;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.expression.LambdaWithBlockWriter;
import it.unipr.ailab.sonneteer.expression.MethodCallExpressionWriter;
import it.unipr.ailab.sonneteer.expression.SimpleExpressionWriter;
import it.unipr.ailab.sonneteer.file.FileWriter;
import it.unipr.ailab.sonneteer.file.ImportWriter;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;
import it.unipr.ailab.sonneteer.statement.*;
import it.unipr.ailab.sonneteer.statement.controlflow.ForEachWriter;
import it.unipr.ailab.sonneteer.statement.controlflow.IfStatementWriter;
import it.unipr.ailab.sonneteer.statement.controlflow.TryCatchWriter;
import it.unipr.ailab.sonneteer.statement.controlflow.WhileStatementWriter;
import it.unipr.ailab.sonneteer.type.ClassDeclarationWriter;
import it.unipr.ailab.sonneteer.type.IClassDeclarationWriter;
import it.unipr.ailab.sonneteer.type.ITypeDeclarationWriter;
import it.unipr.ailab.sonneteer.type.InterfaceDeclarationWriter;

import java.util.List;

public class WriterFactory {

    private static WriterFactory instance = null;
    public final ExpressionWriter False = this.expr("false");
    public final ExpressionWriter True = this.expr("true");
    public final ExpressionWriter Null = this.expr("null");


    private WriterFactory() {
    } //don't use ctor


    public static WriterFactory getInstance() {
        if (instance == null) {
            instance = new WriterFactory();
        }
        return instance;
    }


    public LocalClassStatementWriter localClass(String name) {
        return new LocalClassStatementWriter(name);
    }


    public IClassDeclarationWriter clas(
        Visibility visibility,
        boolean isFinal,
        boolean isStatic,
        String name
    ) {
        return new ClassDeclarationWriter(visibility, isFinal, isStatic, name);
    }


    public InterfaceDeclarationWriter interf(
        Visibility visibility,
        boolean isFinal,
        boolean isStatic,
        String name
    ) {
        return new InterfaceDeclarationWriter(
            visibility,
            isFinal,
            isStatic,
            name
        );
    }


    public FileWriter file(
        String name,
        String packageName,
        ITypeDeclarationWriter typeDeclarationPoet
    ) {
        return new FileWriter(name, packageName, typeDeclarationPoet);
    }


    public FieldWriter field(
        Visibility visibility, boolean isStatic, boolean isFinal,
        String type, String name
    ) {
        return new FieldWriter(visibility, isStatic, isFinal, type, name);
    }


    public FieldWriter field(
        Visibility visibility, boolean isStatic, boolean isFinal,
        String type, String name, ExpressionWriter initExpression
    ) {
        return new FieldWriter(
            visibility,
            isStatic,
            isFinal,
            type,
            name,
            initExpression
        );
    }


    public MethodWriter method(
        Visibility visibility, boolean isFinal, boolean isStatic,
        String type, String name
    ) {
        return new MethodWriter(visibility, isFinal, isStatic, type, name);
    }


    public MethodWriter method(
        Visibility visibility, boolean isFinal, boolean isStatic,
        String type, String name, BlockWriter body
    ) {
        MethodWriter result = new MethodWriter(
            visibility,
            isFinal,
            isStatic,
            type,
            name
        );
        result.setBody(body);
        return result;
    }


    public MethodSignatureWriter method(String type, String name) {
        return new MethodSignatureWriter(type, name);
    }


    public ParameterWriter param(String type, String name) {
        return new ParameterWriter(type, name);
    }


    public SingleLineCommentWriter comment(String text) {
        return new SingleLineCommentWriter(text);
    }


    public MultilineCommentWriter multiComment(String firstLine) {
        return new MultilineCommentWriter(firstLine);
    }


    public DocumentationCommentWriter docComment(String firstLine) {
        return new DocumentationCommentWriter(firstLine);
    }


    public MethodCallExpressionWriter callExpr(
        String methodName,
        ExpressionWriter... parameters
    ) {
        return new MethodCallExpressionWriter(methodName, parameters);
    }

    public MethodCallExpressionWriter callExpr(
        String methodName,
        List<ExpressionWriter> parameters
    ) {
        return new MethodCallExpressionWriter(methodName, parameters);
    }


    public SimpleExpressionWriter expr(String expr) {
        return new SimpleExpressionWriter(expr);
    }


    public SimpleExpressionWriter stringLiteral(String expr) {
        return new SimpleExpressionWriter("\"" + expr + "\"");
    }


    public ImportWriter importDec(String what) {
        return new ImportWriter(what);
    }


    public ForEachWriter foreach(
        String varType,
        String varName,
        ExpressionWriter iterable,
        StatementWriter body
    ) {
        return new ForEachWriter(varType, varName, iterable, body);
    }


    public SimpleStatementWriter simpleStmt(String stmt) {
        return new SimpleStatementWriter(stmt);
    }


    public IfStatementWriter ifStmnt(
        ExpressionWriter condition,
        BlockWriter ifBranch
    ) {
        return new IfStatementWriter(condition, ifBranch);
    }


    public TryCatchWriter tryCatch(BlockWriter tryBranch) {
        return new TryCatchWriter(tryBranch);
    }


    public WhileStatementWriter whileStmnt(
        ExpressionWriter condition,
        StatementWriter body
    ) {
        return new WhileStatementWriter(condition, body);
    }


    public AssignmentWriter assign(
        String leftSide,
        ExpressionWriter rightSide
    ) {
        return new AssignmentWriter(leftSide, rightSide);
    }


    public BlockWriter block() {
        return new BlockWriter();
    }


    public BreakStatementWriter breakStmnt() {
        return new BreakStatementWriter();
    }


    public SingleLineStatementCommentWriter commentStmt(String comment) {
        return new SingleLineStatementCommentWriter(comment);
    }


    public BreakStatementWriter continueStmnt() {
        return new BreakStatementWriter(true);
    }


    public MethodCallStatementWriter callStmnt(
        String methodName,
        ExpressionWriter... params
    ) {
        return new MethodCallStatementWriter(methodName, params);
    }


    public MethodCallStatementWriter callStmnt(
        String methodName,
        List<ExpressionWriter> params
    ) {
        return new MethodCallStatementWriter(methodName, params);
    }


    public ReturnStatementWriter returnStmnt(ExpressionWriter expr) {
        return new ReturnStatementWriter(expr);
    }


    public ThrowStatementWriter throwStmt(ExpressionWriter expr) {
        return new ThrowStatementWriter(expr);
    }


    public ReturnStatementWriter returnStmnt() {
        return new ReturnStatementWriter();
    }


    public VariableDeclarationWriter variable(String type, String name) {
        return new VariableDeclarationWriter(type, name);
    }


    public VariableDeclarationWriter variable(
        String type,
        String name,
        ExpressionWriter init
    ) {
        return new VariableDeclarationWriter(type, name, init);
    }


    public LambdaWithBlockWriter blockLambda() {
        return new LambdaWithBlockWriter();
    }


}
