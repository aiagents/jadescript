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
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
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

    private static Boolean noRest(Maybe<Subscript> input) {
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
        CompilationOutputAcceptor acceptor
    ) {
        final IJadescriptType restType = this.doOnRest(
            input,
            (s, i) -> s.inferType(i, state)
        );
        final PSR<String> restPSR = this.doOnRest(
            input,
            (s, i) -> s.compile(i, state, acceptor)
        );

        final String restCompiled = restPSR.result();
        final StaticState afterRest = restPSR.state();


        final PSR<String> keyPSR = module.get(RValueExpressionSemantics.class)
            .compile(input.__(Subscript::getKey), afterRest, acceptor);

        final String keyCompiled = keyPSR.result();
        final StaticState afterKey = keyPSR.state();

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

        return afterKey;
    }

    @Override
    public boolean validateAssignmentInternal(
        Maybe<Subscript> input,
        Maybe<RValueExpression> expression,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {


        PSR<Boolean> rightPSR = module.get(RValueExpressionSemantics.class)
            .validate(expression, state, acceptor);
        boolean rightValidation = rightPSR.result();
        StaticState afterRight = rightPSR.state();
        if (rightValidation == INVALID) {
            return rightPSR;
        }
        IJadescriptType rightType = module.get(RValueExpressionSemantics.class)
            .inferType(expression, state);

        PSR<Boolean> restPSR = doOnRest(
            input,
            (s, i) -> s.validate(i, afterRight, acceptor)
        );
        boolean restValidation = restPSR.result();
        StaticState afterRest = restPSR.state();
        if (restValidation == INVALID) {
            return restPSR;
        }
        IJadescriptType restType = doOnRest(
            input,
            (s, i) -> s.inferType(i, afterRight)
        );

        Maybe<RValueExpression> key = input.__(Subscript::getKey);

        PSR<Boolean> keyPSR = module.get(RValueExpressionSemantics.class)
            .validate(key, afterRest, acceptor);
        boolean keyValidation = keyPSR.result();
        StaticState afterKey = keyPSR.state();
        if (keyValidation == INVALID) {
            return keyPSR;
        }

        if (noRest(input)) {
            return afterKey.INVALID();
        }

        keyValidation = module.get(ValidationHelper.class).assertion(
            !module.get(TypeHelper.class).TEXT.isAssignableFrom(restType),
            "InvalidAssignment",
            "Invalid assignment; values of 'text' are immutable.",
            key,
            acceptor
        );
        if (keyValidation == INVALID) {
            return afterKey.INVALID();
        }

        IJadescriptType keyType = module.get(RValueExpressionSemantics.class)
            .inferType(key, afterRest);


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
                            && t.apply(0).isAssignableFrom(keyType)
                            && t.apply(1).isAssignableFrom(rightType)
                    )
                ).collect(Collectors.toList());

            if (matchesFound.size() != 1) {
                return afterKey.with(
                    module.get(ValidationHelper.class).emitError(
                        "InvalidElementAccessOperation",
                        "Cannot perform '[]' subscript on values of type " +
                            restType.getJadescriptName() +
                            ", with index of type " +
                            keyType.getJadescriptName() +
                            ", assigning values of type " +
                            rightType.getJadescriptName() + ".",
                        key,
                        acceptor
                    )
                );
            } else {
                return afterKey.VALID();
            }
        } else {
            return afterKey.with(module.get(ValidationHelper.class).emitError(
                "InvalidSubscriptOperation",
                "[] operator cannot be used on types that are " +
                    "not list, map, or text. Type found: " +
                    restType.getJadescriptName(),
                key,
                acceptor
            ));
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
        return Stream.concat(
            doOnRest(input, ExpressionSemantics::getSubExpressions),
            Stream.of(new SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                input.__(Subscript::getKey)
            ))
        );
    }

    @Override
    protected String compileInternal(
        Maybe<Subscript> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        final IJadescriptType restType = doOnRest(
            input,
            (s, i) -> s.inferType(i, state)
        );
        final PSR<String> restPSR = doOnRest(
            input,
            (s, i) -> s.compile(i, state, acceptor)
        );
        final StaticState afterRest = restPSR.state();
        final String restCompiled = restPSR.result();

        final Maybe<RValueExpression> key = input.__(Subscript::getKey);
        final PSR<String> keyPSR = module.get(RValueExpressionSemantics.class)
            .compile(key, afterRest, acceptor);
        final StaticState afterKey = keyPSR.state();
        final String keyCompiled = keyPSR.result();


        if (module.get(TypeHelper.class).TEXT.isAssignableFrom(restType)) {
            return afterKey.with("(\"\"+" + restCompiled +
                ".charAt(" + keyCompiled + "))");
        } else if (restType instanceof TupleType) {
            final Optional<Integer> integer = extractIntegerIfAvailable(key);
            if (integer.isPresent()) {
                return afterKey.with(((TupleType) restType).compileGet(
                    restCompiled,
                    integer.get()
                ));
            }
        }

        return afterKey.with(restCompiled + ".get(" + keyCompiled + ")");
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
        if (typeHelper.TEXT.isAssignableFrom(restType)) {
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
        return typeHelper.jtFromJvmTypeRef(
            typeHelper.getArrayListMapComponentType(
                restType.asJvmTypeReference()
            )
        );
    }

    @Override
    protected boolean validateInternal(
        Maybe<Subscript> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        if (noRest(input)) {
            return state.INVALID();
        }
        PSR<Boolean> restPSR = doOnRest(
            input,
            (s, i) -> s.validate(i, state, acceptor)
        );
        boolean restValidation = restPSR.result();
        StaticState afterRest = restPSR.state();

        if (restValidation == INVALID) {
            return restPSR;
        }


        IJadescriptType restType = doOnRest(
            input,
            (s, i) -> s.inferType(i, state)
        );

        final Maybe<RValueExpression> key = input.__(Subscript::getKey);

        PSR<Boolean> keyPSR = module.get(RValueExpressionSemantics.class)
            .validate(key, afterRest, acceptor);
        boolean keyValidation = keyPSR.result();
        StaticState afterKey = keyPSR.state();

        if (keyValidation == INVALID) {
            return keyPSR;
        }

        IJadescriptType keyType = module.get(RValueExpressionSemantics.class)
            .inferType(key, state);

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
                return afterKey.INVALID();
            }

            final Optional<Integer> integer = extractIntegerIfAvailable(key);
            boolean indexValidation = module.get(ValidationHelper.class)
                .assertion(
                    integer.isPresent(),
                    "InvalidTupleIndex",
                    "Invalid index for tuples. To access a tuple element via " +
                        "index number, only" +
                        " index expressions whose value is trivially known at" +
                        " " +
                        "compile time can be used." +
                        " Please use an integer literal constant.",
                    key,
                    acceptor
                );

            if (integer.isPresent()) {
                final TupleType tupleType = (TupleType) restType;
                indexValidation = module.get(ValidationHelper.class).assertion(
                    integer.get() >= 0
                        && integer.get() < tupleType.getElementTypes().size(),
                    "InvalidTupleIndex",
                    "Index out of range. Index: " + integer.get() + "; " +
                        "tuple length: " + tupleType.getElementTypes().size(),
                    key,
                    acceptor
                );
            }

            return afterKey.with(indexValidation);

        } else if (module.get(TypeHelper.class).TEXT
            .isAssignableFrom(restType)) {
            return afterKey.with(module.get(ValidationHelper.class)
                .assertExpectedType(
                    module.get(TypeHelper.class).INTEGER,
                    keyType,
                    "InvalidStringSubscription",
                    key,
                    acceptor
                ));
        } else if (restType instanceof MapType
            || restType instanceof ListType) {
            final List<? extends CallableSymbol> matchesFound =
                restType.namespace().searchAs(
                    CallableSymbol.Searcher.class,
                    searcher -> searcher.searchCallable(
                        "get",
                        null,
                        (s, n) -> s == 1,
                        (s, t) -> s == 1 && t.apply(0).isAssignableFrom(keyType)
                    )
                ).collect(Collectors.toList());

            if (matchesFound.size() != 1) {

                return afterKey.with(module.get(ValidationHelper.class)
                    .emitError(
                        "InvalidElementAccessOperation",
                        "Cannot perform '[]' operator on values of type "
                            + restType.getJadescriptName() + ", using values " +
                            "of type" + keyType.getJadescriptName() +
                            "as index.",
                        key,
                        acceptor
                    ));
            } else {
                return afterKey.VALID();
            }
        } else {
            //It's neither an array nor a list/map... error!
            return afterKey.with(module.get(ValidationHelper.class).emitError(
                "InvalidElementAccessOperation",
                "[] operator cannot be used on types that are " +
                    "not list, map, or text. Type found: " +
                    restType.getJadescriptName(),
                key,
                acceptor
            ));
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
        return state;
    }

    @Override
    protected boolean isAlwaysPureInternal(
        Maybe<Subscript> input,
        StaticState state
    ) {
        return subExpressionsAllAlwaysPure(input, state);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<Subscript> input) {
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
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<Subscript> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        return state.emptyMatcher(input);
    }

    @Override
    public PatternType inferPatternTypeInternal(
        Maybe<Subscript> input,
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
        return state.VALID();
    }

}
