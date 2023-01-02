package it.unipr.ailab.jadescript.semantics;

import com.google.common.collect.Streams;
import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.NamedArgumentList;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.SimpleArgumentList;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.Symbol;
import it.unipr.ailab.jadescript.semantics.expression.AssignableExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.MethodCall;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.jadescript.semantics.utils.Util.Tuple2;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 28/02/2020.
 */
@Singleton
public class MethodCallSemantics
    extends AssignableExpressionSemantics<MethodCall> {


    public MethodCallSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
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

    @Override
    protected boolean mustTraverse(Maybe<MethodCall> input) {
        return false;
    }

    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverse(Maybe<MethodCall> input) {
        return Optional.empty();
    }

    private Maybe<SimpleArgumentList> extractSimpleArgs(
        Maybe<MethodCall> input
    ) {
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

    @Override
    protected String compileInternal(
        Maybe<MethodCall> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        Maybe<String> name = input.__(MethodCall::getName);
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();

        if (name.isNothing()) {
            return "";
        }

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        String nameSafe = name.toNullable();

        if (noArgs || simpleArgs.isPresent()) {
            final List<Maybe<RValueExpression>> args = toListOfMaybes(
                simpleArgs.__(SimpleArgumentList::getExpressions)
            );

            int argsize = noArgs ? 0 : args.size();

            List<String> compiledArgs = new ArrayList<>(argsize);
            List<IJadescriptType> argTypes = new ArrayList<>(argsize);
            StaticState newState = state;
            for (Maybe<RValueExpression> arg : args) {
                compiledArgs.add(rves.compile(arg, newState, acceptor));
                argTypes.add(rves.inferType(arg, newState));
                newState = rves.advance(arg, newState);
            }

            Optional<? extends CallableSymbol> methodsFound = newState.searchAs(
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
                final List<String> compiledRexprs =
                    module.get(CompilationHelper.class)
                        .adaptAndCompileRValueList(
                            compiledArgs,
                            argTypes,
                            method.parameterTypes()
                        );
                return method.compileInvokeByArity("", compiledRexprs);
            }
            // Falling back to common invocation
            return name + "(" + String.join(", ", compiledArgs) + ")";

        } else if (namedArgs.isPresent()) {
            List<String> argNames = namedArgs
                .__(NamedArgumentList::getParameterNames)
                .extract(Maybe::nullAsEmptyList);
            Map<String, ? extends RValueExpression> args = Streams.zip(
                argNames.stream(),
                ((List<? extends RValueExpression>) namedArgs
                    .__(NamedArgumentList::getParameterValues)
                    .extract(Maybe::nullAsEmptyList)).stream(),
                Tuple2::new
            ).collect(Collectors.toMap(
                Tuple2::get_1,
                Tuple2::get_2
            ));

            final Map<String, String> compiledArgs = new HashMap<>();
            final Map<String, IJadescriptType> argTypes = new HashMap<>();
            StaticState newState = state;
            for (String argName : argNames) {
                final Maybe<RValueExpression> expr = of(args.get(argName));
                String compiled = rves.compile(expr, newState, acceptor);
                compiledArgs.put(argName, compiled);
                IJadescriptType type = rves.inferType(expr, newState);
                argTypes.put(argName, type);
                newState = rves.advance(expr, newState);
            }

            Optional<? extends CallableSymbol> methodsFound =
                module.get(ContextManager.class).currentContext()
                    .searchAs(
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

            //noinspection OptionalIsPresent
            if (methodsFound.isPresent()) {

                Map<String, String> compiledRexprs = new HashMap<>();
                for (String argName : argNames) {
                    final IJadescriptType destType =
                        methodsFound.get().parameterTypesByName().get(argName);
                    String compiled = compiledArgs.get(argName);
                    IJadescriptType type = argTypes.get(argName);
                    if (destType != null) {
                        compiled = module.get(TypeHelper.class)
                            .compileWithEventualImplicitConversions(
                                compiled, type, destType
                            );
                    }
                    compiledRexprs.put(argName, compiled);
                }


                return methodsFound.get().compileInvokeByName(
                    "",
                    compiledRexprs
                );
            } else {
                StringJoiner joiner = new StringJoiner(", ");
                for (String argName : argNames) {
                    String s = compiledArgs.get(argName);
                    joiner.add(s);
                }
                return name + "(" + joiner + ")";
            }
        } else {
            return "";
        }

    }

    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<MethodCall> input,
        StaticState state
    ) {
        //TODO continue from here 2023-01-02 21:22 UTC+1:00
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        Maybe<String> name = input.__(MethodCall::getName);

        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        AtomicReference<IJadescriptType> result =
            new AtomicReference<>(module.get(TypeHelper.class).ANY);

        name.safeDo(nameSafe -> {
            if (noArgs) {
                Optional<? extends CallableSymbol> methodsFound =
                    module.get(ContextManager.class).currentContext().searchAs(
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
                        Optional<? extends CallableSymbol> methodsFound =
                            module.get(ContextManager.class).currentContext().searchAs(
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
                        Optional<? extends CallableSymbol> methodsFound =
                            module.get(ContextManager.class).currentContext().searchAs(
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

    @Override
    protected boolean validateInternal(
        Maybe<MethodCall> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        Maybe<String> name = input.__(MethodCall::getName);
        boolean isProcedure =
            input.__(MethodCall::isProcedure).extract(nullAsTrue);

        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        String procOrFunc = isProcedure ? "procedure" : "function";
        String procOrFuncCaps = isProcedure ? "Procedure" : "Function";
        String errorCode = "Invalid" + procOrFuncCaps + "Call";

        final Maybe<? extends EObject> extractedEObject =
            Util.extractEObject(input);
        if (name.isNothing() || extractedEObject.isNothing()) {
            return VALID;
        }
        String nameSafe = name.toNullable();
        EObject inputSafe = extractedEObject.toNullable();

        boolean argumentsValidationResult = VALID;

        final Maybe<EList<RValueExpression>> exprs;
        if (simpleArgs.isPresent()) {
            exprs = simpleArgs.__(SimpleArgumentList::getExpressions);
        } else if (namedArgs.isPresent()) {
            exprs = namedArgs.__(NamedArgumentList::getParameterValues);
        } else {
            exprs = Maybe.nothing();
        }

        for (Maybe<RValueExpression> rvalExpr : Maybe.toListOfMaybes(exprs)) {
            argumentsValidationResult = argumentsValidationResult &&
                module.get(RValueExpressionSemantics.class).validate(
                    rvalExpr, ,
                    acceptor
                );
        }


        if (argumentsValidationResult == INVALID) {
            return INVALID;
        }

        List<CallableSymbol> methodsFound = new ArrayList<>();
        String signature;
        if (simpleArgs.isPresent()) {
            int argsize =
                Maybe.toListOfMaybes(simpleArgs.__(SimpleArgumentList::getExpressions)).size();
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

            signature = Util.getSignature(nameSafe, argsize);
        } else if (namedArgs.isPresent()) {
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

            List<Maybe<RValueExpression>> args =
                toListOfMaybes(namedArgs.__(NamedArgumentList::getParameterValues));
            List<Maybe<String>> argNames =
                toListOfMaybes(namedArgs.__(NamedArgumentList::getParameterNames));

            signature = Util.getSignature(
                nameSafe,
                args.stream()
                    .map(input1 -> module.get(RValueExpressionSemantics.class).inferType(input1, ))
                    .collect(Collectors.toList()),
                argNames.stream()
                    .flatMap(Maybe::filterNulls)
                    .collect(Collectors.toList())
            );
        } else {
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


            signature = Util.getSignature(nameSafe, 0);
        }


        if (methodsFound.isEmpty()) {
            return module.get(ValidationHelper.class).emitError(
                errorCode,
                "Cannot resolve " + procOrFunc + ": " + signature,
                input,
                acceptor
            );
        } else if (methodsFound.size() > 1) {
            List<String> candidatesMessages = new ArrayList<>();
            for (CallableSymbol match : methodsFound) {
                candidatesMessages.add(Util.getSignature(
                    nameSafe,
                    match.parameterTypes(),
                    match.parameterNames()
                ) + " in " + match.sourceLocation() + ";");
            }

            return module.get(ValidationHelper.class).emitError(
                errorCode,
                "Ambiguous " + procOrFunc + " call: " + signature + ". " +
                    "Candidates:" +
                    "\n• " +
                    String.join("\n• ", candidatesMessages),
                input,
                acceptor
            );
        } else {
            CallableSymbol match = methodsFound.get(0);

            boolean isCorrectOperationKind =
                module.get(ValidationHelper.class).assertion(
                    isProcedure == module.get(TypeHelper.class).VOID.typeEquals(match.returnType()),
                    errorCode,
                    "'" + nameSafe + "' is not a " + procOrFunc,
                    input,
                    acceptor
                );

            if (isCorrectOperationKind == VALID && !noArgs) {
                List<RValueExpression> argExpressions = eitherCall(simpleArgs
                    , namedArgs,
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

                // function that defines the change in position from old
                // index to new index,
                //      after the eventual sorting for named argument invocation
                // (given an index of the called method parameter, returns
                // the position of the corresponding
                //    expression EObject given as argument)
                Function<Integer, Integer> rearrangementFunction =
                    eitherCall(simpleArgs, namedArgs,
                        s -> i -> i, // identity for call by arity
                        namedArgsSafe -> (Function<Integer, Integer>) i -> {
                            return callNamesMap.get(match.parameterNames().get(i));
                        }
                    ).orElse(i -> i);

                EReference metaObject = eitherCall(simpleArgs, namedArgs,
                    j -> JadescriptPackage.eINSTANCE.getSimpleArgumentList_Expressions(),
                    j -> JadescriptPackage.eINSTANCE.getNamedArgumentList_ParameterValues()
                ).toNullable();


                boolean paramTypeCheck = VALID;
                for (int i = 0; i < argExpressions.size(); i++) {
                    IJadescriptType argType =
                        module.get(RValueExpressionSemantics.class)
                            .inferType(Maybe.of(argExpressions.get(i)), );
                    IJadescriptType paramType = match.parameterTypes().get(i);

                    final boolean argCheck =
                        module.get(ValidationHelper.class).assertExpectedType(
                            paramType,
                            argType,
                            "InvalidArgumentType",
                            eitherGet(simpleArgs, namedArgs),
                            metaObject,
                            rearrangementFunction.apply(i),
                            acceptor
                        );
                    paramTypeCheck = paramTypeCheck && argCheck;
                }

                return paramTypeCheck;
            }

            return isCorrectOperationKind;
        }

    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<MethodCall> input
    ) {
        //only arguments can be sub-expressions
        return Maybe.toListOfMaybes(eitherCall(
                extractSimpleArgs(input), extractNamedArgs(input),
                SimpleArgumentList::getExpressions,
                NamedArgumentList::getParameterValues
            )).stream()
            .map(x -> new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), x));
    }


    @Override
    protected boolean isHoledInternal(
        Maybe<MethodCall> input,
        StaticState state
    ) {
        return subExpressionsAnyHoled(input, );
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<MethodCall> input,
        StaticState state
    ) {
        /*
        Functional-notation patterns are identified by name and number of
        arguments, and, when resolved, have always
         a compile-time-known non-holed type. Therefore, they are never
         typely-holed, even when their arguments
         are/have holes.
        */
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<MethodCall> input,
        StaticState state
    ) {
        return subExpressionsAnyUnbound(input, );
    }

    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<MethodCall> input,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        final Maybe<? extends CallableSymbol> method =
            resolve(input.getPattern());

        Maybe<SimpleArgumentList> simpleArgs =
            extractSimpleArgs(input.getPattern());
        Maybe<NamedArgumentList> namedArgs =
            extractNamedArgs(input.getPattern());
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        List<Maybe<RValueExpression>> argExpressions;
        if (noArgs) {
            argExpressions = Collections.emptyList();
        } else if (simpleArgs.isPresent()) {
            argExpressions =
                toListOfMaybes(simpleArgs.__(SimpleArgumentList::getExpressions));
        } else /*(namedArgs.isPresent())*/ {
            argExpressions =
                toListOfMaybes(namedArgs.__(NamedArgumentList::getParameterValues));
        }


        if (method.isPresent()) {
            final RValueExpressionSemantics rves =
                module.get(RValueExpressionSemantics.class);
            CallableSymbol m = method.toNullable();
            List<IJadescriptType> patternTermTypes = m.parameterTypes();
            if (namedArgs.isPresent()) {
                List<String> argNames =
                    toListOfMaybes(namedArgs.__(NamedArgumentList::getParameterNames)).stream()
                        .map(Maybe::toNullable)
                        .collect(Collectors.toList());
                argExpressions = sortToMatchParamNames(argExpressions,
                    argNames, m.parameterNames()
                );
            }
            List<PatternMatcher> subResults =
                new ArrayList<>(argExpressions.size());
            for (int i = 0; i < argExpressions.size(); i++) {
                Maybe<RValueExpression> term = argExpressions.get(i);
                IJadescriptType upperBound = patternTermTypes.get(i);
                final PatternMatcher termOutput =
                    rves.compilePatternMatch(input.subPattern(
                        upperBound,
                        __ -> term.toNullable(),
                        "_" + i
                    ), , acceptor);
                subResults.add(termOutput);
            }

            PatternType patternType = inferPatternType(input.getPattern(),
                input.getMode(),
                );
            IJadescriptType solvedPatternType =
                patternType.solve(input.getProvidedInputType());

            List<String> compiledSubInputs =
                new ArrayList<>(m.parameterNames().size());
            for (int i = 0; i < subResults.size(); i++) {
                compiledSubInputs.add("__x.get" + Strings.toFirstUpper(m.parameterNames().get(i)) + "()");
            }


            return input.createCompositeMethodOutput(
                solvedPatternType,
                i -> (i < 0 || i >= compiledSubInputs.size())
                    ? "/*IndexOutOfBounds*/"
                    : compiledSubInputs.get(i),
                subResults
            );

        } else {
            return input.createEmptyCompileOutput();
        }
    }


    @Override
    public boolean isAlwaysPureInternal(
        Maybe<MethodCall> input,
        StaticState state
    ) {
        final Maybe<? extends CallableSymbol> resolve = resolve(input);
        return resolve.__(CallableSymbol::isPure).extract(nullAsTrue)
            && subExpressionsAllAlwaysPure(input, state);
    }

    @Override
    public boolean isPatternEvaluationPureInternal(
        PatternMatchInput<MethodCall> input,
        StaticState state
    ) {
        //TODO this assumption (if its pure as call, then its pure as pattern
        // evaluation) is not valid when the new
        // pattern resolution system will be introduced
        final Maybe<? extends CallableSymbol> resolve = resolve(input);
        return resolve.__(CallableSymbol::isPure).extract(nullAsTrue)
            && subPatternEvaluationsAllPure(input, state);
    }

    @Override
    public PatternType inferPatternTypeInternal(
        Maybe<MethodCall> input,
        StaticState state
    ) {
        final Maybe<? extends CallableSymbol> method = resolve(input);
        if (method.isPresent()) {
            return PatternType.simple(method.toNullable().returnType());
        } else {
            return PatternType.empty(module);
        }

    }

    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<MethodCall> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        final List<? extends CallableSymbol> methods =
            resolveCandidates(input.getPattern());
        Maybe<MethodCall> patternCall = input.getPattern();
        Maybe<SimpleArgumentList> simpleArgs =
            extractSimpleArgs(input.getPattern());
        Maybe<NamedArgumentList> namedArgs =
            extractNamedArgs(input.getPattern());
        Maybe<String> name = input.getPattern().__(MethodCall::getName);
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        List<Maybe<RValueExpression>> argExpressions;
        if (noArgs) {
            argExpressions = Collections.emptyList();
        } else if (simpleArgs.isPresent()) {
            argExpressions =
                toListOfMaybes(simpleArgs.__(SimpleArgumentList::getExpressions));
        } else /*(namedArgs.isPresent())*/ {
            argExpressions =
                toListOfMaybes(namedArgs.__(NamedArgumentList::getParameterValues));
        }

        if (methods.size() == 0) {
            if (patternCall.isPresent()) {
                acceptor.acceptError(
                    "Cannot resolve structural pattern: "
                        + Util.getSignature(
                        name.orElse(""),
                        argExpressions.size()
                    ),
                    patternCall.toNullable().getProxyEObject(),
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidPattern"
                );
            }
            return VALID;
        } else if (methods.size() > 1) {
            if (patternCall.isPresent()) {
                List<String> candidatesMessage = new ArrayList<>();
                for (CallableSymbol c : methods) {
                    candidatesMessage.add(Util.getSignature(
                        c.name(),
                        c.parameterTypes()
                    ) + " in " +
                        c.sourceLocation() + ";");
                }

                acceptor.acceptError(
                    "Ambiguous pattern resolution: "
                        + Util.getSignature(
                        name.orElse(""),
                        argExpressions.size()
                    )
                        + ". Candidates: \n• " + String.join(
                        "\n•",
                        candidatesMessage
                    ),
                    patternCall.toNullable().getProxyEObject(),
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidPattern"
                );
            }
            return VALID;
        } else { // => methods.size() == 1
            //TODO this should ensure that the resolved method corresponds to
            // a pattern-matchable value
            // => find a metadata method created for this OR use an actual
            // method
            final RValueExpressionSemantics rves =
                module.get(RValueExpressionSemantics.class);
            CallableSymbol m = methods.get(0);
            List<IJadescriptType> patternTermTypes = m.parameterTypes();
            if (namedArgs.isPresent()) {
                List<String> argNames =
                    toListOfMaybes(namedArgs.__(NamedArgumentList::getParameterNames)).stream()
                        .map(Maybe::toNullable)
                        .collect(Collectors.toList());
                argExpressions = sortToMatchParamNames(argExpressions,
                    argNames, m.parameterNames()
                );
            }

            boolean allArgsCheck = VALID;
            for (int i = 0; i < argExpressions.size(); i++) {
                Maybe<RValueExpression> term = argExpressions.get(i);
                IJadescriptType upperBound = patternTermTypes.get(i);
                boolean argCheck = rves.validatePatternMatch(
                    input.subPattern(
                        upperBound,
                        __ -> term.toNullable(),
                        "_" + i
                    ), ,
                    acceptor
                );
                allArgsCheck = allArgsCheck && argCheck;
            }

            return allArgsCheck;
        }
    }

    public List<? extends CallableSymbol> resolveCandidates(Maybe<MethodCall> input) {
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        Maybe<String> name = input.__(MethodCall::getName);
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();

        return name.__(nameSafe -> {
            if (noArgs) {
                return module.get(ContextManager.class).currentContext().searchAs(
                        CallableSymbol.Searcher.class,
                        searcher -> searcher.searchCallable(
                            nameSafe,
                            CallableSymbol.Searcher.ANY_RETURN_TYPE,
                            (s, n) -> s == 0,
                            (s, t) -> s == 0
                        )
                    ).filter(Util.dinstinctBy(Symbol::sourceLocation))
                    .collect(Collectors.toList());
            } else if (simpleArgs.isPresent()) {
                final SimpleArgumentList simpleArgsSafe =
                    simpleArgs.toNullable();
                int argsize = simpleArgsSafe.getExpressions().size();
                return module.get(ContextManager.class).currentContext().searchAs(
                        CallableSymbol.Searcher.class,
                        searcher -> searcher.searchCallable(
                            nameSafe,
                            null,
                            (s, n) -> s == argsize,
                            (s, t) -> s == argsize
                        )
                    ).filter(Util.dinstinctBy(Symbol::sourceLocation))
                    .collect(Collectors.toList());
            } else /*(namedArgs.isPresent())*/ {
                return module.get(ContextManager.class).currentContext().searchAs(
                        CallableSymbol.Searcher.class,
                        searcher -> searcher.searchCallable(
                            nameSafe,
                            CallableSymbol.Searcher.ANY_RETURN_TYPE,
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

            }
        }).orElseGet(Collections::emptyList);
    }

    public Maybe<? extends CallableSymbol> resolve(Maybe<MethodCall> input) {
        final List<? extends CallableSymbol> callableSymbols =
            resolveCandidates(input);
        if (callableSymbols.size() == 1) {
            return Maybe.of(callableSymbols.get(0));
        } else {
            return Maybe.nothing();
        }
    }

    public boolean resolves(Maybe<MethodCall> input) {
        return resolve(input).isPresent();
    }

    @Override
    public boolean isValidLExprInternal(Maybe<MethodCall> input) {
        return false;
    }

    @Override
    public boolean canBeHoledInternal(Maybe<MethodCall> input) {
        return true;
    }

    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(Maybe<MethodCall> input, StaticState state) {
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        Maybe<String> name = input.__(MethodCall::getName);
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        if (noArgs && isAlwaysPure(input, ) && name.isPresent() && !name.toNullable().isBlank()) {
            return List.of(name.toNullable());
        }
        return List.of();
    }

    @Override
    protected StaticState advanceInternal(
        Maybe<MethodCall> input,
        StaticState state
    ) {
        return ExpressionTypeKB.empty();
    }
}
