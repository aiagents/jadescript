package it.unipr.ailab.jadescript.semantics.statement;

import it.unipr.ailab.jadescript.jadescript.PutbackStatement;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PutBackStatementSemantics extends StatementSemantics<PutbackStatement> {
    public PutBackStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<PutbackStatement> input) {
        return Collections.singletonList(new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                input.__(PutbackStatement::getMessage)
        ));
    }

    @Override
    public void compileStatement(Maybe<PutbackStatement> input, StatementCompilationOutputAcceptor acceptor) {
        if(input!=null) {
            input.safeDo(inputSafe -> {
                acceptor.accept(
                        w.callStmnt(THE_AGENT + "().__putBackMessage", w.expr(
                                        module.get(RValueExpressionSemantics.class)
                                                .compile(input.__(PutbackStatement::getMessage), acceptor).orElse("")
                                )
                        )
                );
            });
        }
    }

    @Override
    public void validate(Maybe<PutbackStatement> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);

        Maybe<RValueExpression> message = input.__(PutbackStatement::getMessage);
        module.get(RValueExpressionSemantics.class).validate(message, subValidation);

        if(!subValidation.thereAreErrors()){
            IJadescriptType messageType = module.get(RValueExpressionSemantics.class).inferType(message);
            module.get(ValidationHelper.class).assertExpectedType(
                    module.get(TypeHelper.class).ANYMESSAGE,
                    messageType,
                    "InvalidPutbackStatement",
                    message,
                    acceptor
            );
        }
    }
}
