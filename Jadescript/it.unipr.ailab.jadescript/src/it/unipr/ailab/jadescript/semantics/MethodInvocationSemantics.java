package it.unipr.ailab.jadescript.semantics;

import com.google.common.collect.Streams;
import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.NamedArgumentList;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.SimpleArgumentList;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.Symbol;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.MethodCall;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.ProxyEObject;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.jadescript.semantics.utils.Util.Tuple2;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 28/02/2020.
 */
@Singleton
public class MethodInvocationSemantics extends Semantics<MethodCall> {

    public MethodInvocationSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    private Maybe<SimpleArgumentList> extractSimpleArgs(Maybe<MethodCall> input) {
        if (input.isPresent()) {
            return input.toNullable().getSimpleArgs();
        } else {
            return nothing();
        }
    }

    private Maybe<NamedArgumentList> extractNamedArgs(Maybe<MethodCall> input) {
        if (input.isPresent()) {
            return input.toNullable().getNamedArgs();
        } else {
            return nothing();
        }
    }

    public String compile(
            Maybe<MethodCall> input
    ) {
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        Maybe<String> name = input.__(MethodCall::getName);
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();

        return name.__(nameSafe -> {

            if (noArgs || simpleArgs.isPresent()) {
                SimpleArgumentList argumentsNotSafe = simpleArgs.toNullable();
                final List<RValueExpression> argumentsSafe;
                if (argumentsNotSafe != null && argumentsNotSafe.getExpressions()!=null) {
                    argumentsSafe = argumentsNotSafe.getExpressions();
                }else{
                    argumentsSafe = List.of();
                }

                int argsize = noArgs ? 0 : argumentsSafe.size();
                Optional<? extends CallableSymbol> methodsFound = module.get(ContextManager.class).currentContext().searchAs(
                        CallableSymbol.Searcher.class,
                        searcher -> searcher.searchCallable(
                                nameSafe,
                                null,
                                (s, n) -> s == argsize,
                                (s, t) -> s == argsize
                        )
                ).findFirst();

                if (methodsFound.isPresent()) {
                    CallableSymbol method = methodsFound.get();
                    final List<String> compiledRexprs = module.get(CompilationHelper.class).adaptAndCompileRValueList(
                            argumentsSafe,
                            method.parameterTypes()
                    );
                    return method.compileInvokeByArity("", compiledRexprs);
                }
                // Falling back to common invocation
                return name + "(" + argumentsSafe.stream()
                        .map(Maybe::of)
                        .map(module.get(RValueExpressionSemantics.class)::compile)
                        .map(x -> x.orElse(""))
                        .collect(Collectors.joining(", ")) + ")";

            } else if (namedArgs.isPresent()) {
                Optional<? extends CallableSymbol> methodsFound = module.get(ContextManager.class).currentContext().searchAs(
                        CallableSymbol.Searcher.class,
                        searcher -> searcher.searchCallable(
                                nameSafe,
                                null,
                                (s, n) -> namedArgs
                                        .__(NamedArgumentList::getParameterNames)
                                        .__(List::size)
                                        .wrappedEquals(s),
                                (s, t) -> namedArgs
                                        .__(NamedArgumentList::getParameterValues)
                                        .__(List::size)
                                        .wrappedEquals(s)
                        )
                ).findFirst();

                if (methodsFound.isPresent()) {
                    return methodsFound.get().compileInvokeByName("", compileNamedArgs(
                            namedArgs.__(NamedArgumentList::getParameterNames).extract(Maybe::nullAsEmptyList),
                            namedArgs.__(NamedArgumentList::getParameterValues).extract(Maybe::nullAsEmptyList),
                            methodsFound.get().parameterTypesByName()
                    ));
                }
                return name + "(" + toListOfMaybes(namedArgs.__(NamedArgumentList::getParameterValues))
                        .stream()
                        .map(module.get(RValueExpressionSemantics.class)::compile)
                        .map(s -> s.orElse(""))
                        .collect(Collectors.joining(", ")) + ")";
            } else {
                //noinspection ReturnOfNull
                return null;
            }
        }).extract(nullAsEmptyString);

    }

