package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.AidLiteral;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TypeCast;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;
import static it.unipr.ailab.maybe.Maybe.nullAsTrue;

public class AidLiteralExpressionSemantics
    extends AssignableExpressionSemantics<AidLiteral> {

    public AidLiteralExpressionSemantics(SemanticsModule module) {
        super(module);
    }

    @Override
    protected boolean validateInternal(
        Maybe<AidLiteral> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final TypeCastExpressionSemantics tces =
            module.get(TypeCastExpressionSemantics.class);
        final Maybe<TypeCast> left = input.__(AidLiteral::getTypeCast);
        boolean leftValidation = tces.validate(left, state, acceptor);
        boolean leftTypeValidation = VALID;
        boolean rightValidation = VALID;
        boolean rightTypeValidation = VALID;

        if (leftValidation == VALID
            && input.__(AidLiteral::isIsAidExpr).extract(nullAsFalse)) {
            leftTypeValidation = module.get(ValidationHelper.class)
                .assertExpectedType(
                    module.get(TypeHelper.class).TEXT,
                    tces.inferType(left, state),
                    "InvalidAIDNickname",
                    left,
                    acceptor
                );
        }

        StaticState finalState = tces.advance(left, state);
        final Maybe<TypeCast> right = input.__(AidLiteral::getHap);
        if (right.isPresent()) {
            rightValidation = tces.validate(right, finalState, acceptor);
            if (rightValidation == VALID) {
                rightTypeValidation = module.get(ValidationHelper.class)
                    .assertExpectedType(
                        module.get(TypeHelper.class).TEXT,
                        tces.inferType(right, finalState),
                        "InvalidAIDHAP",
                        right,
                        acceptor
                    );
            }
        }

        return leftValidation && leftTypeValidation
            && rightValidation && rightTypeValidation;
    }

    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<AidLiteral> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }

    @Override
    protected StaticState advanceInternal(
        Maybe<AidLiteral> input,
        StaticState state
    ) {
        final TypeCastExpressionSemantics tces =
            module.get(TypeCastExpressionSemantics.class);
        final Maybe<TypeCast> left = input.__(AidLiteral::getTypeCast);

        StaticState finalState = tces.advance(left, state);
        final Maybe<TypeCast> right = input.__(AidLiteral::getHap);
        if (right.isPresent()) {
            finalState = tces.advance(right, finalState);
        }

        return finalState;
    }

    @Override
    protected StaticState useStateAsPatternInternal(
        PatternMatchInput<AidLiteral> input,
        StaticState state
    ) {
        final Maybe<TypeCast> hap = input.getPattern().__(AidLiteral::getHap);
        final TypeCastExpressionSemantics tces =
            module.get(TypeCastExpressionSemantics.class);
        final IJadescriptType textType = module.get(TypeHelper.class).TEXT;

        StaticState newState = tces.advancePattern(
            input.subPattern(
                textType,
                AidLiteral::getTypeCast,
                "_localname"
            ),
            state
        );
        if (hap.isPresent()) {
            newState = tces.advancePattern(
                input.subPattern(textType, __ -> hap.toNullable(), "_hap"),
                newState
            );
        }

        return newState;
    }

    @Override
    public void compileAssignmentInternal(
        Maybe<AidLiteral> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        // CANNOT BE L-EXPRESSION
    }

    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<AidLiteral> left,
        IJadescriptType rightType,
        StaticState state
    ) {
        // CANNOT BE L-EXPRESSION
        return state;
    }

    @Override
    public boolean validateAssignmentInternal(
        Maybe<AidLiteral> input,
        Maybe<RValueExpression> expression,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        // CANNOT BE L-EXPRESSION
        return errorNotLvalue(input, acceptor);
    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<AidLiteral> input,
        ValidationMessageAcceptor acceptor
    ) {
        return errorNotLvalue(input, acceptor);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<AidLiteral> input) {
        return false;
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<AidLiteral> input, StaticState state
    ) {
        return subPatternEvaluationsAllPure(input, state);
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<AidLiteral> input) {
        return true;
    }

    @Override
    protected boolean isHoledInternal(
        Maybe<AidLiteral> input,
        StaticState state
    ) {
        return subExpressionsAnyHoled(input, state);
    }

    @Override
    protected boolean isUnboundInternal(
        Maybe<AidLiteral> input,
        StaticState state
    ) {
        return subExpressionsAnyUnbound(input, state);
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<AidLiteral> input,
        StaticState state
    ) {
        return subExpressionsAnyHoled(input, state);
    }

    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<AidLiteral> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        final Maybe<TypeCast> hap = input.getPattern().__(AidLiteral::getHap);
        final TypeCastExpressionSemantics tces =
            module.get(TypeCastExpressionSemantics.class);

        final IJadescriptType textType = module.get(TypeHelper.class).TEXT;
        List<PatternMatcher> subResults = new ArrayList<>(2);

        final IJadescriptType patternType = inferPatternType(
            input.getPattern(),
            input.getMode(),
            state
        ).solve(input.getProvidedInputType());

        final PatternMatchInput.SubPattern<TypeCast, AidLiteral>
            localNameSubpattern = input.subPattern(
            textType,
            AidLiteral::getTypeCast,
            "_localname"
        );
        subResults.add(tces.compilePatternMatch(
            localNameSubpattern,
            state,
            acceptor
        ));
        StaticState newState = tces.advancePattern(
            localNameSubpattern,
            state
        );

        Function<Integer, String> compiledSubinputs;
        if (hap.isPresent()) {
            final PatternMatchInput.SubPattern<TypeCast, AidLiteral>
                hapSubpattern = input.subPattern(
                textType,
                __ -> hap.toNullable(),
                "_hap"
            );
            subResults.add(tces.compilePatternMatch(
                hapSubpattern,
                newState,
                acceptor
            ));


            compiledSubinputs = (i) -> i == 0
                ? "__x.getLocalName()"
                : "__x.getHap()";
        } else {
            compiledSubinputs = (__) -> "__x.getLocalName()";
        }


        return input.createCompositeMethodOutput(
            patternType,
            compiledSubinputs,
            subResults
        );
    }

    @Override
    public PatternType inferPatternTypeInternal(
        Maybe<AidLiteral> input,
        StaticState state
    ) {
        return PatternType.simple(module.get(TypeHelper.class).AID);
    }

    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<AidLiteral> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final Maybe<TypeCast> hap = input.getPattern().__(AidLiteral::getHap);
        final TypeCastExpressionSemantics tces =
            module.get(TypeCastExpressionSemantics.class);
        final IJadescriptType textType = module.get(TypeHelper.class).TEXT;


        final PatternMatchInput.SubPattern<TypeCast, AidLiteral>
            localNameSubpattern = input.subPattern(
            textType,
            AidLiteral::getTypeCast,
            "_localname"
        );
        final boolean localNameCheck = tces.validatePatternMatch(
            localNameSubpattern,
            state,
            acceptor
        );

        StaticState newState = tces.advancePattern(
            localNameSubpattern,
            state
        );

        boolean hapCheck = VALID;
        if (hap.isPresent()) {
            final PatternMatchInput.SubPattern<TypeCast, AidLiteral>
                hapSubpattern = input.subPattern(
                textType,
                __ -> hap.toNullable(),
                "_hap"
            );
            hapCheck = tces.validatePatternMatch(
                hapSubpattern,
                newState,
                acceptor
            );

        }

        return localNameCheck && hapCheck;
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<AidLiteral> input
    ) {
        final TypeCastExpressionSemantics tces =
            module.get(TypeCastExpressionSemantics.class);
        final SemanticsBoundToExpression<TypeCast> localName =
            input.__(AidLiteral::getTypeCast)
                .extract(x -> new SemanticsBoundToExpression<>(tces, x));

        Stream<SemanticsBoundToExpression<?>> result = Stream.of(localName);

        if (input.__(AidLiteral::getHap).isPresent()) {
            final SemanticsBoundToExpression<TypeCast> hap =
                input.__(AidLiteral::getHap)
                    .extract(x -> new SemanticsBoundToExpression<>(tces, x));
            result = Stream.concat(result, Stream.of(hap));
        }
        return result;
    }

    @Override
    protected String compileInternal(
        Maybe<AidLiteral> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        final Maybe<TypeCast> localName = input.__(AidLiteral::getTypeCast);
        final TypeCastExpressionSemantics tces =
            module.get(TypeCastExpressionSemantics.class);
        final String localNameCompiled = tces.compile(
            localName,
            state,
            acceptor
        );

        StaticState newState = tces.advance(localName, state);
        String argString = "java.lang.String.valueOf("
            + localNameCompiled + ")";
        String isGuid = "false";

        final Maybe<TypeCast> hap = input.__(AidLiteral::getHap);
        if (hap.isPresent()) {
            isGuid = "true";
            final String hapCompiled = tces.compile(
                hap,
                newState,
                acceptor
            );
            argString += " + \"@\" + " +
                "java.lang.String.valueOf(" + hapCompiled + ")";
        }
        return "new jade.core.AID(" + argString + ", " + isGuid + ")";

    }

    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<AidLiteral> input,
        StaticState state
    ) {
        return module.get(TypeHelper.class).AID;
    }

    @Override
    protected boolean mustTraverse(Maybe<AidLiteral> input) {
        return !input.__(AidLiteral::isIsAidExpr).extract(nullAsTrue);
    }

    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverse(Maybe<AidLiteral> input) {
        if (mustTraverse(input)) {
            return Optional.ofNullable(input.__(AidLiteral::getTypeCast))
                .map(x -> new SemanticsBoundToAssignableExpression<>(
                    module.get(TypeCastExpressionSemantics.class), x));
        } else {
            return Optional.empty();
        }

    }

    @Override
    protected boolean isAlwaysPureInternal(
        Maybe<AidLiteral> input,
        StaticState state
    ) {
        return subExpressionsAllAlwaysPure(input, state);
    }


}
