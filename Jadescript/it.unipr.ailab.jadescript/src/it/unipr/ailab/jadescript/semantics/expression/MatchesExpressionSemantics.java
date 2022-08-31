package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Matches;
import it.unipr.ailab.jadescript.jadescript.Pattern;
import it.unipr.ailab.jadescript.jadescript.UnaryPrefix;
import it.unipr.ailab.jadescript.semantics.PatternMatchingSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c2feature.HandlerWhenExpressionContext;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.FlowTypeInferringTerm;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.PatternMatchRequest;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

/**
 * Created on 2019-08-18.
 */
@Singleton
public class MatchesExpressionSemantics extends ExpressionSemantics<Matches> {


    public MatchesExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<Matches> input) {
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }

        return Collections.singletonList(
                unary.extract(x -> new SemanticsBoundToExpression<>(module.get(UnaryPrefixExpressionSemantics.class), x)));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Maybe<String> compile(Maybe<Matches> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                //noinspection rawtypes
                return traversed.get().getSemantics().compile((Maybe) traversed.get().getInput());
            }
        }
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        return module.get(PatternMatchingSemantics.class).compileMatchesExpression(input, unary);
    }

    private boolean isInWhenExpression() {
        return module.get(ContextManager.class).currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst().isPresent();
    }

    @Override
    public List<? extends StatementWriter> generateAuxiliaryStatements(Maybe<Matches> input) {
        if (mustTraverse(input) || input.isNothing()) {
            return super.generateAuxiliaryStatements(input);
        }
        final Maybe<Pattern> pattern = input.__(Matches::getPattern);
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        return module.get(PatternMatchingSemantics.class).generateAuxiliaryStatements(
                PatternMatchRequest.patternMatchRequest(input, pattern, unary, isInWhenExpression())
        );
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public IJadescriptType inferType(Maybe<Matches> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return traversed.get().getSemantics().inferType((Maybe) traversed.get().getInput());
            }
        }

        return module.get(TypeHelper.class).BOOLEAN;
    }

    @Override
    public boolean mustTraverse(Maybe<Matches> input) {
        final Maybe<Pattern> pattern = input.__(Matches::getPattern);
        final boolean isMatches = input.__(Matches::isMatches).extract(nullAsFalse);
        return !isMatches || pattern.isNothing();
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<Matches> input) {
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        return Optional.of(new SemanticsBoundToExpression<>(module.get(UnaryPrefixExpressionSemantics.class), unary));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void validate(Maybe<Matches> input, ValidationMessageAcceptor acceptor) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);

            if (traversed.isPresent()) {
                traversed.get().getSemantics().validate((Maybe) traversed.get().getInput(), acceptor);
                return;
            }
        }

        final Maybe<Pattern> pattern = input.__(Matches::getPattern);
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        module.get(PatternMatchingSemantics.class).validate(
                PatternMatchRequest.patternMatchRequest(
                        input,
                        pattern,
                        unary,
                        isInWhenExpression()
                ),
                acceptor
        );
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ExpressionTypeKB extractFlowTypeTruths(Maybe<Matches> input) {

        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                //noinspection rawtypes
                return traversed.get().getSemantics().extractFlowTypeTruths((Maybe) traversed.get().getInput());
            }
        }

        final Maybe<Pattern> pattern = input.__(Matches::getPattern);
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        ExpressionTypeKB subKb = module.get(UnaryPrefixExpressionSemantics.class).extractFlowTypeTruths(unary);
        List<String> strings = module.get(UnaryPrefixExpressionSemantics.class).extractPropertyChain(unary);


        pattern.safeDo(patternSafe -> {
            subKb.add(FlowTypeInferringTerm.of(inferPatternType(patternSafe)
                    .orElse(module.get(TypeHelper.class).ANY)), strings);
        });
        return subKb;
    }


    public Maybe<IJadescriptType> inferPatternType(Pattern pattern) {
        return module.get(PatternMatchingSemantics.class).inferPatternType(pattern);
    }

}
