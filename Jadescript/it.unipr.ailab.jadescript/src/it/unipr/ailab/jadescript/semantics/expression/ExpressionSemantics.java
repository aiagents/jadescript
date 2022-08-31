package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.semantics.Semantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Created on 27/12/16.
 *
 * @author Giuseppe Petrosino - giuseppe.petrosino@studenti.unipr.it
 */
@Singleton
public abstract class ExpressionSemantics<T> extends Semantics<T> {

    public ExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }



    public static class SemanticsBoundToExpression<T extends EObject>{
        private final ExpressionSemantics<T> semantics;
        private final Maybe<T> input;

        public SemanticsBoundToExpression(ExpressionSemantics<T> semantics, Maybe<T> input) {
            this.semantics = semantics;
            this.input = input;
        }

        public ExpressionSemantics<T> getSemantics() {
            return semantics;
        }

        public Maybe<T> getInput() {
            return input;
        }

        public void doWithBinding(BiConsumer<ExpressionSemantics<T>, Maybe<T>> consumer) {
            consumer.accept(semantics, input);
        }

        public <R> R doWithBinding(BiFunction<ExpressionSemantics<T>, Maybe<T>, R> function) {
            return function.apply(semantics, input);
        }
    }

    public abstract List<ExpressionSemantics.SemanticsBoundToExpression<?>> getSubExpressions(Maybe<T> input);

    public List<StatementWriter> getAuxiliaryStatementsLocally(Maybe<T> input) {
        return Collections.emptyList();
    }

    public abstract Maybe<String> compile(Maybe<T> input);

    public abstract IJadescriptType inferType(Maybe<T> input);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ExpressionTypeKB extractFlowTypeTruths(Maybe<T> input) {
        if (mustTraverse(input)) {
            Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                //noinspection unchecked,rawtypes
                return traversed.get().getSemantics().extractFlowTypeTruths((Maybe)traversed.get().getInput());
            }
        }
        return ExpressionTypeKB.empty();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<String> extractPropertyChain(Maybe<T> input) {
        if (mustTraverse(input)) {
            Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                //noinspection unchecked,rawtypes
                return traversed.get().getSemantics().extractPropertyChain((Maybe)traversed.get().getInput());
            }
        }

        return Collections.emptyList();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean isAlwaysPure(Maybe<T> input) {
        if (mustTraverse(input)) {
            Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                //noinspection unchecked,rawtypes
                return traversed.get().getSemantics().isAlwaysPure((Maybe)traversed.get().getInput());
            }
        }
        return true;//expressions are mostly pure
    }

    public abstract boolean mustTraverse(Maybe<T> input);

    public abstract Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traverse(Maybe<T> input);


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<? extends StatementWriter> generateAuxiliaryStatements(Maybe<T> input) {
        if (mustTraverse(input)) {
            Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                //noinspection unchecked,rawtypes
                return traversed.get().getSemantics().generateAuxiliaryStatements((Maybe)traversed.get().getInput());
            }
        }

        List<StatementWriter> result = new ArrayList<>(this.getAuxiliaryStatementsLocally(input));


        getSubExpressions(input).forEach(bound -> {
            //noinspection unchecked,rawtypes
            result.addAll(bound.getSemantics().generateAuxiliaryStatements((Maybe)bound.getInput()));
        });

        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <R> List<? extends R> collectFromAllLeaves(
            Maybe<T> input,
            BiFunction<Maybe<?>, ExpressionSemantics<?>,  R> function
    ) {
    	if(mustTraverse(input)){
            Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()){
                //noinspection unchecked,rawtypes
                return traversed.get().getSemantics().collectFromAllLeaves((Maybe)traversed.get().getInput(), function);
            }
        }

        List<R> result = new ArrayList<>();
        result.add(function.apply(input, this));

        getSubExpressions(input).forEach(bounds -> {
            //noinspection unchecked,rawtypes
            result.addAll(bounds.getSemantics().collectFromAllLeaves((Maybe)bounds.getInput(), function));
        });

        return result;
    }

    public void validateUsageAsWhenExpression(Maybe<T> input, Maybe<? extends EObject> refObject, ValidationMessageAcceptor acceptor) {
        module.get(ValidationHelper.class).assertion(isAlwaysPure(input),
                "InvalidWhenExpression",
                "Only expressions without side effects can be used in a when expression",
                refObject,
                acceptor
        );
    }


}
