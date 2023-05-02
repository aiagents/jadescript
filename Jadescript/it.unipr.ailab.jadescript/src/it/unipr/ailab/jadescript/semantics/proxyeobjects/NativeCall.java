package it.unipr.ailab.jadescript.semantics.proxyeobjects;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;

public class NativeCall extends ProxyEObject {

    public static final boolean IS_EXPRESSION = false;
    public static final boolean IS_STATEMENT = true;

    private final SimpleArgumentList simpleArguments;
    private final RValueExpression nameExpr;
    private final JavaFullyQualifiedName javaFQName;
    private final TypeExpression typeClause;
    private final boolean isStatement;


    private NativeCall(
        EObject input,
        SimpleArgumentList simpleArguments,
        RValueExpression nameExpr,
        JavaFullyQualifiedName javaFQName,
        TypeExpression typeClause,
        boolean isStatement
    ) {
        super(input);
        this.simpleArguments = simpleArguments;
        this.nameExpr = nameExpr;
        this.javaFQName = javaFQName;
        this.typeClause = typeClause;
        this.isStatement = isStatement;
    }


    public static Maybe<NativeCall> fromExpression(
        Maybe<NativeExpression> input
    ) {
        return input.__(i -> new NativeCall(
            i,
            i.getSimpleArguments(),
            i.getNameExpr(),
            i.getJavaFQName(),
            i.getType(),
            IS_EXPRESSION
        ));
    }


    public static Maybe<NativeCall> fromStatement(
        Maybe<ProcedureCallStatement> input
    ) {
        return input.__(i -> new NativeCall(
            i,
            i.getSimpleArgs(),
            i.getNameExpr(),
            i.getJavaFQName(),
            null,
            IS_STATEMENT
        ));
    }


    public SimpleArgumentList getSimpleArguments() {
        return simpleArguments;
    }


    public RValueExpression getNameExpr() {
        return nameExpr;
    }


    public boolean isResolveDynamically() {
        return nameExpr != null;
    }


    public TypeExpression getType() {
        return typeClause;
    }


    public boolean hasTypeClause() {
        return typeClause != null;
    }


    public JavaFullyQualifiedName getJavaFQName() {
        return javaFQName;
    }


    public boolean isStatement() {
        return isStatement;
    }

}
