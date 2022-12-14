package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.NamedArgumentList;
import it.unipr.ailab.jadescript.jadescript.ProcedureCallStatement;
import it.unipr.ailab.jadescript.jadescript.SimpleArgumentList;
import it.unipr.ailab.jadescript.semantics.MethodInvocationSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.MethodCall;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import it.unipr.ailab.sonneteer.statement.SingleLineStatementCommentWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created on 29/04/18.
 */
@Singleton
public class ProcedureCallStatementSemantics extends StatementSemantics<ProcedureCallStatement> {


    public ProcedureCallStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<BlockWriterElement> compileStatement(Maybe<ProcedureCallStatement> input) {

        if (input.__(ProcedureCallStatement::isIsNothing).extract(Maybe.nullAsTrue)) {
            return Collections.singletonList(new SingleLineStatementCommentWriter("do nothing;"));
        } else {
            //writes a java method call statement
            Maybe<String> name = input.__(ProcedureCallStatement::getName);
            Maybe<SimpleArgumentList> simpleArgs = input.__(ProcedureCallStatement::getSimpleArgs);
            Maybe<NamedArgumentList> namedArgs = input.__(ProcedureCallStatement::getNamedArgs);

            return Collections.singletonList(w.simplStmt(
                    module.get(MethodInvocationSemantics.class).compile(MethodCall.methodCall(
                            input,
                            name,
                            simpleArgs,
                            namedArgs,
                            true
                    ))
            ));
        }
    }


    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(
            Maybe<ProcedureCallStatement> input
    ) {
        Maybe<String> name = input.__(ProcedureCallStatement::getName);
        Maybe<SimpleArgumentList> simpleArgs = input.__(ProcedureCallStatement::getSimpleArgs);
        Maybe<NamedArgumentList> namedArgs = input.__(ProcedureCallStatement::getNamedArgs);
        return new ArrayList<>(module.get(MethodInvocationSemantics.class).getSubExpressions(
                MethodCall.methodCall(input, name, simpleArgs, namedArgs, true)
        ));
    }

    @Override
    public void validate(Maybe<ProcedureCallStatement> input, ValidationMessageAcceptor acceptor) {
        if (input.__(ProcedureCallStatement::isIsNothing).extract(Maybe.nullAsTrue)) {
            //do nothing
        } else {
            Maybe<String> name = input.__(ProcedureCallStatement::getName);
            Maybe<SimpleArgumentList> simpleArgs = input.__(ProcedureCallStatement::getSimpleArgs);
            Maybe<NamedArgumentList> namedArgs = input.__(ProcedureCallStatement::getNamedArgs);
            module.get(MethodInvocationSemantics.class).validate(MethodCall.methodCall(input, name, simpleArgs, namedArgs, true), acceptor);
        }
    }


}
