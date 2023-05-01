package it.unipr.ailab.jadescript.semantics;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Streams;
import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.NamedArgumentList;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.SimpleArgumentList;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.staticstate.TypeInterval;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalPattern;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.Located;
import it.unipr.ailab.jadescript.semantics.expression.AssignableExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.SubPattern;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.implicit.ImplicitConversionsHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.Call;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils.Tuple2;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import it.unipr.ailab.maybe.utils.ImmutableList;
import jdk.jshell.execution.Util;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.jetbrains.annotations.Nullable;

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


    private final Cache<EObject, CompilableCallable> resolutionCache
        = CacheBuilder.newBuilder().maximumSize(100).build();

    public static <T> List<T> sortToMatchParamNames(
        List<T> args,
        List<String> argNames,
        List<String> paramNames
    ) {
        List<SemanticsUtils.Tuple2<Integer, T>> tmp = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            T arg = args.get(i);
            if (argNames.get(i).equals(AGENT_ENV)) {
                tmp.add(new SemanticsUtils.Tuple2<>(-1, arg));
            } else {
                Integer x = paramNames.indexOf(argNames.get(i));
                tmp.add(new SemanticsUtils.Tuple2<>(x, arg));
            }
        }
        return tmp.stream()
            .sorted(Comparator.comparingInt(Tuple2::get_1))
            .map(Tuple2::get_2)
            .collect(Collectors.toList());
    }


    public static <T> MaybeList<T> sortToMatchParamNames(
        MaybeList<T> args,
        List<String> argNames,
        List<String> paramNames
    ) {
        List<SemanticsUtils.Tuple2<Integer, Maybe<T>>> tmp = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            Maybe<T> arg = args.get(i);
            if (argNames.get(i).equals(AGENT_ENV)) {
                tmp.add(new SemanticsUtils.Tuple2<>(-1, arg));
            } else {
                Integer x = paramNames.indexOf(argNames.get(i));
                tmp.add(new SemanticsUtils.Tuple2<>(x, arg));
            }
        }
        return tmp.stream()
            .sorted(Comparator.comparingInt(Tuple2::get_1))
            .map(Tuple2::get_2)
            .collect(MaybeList.collectFromStreamOfMaybes());
    }


    public static <T> List<T> sortToMatchParamNames(
        Map<String, T> namedArgs,
        List<String> paramNames
    ) {
        final List<T> collect = paramNames.stream()
            .map(namedArgs::get)
            .collect(Collectors.toCollection(ArrayList::new));
        if (namedArgs.containsKey(AGENT_ENV)) {
            collect.add(0, namedArgs.get(AGENT_ENV));
        }
        return collect;
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
    protected IJadescriptType assignableTypeInternal(
        Maybe<Call> input,
        StaticState state
    ) {
        // NOT USABLE AS L-EXPRESSION
        return module.get(BuiltinTypeProvider.class).nothing("");
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
        final Maybe<? extends GlobalPattern> patternSymbol = resolvePattern(
            input.getPattern(),
            state
        );

        Maybe<SimpleArgumentList> simpleArgs =
            extractSimpleArgs(input.getPattern());
        Maybe<NamedArgumentList> namedArgs =
            extractNamedArgs(input.getPattern());
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        MaybeList<RValueExpression> argExpressions;
        if (noArgs) {
            argExpressions = MaybeList.empty();
        } else if (simpleArgs.isPresent()) {
            argExpressions =
                simpleArgs.__toList(SimpleArgumentList::getExpressions);
        } else /*(namedArgs.isPresent())*/ {
            argExpressions =
                namedArgs.__toList(NamedArgumentList::getParameterValues);
        }


        if (patternSymbol.isNothing()) {
            return state;
        }


        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        GlobalPattern ps = patternSymbol.toNullable();

        List<IJadescriptType> patternTermTypes = ps.termTypes();


        if (namedArgs.isPresent()) {
            List<String> argNames =
                someStream(namedArgs.__(NamedArgumentList::getParameterNames))
                    .map(Maybe::toNullable)
                    .collect(Collectors.toList());
            argExpressions = sortToMatchParamNames(
                argExpressions,
                argNames,
                ps.termNames()
            );
        }

        StaticState runningState = state;
        for (int i = 0; i < argExpressions.size(); i++) {
            Maybe<RValueExpression> term = argExpressions.get(i);
            String termName = ps.termNames().get(i);
            IJadescriptType upperBound = patternTermTypes.get(i);
            final SubPattern<RValueExpression, Call> termSubpattern =
                input.subPatternForProperty(
                    upperBound,
                    __ -> term.toNullable(),
                    "_" + i,
                    termName
                );
            runningState = rves.advancePattern(
                termSubpattern,
                runningState
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
        final Maybe<? extends GlobalPattern> ps = resolvePattern(
            input.getPattern(),
            state
        );

        Maybe<SimpleArgumentList> simpleArgs =
            extractSimpleArgs(input.getPattern());
        Maybe<NamedArgumentList> namedArgs =
            extractNamedArgs(input.getPattern());
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        MaybeList<RValueExpression> argExpressions;
        if (noArgs) {
            argExpressions = MaybeList.empty();
        } else if (simpleArgs.isPresent()) {
            argExpressions =
                simpleArgs.__toList(SimpleArgumentList::getExpressions);
        } else /*(namedArgs.isPresent())*/ {
            argExpressions =
                namedArgs.__toList(NamedArgumentList::getParameterValues);
        }


        if (ps.isNothing()) {
            return state;
        }


        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        GlobalPattern patternSymbol = ps.toNullable();

        List<IJadescriptType> patternTermTypes = patternSymbol.termTypes();


        if (namedArgs.isPresent()) {
            List<String> argNames =
                someStream(namedArgs.__(NamedArgumentList::getParameterNames))
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
            String termName = patternSymbol.termNames().get(i);
            IJadescriptType upperBound = patternTermTypes.get(i);
            final SubPattern<RValueExpression, Call> termSubpattern =
                input.subPatternForProperty(
                    upperBound,
                    __ -> term.toNullable(),
                    "_structterm" + i,
                    termName
                );
            runningState = rves.advancePattern(
                termSubpattern,
                runningState
            );
            if (i < argExpressions.size() - 1) {
                runningState = rves.assertDidMatch(
                    termSubpattern,
                    runningState
                );
            }
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
        final MaybeList<RValueExpression> args =
            simpleArgs.__toList(SimpleArgumentList::getExpressions);
        int argsize = noArgs ? 0 : args.size();

        StaticState newState = advanceCallByArityParameters(
            state,
            rves,
            args
        );


        Optional<? extends CompilableCallable> methodsFound = newState.searchAs(
            CompilableCallable.Namespace.class,
            searcher -> searcher.compilableCallables(nameSafe)
                .filter(cc -> cc.arity() == argsize)
        ).findFirst();

        if (methodsFound.isEmpty()) {
            return state;
        }

        return newState;
    }


    private StaticState advanceCallByArityParameters(
        StaticState state,
        RValueExpressionSemantics rves,
        MaybeList<RValueExpression> args
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
            .<List<String>>__(NamedArgumentList::getParameterNames)
            .orElse(List.of());
        Map<String, Maybe<RValueExpression>> args = Streams.zip(
            argNames.stream(),
            someStream(namedArgs.__(NamedArgumentList::getParameterValues)),
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

        Optional<? extends CompilableCallable> methodFound = newState.searchAs(
            CompilableCallable.Namespace.class,
            searcher -> searcher.compilableCallables(nameSafe)
                .filter(cc -> namedArgs
                    .__(NamedArgumentList::getParameterNames)
                    .__(List::size)
                    .wrappedEquals(cc.parameterNames().size()))
                .filter(cc -> namedArgs
                    .__(NamedArgumentList::getParameterValues)
                    .__(List::size)
                    .wrappedEquals(cc.parameterTypes().size())
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
            final MaybeList<RValueExpression> args =
                simpleArgs.__toList(SimpleArgumentList::getExpressions);

            int argsize = noArgs ? 0 : args.size();

            List<String> compiledArgs = new ArrayList<>(argsize);
            List<IJadescriptType> argTypes = new ArrayList<>(argsize);
            StaticState afterArguments = state;
            for (Maybe<RValueExpression> arg : args) {
                compiledArgs.add(rves.compile(arg, afterArguments, acceptor));
                argTypes.add(rves.inferType(arg, afterArguments));
                afterArguments = rves.advance(arg, afterArguments);
            }


            Maybe<? extends CompilableCallable> methodsFound = resolve(
                input,
                afterArguments,
                false
            );


            if (methodsFound.isPresent()) {
                CompilableCallable method = methodsFound.toNullable();
                final List<String> compiledRexprs =
                    module.get(CompilationHelper.class)
                        .implicitConversionsOnRValueList(
                            compiledArgs,
                            argTypes,
                            method.parameterTypes()
                        );

                return method.compileInvokeByArity(compiledRexprs, acceptor);
            }
            // Falling back to common invocation
            return name + "(" + String.join(", ", compiledArgs) + ")";

        } else if (namedArgs.isPresent()) {
            List<String> argNames = namedArgs
                .<List<String>>__(NamedArgumentList::getParameterNames)
                .orElse(List.of());
            Map<String, ? extends RValueExpression> args = Streams.zip(
                argNames.stream(),
                namedArgs.<List<? extends RValueExpression>>__(
                    NamedArgumentList::getParameterValues
                ).orElse(List.of()).stream(),
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

            Maybe<? extends CompilableCallable> methodsFound = resolve(
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
                        compiled = module.get(ImplicitConversionsHelper.class)
                            .compileWithEventualImplicitConversions(
                                compiled, type, destType
                            );
                    }
                    compiledRexprs.put(argName, compiled);
                }


                return methodsFound.toNullable()
                    .compileInvokeByName(compiledRexprs, acceptor);
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
        return resolve(input, state, true)
            .__(CompilableCallable::returnType)
            .orElseGet(() -> module.get(BuiltinTypeProvider.class).any(
                "Unresolved callable symbol."
            ));
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
            input.__(Call::isProcedure).orElse(true);

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
        final MaybeList<RValueExpression> args =
            simpleArgs.__toList(SimpleArgumentList::getExpressions);

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

        List<? extends CompilableCallable> methodsFound = resolveCandidates(
            input,
            afterArgs,
            false
        );

        String signature = SemanticsUtils.getSignature(nameSafe, argsize);

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
            CompilableCallable match = methodsFound.get(0);

            final ValidationHelper validationHelper =
                module.get(ValidationHelper.class);
            boolean isCorrectOperationKind = validationHelper.asserting(
                isProcedure == match.returnType().category().isJavaVoid(),
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
            .<List<String>>__(NamedArgumentList::getParameterNames)
            .orElse(List.of());

        Map<String, Maybe<RValueExpression>> argsByName =
            Streams.zip(
                argNames.stream(),
                someStream(namedArgs
                    .__(NamedArgumentList::getParameterValues)),
                Tuple2::new
            ).collect(Collectors.toMap(
                Tuple2::get_1,
                Tuple2::get_2
            ));


        List<IJadescriptType> argTypes = new ArrayList<>(argNames.size());

        List<Maybe<RValueExpression>> argsExprs =
            new ArrayList<>(argNames.size());

        StaticState afterArgs = state;
        boolean allArgsCheck = VALID;
        for (String argName : argNames) {
            final Maybe<RValueExpression> argExpr = argsByName.get(argName);
            argsExprs.add(argExpr);
            boolean argCheck = rves.validate(argExpr, afterArgs, acceptor);
            argTypes.add(rves.inferType(argExpr, afterArgs));
            allArgsCheck = allArgsCheck && argCheck;
            afterArgs = rves.advance(argExpr, afterArgs);
        }

        if (allArgsCheck == INVALID) {
            return INVALID;
        }


        List<? extends CompilableCallable> methodsFound = resolveCandidates(
            input,
            afterArgs,
            false
        );


        String signature = SemanticsUtils.getSignature(
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
            CompilableCallable match = methodsFound.get(0);

            final ValidationHelper validationHelper = module.get(
                ValidationHelper.class);
            boolean isCorrectOperationKind =
                validationHelper.asserting(
                    isProcedure == match.returnType()
                        .category().isJavaVoid(),
                    errorCode,
                    "'" + nameSafe + "' is not a " + procOrFunc,
                    input,
                    acceptor
                );

            boolean allNamesAreThere = VALID;

            final Maybe<EList<String>> paramNamesMaybe =
                namedArgs.__(NamedArgumentList::getParameterNames);

            if (paramNamesMaybe.isPresent()) {
                final EList<String> names = paramNamesMaybe.toNullable();

                for (int i = 0; i < names.size(); i++) {
                    final String n = names.get(i);

                    if (n == null || n.isBlank()) {
                        continue;
                    }

                    boolean nameIsThere = validationHelper.asserting(
                        match.parameterNames().contains(n),
                        errorCode,
                        "parameter with name '" + n + "' not found in " +
                            "resolved '" + match.getSignature() + "'",
                        namedArgs,
                        JadescriptPackage.eINSTANCE
                            .getNamedArgumentList_ParameterNames(),
                        i,
                        acceptor
                    );

                    allNamesAreThere = allNamesAreThere && nameIsThere;
                }
            }

            if (isCorrectOperationKind == INVALID
                || allNamesAreThere == INVALID) {
                return INVALID;
            }


            final List<Maybe<RValueExpression>> argsSorted =
                sortToMatchParamNames(
                    argsExprs,
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
                    validationHelper.assertExpectedType(
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
        List<? extends CompilableCallable> methodsFound,
        String signature
    ) {
        List<String> candidatesMessages = new ArrayList<>();
        for (CompilableCallable match : methodsFound) {
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
        return Maybe.someStream(eitherCall(
                extractSimpleArgs(input), extractNamedArgs(input),
                SimpleArgumentList::getExpressions,
                NamedArgumentList::getParameterValues
            ))
            .filter(Maybe::isPresent)
            .map(x -> new SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class), x
            ));
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<Call> input,
        StaticState state
    ) {
        return subExpressionsAnyHoled(input, state);
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<Call> input,
        StaticState state
    ) {
        /*
        Functional-notation patterns are identified by name and number of
        arguments, and, when resolved, have always a compile-time-known
        non-holed type. Therefore, they are never typely-holed, even when
        their arguments are/have holes.
        */
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<Call> input,
        StaticState state
    ) {
        return subExpressionsAnyUnbound(input, state);
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<Call> input,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        final Maybe<? extends GlobalPattern> patternSymbol =
            resolvePattern(input.getPattern(), state);

        Maybe<SimpleArgumentList> simpleArgs =
            extractSimpleArgs(input.getPattern());
        Maybe<NamedArgumentList> namedArgs =
            extractNamedArgs(input.getPattern());
        boolean noArgs = simpleArgs.isNothing() && namedArgs.isNothing();
        MaybeList<RValueExpression> argExpressions;
        if (noArgs) {
            argExpressions = MaybeList.empty();
        } else if (simpleArgs.isPresent()) {
            argExpressions =
                simpleArgs.__toList(SimpleArgumentList::getExpressions);
        } else /*(namedArgs.isPresent())*/ {
            argExpressions =
                namedArgs.__toList(NamedArgumentList::getParameterValues);
        }


        if (patternSymbol.isPresent()) {
            final RValueExpressionSemantics rves =
                module.get(RValueExpressionSemantics.class);
            GlobalPattern ps = patternSymbol.toNullable();

            List<IJadescriptType> patternTermTypes = ps.termTypes();
            if (namedArgs.isPresent()) {
                List<String> argNames = someStream(
                    namedArgs.__(NamedArgumentList::getParameterNames)
                )
                    .map(Maybe::toNullable)
                    .collect(Collectors.toList());
                argExpressions = sortToMatchParamNames(
                    argExpressions,
                    argNames, ps.termNames()
                );
            }
            List<PatternMatcher> subResults =
                new ArrayList<>(argExpressions.size());
            StaticState runningState = state;
            for (int i = 0; i < argExpressions.size(); i++) {
                Maybe<RValueExpression> term = argExpressions.get(i);
                String termName = ps.termNames().get(i);
                IJadescriptType upperBound = patternTermTypes.get(i);
                final SubPattern<RValueExpression, Call> termSubpattern =
                    input.subPatternForProperty(
                        upperBound,
                        __ -> term.toNullable(),
                        "_structterm" + i,
                        termName
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

                if (i < argExpressions.size() - 1) {
                    runningState = rves.assertDidMatch(
                        termSubpattern,
                        runningState
                    );
                }

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
                .__(CompilableCallable::isWithoutSideEffects)
                .orElse(true);
        }
    }


    @Override
    public boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<Call> input,
        StaticState state
    ) {
        final Maybe<? extends GlobalPattern> resolve =
            resolvePattern(input.getPattern(), state);

        return resolve.__(GlobalPattern::isWithoutSideEffects).orElse(true)
            && subPatternEvaluationsAllPure(input, state);
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<Call> input,
        StaticState state
    ) {
        final Maybe<? extends GlobalPattern> resolve =
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
        final List<? extends GlobalPattern> pss = resolvePatternCandidates(
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
        MaybeList<RValueExpression> argExpressions;
        if (noArgs) {
            argExpressions = MaybeList.empty();
        } else if (simpleArgs.isPresent()) {
            argExpressions =
                simpleArgs.__toList(SimpleArgumentList::getExpressions);
        } else /*(namedArgs.isPresent())*/ {
            argExpressions =
                namedArgs.__toList(NamedArgumentList::getParameterValues);
        }

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        if (pss.size() == 0) {
            return validationHelper.emitError(
                "InvalidPattern",
                "Cannot resolve structural pattern: "
                    + SemanticsUtils.getSignature(
                    name.orElse(""), argExpressions.size()
                ),
                patternCall,
                acceptor
            );

        } else if (pss.size() > 1) {
            List<String> candidatesMessage = new ArrayList<>();
            for (GlobalPattern ps : pss) {
                candidatesMessage.add(ps.getSignature() +
                    " in " + ps.sourceLocation() + ";");
            }

            return validationHelper.emitError(
                "InvalidPattern",
                "Ambiguous pattern resolution: " + SemanticsUtils.getSignature(
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
            GlobalPattern ps = pss.get(0);

            List<IJadescriptType> patternTermTypes = ps.termTypes();

            if (namedArgs.isPresent()) {
                List<String> argNames = someStream(
                    namedArgs.__(NamedArgumentList::getParameterNames)
                )
                    .map(an -> an.orElse(""))
                    .collect(Collectors.toList());
                argExpressions = sortToMatchParamNames(
                    argExpressions,
                    argNames,
                    ps.termNames()
                );
            }

            boolean allArgsCheck = VALID;
            StaticState runningState = state;
            for (int i = 0; i < argExpressions.size(); i++) {
                Maybe<RValueExpression> term = argExpressions.get(i);
                String termName = ps.termNames().get(i);
                IJadescriptType upperBound = patternTermTypes.get(i);
                final SubPattern<RValueExpression, Call> termSubPattern =
                    input.subPatternForProperty(
                        upperBound,
                        __ -> term.toNullable(),
                        "_structterm" + i,
                        termName
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
                if (i < argExpressions.size() - 1) {
                    runningState = rves.assertDidMatch(
                        termSubPattern,
                        runningState
                    );
                }

            }

            return allArgsCheck;
        }
    }


    public List<? extends CompilableCallable> resolveCandidates(
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
            MaybeList<String> namesMaybes =
                namedArgs.__toList(NamedArgumentList::getParameterNames);
            Map<String, Maybe<RValueExpression>> args = new HashMap<>();
            final MaybeList<RValueExpression> argsMaybes =
                namedArgs.__toList(NamedArgumentList::getParameterValues);

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
                    CompilableCallable.Namespace.class,
                    searcher -> searcher.compilableCallables(nameSafe)
                        .filter(cc -> cc.arity() == assumedSize)
                ).filter(SemanticsUtils.dinstinctBy(Located::sourceLocation))
                .collect(Collectors.toList());

        } else  /*ASSUMING  (noArgs || simpleArgs.isPresent())*/ {
            MaybeList<RValueExpression> args =
                simpleArgs.__toList(SimpleArgumentList::getExpressions);

            int argsize = noArgs ? 0 : args.size();
            final StaticState afterArguments;
            if (!noArgs && advanceStateOnArguments) {
                afterArguments =
                    advanceCallByArityParameters(state, rves, args);
            } else {
                afterArguments = state;
            }
            return afterArguments.searchAs(
                    CompilableCallable.Namespace.class,
                    searcher -> searcher.compilableCallables(nameSafe)
                        .filter(cc -> cc.arity() == argsize)
                ).filter(SemanticsUtils.dinstinctBy(Located::sourceLocation))
                .collect(Collectors.toList());
        }
    }

    private boolean isCachable(
        boolean isProcedure,
        CompilableCallable callable
    ){
        if(callable == null){
            return false;
        }

        if(isProcedure != callable.returnType().category().isJavaVoid()){
            return false;
        }

        for (IJadescriptType parameterType : callable.parameterTypes()) {
            if(parameterType.isErroneous()){
                return false;
            }
        }

        return true;
    }

    public Maybe<? extends CompilableCallable> resolve(
        Maybe<Call> input,
        StaticState state,
        boolean advanceStateOnArguments
    ) {
        final Maybe<? extends EObject> extracted
            = SemanticsUtils.extractEObject(input);
        if(extracted.isPresent()){
            final @Nullable CompilableCallable cached =
                this.resolutionCache.getIfPresent(extracted.toNullable());
            if(cached != null){
                return some(cached);
            }
        }
        final List<? extends CompilableCallable> callableSymbols =
            resolveCandidates(
                input,
                state,
                advanceStateOnArguments
            );
        if (callableSymbols.size() == 1) {
            final CompilableCallable result = callableSymbols.get(0);
            if(extracted.isPresent() && isCachable(
                input.__(Call::isProcedure).orElse(false),
                result
            )){
                this.resolutionCache.put(extracted.toNullable(), result);
            }
            return Maybe.some(result);
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


    public List<? extends GlobalPattern> resolvePatternCandidates(
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
            MaybeList<String> namesMaybes =
                namedArgs.__toList(NamedArgumentList::getParameterNames);
            final MaybeList<RValueExpression> argsMaybes =
                namedArgs.__toList(NamedArgumentList::getParameterValues);
            int assumedSize = Math.min(argsMaybes.size(), namesMaybes.size());

            return state.searchAs(
                    GlobalPattern.Namespace.class,
                    searcher -> searcher.globalPatterns(nameSafe)
                        .filter(gp -> gp.termCount() == assumedSize)
                ).filter(SemanticsUtils.dinstinctBy(Located::sourceLocation))
                .collect(Collectors.toList());

        } else  /*ASSUMING  (noArgs || simpleArgs.isPresent())*/ {
            MaybeList<RValueExpression> args =
                simpleArgs.__toList(SimpleArgumentList::getExpressions);

            int argsize = noArgs ? 0 : args.size();


            return state.searchAs(
                    GlobalPattern.Namespace.class,
                    searcher -> searcher.globalPatterns(nameSafe)
                        .filter(gp -> gp.termCount() == argsize)
                ).filter(SemanticsUtils.dinstinctBy(Located::sourceLocation))
                .collect(Collectors.toList());
        }
    }


    public Maybe<? extends GlobalPattern> resolvePattern(
        Maybe<Call> input,
        StaticState state
    ) {
        final List<? extends GlobalPattern> patternSymbols =
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