    private Map<String, String> compileNamedArgs(
            List<String> argNames,
            List<? extends RValueExpression> argRexprs,
            Map<String, IJadescriptType> namedParameters
    ) {
        Map<String, ? extends RValueExpression> args = Streams.zip(
                argNames.stream(),
                argRexprs.stream(),
                Tuple2::new
        ).collect(Collectors.toMap(
                Tuple2::get_1,
                Tuple2::get_2
        ));

        Map<String, String> result = new HashMap<>();
        for (String name : argNames) {
            final Maybe<RValueExpression> expr = of(args.get(name));
            IJadescriptType type = module.get(RValueExpressionSemantics.class).inferType(expr);
            String compiled = module.get(RValueExpressionSemantics.class).compile(expr).orElse("");
            final IJadescriptType destType = namedParameters.get(name);
            if (destType != null) {
                compiled = module.get(TypeHelper.class).compileWithEventualImplicitConversions(
                        compiled, type, destType
                );
            }
            result.put(name, compiled);
        }
        return result;

    }


    public IJadescriptType inferType(
            Maybe<MethodCall> input
    ) {
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        Maybe<String> name = input.__(MethodCall::getName);

        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        AtomicReference<IJadescriptType> result = new AtomicReference<>(module.get(TypeHelper.class).ANY);

        name.safeDo(nameSafe -> {
            if (noArgs) {
                Optional<? extends CallableSymbol> methodsFound = module.get(ContextManager.class).currentContext().searchAs(
                        CallableSymbol.Searcher.class,
                        searcher -> searcher.searchCallable(
                                nameSafe,
                                null,
                                (s, n) -> s == 0,
                                (s, t) -> s == 0
                        )
                ).findFirst();

                if (methodsFound.isPresent()) {
                    result.set(methodsFound.get().returnType());
                } else {
                    result.set(module.get(TypeHelper.class).ANY);
                }
            } else {
                eitherDo(simpleArgs, namedArgs,
                        //case simpleArgs!=null
                        simpleArgsSafe -> {
                            int argsize = simpleArgsSafe.getExpressions().size();
                            Optional<? extends CallableSymbol> methodsFound = module.get(ContextManager.class).currentContext().searchAs(
                                    CallableSymbol.Searcher.class,
                                    searcher -> searcher.searchCallable(
                                            nameSafe,
                                            null,
                                            (s, n) -> s == argsize,
                                            (s, t) -> s == argsize
                                    )
                            ).findFirst();


                            if (methodsFound.isPresent()) {
                                result.set(methodsFound.get().returnType());
                            } else {
                                result.set(module.get(TypeHelper.class).ANY);
                            }
                        },
                        //case namedArgs!=null
                        namedArgsSafe -> {
                            Optional<? extends CallableSymbol> methodsFound = module.get(ContextManager.class).currentContext().searchAs(
                                    CallableSymbol.Searcher.class,
                                    searcher -> searcher.searchCallable(
                                            nameSafe,
                                            null,
                                            (s, n) -> namedArgs
                                                    .__(NamedArgumentList::getParameterNames)
                                                    .__(List::size)
                                                    .wrappedEquals(s),
                                            (s, t) -> namedArgs
                                                    .__(NamedArgumentList::getParameterValues)
                                                    .__(List::size)
                                                    .wrappedEquals(s)
                                    )
                            ).findFirst();

                            if (methodsFound.isPresent()) {
                                result.set(methodsFound.get().returnType());
                            } else {
                                result.set(module.get(TypeHelper.class).ANY);
                            }
                        }
                );
            }

        });

        return of(result.get()).orElse(module.get(TypeHelper.class).ANY);

    }


