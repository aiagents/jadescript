package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.LogicalAnd;
import it.unipr.ailab.jadescript.jadescript.LogicalOr;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.nothing;

/**
 * Created on 28/12/16.
 *
 * @author Giuseppe Petrosino - giuseppe.petrosino@studenti.unipr.it
 */
@Singleton
public class LogicalOrExpressionSemantics extends ExpressionSemantics<LogicalOr> {


    public LogicalOrExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<LogicalOr> input) {
        Maybe<EList<LogicalAnd>> logicalAnds = input.__(LogicalOr::getLogicalAnd);
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }

        return Maybe.toListOfMaybes(logicalAnds).stream()
                .map(x -> new SemanticsBoundToExpression<>(module.get(LogicalAndExpressionSemantics.class), x))
                .collect(Collectors.toList());
    }

    @Override
    public Maybe<String> compile(Maybe<LogicalOr> input) {
        if (input == null) return nothing();
        Maybe<EList<LogicalAnd>> logicalAnds = input.__(LogicalOr::getLogicalAnd);

        StringBuilder sb = new StringBuilder();
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(logicalAnds);
        for (int i = 0; i < ands.size(); i++) {
            Maybe<LogicalAnd> and = ands.get(i);
            if (i != 0) {
                sb.append(" || ");
            }
            sb.append(module.get(LogicalAndExpressionSemantics.class).compile(and).orElse(""));
        }
        return Maybe.of(sb.toString());
    }

    @Override
    public IJadescriptType inferType(Maybe<LogicalOr> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        Maybe<EList<LogicalAnd>> logicalAnds = input.__(LogicalOr::getLogicalAnd);
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(logicalAnds);
        if (ands.size() > 1) {
            return module.get(TypeHelper.class).BOOLEAN;
        } else if (ands.size() == 1) {
            return module.get(LogicalAndExpressionSemantics.class).inferType(ands.get(0));
        } else {
            return module.get(TypeHelper.class).ANY;
        }
    }

    @Override
    public ExpressionTypeKB extractFlowTypeTruths(Maybe<LogicalOr> input) {
        Maybe<EList<LogicalAnd>> logicalAnds = input.__(LogicalOr::getLogicalAnd);
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(logicalAnds);
        if (ands.size() < 1) {
            return ExpressionTypeKB.empty();
        } else if (ands.size() == 1) {
            return module.get(LogicalAndExpressionSemantics.class).extractFlowTypeTruths(ands.get(0));
        } else {
            ExpressionTypeKB t = module.get(LogicalAndExpressionSemantics.class).extractFlowTypeTruths(ands.get(0));
            for (int i = 1; i < ands.size(); i++) {
                ExpressionTypeKB t2 = module.get(LogicalAndExpressionSemantics.class).extractFlowTypeTruths(ands.get(i));
                t = module.get(TypeHelper.class).mergeByLUB(t, t2);
            }

            return t;
        }

    }

    @Override
    public boolean mustTraverse(Maybe<LogicalOr> input) {
        Maybe<EList<LogicalAnd>> logicalAnds = input.__(LogicalOr::getLogicalAnd);
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(logicalAnds);

        return ands.size() == 1;
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<LogicalOr> input) {
        if (mustTraverse(input)) {
            Maybe<EList<LogicalAnd>> logicalAnds = input.__(LogicalOr::getLogicalAnd);
            List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(logicalAnds);
            return Optional.of(new SemanticsBoundToExpression<>(module.get(LogicalAndExpressionSemantics.class),ands.get(0)));
        }

        return Optional.empty();
    }

    @Override
    public void validate(Maybe<LogicalOr> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        Maybe<EList<LogicalAnd>> logicalAnds = input.__(LogicalOr::getLogicalAnd);
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(logicalAnds);

        if (ands.size() == 1) {
            module.get(LogicalAndExpressionSemantics.class).validate(ands.get(0), acceptor);
            return;
        }

        if (ands.size() > 1) {
            for (int i = 0; i < ands.size(); i++) {
                Maybe<LogicalAnd> and = ands.get(i);
                InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);
                module.get(LogicalAndExpressionSemantics.class).validate(and, subValidation);
                if (!subValidation.thereAreErrors()) {
                    IJadescriptType type = module.get(LogicalAndExpressionSemantics.class).inferType(and);
                    module.get(ValidationHelper.class).assertExpectedType(Boolean.class, type,
                            "InvalidOperandType",
                            input,
                            JadescriptPackage.eINSTANCE.getLogicalOr_LogicalAnd(),
                            i,
                            acceptor);
                }
            }
        }

    }


}
