package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.AtomExpr;
import it.unipr.ailab.jadescript.jadescript.Literal;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.*;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.eclipse.xtext.xbase.typesystem.computation.NumberLiterals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts.INVALID;
import static it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts.VALID;


/**
 * Created on 26/08/18.
 */
@SuppressWarnings("restriction")
public class SubscriptionElement extends TrailersExpressionChainElement {

    private final Maybe<RValueExpression> key;

    private final RValueExpressionSemantics expressionSemantics;

    private final TypeHelper typeHelper;

    public SubscriptionElement(SemanticsModule module, Maybe<RValueExpression> key) {
        super(module);
        this.key = key;
        this.expressionSemantics = module.get(RValueExpressionSemantics.class);
        this.typeHelper = module.get(TypeHelper.class);
    }


    @Override
    public String compile(ReversedTrailerChain rest, CompilationOutputAcceptor acceptor) {
        String operandCompiled = rest.compile(acceptor);
        String keyCompiled = expressionSemantics.compile(key, acceptor);
        final IJadescriptType restType = rest.inferType();
        if (module.get(TypeHelper.class).TEXT.isAssignableFrom(restType)) {
            return "(\"\"+" + operandCompiled + ".charAt(" + keyCompiled + "))";
        }
        if (restType instanceof TupleType) {
            final Optional<Integer> integer = extractIntegerIfAvailable(key);
            if (integer.isPresent()) {
                return ((TupleType) restType).compileGet(operandCompiled, integer.get());
            }
        }

        return operandCompiled + ".get(" + keyCompiled + ")";

    }

    @Override
    public IJadescriptType inferType(ReversedTrailerChain rest) {
        final IJadescriptType restType = rest.inferType();

        if (module.get(TypeHelper.class).TEXT.isAssignableFrom(restType)) {
            return module.get(TypeHelper.class).TEXT;
        }
        if (restType instanceof TupleType) {
            final Optional<Integer> integer = extractIntegerIfAvailable(key);
            if (integer.isPresent()) {
                return ((TupleType) restType).getElementTypes().get(integer.get());
            }
        }
        return typeHelper.jtFromJvmTypeRef(
                typeHelper.getArrayListMapComponentType(rest.inferType().asJvmTypeReference())
        );
    }

