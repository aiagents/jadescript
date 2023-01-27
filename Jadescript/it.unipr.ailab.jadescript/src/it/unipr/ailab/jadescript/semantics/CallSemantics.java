package it.unipr.ailab.jadescript.semantics;

import com.google.common.collect.Streams;
import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.NamedArgumentList;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.SimpleArgumentList;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.PatternSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.Symbol;
import it.unipr.ailab.jadescript.semantics.expression.AssignableExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.SubPattern;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.Call;
import it.unipr.ailab.jadescript.semantics.utils.ImmutableList;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.jadescript.semantics.utils.Util.Tuple2;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 28/02/2020.
 */
@Singleton
public class CallSemantics extends AssignableExpressionSemantics<Call> {


    public CallSemantics(SemanticsModule semanticsModule) {
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


    private static StaticState advanceCallByNameParameters(
        StaticState state,
        RValueExpressionSemantics rves,
        List<String> argNames,
        Map<String, Maybe<RValueExpression>> args
    ) {
        StaticState newState = state;
        for (String argName : argNames) {
            final Maybe<RValueExpression> expr = args.get(argName);
            newState = rves.advance(expr, newState);
        }
        return newState;
    }


    @Override
    protected boolean mustTraverse(Maybe<Call> input) {
        return false;
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(Maybe<Call> input) {
        return Optional.empty();
    }


    @Override
    protected void compileAssignmentInternal(
        Maybe<Call> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        // NOT USABLE AS L-EXPRESSION
    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<Call> input,
        IJadescriptType rightType,
        StaticState state
    ) {
        // NOT USABLE AS L-EXPRESSION
        return state;
    }


    @Override
    public boolean validateAssignmentInternal(
        Maybe<Call> input,
        Maybe<RValueExpression> expression,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        // NOT USABLE AS L-EXPRESSION
        return VALID;
    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<Call> input,
        ValidationMessageAcceptor acceptor
    ) {
        return errorNotLvalue(input, acceptor);
    }


    private Maybe<SimpleArgumentList> extractSimpleArgs(
        Maybe<Call> input
    ) {
        if (input.isPresent()) {
            return input.toNullable().getSimpleArgs();
        } else {
            return nothing();
        }
    }


    private Maybe<NamedArgumentList> extractNamedArgs(Maybe<Call> input) {
        if (input.isPresent()) {
            return input.toNullable().getNamedArgs();
        } else {
            return nothing();
        }
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<Call> input,
        StaticState state
    ) {
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        Maybe<String> name = input.__(Call::getName);
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();


        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        if (name.nullIf(String::isBlank).isNothing()) {
            return state;
        }
        String nameSafe = name.toNullable();

        if (noArgs || simpleArgs.isPresent()) {
            return advanceCallByArity(
                state,
                simpleArgs,
                noArgs,
                rves,
                nameSafe
            );
        } else if (namedArgs.isPresent()) {
            return advanceCallByName(
                state,
                namedArgs,
                rves,
                nameSafe
            );
        } else {
            return state;
        }
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<Call> input,
        StaticState state
    ) {
        final Maybe<? extends CallableSymbol> method = resolve(
            input.getPattern(),
            state,
            false //patterns do not advance on any argument before resolving
        );

        Maybe<SimpleArgumentList> simpleArgs =
            extractSimpleArgs(input.getPattern());
        Maybe<NamedArgumentList> namedArgs =
            extractNamedArgs(input.getPattern());
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        List<Maybe<RValueExpression>> argExpressions;
        if (noArgs) {
            argExpressions = Collections.emptyList();
        } else if (simpleArgs.isPresent()) {
            argExpressions = toListOfMaybes(
                simpleArgs.__(SimpleArgumentList::getExpressions)
            );
        } else /*(namedArgs.isPresent())*/ {
            argExpressions = toListOfMaybes(
                namedArgs.__(NamedArgumentList::getParameterValues)
            );
        }


        if (method.isNothing()) {
            return state;
        }


        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        CallableSymbol m = method.toNullable();

        List<IJadescriptType> patternTermTypes = m.parameterTypes();


        if (namedArgs.isPresent()) {
            List<String> argNames = toListOfMaybes(
                namedArgs.__(NamedArgumentList::getParameterNames)
            ).stream()
                .map(Maybe::toNullable)
                .collect(Collectors.toList());
            argExpressions = sortToMatchParamNames(
                argExpressions,
                argNames,
                m.parameterNames()
            );
        }

        StaticState runningState = state;
        for (int i = 0; i < argExpressions.size(); i++) {
            Maybe<RValueExpression> term = argExpressions.get(i);
            IJadescriptType upperBound = patternTermTypes.get(i);
            final SubPattern<RValueExpression, Call> termSubpattern =
                input.subPattern(
                    upperBound,
                    __ -> term.toNullable(),
                    "_" + i
                );
            runningState = rves.assertDidMatch(
                termSubpattern,
                runningState
            );
        }

        return runningState;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<Call> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<Call> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<Call> input,
        StaticState state
    ) {
        final Maybe<? extends PatternSymbol> ps = resolvePattern(
            input.getPattern(),
            state
        );

        Maybe<SimpleArgumentList> simpleArgs =
            extractSimpleArgs(input.getPattern());
        Maybe<NamedArgumentList> namedArgs =
            extractNamedArgs(input.getPattern());
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        List<Maybe<RValueExpression>> argExpressions;
        if (noArgs) {
            argExpressions = Collections.emptyList();
        } else if (simpleArgs.isPresent()) {
            argExpressions = toListOfMaybes(
                simpleArgs.__(SimpleArgumentList::getExpressions)
            );
        } else /*(namedArgs.isPresent())*/ {
            argExpressions = toListOfMaybes(
                namedArgs.__(NamedArgumentList::getParameterValues)
            );
        }


        if (ps.isNothing()) {
            return state;
        }


        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        PatternSymbol patternSymbol = ps.toNullable();

        List<IJadescriptType> patternTermTypes = patternSymbol.termTypes();


        if (namedArgs.isPresent()) {
            List<String> argNames = toListOfMaybes(
                namedArgs.__(NamedArgumentList::getParameterNames)
            ).stream()
                .map(Maybe::toNullable)
                .collect(Collectors.toList());
            argExpressions = sortToMatchParamNames(
                argExpressions,
                argNames,
                patternSymbol.termNames()
            );
        }

        StaticState runningState = state;
        for (int i = 0; i < argExpressions.size(); i++) {
            Maybe<RValueExpression> term = argExpressions.get(i);
            IJadescriptType upperBound = patternTermTypes.get(i);
            final SubPattern<RValueExpression, Call> termSubpattern =
                input.subPattern(
                    upperBound,
                    __ -> term.toNullable(),
                    "_structterm" + i
                );
            runningState = rves.advancePattern(
                termSubpattern,
                runningState
            );
        }

        return runningState;
    }


    private StaticState advanceCallByArity(
        StaticState state,
        Maybe<SimpleArgumentList> simpleArgs,
        boolean noArgs,
        RValueExpressionSemantics rves,
        String nameSafe
    ) {
        final List<Maybe<RValueExpression>> args = toListOfMaybes(
            simpleArgs.__(SimpleArgumentList::getExpressions)
        );
        int argsize = noArgs ? 0 : args.size();

        StaticState newState = advanceCallByArityParameters(
            state,
            rves,
            args
        );

        Optional<? extends CallableSymbol> methodsFound = newState.searchAs(
            CallableSymbol.Searcher.class,
            searcher -> searcher.searchCallable(
                nameSafe,
                null,
                (s, n) -> s == argsize,
                (s, t) -> s == argsize
            )
        ).findFirst();

        if (methodsFound.isEmpty()) {
            return state;
        }

        return newState;
    }


    private StaticState advanceCallByArityParameters(
        StaticState state,
        RValueExpressionSemantics rves,
        List<Maybe<RValueExpression>> args
    ) {
        StaticState newState = state;
        for (Maybe<RValueExpression> arg : args) {
            newState = rves.advance(arg, newState);
        }
        return newState;
    }


    private StaticState advanceCallByName(
        StaticState state, Maybe<NamedArgumentList> namedArgs,
        RValueExpressionSemantics rves, String nameSafe
    ) {
        List<String> argNames = namedArgs
            .__(NamedArgumentList::getParameterNames)
            .extract(Maybe::nullAsEmptyList);
        Map<String, Maybe<RValueExpression>> args = Streams.zip(
            argNames.stream(),
            toListOfMaybes(
                namedArgs.__(NamedArgumentList::getParameterValues)
            ).stream(),
            Tuple2::new
        ).collect(Collectors.toMap(
            Tuple2::get_1,
            Tuple2::get_2
        ));

        StaticState newState = advanceCallByNameParameters(
            state,
            rves,
            argNames,
            args
        );

        Optional<? extends CallableSymbol> methodFound =
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

        if (methodFound.isEmpty()) {
            return state;
        }

        return newState;
    }


    @Override
    protected String compileInternal(
        Maybe<Call> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        Maybe<String> name = input.__(Call::getName);
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();

        if (name.nullIf(String::isBlank).isNothing()) {
            return "";
        }

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        if (noArgs || simpleArgs.isPresent()) {
            final List<Maybe<RValueExpression>> args = toListOfMaybes(
                simpleArgs.__(SimpleArgumentList::getExpressions)
            );

            int argsize = noArgs ? 0 : args.size();

            List<String> compiledArgs = new ArrayList<>(argsize);
            List<IJadescriptType> argTypes = new ArrayList<>(argsize);
            StaticState afterArguments = state;
            for (Maybe<RValueExpression> arg : args) {
                compiledArgs.add(rves.compile(arg, afterArguments, acceptor));
                argTypes.add(rves.inferType(arg, afterArguments));
                afterArguments = rves.advance(arg, afterArguments);
            }


            Maybe<? extends CallableSymbol> methodsFound = resolve(
                input,
                afterArguments,
                false
            );


            if (methodsFound.isPresent()) {
                CallableSymbol method = methodsFound.toNullable();
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
            StaticState afterArguments = state;
            for (String argName : argNames) {
                final Maybe<RValueExpression> expr = some(args.get(argName));
                String compiled = rves.compile(expr, afterArguments, acceptor);
                compiledArgs.put(argName, compiled);
                IJadescriptType type = rves.inferType(expr, afterArguments);
                argTypes.put(argName, type);
                afterArguments = rves.advance(expr, afterArguments);
            }

            Maybe<? extends CallableSymbol> methodsFound = resolve(
                input,
                afterArguments,
                false
            );

            if (methodsFound.isPresent()) {

                Map<String, String> compiledRexprs = new HashMap<>();
                for (String argName : argNames) {
                    final IJadescriptType destType = methodsFound
                        .toNullable()
                        .parameterTypesByName()
                        .get(argName);
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


                return methodsFound.toNullable().compileInvokeByName(
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
        Maybe<Call> input,
        StaticState state
    ) {
        return resolve(
            input,
            state,
            true
        ).__(
            CallableSymbol::returnType
        ).orElseGet(
            () -> module.get(TypeHelper.class).ANY
        );
    }


    @Override
    protected boolean validateInternal(
        Maybe<Call> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        Maybe<String> name = input.__(Call::getName);
        boolean isProcedure =
            input.__(Call::isProcedure).extract(nullAsTrue);

        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        String procOrFunc = isProcedure ? "procedure" : "function";
        String procOrFuncCaps = isProcedure ? "Procedure" : "Function";
        String errorCode = "Invalid" + procOrFuncCaps + "Call";

        if (name.isNothing() || input.isNothing()) {
            return VALID;
        }


        String nameSafe = name.toNullable();


        if (namedArgs.isPresent()) {
            return validateCallByName(
                input,
                state,
                acceptor,
                namedArgs,
                isProcedure,
                procOrFunc,
                errorCode,
                nameSafe
            );
        } else /* ASSUMING (noArgs || simpleArgs.isPresent()) */ {
            return validateCallByArity(
                input,
                state,
                acceptor,
                simpleArgs,
                isProcedure,
                noArgs,
                procOrFunc,
                errorCode,
                nameSafe
            );
        }

    }


    private boolean validateCallByArity(
        Maybe<Call> input,
        StaticState state,
        ValidationMessageAcceptor acceptor,
        Maybe<SimpleArgumentList> simpleArgs,
        boolean isProcedure,
        boolean noArgs,
        String procOrFunc,
        String errorCode,
        String nameSafe
    ) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        boolean allArgsCheck = VALID;
        final List<Maybe<RValueExpression>> args = toListOfMaybes(
            simpleArgs.__(SimpleArgumentList::getExpressions)
        );

        int argsize = noArgs ? 0 : args.size();
        StaticState afterArgs = state;
        List<IJadescriptType> argTypes = new ArrayList<>(argsize);
        for (Maybe<RValueExpression> arg : args) {
            final boolean argCheck = rves.validate(
                arg,
                afterArgs,
                acceptor
            );
            allArgsCheck = allArgsCheck && argCheck;
            argTypes.add(rves.inferType(arg, afterArgs));
            afterArgs = rves.advance(arg, afterArgs);
        }

        if (allArgsCheck == INVALID) {
            return INVALID;
        }

        List<? extends CallableSymbol> methodsFound = resolveCandidates(
            input,
            afterArgs,
            false
        );

        String signature = Util.getSignature(nameSafe, argsize);

        if (methodsFound.isEmpty()) {
            return emitUnresolvedError(
                input,
                acceptor,
                procOrFunc,
                errorCode,
                signature
            );
        } else if (methodsFound.size() > 1) {
            return emitAmbiguousError(
                input,
                acceptor,
                procOrFunc,
                errorCode,
                methodsFound,
                signature
            );
        } else {
            CallableSymbol match = methodsFound.get(0);

            final ValidationHelper validationHelper = module.get(
                ValidationHelper.class);
            boolean isCorrectOperationKind = validationHelper.asserting(
                isProcedure == module.get(TypeHelper.class)
                    .VOID.typeEquals(match.returnType()),
                errorCode,
                "'" + nameSafe + "' is not a " + procOrFunc,
                input,
                acceptor
            );

            if (isCorrectOperationKind == INVALID) {
                return INVALID;
            }


            boolean paramTypeCheck = VALID;
            for (int i = 0; i < args.size(); i++) {
                final Maybe<RValueExpression> arg = args.get(i);

                final IJadescriptType paramType =
                    match.parameterTypes().get(i);

                final IJadescriptType argType = argTypes.get(i);

                final boolean argCheck = validationHelper
                    .assertExpectedType(
                        paramType,
                        argType,
                        "InvalidArgumentType",
                        arg,
                        acceptor
                    );
                paramTypeCheck = paramTypeCheck && argCheck;
            }

            return paramTypeCheck;
        }
    }


    private boolean validateCallByName(
        Maybe<Call> input,
        StaticState state,
        ValidationMessageAcceptor acceptor,
        Maybe<NamedArgumentList> namedArgs,
        boolean isProcedure,
        String procOrFunc,
        String errorCode,
        String nameSafe
    ) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        List<String> argNames = namedArgs
            .__(NamedArgumentList::getParameterNames)
            .extract(Maybe::nullAsEmptyList);

        Map<String, Maybe<RValueExpression>> argsByName =
            Streams.zip(
                argNames.stream(),
                toListOfMaybes(
                    namedArgs.__(NamedArgumentList::getParameterValues)
                ).stream(),
                Tuple2::new
            ).collect(Collectors.toMap(
                Tuple2::get_1,
                Tuple2::get_2
            ));


        List<IJadescriptType> argTypes = new ArrayList<>(argNames.size());

        List<Maybe<RValueExpression>> args = new ArrayList<>(
            argNames.size()
        );

        StaticState afterArgs = state;
        boolean allArgsCheck = VALID;
        for (String argName : argNames) {
            final Maybe<RValueExpression> arg = argsByName.get(argName);
            args.add(arg);
            boolean argCheck = rves.validate(arg, afterArgs, acceptor);
            argTypes.add(rves.inferType(arg, afterArgs));
            allArgsCheck = allArgsCheck && argCheck;
            afterArgs = rves.advance(arg, afterArgs);
        }

        if (allArgsCheck == INVALID) {
            return INVALID;
        }


        List<? extends CallableSymbol> methodsFound = resolveCandidates(
            input,
            afterArgs,
            false
        );


        String signature = Util.getSignature(
            nameSafe,
            argTypes,
            argNames
        );
        if (methodsFound.isEmpty()) {
            return emitUnresolvedError(
                input,
                acceptor,
                procOrFunc,
                errorCode,
                signature
            );
        } else if (methodsFound.size() > 1) {
            return emitAmbiguousError(
                input,
                acceptor,
                procOrFunc,
                errorCode,
                methodsFound,
                signature
            );
        } else {
            CallableSymbol match = methodsFound.get(0);

            boolean isCorrectOperationKind =
                module.get(ValidationHelper.class).asserting(
                    isProcedure == module.get(TypeHelper.class)
                        .VOID.typeEquals(match.returnType()),
                    errorCode,
                    "'" + nameSafe + "' is not a " + procOrFunc,
                    input,
                    acceptor
                );

            if (isCorrectOperationKind == INVALID) {
                return INVALID;
            }


            final List<Maybe<RValueExpression>> argsSorted =
                sortToMatchParamNames(
                    args,
                    argNames,
                    match.parameterNames()
                );

            final List<IJadescriptType> argTypesSorted =
                sortToMatchParamNames(
                    argTypes,
                    argNames,
                    match.parameterNames()
                );

            boolean paramTypeCheck = VALID;
            for (int i = 0; i < argsSorted.size(); i++) {

                Maybe<RValueExpression> arg = argsSorted.get(i);
                IJadescriptType argType = argTypesSorted.get(i);
                IJadescriptType paramType = match.parameterTypes().get(i);

                final boolean argCheck =
                    module.get(ValidationHelper.class).assertExpectedType(
                        paramType,
                        argType,
                        "InvalidArgumentType",
                        arg,
                        acceptor
                    );
                paramTypeCheck = paramTypeCheck && argCheck;
            }

            return paramTypeCheck;
        }
    }


    private boolean emitAmbiguousError(
        Maybe<Call> input,
        ValidationMessageAcceptor acceptor,
        String procOrFunc,
        String errorCode,
        List<? extends CallableSymbol> methodsFound,
        String signature
    ) {
        List<String> candidatesMessages = new ArrayList<>();
        for (CallableSymbol match : methodsFound) {
            candidatesMessages.add(
                match.getSignature() + " in " + match.sourceLocation() + ";"
            );
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
    }


    private boolean emitUnresolvedError(
        Maybe<Call> input,
        ValidationMessageAcceptor acceptor,
        String procOrFunc,
        String errorCode,
        String signature
    ) {
        return module.get(ValidationHelper.class).emitError(
            errorCode,
            "Cannot resolve " + procOrFunc + ": " + signature,
            input,
            acceptor
        );
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<Call> input
    ) {
        //only arguments can be sub-expressions
        return Maybe.toListOfMaybes(eitherCall(
                extractSimpleArgs(input), extractNamedArgs(input),
                SimpleArgumentList::getExpressions,
                NamedArgumentList::getParameterValues
            )).stream()
            .filter(Maybe::isPresent)
            .map(x -> new SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class), x
            ));
    }


    @Override
    protected boolean isHoledInternal(
        Maybe<Call> input,
        StaticState state
    ) {
        return subExpressionsAnyHoled(input, state);
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<Call> input,
        StaticState state
    ) {
        /*
        Functional-notation patterns are identified by name and number of
        arguments, and, when resolved, have alwaysa compile-time-known
        non-holed type. Therefore, they are never typely-holed, even when
        their arguments are/have holes.
        */
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<Call> input,
        StaticState state
    ) {
        return subExpressionsAnyUnbound(input, state);
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<Call> input,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        final Maybe<? extends PatternSymbol> patternSymbol = resolvePattern(
            input.getPattern(),
            state
        );

        Maybe<SimpleArgumentList> simpleArgs =
            extractSimpleArgs(input.getPattern());
        Maybe<NamedArgumentList> namedArgs =
            extractNamedArgs(input.getPattern());
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        List<Maybe<RValueExpression>> argExpressions;
        if (noArgs) {
            argExpressions = Collections.emptyList();
        } else if (simpleArgs.isPresent()) {
            argExpressions = toListOfMaybes(
                simpleArgs.__(SimpleArgumentList::getExpressions)
            );
        } else /*(namedArgs.isPresent())*/ {
            argExpressions = toListOfMaybes(
                namedArgs.__(NamedArgumentList::getParameterValues)
            );
        }


        if (patternSymbol.isPresent()) {
            final RValueExpressionSemantics rves =
                module.get(RValueExpressionSemantics.class);
            PatternSymbol ps = patternSymbol.toNullable();

            List<IJadescriptType> patternTermTypes = ps.termTypes();
            if (namedArgs.isPresent()) {
                List<String> argNames = toListOfMaybes(
                    namedArgs.__(NamedArgumentList::getParameterNames)
                ).stream()
                    .map(Maybe::toNullable)
                    .collect(Collectors.toList());
                argExpressions = sortToMatchParamNames(argExpressions,
                    argNames, ps.termNames()
                );
            }
            List<PatternMatcher> subResults =
                new ArrayList<>(argExpressions.size());
            StaticState runningState = state;
            for (int i = 0; i < argExpressions.size(); i++) {
                Maybe<RValueExpression> term = argExpressions.get(i);
                IJadescriptType upperBound = patternTermTypes.get(i);
                final SubPattern<RValueExpression, Call> termSubpattern =
                    input.subPattern(
                        upperBound,
                        __ -> term.toNullable(),
                        "_structterm" + i
                    );
                final PatternMatcher termOutput = rves.compilePatternMatch(
                    termSubpattern,
                    runningState,
                    acceptor
                );
                subResults.add(termOutput);
                runningState = rves.advancePattern(
                    termSubpattern,
                    runningState
                );
            }

            IJadescriptType solvedPatternType = inferPatternType(input, state)
                .solve(input.getProvidedInputType());

            List<String> compiledSubInputs =
                new ArrayList<>(ps.termNames().size());
            for (int i = 0; i < subResults.size(); i++) {
                compiledSubInputs.add(
                    "__x.get" +
                        Strings.toFirstUpper(ps.termNames().get(i)) + "()"
                );
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
    public boolean isWithoutSideEffectsInternal(
        Maybe<Call> input,
        StaticState state
    ) {
        AtomicReference<StaticState> runningState = new AtomicReference<>(
            state
        );

        AtomicBoolean result = new AtomicBoolean(true);

        forEachSubExpression(input, (s, i) -> {
            if (!result.get()) {
                return;
            }
            result.set(s.isWithoutSideEffects(i, runningState.get()));
            runningState.set(s.advance(i, runningState.get()));
        });

        if (!result.get()) {
            return false;
        } else {
            return resolve(input, runningState.get(), false)
                .__(CallableSymbol::isWithoutSideEffects)
                .extract(nullAsTrue);
        }
    }


    @Override
    public boolean isPatternEvaluationPureInternal(
        PatternMatchInput<Call> input,
        StaticState state
    ) {
        final Maybe<? extends PatternSymbol> resolve =
            resolvePattern(input.getPattern(), state);

        return resolve.__(PatternSymbol::isWithoutSideEffects)
            .extract(nullAsTrue)
            && subPatternEvaluationsAllPure(input, state);
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<Call> input,
        StaticState state
    ) {
        final Maybe<? extends PatternSymbol> resolve =
            resolvePattern(input.getPattern(), state);

        if (resolve.isPresent()) {
            return PatternType.simple(resolve.toNullable().inputType());
        } else {
            return PatternType.empty(module);
        }

    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<Call> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        final List<? extends PatternSymbol> pss = resolvePatternCandidates(
            input.getPattern(),
            state
        );

        Maybe<Call> patternCall = input.getPattern();
        Maybe<SimpleArgumentList> simpleArgs =
            extractSimpleArgs(input.getPattern());
        Maybe<NamedArgumentList> namedArgs =
            extractNamedArgs(input.getPattern());
        Maybe<String> name = input.getPattern().__(Call::getName);
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        List<Maybe<RValueExpression>> argExpressions;
        if (noArgs) {
            argExpressions = Collections.emptyList();
        } else if (simpleArgs.isPresent()) {
            argExpressions = toListOfMaybes(
                simpleArgs.__(SimpleArgumentList::getExpressions)
            );
        } else /*(namedArgs.isPresent())*/ {
            argExpressions = toListOfMaybes(
                namedArgs.__(NamedArgumentList::getParameterValues)
            );
        }

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        if (pss.size() == 0) {
            return validationHelper.emitError(
                "InvalidPattern",
                "Cannot resolve structural pattern: "
                    + Util.getSignature(
                    name.orElse(""), argExpressions.size()
                ),
                patternCall,
                acceptor
            );

        } else if (pss.size() > 1) {
            List<String> candidatesMessage = new ArrayList<>();
            for (PatternSymbol ps : pss) {
                candidatesMessage.add(ps.getSignature() +
                    " in " + ps.sourceLocation() + ";");
            }

            return validationHelper.emitError(
                "InvalidPattern",
                "Ambiguous pattern resolution: " + Util.getSignature(
                    name.orElse(""),
                    argExpressions.size()
                ) + ". Candidates: \n• " + String.join(
                    "\n•",
                    candidatesMessage
                ),
                patternCall,
                acceptor
            );
        } else { // => pss.size() == 1

            final RValueExpressionSemantics rves =
                module.get(RValueExpressionSemantics.class);
            PatternSymbol ps = pss.get(0);

            List<IJadescriptType> patternTermTypes = ps.termTypes();

            if (namedArgs.isPresent()) {
                List<String> argNames = toListOfMaybes(
                    namedArgs.__(NamedArgumentList::getParameterNames)
                ).stream()
                    .map(an -> an.orElse(""))
                    .collect(Collectors.toList());
                argExpressions = sortToMatchParamNames(argExpressions,
                    argNames, ps.termNames()
                );
            }

            boolean allArgsCheck = VALID;
            StaticState runningState = state;
            for (int i = 0; i < argExpressions.size(); i++) {
                Maybe<RValueExpression> term = argExpressions.get(i);
                IJadescriptType upperBound = patternTermTypes.get(i);
                final SubPattern<RValueExpression, Call> termSubPattern =
                    input.subPattern(
                        upperBound,
                        __ -> term.toNullable(),
                        "_structterm" + i
                    );
                boolean argCheck = rves.validatePatternMatch(
                    termSubPattern,
                    runningState,
                    acceptor
                );
                allArgsCheck = allArgsCheck && argCheck;
                runningState = rves.advancePattern(
                    termSubPattern,
                    runningState
                );
            }

            return allArgsCheck;
        }
    }


    public List<? extends CallableSymbol> resolveCandidates(
        Maybe<Call> input,
        StaticState state,
        boolean advanceStateOnArguments
    ) {
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        Maybe<String> name = input.__(Call::getName);
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();

        if (name.nullIf(String::isBlank).isNothing()) {
            return List.of();
        }
        String nameSafe = name.toNullable();
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        if (namedArgs.isPresent()) {


            List<String> names = new ArrayList<>();
            List<Maybe<String>> namesMaybes = toListOfMaybes(
                namedArgs.__(NamedArgumentList::getParameterNames)
            );
            Map<String, Maybe<RValueExpression>> args = new HashMap<>();
            final List<Maybe<RValueExpression>> argsMaybes = toListOfMaybes(
                namedArgs.__(NamedArgumentList::getParameterValues)
            );
            int assumedSize = Math.min(argsMaybes.size(), namesMaybes.size());
            for (int i = 0; i < assumedSize; i++) {
                Maybe<String> mn = namesMaybes.get(i);
                final String argName = mn.orElse("_" + i);
                names.add(argName);
                args.put(argName, argsMaybes.get(i));
            }


            final StaticState afterArguments;
            if (advanceStateOnArguments) {
                afterArguments = advanceCallByNameParameters(
                    state,
                    rves,
                    names,
                    args
                );
            } else {
                afterArguments = state;
            }

            return afterArguments.searchAs(
                    CallableSymbol.Searcher.class,
                    searcher -> searcher.searchCallable(
                        nameSafe,
                        CallableSymbol.Searcher.ANY_RETURN_TYPE,
                        CallableSymbol.Searcher.arityIs(assumedSize),
                        CallableSymbol.Searcher.arityIs(assumedSize)
                    )
                ).filter(Util.dinstinctBy(Symbol::sourceLocation))
                .collect(Collectors.toList());

        } else  /*ASSUMING  (noArgs || simpleArgs.isPresent())*/ {
            List<Maybe<RValueExpression>> args = toListOfMaybes(
                simpleArgs.__(SimpleArgumentList::getExpressions)
            );
            int argsize = noArgs ? 0 : args.size();
            final StaticState afterArguments;
            if (!noArgs && advanceStateOnArguments) {
                afterArguments = advanceCallByArityParameters(
                    state,
                    rves,
                    args
                );
            } else {
                afterArguments = state;
            }
            return afterArguments.searchAs(
                    CallableSymbol.Searcher.class,
                    searcher -> searcher.searchCallable(
                        nameSafe,
                        CallableSymbol.Searcher.ANY_RETURN_TYPE,
                        CallableSymbol.Searcher.arityIs(argsize),
                        CallableSymbol.Searcher.arityIs(argsize)
                    )
                ).filter(Util.dinstinctBy(Symbol::sourceLocation))
                .collect(Collectors.toList());
        }
    }


    public Maybe<? extends CallableSymbol> resolve(
        Maybe<Call> input,
        StaticState state,
        boolean advanceStateOnArguments
    ) {
        final List<? extends CallableSymbol> callableSymbols =
            resolveCandidates(
                input,
                state,
                advanceStateOnArguments
            );
        if (callableSymbols.size() == 1) {
            return Maybe.some(callableSymbols.get(0));
        } else {
            return Maybe.nothing();
        }
    }


    public boolean resolves(
        Maybe<Call> input,
        StaticState state,
        boolean advanceStateOnArguments
    ) {
        return resolve(input, state, advanceStateOnArguments).isPresent();
    }


    public List<? extends PatternSymbol> resolvePatternCandidates(
        Maybe<Call> input,
        StaticState state
    ) {
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        Maybe<String> name = input.__(Call::getName);
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();

        if (name.nullIf(String::isBlank).isNothing()) {
            return List.of();
        }
        String nameSafe = name.toNullable();

        if (namedArgs.isPresent()) {

            List<Maybe<String>> namesMaybes = toListOfMaybes(
                namedArgs.__(NamedArgumentList::getParameterNames)
            );
            final List<Maybe<RValueExpression>> argsMaybes = toListOfMaybes(
                namedArgs.__(NamedArgumentList::getParameterValues)
            );
            int assumedSize = Math.min(argsMaybes.size(), namesMaybes.size());

            return state.searchAs(
                    PatternSymbol.Searcher.class,
                    searcher -> searcher.searchPattern(
                        nameSafe,
                        PatternSymbol.Searcher.ANY_INPUT_TYPE,
                        PatternSymbol.Searcher.termCountIs(assumedSize),
                        PatternSymbol.Searcher.termCountIs(assumedSize)
                    )
                )
                .filter(Util.dinstinctBy(Symbol::sourceLocation))
                .collect(Collectors.toList());

        } else  /*ASSUMING  (noArgs || simpleArgs.isPresent())*/ {
            List<Maybe<RValueExpression>> args = toListOfMaybes(
                simpleArgs.__(SimpleArgumentList::getExpressions)
            );

            int argsize = noArgs ? 0 : args.size();


            return state.searchAs(
                    PatternSymbol.Searcher.class,
                    searcher -> searcher.searchPattern(
                        nameSafe,
                        CallableSymbol.Searcher.ANY_RETURN_TYPE,
                        CallableSymbol.Searcher.arityIs(argsize),
                        CallableSymbol.Searcher.arityIs(argsize)
                    )
                )
                .filter(Util.dinstinctBy(Symbol::sourceLocation))
                .collect(Collectors.toList());
        }
    }


    public Maybe<? extends PatternSymbol> resolvePattern(
        Maybe<Call> input,
        StaticState state
    ) {
        final List<? extends PatternSymbol> patternSymbols =
            resolvePatternCandidates(
                input,
                state
            );

        if (patternSymbols.size() == 1) {
            return Maybe.some(patternSymbols.get(0));
        } else {
            return Maybe.nothing();
        }
    }


    @SuppressWarnings("unused")
    public boolean resolvesPattern(Maybe<Call> input, StaticState state) {
        return resolvePattern(input, state).isPresent();
    }


    @Override
    public boolean isLExpreableInternal(Maybe<Call> input) {
        return false;
    }


    @Override
    public boolean canBeHoledInternal(Maybe<Call> input) {
        return true;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<Call> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<Call> input, StaticState state
    ) {
        Maybe<SimpleArgumentList> simpleArgs = extractSimpleArgs(input);
        Maybe<NamedArgumentList> namedArgs = extractNamedArgs(input);
        Maybe<String> name = input.__(Call::getName);
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        if (noArgs
            && isWithoutSideEffects(input, state)
            && name.nullIf(String::isBlank).isPresent()) {
            return Maybe.some(new ExpressionDescriptor.PropertyChain(
                ImmutableList.of(name.toNullable())
            ));
        }
        return Maybe.nothing();
    }


    @Override
    protected boolean validateAsStatementInternal(
        Maybe<Call> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {

        if (resolves(input, state, true)) {
            return validate(input, state, acceptor);
        } else {
            return errorNotStatement(input, acceptor);
        }
    }

}
