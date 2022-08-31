package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ActivateStatement;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociationComputer;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.BoundedTypeArgument;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.UserDefinedBehaviourType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import jade.core.behaviours.Behaviour;
import jadescript.core.behaviours.OneShot;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.maybe.Maybe.of;

/**
 * Created on 09/03/18.
 *
 * @author Giuseppe Petrosino - giuseppe.petrosino@studenti.unipr.it
 */
@Singleton
public class ActivateStatementSemantics extends StatementSemantics<ActivateStatement> {


    public ActivateStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<ActivateStatement> input) {
        List<ExpressionSemantics.SemanticsBoundToExpression<?>> result = new ArrayList<>();
        input.__(ActivateStatement::getExpression).safeDo(exprSafe -> {
            result.add(new ExpressionSemantics.SemanticsBoundToExpression<>(
                    module.get(RValueExpressionSemantics.class), of(exprSafe)
            ));
        });
        return result;
    }

    @Override
    public void validate(Maybe<ActivateStatement> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        Maybe<RValueExpression> expr = input.__(ActivateStatement::getExpression);
        Maybe<RValueExpression> period = input.__(ActivateStatement::getPeriod);
        Maybe<RValueExpression> delay = input.__(ActivateStatement::getDelay);
        Maybe<RValueExpression> start = input.__(ActivateStatement::getStartTime);
        InterceptAcceptor exprValidations = new InterceptAcceptor(acceptor);
        module.get(RValueExpressionSemantics.class).validate(expr, exprValidations);
        IJadescriptType exprType = module.get(RValueExpressionSemantics.class).inferType(expr);
        if (!exprValidations.thereAreErrors()) {
            module.get(ValidationHelper.class).assertExpectedType(Behaviour.class, exprType,
                    "InvalidBehaviourExpressionType",
                    expr,
                    acceptor
            );

        }

        module.get(ValidationHelper.class).assertCanUseAgentReference(expr, acceptor);
        final TypeHelper th = module.get(TypeHelper.class);
        if (input.isPresent()) {
            final Optional<IJadescriptType> agentType = module.get(ContextManager.class).currentContext().actAs(
                    AgentAssociated.class
            ).findFirst().flatMap(agentAssociated ->
                    agentAssociated.computeAllAgentAssociations().sorted()
                            .findFirst()
                            .map(AgentAssociation::getAgent)
            );


            if (agentType.isPresent()
                    && exprType instanceof UserDefinedBehaviourType
                    && !exprValidations.thereAreErrors()) {
                final IJadescriptType forAgentType = ((UserDefinedBehaviourType) exprType).getForAgentType();
                module.get(ValidationHelper.class).assertion(
                        forAgentType.isAssignableFrom(agentType.get()),
                        "InvalidBehaviourActivation",
                        "An agent of type '" + agentType.get().getJadescriptName() + "' can not activate a behaviour " +
                                "designed for agents of type '" + forAgentType.getJadescriptName() + "'.",
                        expr,
                        acceptor
                );
            }
        }

        module.get(RValueExpressionSemantics.class).validate(period, exprValidations);
        if (!exprValidations.thereAreErrors()) {
            IJadescriptType periodType = module.get(RValueExpressionSemantics.class).inferType(period);
            module.get(ValidationHelper.class).assertExpectedType(
                    th.DURATION,
                    periodType,
                    "InvalidPeriodType",
                    period,
                    acceptor
            );
        }

        module.get(ValidationHelper.class).assertion(
                Util.implication(period.isPresent(), !th.isAssignable(OneShot.class, exprType)),
                "InvalidEveryClause",
                "Can not apply 'every' clause to the activation of a one-shot behaviour",
                period,
                acceptor
        );

        if (delay.isPresent()) {
            module.get(RValueExpressionSemantics.class).validate(delay, exprValidations);
            if (!exprValidations.thereAreErrors()) {
                module.get(ValidationHelper.class).assertExpectedType(
                        th.DURATION,
                        module.get(RValueExpressionSemantics.class).inferType(delay),
                        "InvalidDelayType",
                        delay,
                        acceptor
                );
            }
        }

        if (start.isPresent()) {
            module.get(RValueExpressionSemantics.class).validate(start, exprValidations);
            if (!exprValidations.thereAreErrors()) {
                module.get(ValidationHelper.class).assertExpectedType(
                        th.TIMESTAMP,
                        module.get(RValueExpressionSemantics.class).inferType(start),
                        "InvalidDelayType",
                        start,
                        acceptor
                );
            }
        }


    }

    @Override
    public List<BlockWriterElement> compileStatement(Maybe<ActivateStatement> input) {
        List<BlockWriterElement> result = new ArrayList<>();
        result.add(w.commentStmt("Activate statement"));
        Maybe<RValueExpression> expr = input.__(ActivateStatement::getExpression);
        Maybe<RValueExpression> period = input.__(ActivateStatement::getPeriod);
        Maybe<RValueExpression> delay = input.__(ActivateStatement::getDelay);
        Maybe<RValueExpression> start = input.__(ActivateStatement::getStartTime);
        String methodName = "activate";
        List<ExpressionWriter> params = new ArrayList<>();
        params.add(w.expr(THE_AGENT+"()"));
        if (delay.isPresent()) {
            methodName += "_after";
            params.add(w.expr(module.get(RValueExpressionSemantics.class).compile(delay).orElse("")));
        }

        if (start.isPresent()) {
            methodName += "_at";
            params.add(w.expr(module.get(RValueExpressionSemantics.class).compile(start).orElse("")));
        }

        if (period.isPresent()) {
            methodName += "_every";
            params.add(w.expr(module.get(RValueExpressionSemantics.class).compile(period).orElse("")));
        }


        result.add(w.callStmnt(
                module.get(RValueExpressionSemantics.class).compile(expr).orElse("") + "." + methodName,
                params
        ));
        return result;
    }


}
