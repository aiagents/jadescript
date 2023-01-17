package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.NamedArgumentList;
import it.unipr.ailab.jadescript.jadescript.ProcedureCallStatement;
import it.unipr.ailab.jadescript.jadescript.SimpleArgumentList;
import it.unipr.ailab.jadescript.semantics.CallSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.Call;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.stream.Stream;

/**
 * Created on 29/04/18.
 */
@Singleton
public class ProcedureCallStatementSemantics
    extends StatementSemantics<ProcedureCallStatement> {


    public ProcedureCallStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<ProcedureCallStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {

        if (input.__(ProcedureCallStatement::isIsNothing)
            .extract(Maybe.nullAsTrue)) {
            acceptor.accept(w.commentStmt("do nothing;"));
            return state;
        }

        Maybe<String> name =
            input.__(ProcedureCallStatement::getName);
        Maybe<SimpleArgumentList> simpleArgs =
            input.__(ProcedureCallStatement::getSimpleArgs);
        Maybe<NamedArgumentList> namedArgs =
            input.__(ProcedureCallStatement::getNamedArgs);

        final CallSemantics mcs =
            module.get(CallSemantics.class);

        final Maybe<Call> mcInput = Call.call(
            input,
            name,
            simpleArgs,
            namedArgs,
            Call.IS_PROCEDURE
        );

        acceptor.accept(w.simpleStmt(
            mcs.compile(mcInput, state, acceptor)
        ));

        return mcs.advance(mcInput, state);
    }


    @Override
    public Stream<SemanticsBoundToExpression<?>>
    includedExpressions(Maybe<ProcedureCallStatement> input) {
        Maybe<String> name =
            input.__(ProcedureCallStatement::getName);
        Maybe<SimpleArgumentList> simpleArgs =
            input.__(ProcedureCallStatement::getSimpleArgs);
        Maybe<NamedArgumentList> namedArgs =
            input.__(ProcedureCallStatement::getNamedArgs);
        return module.get(CallSemantics.class).getSubExpressions(
            Call.call(
                input,
                name,
                simpleArgs,
                namedArgs,
                Call.IS_PROCEDURE
            )
        );
    }


    @Override
    public StaticState validateStatement(
        Maybe<ProcedureCallStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input.__(ProcedureCallStatement::isIsNothing)
            .extract(Maybe.nullAsTrue)) {
            //do nothing
            return state;
        }
        Maybe<String> name = input.__(ProcedureCallStatement::getName);
        Maybe<SimpleArgumentList> simpleArgs = input.__(
            ProcedureCallStatement::getSimpleArgs);
        Maybe<NamedArgumentList> namedArgs =
            input.__(ProcedureCallStatement::getNamedArgs);
        final CallSemantics mcs = module.get(
            CallSemantics.class);
        final Maybe<Call> mcInput = Call.call(
            input,
            name,
            simpleArgs,
            namedArgs,
            Call.IS_PROCEDURE
        );
        boolean callCheck = mcs.validate(mcInput, state, acceptor);

        if (callCheck == INVALID) {
            return state;
        }

        return mcs.advance(mcInput, state);
    }


}
