package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.AtomExpr;
import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.jadescript.Trailer;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.AtomWithTrailersExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.PrimaryExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;

/**
 * Created on 21/08/18.
 *
 */
@Singleton
public class AtomWithTrailersStatementSemantics extends StatementSemantics<AtomExpr> {


    public AtomWithTrailersStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<BlockWriterElement> compileStatement(Maybe<AtomExpr> input) {
        return Collections.singletonList(w.simplStmt(module.get(AtomWithTrailersExpressionSemantics.class).compile(input).orElse("")));
    }

    @Override
    public void validate(Maybe<AtomExpr> input, ValidationMessageAcceptor acceptor) {
        if(input ==null)return;
        Maybe<Primary> atom = input.__(AtomExpr::getAtom);
        Maybe<EList<Trailer>> trailers = input.__(AtomExpr::getTrailers);
        if(trailers.__(List::isEmpty).extract(Maybe.nullAsTrue)){
            if (atom.isPresent()) {
                module.get(PrimaryExpressionSemantics.class).syntacticValidateStatement(atom, acceptor);
            } else {
                input.safeDo(inputSafe->{

                    acceptor.acceptError("Invalid statement",
                            inputSafe,
                            null,
                            ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                            ISSUE_CODE_PREFIX+"InvalidStatement");
                });
            }
        }else{
            //check if it is valid as statement (can only be a method call)
            Maybe<Trailer> lastTrailer = trailers.__(trailersSafe->trailersSafe.get(trailersSafe.size()-1));
            InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
            module.get(ValidationHelper.class).assertion(lastTrailer.__(Trailer::isIsACall),
                    "InvalidStatement",
                    "Not a statement",
                    input,
                    interceptAcceptor);


            if(!interceptAcceptor.thereAreErrors()){
                //check as common rExp
                module.get(AtomWithTrailersExpressionSemantics.class).validate(input, acceptor);
            }

        }
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<AtomExpr> input) {
        return Collections.singletonList(new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(AtomWithTrailersExpressionSemantics.class), input));
    }
}
