package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.NamedArgumentList;
import it.unipr.ailab.jadescript.jadescript.ProcedureCallStatement;
import it.unipr.ailab.jadescript.jadescript.SimpleArgumentList;
import it.unipr.ailab.jadescript.semantics.MethodCallSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.MethodCall;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Created on 29/04/18.
 */
@Singleton
public class ProcedureCallStatementSemantics extends StatementSemantics<ProcedureCallStatement> {


    public ProcedureCallStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public StaticState compileStatement(Maybe<ProcedureCallStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor) {

        if (input.__(ProcedureCallStatement::isIsNothing).extract(Maybe.nullAsTrue)) {
            acceptor.accept(w.commentStmt("do nothing;"));
        } else {
            //writes a java method call statement
            Maybe<String> name = input.__(ProcedureCallStatement::getName);
            Maybe<SimpleArgumentList> simpleArgs = input.__(ProcedureCallStatement::getSimpleArgs);
            Maybe<NamedArgumentList> namedArgs = input.__(ProcedureCallStatement::getNamedArgs);

            acceptor.accept(w.simpleStmt(
                    module.get(MethodCallSemantics.class).compile(
                            MethodCall.methodCall(
                                    input,
                                    name,
                                    simpleArgs,
                                    namedArgs,
                                    true
                            ), ,
                            acceptor
                    ).toString()
            ));
        }
    }


    @Override
    public Stream<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(
            Maybe<ProcedureCallStatement> input
    ) {
        Maybe<String> name = input.__(ProcedureCallStatement::getName);
        Maybe<SimpleArgumentList> simpleArgs = input.__(ProcedureCallStatement::getSimpleArgs);
        Maybe<NamedArgumentList> namedArgs = input.__(ProcedureCallStatement::getNamedArgs);
        return new ArrayList<>(module.get(MethodCallSemantics.class).getSubExpressions(
                MethodCall.methodCall(input, name, simpleArgs, namedArgs, true)
        ));
    }

    @Override
    public StaticState validateStatement(Maybe<ProcedureCallStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor) {
        if (input.__(ProcedureCallStatement::isIsNothing).extract(Maybe.nullAsTrue)) {
            //do nothing
        } else {
            Maybe<String> name = input.__(ProcedureCallStatement::getName);
            Maybe<SimpleArgumentList> simpleArgs = input.__(ProcedureCallStatement::getSimpleArgs);
            Maybe<NamedArgumentList> namedArgs = input.__(ProcedureCallStatement::getNamedArgs);
            module.get(MethodCallSemantics.class).validate(MethodCall.methodCall(input, name, simpleArgs, namedArgs, true), , acceptor);
        }
    }


}
