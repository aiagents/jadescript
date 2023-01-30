package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.Literal;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.expression.trailersexprchain.ReversedTrailerChain;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ListType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.MapType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TupleType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.Subscript;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.eclipse.xtext.xbase.typesystem.computation.NumberLiterals;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

public class SubscriptExpressionSemantics
    extends AssignableExpressionSemantics<Subscript> {

    public SubscriptExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(Maybe<Subscript> input) {
        return Optional.empty();
    }


    private static boolean noRest(Maybe<Subscript> input) {
        return input.__(Subscript::getRest)
            .__(ReversedTrailerChain::getElements)
            .__(List::isEmpty)
            .extract(nullAsFalse);
    }


    private Maybe<SemanticsBoundToAssignableExpression<?>> restSBTE(
        Maybe<Subscript> input
    ) {
        return input
            .__(Subscript::getRest)
            .flatMap(ReversedTrailerChain::resolveChain);
    }


    @SuppressWarnings({"unchecked"})
    private <T, X extends EObject> T doOnRest(
        Maybe<Subscript> input,
        BiFunction<
            ? super AssignableExpressionSemantics<X>,
            ? super Maybe<X>,
            ? extends T
            > func
    ) {
        final Maybe<SemanticsBoundToAssignableExpression<?>> restSBTE =
            restSBTE(input);

        final AssignableExpressionSemantics<X> semantics = restSBTE
            .__((x) -> (AssignableExpressionSemantics<X>) x.getSemantics())
            .orElseGet(this::emptyAssignableSemantics);

        final Maybe<X> rest = restSBTE
            .__((x) -> (Maybe<X>) x.getInput())
            .orElseGet(Maybe::nothing);


        return func.apply(semantics, rest);
    }


    @Override
    protected void compileAssignmentInternal(
        Maybe<Subscript> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final IJadescriptType restType = this.doOnRest(
            input,
            (s, i) -> s.inferType(i, state)
        );
        String restCompiled = doOnRest(
            input,
            (s, i) -> s.compile(i, state, acceptor)
        );


        final StaticState afterRest = doOnRest(
            input,
            (s, i) -> s.advance(i, state)
        );

        final String keyCompiled = module.get(RValueExpressionSemantics.class)
            .compile(input.__(Subscript::getKey), afterRest, acceptor);

        //NOT NEEDED
//        final StaticState afterKey =
//            module.get(RValueExpressionSemantics.class)
//                .advance(input.__(Subscript::getKey), afterRest);

        if (restType instanceof ListType) {
            acceptor.accept(w.simpleStmt(
                restCompiled + ".set(" + keyCompiled
                    + ", " + compiledExpression + ")"
            ));
        } else if (restType instanceof MapType) {
            acceptor.accept(w.simpleStmt(
                restCompiled + ".put(" + keyCompiled
                    + ", " + compiledExpression + ")"
            ));
        } else {
            acceptor.accept(w.simpleStmt(
                restCompiled + "[" + keyCompiled
                    + "] = " + compiledExpression
            ));
        }
    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<Subscript> input,
        IJadescriptType rightType,
        StaticState state
    ) {
        final StaticState afterRest = doOnRest(
            input,
            (s, i) -> s.advance(i, state)
        );
        return module.get(RValueExpressionSemantics.class)
            .advance(input.__(Subscript::getKey), afterRest);

    }


    @Override
    public boolean validateAssignmentInternal(
        Maybe<Subscript> input,
        Maybe<RValueExpression> expression,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {


        final RValueExpressionSemantics rves = module.get(
            RValueExpressionSemantics.class);
        boolean rightCheck = rves.validate(expression, state, acceptor);

        if (rightCheck == INVALID) {
            return INVALID;
        }
        IJadescriptType rightType = rves.inferType(expression, state);
        StaticState afterRight = rves.advance(expression, state);

        boolean restCheck = doOnRest(
            input,
            (s, i) -> s.validate(i, afterRight, acceptor)
        );
        if (restCheck == INVALID) {
            return INVALID;
        }
        IJadescriptType restType = doOnRest(
            input,
            (s, i) -> s.inferType(i, afterRight)
        );
        StaticState afterRest = doOnRest(
            input,
            (s, i) -> s.advance(i, afterRight)
        );

        Maybe<RValueExpression> key = input.__(Subscript::getKey);

        boolean keyCheck = rves.validate(key, afterRest, acceptor);

        if (keyCheck == INVALID) {
            return INVALID;
        }

        //NOT NEEDED
//        StaticState afterKey = rves.advance(key, afterRest);

        if (noRest(input)) {
            return INVALID;
        }

        keyCheck = module.get(ValidationHelper.class).asserting(
            !module.get(TypeHelper.class).TEXT.isSupEqualTo(restType),
            "InvalidAssignment",
            "Invalid assignment; values of 'text' are immutable.",
            key,
            acceptor
        );
        if (keyCheck == INVALID) {
            return INVALID;
        }

        IJadescriptType keyType = rves.inferType(key, afterRest);


        if (restType instanceof ListType || restType instanceof MapType) {
            String methodName;

            if (restType instanceof ListType) {
                methodName = "set";
            } else {
                methodName = "put";
            }

            final List<? extends CallableSymbol> matchesFound =
                restType.namespace().searchAs(
                    CallableSymbol.Searcher.class,
                    searcher -> searcher.searchCallable(
                        methodName,
                        null,
                        (s, n) -> s == 2,
                        (s, t) -> s == 2
                            && t.apply(0).isSupEqualTo(keyType)
                            && t.apply(1).isSupEqualTo(rightType)
                    )
                ).collect(Collectors.toList());

            if (matchesFound.size() != 1) {
                return module.get(ValidationHelper.class).emitError(
                    "InvalidElementAccessOperation",
                    "Cannot perform '[]' subscript on values of type " +
                        restType.getJadescriptName() +
                        ", with index of type " +
                        keyType.getJadescriptName() +
                        ", assigning values of type " +
                        rightType.getJadescriptName() + ".",
                    key,
                    acceptor
                );
            } else {
                return INVALID;
            }
        } else {
            return module.get(ValidationHelper.class).emitError(
                "InvalidSubscriptOperation",
                "[] operator cannot be used on types that are " +
                    "not list, map, or text. Type found: " +
                    restType.getJadescriptName(),
                key,
                acceptor
            );
        }

    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<Subscript> input,
        ValidationMessageAcceptor acceptor
    ) {
        //a trailer chain ending with [] operator is always a valid lvalue.
        return VALID;
    }


    @Override
    protected boolean mustTraverse(Maybe<Subscript> input) {
        return false;
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<Subscript> input
    ) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        return Stream.concat(
            doOnRest(input, ExpressionSemantics::getSubExpressions),
            Stream.of(input.__(Subscript::getKey))
                .filter(Maybe::isPresent)
                .map(i -> new SemanticsBoundToExpression<>(rves, i))
        );
    }


    @Override
    protected String compileInternal(
        Maybe<Subscript> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final IJadescriptType restType = doOnRest(
            input,
            (s, i) -> s.inferType(i, state)
        );
        final String restCompiled = doOnRest(
            input,
            (s, i) -> s.compile(i, state, acceptor)
        );
        final StaticState afterRest = doOnRest(
            input,
            (s, i) -> s.advance(i, state)
        );

        final Maybe<RValueExpression> key = input.__(Subscript::getKey);
        final String keyCompiled = module.get(RValueExpressionSemantics.class)
            .compile(key, afterRest, acceptor);
        //NOT NEEDED:
//        final StaticState afterKey =
//            module.get(RValueExpressionSemantics.class)
//                .advance(key, afterRest);


        if (module.get(TypeHelper.class).TEXT.isSupEqualTo(restType)) {
            return "(\"\"+" + restCompiled + ".charAt(" + keyCompiled + "))";
        } else if (restType instanceof TupleType) {
            final Optional<Integer> integer = extractIntegerIfAvailable(key);
            if (integer.isPresent()) {
                return ((TupleType) restType).compileGet(
                    restCompiled,
                    integer.get()
                );
            }
        }

        return restCompiled + ".get(" + keyCompiled + ")";
    }


    private Optional<Integer> extractIntegerIfAvailable(
        Maybe<RValueExpression> key
    ) {
        final SemanticsBoundToExpression<?> sbte =
            module.get(RValueExpressionSemantics.class)
                .deepTraverse(key);
        if (sbte.getInput().isInstanceOf(Literal.class)
            && sbte.getInput().isPresent()) {
            return sbte.getInput()
                .__(x -> (Literal) x)
                .__(Literal::getNumber)
                .__(number -> {
                    NumberLiterals numberLiterals =
                        module.get(NumberLiterals.class);
                    XNumberLiteral xNumberLiteral =
                        XbaseFactory.eINSTANCE.createXNumberLiteral();
                    xNumberLiteral.setValue(number);
                    final Number n = numberLiterals.numberValue(
                        xNumberLiteral,
                        numberLiterals.getJavaType(xNumberLiteral)
                    );
                    if (n instanceof Integer) {
                        return (Integer) n;
                    } else {
                        return null;
                    }
                }).toOpt();
        }
        return Optional.empty();
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<Subscript> input,
        StaticState state
    ) {
        final Maybe<RValueExpression> key = input.__(Subscript::getKey);
        final IJadescriptType restType = doOnRest(
            input,
            (s, i) -> s.inferType(i, state)
        );
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (typeHelper.TEXT.isSupEqualTo(restType)) {
            return typeHelper.TEXT;
        }
        if (restType instanceof TupleType) {
            final Optional<Integer> integer = extractIntegerIfAvailable(key);
            if (integer.isPresent()) {
                return ((TupleType) restType).getElementTypes().get(
                    integer.get()
                );
            }
        }
        return restType.getElementTypeIfCollection()
            .orElseGet(() -> typeHelper.ANY);
    }


    @Override
    protected boolean validateInternal(
        Maybe<Subscript> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        if (noRest(input)) {
            return INVALID;
        }
        boolean restCheck = doOnRest(
            input,
            (s, i) -> s.validate(i, state, acceptor)
        );

        if (restCheck == INVALID) {
            return INVALID;
        }

        IJadescriptType restType = doOnRest(
            input,
            (s, i) -> s.inferType(i, state)
        );
        StaticState afterRest = doOnRest(
            input,
            (s, i) -> s.advance(i, state)
        );

        final Maybe<RValueExpression> key = input.__(Subscript::getKey);

        boolean keyCheck = module.get(RValueExpressionSemantics.class)
            .validate(key, afterRest, acceptor);

        if (keyCheck == INVALID) {
            return INVALID;
        }
        IJadescriptType keyType = module.get(RValueExpressionSemantics.class)
            .inferType(key, state);

        //NOT NEEDED:
//        StaticState afterKey = module.get(RValueExpressionSemantics.class)
//            .advance(key, afterRest);


        if (restType instanceof TupleType) {
            boolean indexTypeValidation =
                module.get(ValidationHelper.class).assertExpectedType(
                    module.get(TypeHelper.class).INTEGER,
                    keyType,
                    "InvalidTupleIndex",
                    key,
                    acceptor
                );

            if (indexTypeValidation == INVALID) {
                return INVALID;
            }

            final Optional<Integer> integer = extractIntegerIfAvailable(key);
            boolean indexValidation = module.get(ValidationHelper.class)
                .asserting(
                    integer.isPresent(),
                    "InvalidTupleIndex",
                    "Invalid index for tuples. To access a tuple element via " +
                        "index number, only index expressions whose value is " +
                        "trivially known at compile time can be used. Please " +
                        "use an integer literal constant.",
                    key,
                    acceptor
                );

            if (integer.isPresent()) {
                final TupleType tupleType = (TupleType) restType;
                indexValidation = module.get(ValidationHelper.class).asserting(
                    integer.get() >= 0
                        && integer.get() < tupleType.getElementTypes().size(),
                    "InvalidTupleIndex",
                    "Index out of range. Index: " + integer.get() + "; " +
                        "tuple length: " + tupleType.getElementTypes().size(),
                    key,
                    acceptor
                );
            }

            return indexValidation;

        } else if (module.get(TypeHelper.class).TEXT
            .isSupEqualTo(restType)) {
            return module.get(ValidationHelper.class).assertExpectedType(
                module.get(TypeHelper.class).INTEGER,
                keyType,
                "InvalidStringSubscription",
                key,
                acceptor
            );
        } else if (restType instanceof MapType
            || restType instanceof ListType) {
            final List<? extends CallableSymbol> matchesFound =
                restType.namespace().searchAs(
                    CallableSymbol.Searcher.class,
                    searcher -> searcher.searchCallable(
                        "get",
                        null,
                        (s, n) -> s == 1,
                        (s, t) -> s == 1 && t.apply(0).isSupEqualTo(keyType)
                    )
                ).collect(Collectors.toList());

            if (matchesFound.size() != 1) {

                return module.get(ValidationHelper.class).emitError(
                    "InvalidElementAccessOperation",
                    "Cannot perform '[]' operator on values of type "
                        + restType.getJadescriptName() + ", using values " +
                        "of type" + keyType.getJadescriptName() +
                        "as index.",
                    key,
                    acceptor
                );
            } else {
                return VALID;
            }
        } else {
            //It's neither an array nor a list/map... error!
            return module.get(ValidationHelper.class).emitError(
                "InvalidElementAccessOperation",
                "[] operator cannot be used on types that are " +
                    "not list, map, or text. Type found: " +
                    restType.getJadescriptName(),
                key,
                acceptor
            );
        }

    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<Subscript> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<Subscript> input,
        StaticState state
    ) {
        final StaticState afterRest = doOnRest(
            input,
            (s, i) -> s.advance(i, state)
        );

        return module.get(RValueExpressionSemantics.class)
            .advance(input.__(Subscript::getKey), afterRest);
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<Subscript> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<Subscript> input) {
        return true;
    }


    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<Subscript> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<Subscript> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<Subscript> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<Subscript> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean isHoledInternal(
        Maybe<Subscript> input,
        StaticState state
    ) {
        // Subscript expressions cannot be holed by design.
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<Subscript> input,
        StaticState state
    ) {
        // Subscript expressions cannot be holed by design.
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<Subscript> input,
        StaticState state
    ) {
        // Subscript expressions cannot have unbound terms by design.
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<Subscript> input) {
        // Subscript expressions cannot be holed by design.
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<Subscript> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<Subscript> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<Subscript> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<Subscript> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<Subscript> input,
        StaticState state
    ) {
        return state;
    }



}
