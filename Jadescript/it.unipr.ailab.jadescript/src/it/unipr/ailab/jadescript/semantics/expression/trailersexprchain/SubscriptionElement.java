package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.Literal;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.*;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.eclipse.xtext.xbase.typesystem.computation.NumberLiterals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


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
    public String compile(ReversedTrailerChain rest) {
        Maybe<String> operandCompiled = rest.compile();
        final String keyCompiled = expressionSemantics.compile(key).orElse("");
        final IJadescriptType restType = rest.inferType();
        if (module.get(TypeHelper.class).TEXT.isAssignableFrom(restType)) {
            return "(\"\"+" + operandCompiled + ".charAt(" + keyCompiled + "))";
        }
        if (restType instanceof TupleType) {
            final Optional<Integer> integer = extractIntegerIfAvailable(key);
            if (integer.isPresent()) {
                return ((TupleType) restType).compileGet(operandCompiled.orElse(""), integer.get());
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
    public void validate(ReversedTrailerChain rest, ValidationMessageAcceptor acceptor) {
        InterceptAcceptor restSubvalidation = new InterceptAcceptor(acceptor);
        rest.validate(restSubvalidation);
        if (restSubvalidation.thereAreErrors()) {
            return;
        }


        IJadescriptType restType = rest.inferType();
        if (rest.getElements().isEmpty()) {
            // ERROR! however, a [] subscription without nothing before should be syntactically impossible...
            return;
        }


        InterceptAcceptor keySubValidation = new InterceptAcceptor(acceptor);
        expressionSemantics.validate(key, keySubValidation);
        if (!keySubValidation.thereAreErrors()) {
            IJadescriptType keyType = expressionSemantics.inferType(key);

            if (restType instanceof TupleType) {
                module.get(ValidationHelper.class).assertExpectedType(
                        module.get(TypeHelper.class).INTEGER,
                        module.get(RValueExpressionSemantics.class).inferType(key),
                        "InvalidTupleIndex",
                        key,
                        keySubValidation
                );

                if (!keySubValidation.thereAreErrors()) {
                    final Optional<Integer> integer = extractIntegerIfAvailable(key);
                    module.get(ValidationHelper.class).assertion(
                            integer.isPresent(),
                            "InvalidTupleIndex",
                            "Invalid index for tuples.",
                            key,
                            acceptor
                    );

                    if (integer.isPresent()) {
                        final TupleType tupleType = (TupleType) restType;
                        module.get(ValidationHelper.class).assertion(
                                integer.get() >= 0 && integer.get() < tupleType.getElementTypes().size(),
                                "InvalidTupleIndex",
                                "Index out of range. Index: " + integer.get() + "; tuple length: " + tupleType.getElementTypes().size(),
                                key,
                                acceptor
                        );
                    }
                }

            } else if (module.get(TypeHelper.class).TEXT.isAssignableFrom(restType)) {
                module.get(ValidationHelper.class).assertExpectedType(
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
                }

            } else {
                //It's neither an array nor a list/map... error!Con
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
            }
        }
    }

    @Override
    public void validateAssignment(
            ReversedTrailerChain rest, String assignmentOperator, Maybe<RValueExpression> rValueExpression,
            IJadescriptType typeOfRExpr, ValidationMessageAcceptor acceptor
    ) {


        InterceptAcceptor restSubValidation = new InterceptAcceptor(acceptor);
        rest.validate(restSubValidation);
        if (restSubValidation.thereAreErrors()) {
            return;
        }
        IJadescriptType restType = rest.inferType();
        if (rest.getElements().isEmpty()) {
            // ERROR! however, a dotted identifier trailer without nothing before should be syntactically impossible... internal error system needed
            return;
        }

        InterceptAcceptor keySubValidation = new InterceptAcceptor(acceptor);
        expressionSemantics.validate(key, keySubValidation);
        IJadescriptType keyType = expressionSemantics.inferType(key);
        module.get(ValidationHelper.class).assertion(
                !module.get(TypeHelper.class).TEXT.isAssignableFrom(restType),
                "InvalidAssignment",
                "Invalid assignment; values of 'text' are immutable.",
                key,
                keySubValidation
        );

        if (!keySubValidation.thereAreErrors()) {

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
            }
        }
    }

    @Override
    public void syntacticValidateLValue(InterceptAcceptor acceptor) {
        //a dot notation chain ending with [] operator is always a valid lvalue
    }

    @Override
    public String compileAssignment(
            ReversedTrailerChain rest,
            String compiledExpression,
            IJadescriptType exprType
    ) {
        String restCompiled = rest.compile().orElse("");
        IJadescriptType restType = rest.inferType();


        if (restType instanceof ListType) {
            return restCompiled + ".set("
                    + expressionSemantics.compile(key).orElse("")
                    + ", " + compiledExpression + ")";
        } else if (restType instanceof MapType) {
            return restCompiled + ".put("
                    + expressionSemantics.compile(key).orElse("")
                    + ", " + compiledExpression + ")";
        } else {
            return restCompiled + "[" + expressionSemantics.compile(key).orElse("") + "] = " + compiledExpression;
        }
    }

    @Override
    public boolean isAlwaysPure(ReversedTrailerChain rest) {
        return true;
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(ReversedTrailerChain rest) {
        List<SemanticsBoundToExpression<?>> result = new ArrayList<>();
        result.add(new SemanticsBoundToExpression<>(expressionSemantics, key));
        result.addAll(rest.getSubExpressions());
        return result;
    }


}