    public void validate(Maybe<MethodCall> input, ValidationMessageAcceptor acceptor) {
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        Maybe<String> name = input.__(MethodCall::getName);
        boolean isProcedure = input.__(MethodCall::isProcedure).extract(nullAsTrue);

        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        String procOrFunc = isProcedure ? "procedure" : "function";
        String procOrFuncCaps = isProcedure ? "Procedure" : "Function";
        String errorCode = "Invalid" + procOrFuncCaps + "Call";

        safeDo(name, input.__(ProxyEObject::getProxyEObject),
                /*NULLSAFE REGION*/(nameSafe, inputSafe) -> {
                    //this portion of code is done  only if name and input
                    // are != null (and everything in the dotchains that generated them is !=null too)
                    InterceptAcceptor argumentsValidationResult = new InterceptAcceptor(acceptor);
                    if (!noArgs) {
                        eitherDo(simpleArgs, namedArgs,
                                //case simpleArgs!=null
                                simpleArgsSafe -> {
                                    for (RValueExpression rvalexpr : simpleArgsSafe.getExpressions()) {
                                        module.get(RValueExpressionSemantics.class).validate(Maybe.of(rvalexpr), argumentsValidationResult);
                                    }
                                },
                                //case namedArgs!=null
                                namedArgsSafe -> {
                                    for (RValueExpression rvalexpr : namedArgsSafe.getParameterValues()) {
                                        module.get(RValueExpressionSemantics.class).validate(Maybe.of(rvalexpr), argumentsValidationResult);
                                    }
                                }
                        );
                    }


                    if (argumentsValidationResult.thereAreErrors()) {
                        return;
                    }

                    List<CallableSymbol> methodsFound = new ArrayList<>();
                    AtomicReference<String> signature = new AtomicReference<>("");
                    if (noArgs) {
                        //case no args
                        methodsFound.addAll(module.get(ContextManager.class).currentContext().searchAs(
                                CallableSymbol.Searcher.class,
                                searcher -> searcher.searchCallable(
                                        nameSafe,
                                        null,
                                        (s, n) -> s == 0,
                                        (s, t) -> s == 0
                                )
                        ).filter(Util.dinstinctBy(Symbol::sourceLocation))
                                .collect(Collectors.toList()));


                        signature.set(Util.getSignature(nameSafe, 0));
                    } else {
                        eitherDo(simpleArgs, namedArgs,
                                //case simpleArgs!=null
                                simpleArgsSafe -> {
                                    int argsize = simpleArgsSafe.getExpressions().size();
                                    methodsFound.addAll(module.get(ContextManager.class).currentContext().searchAs(
                                            CallableSymbol.Searcher.class,
                                            searcher -> searcher.searchCallable(
                                                    nameSafe,
                                                    null,
                                                    (s, n) -> s == argsize,
                                                    (s, t) -> s == argsize
                                            )
                                    ).filter(Util.dinstinctBy(Symbol::sourceLocation))
                                            .collect(Collectors.toList()));

                                    signature.set(Util.getSignature(nameSafe, simpleArgsSafe.getExpressions().size()));
                                },
                                //case namedArgs!=null
                                namedArgsSafe -> {
                                    methodsFound.addAll(module.get(ContextManager.class).currentContext().searchAs(
                                            CallableSymbol.Searcher.class,
                                            searcher -> searcher.searchCallable(
                                                    nameSafe,
                                                    null,
                                                    (s, n) -> namedArgs
                                                            .__(NamedArgumentList::getParameterNames)
                                                            .__(List::size)
                                                            .wrappedEquals(s),
                                                    (s, t) -> namedArgs
                                                            .__(NamedArgumentList::getParameterValues)
                                                            .__(List::size)
                                                            .wrappedEquals(s)
                                            )
                                    ).filter(Util.dinstinctBy(Symbol::sourceLocation))
                                            .collect(Collectors.toList()));

                                    List<Maybe<RValueExpression>> args = toListOfMaybes(namedArgs.__(NamedArgumentList::getParameterValues));
                                    List<Maybe<String>> argNames = toListOfMaybes(namedArgs.__(NamedArgumentList::getParameterNames));

                                    signature.set(Util.getSignature(
                                            nameSafe,
                                            args.stream()
                                                    .map(module.get(RValueExpressionSemantics.class)::inferType)
                                                    .collect(Collectors.toList()),
                                            argNames.stream()
                                                    .flatMap(Maybe::filterNulls)
                                                    .collect(Collectors.toList())
                                    ));
                                }
                        );
                    }


                    if (methodsFound.isEmpty()) {
                        acceptor.acceptError(
                                "cannot resolve " + procOrFunc + ": " + signature,
                                inputSafe,
                                null,
                                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                                errorCode
                        );

                    } else if (methodsFound.size() > 1) {
                        List<String> candidatesMessages = new ArrayList<>();
                        for (CallableSymbol match : methodsFound) {
                            candidatesMessages.add(Util.getSignature(
                                    nameSafe,
                                    match.parameterTypes(),
                                    match.parameterNames()
                            )+" in "+match.sourceLocation()+";");
                        }

                        acceptor.acceptError(
                                "Ambiguous " + procOrFunc + " call: " + signature + ". Candidates:" +
                                        "\n??? " +
                                        String.join("\n??? ", candidatesMessages),
                                inputSafe,
                                null,
                                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                                errorCode
                        );
                    } else {
                        CallableSymbol match = methodsFound.get(0);

                        InterceptAcceptor isCorrectOperationKindCheck = new InterceptAcceptor(acceptor);

                        boolean isCorrectOperationKind = isProcedure == module.get(TypeHelper.class).VOID
                                .typeEquals(match.returnType());
                        module.get(ValidationHelper.class).assertion(
                                isCorrectOperationKind,
                                errorCode,
                                "'" + nameSafe + "' is not a " + procOrFunc,
                                input,
                                isCorrectOperationKindCheck
                        );

                        if (!isCorrectOperationKindCheck.thereAreErrors() && !noArgs) {

                            List<RValueExpression> argExpressions = eitherCall(simpleArgs, namedArgs,
                                    SimpleArgumentList::getExpressions,
                                    namedArgsSafe -> sortToMatchParamNames(
                                            namedArgsSafe.getParameterValues(),
                                            namedArgsSafe.getParameterNames(),
                                            match.parameterNames()
                                    )
                            ).extract(Maybe::nullAsEmptyList);

                            HashMap<String, Integer> callNamesMap = new HashMap<>();
                            if (namedArgs.isPresent()) {
                                NamedArgumentList namedArgsSafe = namedArgs.toNullable();
                                for (int i = 0; i < namedArgsSafe.getParameterNames().size(); i++) {
                                    callNamesMap.put(namedArgsSafe.getParameterNames().get(i), i);
                                }
                            }

                            // function that defines the change in position from old index to new index,
                            //      after the eventual sorting for named argument invocation
                            Function<Integer, Integer> rearrangementFunction = eitherCall(simpleArgs, namedArgs,
                                    s -> i -> i, // identity for call by arity
                                    namedArgsSafe -> (Function<Integer, Integer>) integer -> {
                                        return callNamesMap.get(match.parameterNames().get(integer));
                                    }
                            ).orElse(i -> i);

                            EReference metaObject = eitherCall(simpleArgs, namedArgs,
                                    j -> JadescriptPackage.eINSTANCE.getSimpleArgumentList_Expressions(),
                                    j -> JadescriptPackage.eINSTANCE.getNamedArgumentList_ParameterValues()
                            ).toNullable();

                            for (int i = 0; i < argExpressions.size(); i++) {
                                IJadescriptType argType = module.get(RValueExpressionSemantics.class).inferType(Maybe.of(argExpressions.get(i)));
                                IJadescriptType paramType = match.parameterTypes().get(i);

                                module.get(ValidationHelper.class).assertExpectedType(
                                        paramType,
                                        argType,
                                        "InvalidArgumentType",
                                        eitherGet(simpleArgs, namedArgs),
                                        metaObject,
                                        rearrangementFunction.apply(i),
                                        acceptor
                                );
                            }
                        }


                    }


                }/*END NULLSAFE REGION - (nameSafe, inputSafe)*/
        );

    }


