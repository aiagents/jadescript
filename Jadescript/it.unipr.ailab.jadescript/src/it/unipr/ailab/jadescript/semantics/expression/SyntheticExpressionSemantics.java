package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.maybe.Maybe.nothing;

/**
 * Created on 01/11/2018.
 */
@Singleton
public class SyntheticExpressionSemantics extends ExpressionSemantics<SyntheticExpression> {

    public SyntheticExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<SyntheticExpression> input) {
        return Collections.emptyList();
    }

    @Override
    public Maybe<String> compile(Maybe<SyntheticExpression> input) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        switch (type.toNullable()) {
            case AGENT_REFERENCE:
                return Maybe.of(THE_AGENT+"()");
            case CUSTOM:
                return customSemantics.compile();
            default:
                return nothing();
        }
    }

    @Override
    public IJadescriptType inferType(Maybe<SyntheticExpression> input) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        switch (type.toNullable()) {
            case AGENT_REFERENCE: {
                module.get(ContextManager.class).currentContext()
                        .searchAs(
                                AgentAssociated.class,
                                a -> a.computeAllAgentAssociations()
                                        .sorted()
                                        .map(AgentAssociation::getAgent)
                        )
                        .findFirst()
                        .orElseGet(() -> module.get(TypeHelper.class).ANY);
            }
            case CUSTOM:
                return customSemantics.inferType(module.get(TypeHelper.class));
            default:
                return module.get(TypeHelper.class).ANY;
        }
    }

    @Override
    public boolean mustTraverse(Maybe<SyntheticExpression> input) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        //noinspection SwitchStatementWithTooFewBranches
        switch (type.toNullable()) {
            case CUSTOM:
                return customSemantics.mustTraverse();
            default:
                return false;
        }
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<SyntheticExpression> input) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        //noinspection SwitchStatementWithTooFewBranches
        switch (type.toNullable()) {
            case CUSTOM:
                return customSemantics.traverse();
            default:
                return Optional.empty();
        }
    }

    @Override
    public void validate(Maybe<SyntheticExpression> input, ValidationMessageAcceptor acceptor) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        switch (type.toNullable()) {
            case AGENT_REFERENCE:
                module.get(ValidationHelper.class).assertCanUseAgentReference(input, acceptor);
                break;
            case CUSTOM:
                customSemantics.validate(acceptor);
                break;
            default:
                //do nothing
        }
    }
}
