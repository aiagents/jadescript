package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Literal;
import it.unipr.ailab.jadescript.jadescript.NativeExpression;
import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.SingleIdentifier;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.TupledExpressions;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.someStream;

/**
 * Created on 28/12/16.
 */
@Singleton
public class PrimaryExpressionSemantics
    extends AssignableExpressionSemantics<Primary> {

    public PrimaryExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<Primary> input
    ) {
        final MaybeList<RValueExpression> exprs =
            input.__toListNullsRemoved(Primary::getExprs);

        if (exprs.size() == 1) {
            return SemanticsUtils.buildStream(() -> exprs.get(0)
                .<SemanticsBoundToExpression<?>>extract(
                    x -> new SemanticsBoundToExpression<>(
                        module.get(RValueExpressionSemantics.class),
                        x
                    )));
        }
        return Stream.empty();
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<Primary> input,
        StaticState state
    ) {
        final MaybeList<RValueExpression> exprs =
            input.__toListNullsRemoved(Primary::getExprs);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        if (exprs.size() == 1) {
            return rves.describeExpression(exprs.get(0), state);
        } else {
            return nothing();
        }
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<Primary> input,
        StaticState state
    ) {
        final MaybeList<RValueExpression> exprs =
            input.__toListNullsRemoved(Primary::getExprs);

        if (exprs.size() == 1) {
            return module.get(RValueExpressionSemantics.class)
                .advance(exprs.get(0), state);
        } else {
            return state;
        }
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
        final MaybeList<RValueExpression> exprs =
            input.getPattern().__toListNullsRemoved(Primary::getExprs);

        if (exprs.size() == 1) {
            return module.get(RValueExpressionSemantics.class).assertDidMatch(
                input.replacePattern(exprs.get(0)),
                state
            );
        } else {
            return state;
        }
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<Primary> input,
        StaticState state
    ) {
        final MaybeList<RValueExpression> exprs =
            input.__toListNullsRemoved(Primary::getExprs);

        if (exprs.size() == 1) {
            return module.get(RValueExpressionSemantics.class)
                .assertReturnedTrue(exprs.get(0), state);
        } else {
            return state;
        }
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<Primary> input,
        StaticState state
    ) {
        final MaybeList<RValueExpression> exprs =
            input.__toListNullsRemoved(Primary::getExprs);

        if (exprs.size() == 1) {
            return module.get(RValueExpressionSemantics.class)
                .assertReturnedTrue(exprs.get(0), state);
        } else {
            return state;
        }
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
        final MaybeList<RValueExpression> exprs =
            input.getPattern().__toListNullsRemoved(Primary::getExprs);

        if (exprs.size() == 1) {
            return module.get(RValueExpressionSemantics.class)
                .advancePattern(
                    input.subPattern(
                        input.getProvidedInputType(),
                        __ -> exprs.get(0).toNullable(),
                        "__paren"
                    ),
                    state
                );
        } else {
            return state;
        }

    }


    @Override
    protected String compileInternal(
        Maybe<Primary> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {

        final MaybeList<RValueExpression> exprs =
            input.__toListNullsRemoved(Primary::getExprs);

        if (exprs.size() == 1) {
            final String result = module.get(RValueExpressionSemantics.class)
                .compile(
                    exprs.get(0),
                    state,
                    acceptor
                );
            return "(" + result + ")";
        }

        return "/*Unexpected input in PrimaryExpressionSemantics*/";
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<Primary> input,
        StaticState state
    ) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        if (input == null) {
            return builtins.any("");
        }

        final MaybeList<RValueExpression> exprs =
            input.__toListNullsRemoved(Primary::getExprs);

        if (exprs.size() == 1) {
            return module.get(RValueExpressionSemantics.class)
                .inferType(exprs.get(0), state);
        } else {
            return builtins.any("");
        }
    }


    @Override
    protected boolean mustTraverse(Maybe<Primary> input) {
        final boolean isPlaceholder = input.__(Primary::isPlaceholder)
            .orElse(false);
        final MaybeList<RValueExpression> exprs =
            input.__toListNullsRemoved(Primary::getExprs);

        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<NativeExpression> invoke =
            input.__(Primary::getNativeExpression);
        return exprs.size() > 1 // tuple
            || isPlaceholder
            || literal.isPresent()
            || identifier.isPresent()
            || invoke.isPresent();
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(Maybe<Primary> input) {
        final boolean isPlaceholder =
            input.__(Primary::isPlaceholder).orElse(false);
        final MaybeList<RValueExpression> exprs =
            input.__toListNullsRemoved(Primary::getExprs);

        final Maybe<TupledExpressions> tuple;

        if (exprs.size() > 1) {
            tuple = TupledExpressions.tupledExpressions(input);
        } else {
            tuple = nothing();
        }
        final Maybe<Literal> literal = input.__(Primary::getLiteral);
        final Maybe<String> identifier = input.__(Primary::getIdentifier);
        final Maybe<NativeExpression> invoke =
            input.__(Primary::getNativeExpression);
        if (mustTraverse(input)) {
            if (isPlaceholder) {
                return Optional.of(new SemanticsBoundToAssignableExpression<>(
                    module.get(PlaceholderExpressionSemantics.class),
                    input
                ));
            } else if (tuple.isPresent()) {
                return Optional.of(new SemanticsBoundToAssignableExpression<>(
                    module.get(TupleExpressionSemantics.class),
                    tuple
                ));
            } else if (literal.isPresent()) {
                return Optional.of(new SemanticsBoundToAssignableExpression<>(
                    module.get(LiteralExpressionSemantics.class),
                    literal
                ));
            } else if (identifier.isPresent()) {
                return Optional.of(new SemanticsBoundToAssignableExpression<>(
                    module.get(SingleIdentifierExpressionSemantics.class),
                    SingleIdentifier.singleIdentifier(identifier, input)
                ));
            } else if (invoke.isPresent()) {
                return Optional.of(new SemanticsBoundToAssignableExpression<>(
                    module.get(NativeExpressionSemantics.class),
                    invoke
                ));
            }
        }
        return Optional.empty();
    }


    @Override
    protected boolean validateInternal(
        Maybe<Primary> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return VALID;
        }
        final MaybeList<RValueExpression> exprs =
            input.__toListNullsRemoved(Primary::getExprs);


        if (exprs.size() == 1) {
            return module.get(RValueExpressionSemantics.class).validate(
                exprs.get(0),
                state,
                acceptor
            );
        }

        return VALID;
    }


    @Override
    public void compileAssignmentInternal(
        Maybe<Primary> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        // parenthesized expressions cannot be assigned
    }


    @Override
    protected IJadescriptType assignableTypeInternal(
        Maybe<Primary> input,
        StaticState state
    ) {
        return module.get(BuiltinTypeProvider.class).nothing("");
    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<Primary> input,
        IJadescriptType rightType,
        StaticState state
    ) {
        return state;
    }


    @Override
    public boolean validateAssignmentInternal(
        Maybe<Primary> input,
        Maybe<RValueExpression> expression,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        // parenthesized expressions cannot be assigned

        return errorNotLvalue(input, acceptor);
    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<Primary> input,
        ValidationMessageAcceptor acceptor
    ) {
        // parenthesized expressions cannot be assigned
        return errorNotLvalue(input, acceptor);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<Primary> input) {
        // parenthesized expressions cannot be assigned
        return false;
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
        return subPatternEvaluationsAllPure(input, state);
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
        return subExpressionsAnyHoled(input, state);
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
        return subExpressionsAnyTypelyHoled(input, state);
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
        return subExpressionsAnyUnbound(input, state);
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<Primary> input,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        final MaybeList<RValueExpression> exprs =
            input.getPattern().__toListNullsRemoved(Primary::getExprs);

        if (exprs.size() == 1) {
            return module.get(RValueExpressionSemantics.class)
                .compilePatternMatch(
                    input.subPattern(
                        input.getProvidedInputType(),
                        __ -> exprs.get(0).toNullable(),
                        "__paren"
                    ),
                    state,
                    acceptor
                );
        }


        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
        final MaybeList<RValueExpression> exprs = someStream(
            input.getPattern().__(Primary::getExprs)
        )
            .filter(Maybe::isPresent)
            .collect(MaybeList.collectFromStreamOfMaybes());

        if (exprs.size() == 1) {
            return module.get(RValueExpressionSemantics.class).inferPatternType(
                input.subPattern(
                    input.getProvidedInputType(),
                    __ -> exprs.get(0).toNullable(),
                    "__paren"
                ),
                state
            );
        }

        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<Primary> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        final MaybeList<RValueExpression> exprs =
            input.getPattern().__toListNullsRemoved(Primary::getExprs);

        if (exprs.size() == 1) {
            return module.get(RValueExpressionSemantics.class)
                .validatePatternMatch(
                    input.subPattern(
                        input.getProvidedInputType(),
                        __ -> exprs.get(0).toNullable(),
                        "__paren"
                    ),
                    state,
                    acceptor
                );
        } else {
            return VALID;
        }
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<Primary> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<Primary> input) {
        return subExpressionsAllMatch(input, ExpressionSemantics::canBeHoled);
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
        final MaybeList<RValueExpression> exprs =
            input.getPattern().__toListNullsRemoved(Primary::getExprs);

        if (exprs.size() == 1) {
            return module.get(RValueExpressionSemantics.class)
                .isPredictablePatternMatchSuccess(
                    input.subPattern(
                        input.getProvidedInputType(),
                        __ -> exprs.get(0).toNullable(),
                        "__paren"
                    ),
                    state
                );
        } else {
            return VALID;
        }
    }

}