    public List<SemanticsBoundToExpression<?>> getSubExpressions(
            Maybe<MethodCall> input
    ) {
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        //only arguments can be sub-expressions
        return Maybe.toListOfMaybes(
                        eitherCall(
                                simpleArgs, namedArgs,
                                SimpleArgumentList::getExpressions, NamedArgumentList::getParameterValues
                        )
                ).stream()
                .map(x -> new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), x))
                .collect(Collectors.toList());
    }

    public boolean resolves(
            Maybe<MethodCall> input
    ) {
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        Maybe<String> name = input.__(MethodCall::getName);
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        AtomicBoolean result = new AtomicBoolean(false);

        name.safeDo(nameSafe -> {
            if (noArgs) {
                List<? extends CallableSymbol> methodsFound = module.get(ContextManager.class).currentContext().searchAs(
                        CallableSymbol.Searcher.class,
                        searcher -> searcher.searchCallable(
                                nameSafe,
                                null,
                                (s, n) -> s == 0,
                                (s, t) -> s == 0
                        )
                ).filter(Util.dinstinctBy(Symbol::sourceLocation))
                        .collect(Collectors.toList());

                result.set(methodsFound.size() == 1);
            } else {
                eitherDo(simpleArgs, namedArgs,
                        //case simpleArgs!=null
                        simpleArgsSafe -> {
                            int argsize = simpleArgsSafe.getExpressions().size();
                            List<? extends CallableSymbol> methodsFound = module.get(ContextManager.class).currentContext().searchAs(
                                    CallableSymbol.Searcher.class,
                                    searcher -> searcher.searchCallable(
                                            nameSafe,
                                            null,
                                            (s, n) -> s == argsize,
                                            (s, t) -> s == argsize
                                    )
                            ).filter(Util.dinstinctBy(Symbol::sourceLocation))
                                    .collect(Collectors.toList());

                            result.set(methodsFound.size() == 1);
                        },
                        //case namedArgs!=null
                        namedArgsSafe -> {
                            List<? extends CallableSymbol> methodsFound = module.get(ContextManager.class).currentContext().searchAs(
                                    CallableSymbol.Searcher.class,
                                    searcher -> searcher.searchCallable(
                                            nameSafe,
                                            null,
                                            (s, n) -> namedArgs
                                                    .__(NamedArgumentList::getParameterNames)
                                                    .__(List::size)
                                                    .wrappedEquals(s),
                                            (s, t) -> namedArgs
                                                    .__(NamedArgumentList::getParameterValues)
                                                    .__(List::size)
                                                    .wrappedEquals(s)
                                    )
                            ).filter(Util.dinstinctBy(Symbol::sourceLocation))
                                    .collect(Collectors.toList());

                            result.set(methodsFound.size() == 1);
                        }
                );
            }

        });

        return result.get();
    }

    public static <T> List<T> sortToMatchParamNames(
            List<T> args,
            List<String> argNames,
            List<String> paramNames
    ) {
        List<HashMap.SimpleEntry<Integer, T>> tmp = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            T arg = args.get(i);
            Integer x = paramNames.indexOf(argNames.get(i));
            tmp.add(new HashMap.SimpleEntry<>(x, arg));
        }
        return tmp.stream()
                .sorted(Comparator.comparingInt(AbstractMap.SimpleEntry::getKey))
                .map(AbstractMap.SimpleEntry::getValue)
                .collect(Collectors.toList());
    }

    public static <T> List<T> sortToMatchParamNames(
            Map<String, T> namedArgs,
            List<String> paramNames
    ) {
        return paramNames.stream()
                .map(namedArgs::get)
                .collect(Collectors.toList());
    }
}
