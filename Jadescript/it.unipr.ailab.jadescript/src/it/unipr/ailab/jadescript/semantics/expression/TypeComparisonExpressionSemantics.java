package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RelationalComparison;
import it.unipr.ailab.jadescript.jadescript.TypeComparison;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.FlowTypeInferringTerm;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

/**
 * Created on 28/12/16.
 *
 * @author Giuseppe Petrosino - giuseppe.petrosino@studenti.unipr.it
 */
@Singleton
public class TypeComparisonExpressionSemantics extends ExpressionSemantics<TypeComparison> {


    public TypeComparisonExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<TypeComparison> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }
        final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);
        final Maybe<TypeExpression> type = input.__(TypeComparison::getType);

        return Arrays.asList(
                left.extract(x -> new SemanticsBoundToExpression<>(module.get(RelationalComparisonExpressionSemantics.class), x)),
                type.extract(x -> new SemanticsBoundToExpression<>(module.get(TypeExpressionSemantics.class), x))
        );
    }

    @Override
    public Maybe<String> compile(Maybe<TypeComparison> input) {
        if (input == null) return nothing();

        final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);
        final boolean isOp = input.__(TypeComparison::isIsOp).extract(nullAsFalse);
        final Maybe<TypeExpression> type = input.__(TypeComparison::getType);

        String result = module.get(RelationalComparisonExpressionSemantics.class).compile(left).orElse("");
        if (isOp) {
            IJadescriptType typeDesc = module.get(TypeExpressionSemantics.class).toJadescriptType(type);
            String compiledTypeExpression = typeDesc.compileToJavaTypeReference();
            if (module.get(TypeHelper.class).ONTOLOGY.isAssignableFrom(typeDesc)) {
                result = THE_AGENTCLASS + ".__checkOntology(" + result + ", " +
                        compiledTypeExpression + ".class, " +
                        compiledTypeExpression + ".getInstance())";
            } else {
                //attempt to do the "safest" version if possible (safest <=> no compiler errors in java generated code)
                if (!compiledTypeExpression.contains("<")) {
                    result = compiledTypeExpression + ".class.isInstance(" + result + ")";
                } else {
                    result = result + " instanceof " + compiledTypeExpression;
                }
            }
        }
        return Maybe.of(result);
    }

    @Override
    public IJadescriptType inferType(Maybe<TypeComparison> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);
        final boolean isOp = input.__(TypeComparison::isIsOp).extract(nullAsFalse);

        if (isOp) {
            return module.get(TypeHelper.class).BOOLEAN;
        }
        return module.get(RelationalComparisonExpressionSemantics.class).inferType(left);
    }

    @Override
    public boolean mustTraverse(Maybe<TypeComparison> input) {
        final boolean isOp = input.__(TypeComparison::isIsOp).extract(nullAsFalse);
        return !isOp;
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<TypeComparison> input) {
        if (mustTraverse(input)) {
            final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);

            return Optional.of(new SemanticsBoundToExpression<>(module.get(RelationalComparisonExpressionSemantics.class), left));
        }
        return Optional.empty();
    }

    @Override
    public void validate(Maybe<TypeComparison> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);
        final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);
        module.get(RelationalComparisonExpressionSemantics.class).validate(left, subValidation);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    public ExpressionTypeKB extractFlowTypeTruths(Maybe<TypeComparison> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                //noinspection unchecked,rawtypes
                return traversed.get().getSemantics().extractFlowTypeTruths((Maybe)traversed.get().getInput());
            }
        }

        final Maybe<RelationalComparison> left = input.__(TypeComparison::getRelationalComparison);
        final Maybe<TypeExpression> type = input.__(TypeComparison::getType);

        ExpressionTypeKB subKb = module.get(RelationalComparisonExpressionSemantics.class).extractFlowTypeTruths(left);
        List<String> strings = module.get(RelationalComparisonExpressionSemantics.class).extractPropertyChain(left);
        subKb.add(FlowTypeInferringTerm.of(module.get(TypeExpressionSemantics.class).toJadescriptType(type)), strings);
        return subKb;
    }
}