    private Optional<Integer> extractIntegerIfAvailable(Maybe<RValueExpression> key) {
        final SemanticsBoundToExpression<?> sbte = module.get(RValueExpressionSemantics.class).deepTraverse(key);
        if (sbte.getInput().isInstanceOf(Literal.class) && sbte.getInput().isPresent()) {
            return sbte.getInput()
                    .__(x -> (Literal) x)
                    .__(Literal::getNumber)
                    .__(number -> {
                        NumberLiterals numberLiterals = module.get(NumberLiterals.class);
                        XNumberLiteral xNumberLiteral = XbaseFactory.eINSTANCE.createXNumberLiteral();
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
    public boolean validate(ReversedTrailerChain rest, ValidationMessageAcceptor acceptor) {

        boolean restSubvalidation = rest.validate(acceptor);
        if (restSubvalidation == INVALID) {
            return INVALID;
        }


        IJadescriptType restType = rest.inferType();
        if (rest.getElements().isEmpty()) {
            // ERROR! however, a [] subscription without nothing before should be syntactically impossible...
            // (it would be recognized as list literal...)
            return INVALID;
        }


        boolean keySubValidation = expressionSemantics.validate(key, acceptor);
        if (keySubValidation == INVALID) {
            return INVALID;
        }

        IJadescriptType keyType = expressionSemantics.inferType(key);
        if (restType instanceof TupleType) {
            boolean indexType = module.get(ValidationHelper.class).assertExpectedType(
                    module.get(TypeHelper.class).INTEGER,
                    module.get(RValueExpressionSemantics.class).inferType(key),
                    "InvalidTupleIndex",
                    key,
                    acceptor
            );

            if (indexType == INVALID) {
                return INVALID;
            }

            final Optional<Integer> integer = extractIntegerIfAvailable(key);
            boolean indexValid = module.get(ValidationHelper.class).assertion(
                    integer.isPresent(),
                    "InvalidTupleIndex",
                    "Invalid index for tuples. To access a tuple element via index number, only" +
                            " index expressions whose value is trivially known at compile time can be used." +
                            " Please an integer literal constant.",
                    key,
                    acceptor
            );

            if (integer.isPresent()) {
                final TupleType tupleType = (TupleType) restType;
                indexValid = module.get(ValidationHelper.class).assertion(
                        integer.get() >= 0 && integer.get() < tupleType.getElementTypes().size(),
                        "InvalidTupleIndex",
                        "Index out of range. Index: " + integer.get() + "; " +
                                "tuple length: " + tupleType.getElementTypes().size(),
                        key,
                        acceptor
                );
            }

            return indexValid;

        } else if (module.get(TypeHelper.class).TEXT.isAssignableFrom(restType)) {
            return module.get(ValidationHelper.class).assertExpectedType(
                    module.get(TypeHelper.class).INTEGER,
                    module.get(RValueExpressionSemantics.class).inferType(key),
                    "InvalidStringSubscription",
                    key,
                    acceptor
            );
        } else if (restType instanceof MapType || restType instanceof ListType) {
            final List<? extends CallableSymbol> matchesFound = restType.namespace().searchAs(
                    CallableSymbol.Searcher.class,
                    searcher -> searcher.searchCallable(
                            "get",
                            null,
                            (s, n) -> s == 1,
                            (s, t) -> s == 1 && t.apply(0).isAssignableFrom(keyType)
                    )
            ).collect(Collectors.toList());

            if (matchesFound.size() != 1) {
                key.safeDo(keySafe -> {
                    acceptor.acceptError(
                            "cannot perform '[]' operator on values of type: " + restType.getJadescriptName(),
                            keySafe,
                            null,
                            ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                            "InvalidElementAccessOperation"
                    );
                });
                return INVALID;
            }else{
                return VALID;
            }

        } else {
            //It's neither an array nor a list/map... error!
            key.safeDo(keySafe -> {
                acceptor.acceptError(
                        "[] operator cannot be used on types that are " +
                                "not list, map, or text. Type found: " + restType.getJadescriptName(),
                        keySafe,
                        null,
                        ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                        "InvalidElementAccessOperation"
                );
            });

            return INVALID;
        }
    }

    @Override
    public boolean validateAssignment(
            ReversedTrailerChain rest, Maybe<RValueExpression> rValueExpression,
            IJadescriptType typeOfRExpr, ValidationMessageAcceptor acceptor
    ) {


        boolean restSubValidation = rest.validate(acceptor);
        if (restSubValidation == INVALID) {
            return INVALID;
        }
        IJadescriptType restType = rest.inferType();
        if (rest.getElements().isEmpty()) {
            // ERROR! however, a [] subscription without nothing before should be syntactically impossible...
            return INVALID;
        }

        boolean keySubValidation = expressionSemantics.validate(key, acceptor);
        if(keySubValidation == INVALID){
            return INVALID;
        }

        IJadescriptType keyType = expressionSemantics.inferType(key);

        keySubValidation = module.get(ValidationHelper.class).assertion(
                !module.get(TypeHelper.class).TEXT.isAssignableFrom(restType),
                "InvalidAssignment",
                "Invalid assignment; values of 'text' are immutable.",
                key,
                acceptor
        );

        if(keySubValidation == INVALID){
            return INVALID;
        }

        if (restType instanceof ListType || restType instanceof MapType) {
            String methodName;

            if (restType instanceof ListType) {
                methodName = "set";
            } else {
                methodName = "put";
            }

            final List<? extends CallableSymbol> matchesFound = restType.namespace().searchAs(
                    CallableSymbol.Searcher.class,
                    searcher -> searcher.searchCallable(
                            methodName,
                            null,
                            (s, n) -> s == 2,
                            (s, t) -> s == 2 && t.apply(0).isAssignableFrom(keyType)
                                    && t.apply(1).isAssignableFrom(typeOfRExpr)
                    )
            ).collect(Collectors.toList());

            if (matchesFound.size() != 1) {
                key.safeDo(keySafe -> {
                    acceptor.acceptError(
                            "cannot perform '[]' operator on values of type: " + restType.getJadescriptName(),
                            keySafe,
                            null,
                            ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                            "InvalidElementAccessOperation"
                    );
                });
                return INVALID;
            }else{
                return VALID;
            }
        } else {
            //It's neither an array nor a list/map... error!
            key.safeDo(keySafe -> {
                acceptor.acceptError(
                        "[] operator cannot be used on types that are " +
                                "not list, map, or text. Type found: " + restType.getJadescriptName(),
                        keySafe,
                        null,
                        ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                        "InvalidElementAccessOperation"
                );
            });
            return INVALID;
        }
    }

    @Override
    public boolean syntacticValidateLValue(ValidationMessageAcceptor acceptor) {
        //a trailer chain ending with [] operator is always a valid lvalue
        return VALID;
    }

    @Override
    public void compileAssignment(
            ReversedTrailerChain rest,
            String compiledExpression,
            IJadescriptType exprType,
            CompilationOutputAcceptor acceptor
    ) {
        String restCompiled = rest.compile(acceptor);
        IJadescriptType restType = rest.inferType();


        if (restType instanceof ListType) {
            acceptor.accept(w.simpleStmt(
                    restCompiled + ".set("
                            + expressionSemantics.compile(key, acceptor)
                            + ", " + compiledExpression + ")"
            ));
        } else if (restType instanceof MapType) {
            acceptor.accept(w.simpleStmt(
                    restCompiled + ".put("
                            + expressionSemantics.compile(key, acceptor)
                            + ", " + compiledExpression + ")"
            ));
        } else {
            acceptor.accept(w.simpleStmt(
                    restCompiled + "[" + expressionSemantics.compile(key, acceptor)
                            + "] = " + compiledExpression
            ));
        }
    }

    @Override
    public boolean isAlwaysPure(ReversedTrailerChain rest) {
        return true;
    }

    @Override
    public Stream<SemanticsBoundToExpression<?>> getSubExpressions(ReversedTrailerChain rest) {
        return Stream.concat(
                Stream.of(new SemanticsBoundToExpression<>(expressionSemantics, key)),
                rest.getSubExpressions()
        );
    }

    @Override
    public boolean isHoled(ReversedTrailerChain rest) {
        // Subscription expressions cannot be holed by design.
        return false;
    }

    @Override
    public boolean isUnbounded(ReversedTrailerChain rest) {
        // Subscription expressions cannot have unbound terms by design.
        return false;
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest,
            CompilationOutputAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<AtomExpr> input, ReversedTrailerChain rest) {
        return PatternType.empty(module);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest,
            ValidationMessageAcceptor acceptor
    ) {
        return input.createEmptyValidationOutput();
    }

    @Override
    public boolean isTypelyHoled(ReversedTrailerChain rest) {
        // Subscription expressions cannot be holed by design.
        return false;
    }

    @Override
    public boolean isValidLexpr(ReversedTrailerChain rest) {
        // Can be used as L-Expression only if the subscripted operand is a Map or a List.
        IJadescriptType restType = rest.inferType();
        return restType instanceof MapType || restType instanceof ListType;
    }

    @Override
    public boolean isPatternEvaluationPure(ReversedTrailerChain rest) {
        return true;
    }

    @Override
    public boolean canBeHoled(ReversedTrailerChain withoutFirst) {
        return false;
    }

    @Override
    public boolean containsNotHoledAssignableParts(ReversedTrailerChain withoutFirst) {
        return true;
    }
}
