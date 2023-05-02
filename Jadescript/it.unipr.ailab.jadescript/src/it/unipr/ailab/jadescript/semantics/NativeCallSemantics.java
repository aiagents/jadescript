package it.unipr.ailab.jadescript.semantics;

import it.unipr.ailab.jadescript.jadescript.JavaFullyQualifiedName;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.SimpleArgumentList;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.AssignableExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.NativeCall;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NativeCallSemantics
    extends AssignableExpressionSemantics<NativeCall> {

    public NativeCallSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected boolean mustTraverse(Maybe<NativeCall> input) {
        return false;
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(Maybe<NativeCall> input) {
        return Optional.empty();
    }


    @Override
    protected void compileAssignmentInternal(
        Maybe<NativeCall> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        // DO NOTHING; not an L-EXPR
    }


    @Override
    protected IJadescriptType assignableTypeInternal(
        Maybe<NativeCall> input,
        StaticState state
    ) {
        return module.get(BuiltinTypeProvider.class).nothing("");
    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<NativeCall> input,
        IJadescriptType rightType,
        StaticState state
    ) {
        // DO NOTHING; not an L-EXPR
        return state;
    }


    @Override
    public boolean validateAssignmentInternal(
        Maybe<NativeCall> input,
        Maybe<RValueExpression> expression,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        // DO NOTHING; not an L-EXPR
        return errorNotLvalue(input, acceptor);
    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<NativeCall> input,
        ValidationMessageAcceptor acceptor
    ) {
        // DO NOTHING; not an L-EXPR
        return errorNotLvalue(input, acceptor);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<NativeCall> input
    ) {
        final Maybe<SimpleArgumentList> simpleArguments =
            input.__(NativeCall::getSimpleArguments);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        return Stream.concat(
                Stream.of(input.__(NativeCall::getNameExpr)),
                Maybe.someStream(simpleArguments.__(
                    SimpleArgumentList::getExpressions))
            ).filter(Maybe::isPresent)
            .map(i -> new SemanticsBoundToExpression<>(rves, i));
    }


    public boolean isDynamicResolution(Maybe<NativeCall> input) {
        return input.__(NativeCall::isResolveDynamically).orElse(false)
            && input.__(NativeCall::getNameExpr).isPresent();
    }


    @Override
    protected String compileInternal(
        Maybe<NativeCall> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        if (isDynamicResolution(input)) {
            return compileDynamicResolution(input, state, acceptor);
        } else {
            return compileStaticResolution(input, state, acceptor);
        }
    }


    private String compileDynamicResolution(
        Maybe<NativeCall> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {

        MaybeList<RValueExpression> args = input
            .__(NativeCall::getSimpleArguments)
            .__toList(SimpleArgumentList::getExpressions);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        StaticState newState = state;
        String fqName;
        final Maybe<RValueExpression> stringExpr =
            input.__(NativeCall::getNameExpr);
        fqName = rves.compile(
            stringExpr,
            newState,
            acceptor
        );
        newState = rves.advance(stringExpr, newState);

        StringBuilder invocation = new StringBuilder("jadescript.util." +
            "InvokeUtils.executeNative(");
        invocation.append(fqName);

        for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
            Maybe<RValueExpression> arg = args.get(i);
            if (arg.isNothing()) {
                continue;
            }

            invocation.append(", ");

            invocation.append(rves.compile(arg, newState, acceptor));

            if (i < argsSize - 1) {
                newState = rves.advance(arg, newState);
            }
        }


        invocation.append(")");

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);


        IJadescriptType type =
            tes.toJadescriptType(input.__(NativeCall::getType));
        return "(" + type.compileAsJavaCast() + " " + invocation + ")";

    }


    public Stream<JvmOperation> resolveCandidates(
        JvmTypeNamespace jvmNamespace,
        String methodName,
        int arity
    ) {
        return jvmNamespace
            .searchJvmOperation()
            .filter(Objects::nonNull)
            .filter(JvmOperation::isStatic)
            .filter(op -> Objects.equals(methodName, op.getSimpleName()))
            .filter(op -> arity == op.getParameters().size());
    }


    public Stream<JvmOperation> resolveApplicables(
        Stream<JvmOperation> candidates,
        List<IJadescriptType> argTypes
    ) {
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        return candidates.filter(op -> {
            final EList<JvmFormalParameter> parameters = op.getParameters();
            int argsSize = Math.min(argTypes.size(), parameters.size());
            for (int i = 0; i < argsSize; i++) {
                if (!jvm.isAssignable(
                    parameters.get(i).getParameterType(),
                    argTypes.get(i).asJvmTypeReference(),
                    false
                )) {
                    return false;
                }
            }
            return true;
        });
    }


    public List<JvmOperation> mostSpecificApplicables(
        List<JvmOperation> applicables
    ) {
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);

        List<JvmOperation> msmSet = new LinkedList<>();
        for (JvmOperation candidate : applicables) {
            if (msmSet.isEmpty()) {
                msmSet.add(candidate);
                continue;
            }

            final JvmOperation msm = msmSet.get(0);

            int assumedSize = Math.min(
                candidate.getParameters().size(),
                msm.getParameters().size()
            );

            // if true, all parameters types of msm are subtype-or-equal than
            // the corresponding parameter types of candidate
            boolean allSubtypeOrEqual = true;
            // if true, all parameters types of msm are supertype-or-equal than
            // the corresponding parameter types of candidate
            boolean allSupertypeOrEqual = true;


            for (int i = 0; i < assumedSize; i++) {
                final JvmTypeReference aType =
                    msm.getParameters().get(i).getParameterType();

                final JvmTypeReference bType =
                    candidate.getParameters().get(i).getParameterType();

                // A = B
                if (aType.getQualifiedName('$')
                    .equals(bType.getQualifiedName('$'))) {
                    continue;
                }

                // A >: B
                if (jvm.isAssignable(aType, bType, false)) {
                    allSubtypeOrEqual = false;
                    continue;
                }

                // A <: B
                if (jvm.isAssignable(bType, aType, false)) {
                    allSupertypeOrEqual = false;
                    continue;
                }

                // A != B and inconvertibles
                allSubtypeOrEqual = false;
                allSupertypeOrEqual = false;
            }

            if (allSubtypeOrEqual == allSupertypeOrEqual) {
                // If are both true or both false, then the two methods are
                // ambiguous, and they can be put togheter in the msmSet.
                // (msm was already there, just adding candidate):
                msmSet.add(candidate);
            } else if (allSupertypeOrEqual) {
                // If only allSupertypeOrEqual is true, then candidate is more
                // specific than msm.
                // This als means that all the methods in msmSet are less
                // specific than candidate.
                // candidate should be the only one in msmSet.
                msmSet.clear();
                msmSet.add(candidate);
            }
        }

        return msmSet;
    }


    @NotNull
    private String compileStaticResolution(
        Maybe<NativeCall> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {

        MaybeList<RValueExpression> args = input
            .__(NativeCall::getSimpleArguments)
            .__toList(SimpleArgumentList::getExpressions);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);


        final Maybe<JavaFullyQualifiedName> javaFQN =
            input.__(NativeCall::getJavaFQName);


        final MaybeList<String> fqnSegments =
            javaFQN.__toList(JavaFullyQualifiedName::getSegments);

        if (fqnSegments.size() < 2) {
            return "/*Error: not enough segments in fully-qualified " +
                "method name*/null";
        }


        if (fqnSegments.stream()
            .anyMatch(m -> m.isNothing() || m.toNullable().isBlank())) {
            return "/*Error: invalid fully-qualified method name*/null";
        }

        String methodName = fqnSegments.get(fqnSegments.size() - 1)
            .orElse("");

        String classFQName = fqnSegments
            .subList(0, fqnSegments.size() - 1).stream()
            .map(Maybe::toNullable)
            .collect(Collectors.joining("."));


        final TypeSolver typeSolver = module.get(TypeSolver.class);

        final IJadescriptType containingClass =
            typeSolver.fromFullyQualifiedName(classFQName);

        StaticState newState = state;
        int argsSize = args.size();
        List<String> compiledArgs = new ArrayList<>(argsSize);
        List<IJadescriptType> argTypes = new ArrayList<>(argsSize);
        for (int i = 0; i < argsSize; i++) {
            Maybe<RValueExpression> arg = args.get(i);
            if (arg.isNothing()) {
                continue;
            }

            compiledArgs.add(rves.compile(arg, newState, acceptor));

            argTypes.add(rves.inferType(arg, newState));

            if (i < argsSize - 1) {
                newState = rves.advance(arg, newState);
            }
        }

        final JvmTypeNamespace jvmNamespace = containingClass.jvmNamespace();
        final Stream<JvmOperation> candidates = resolveCandidates(
            jvmNamespace,
            methodName,
            argsSize
        );
        final Stream<JvmOperation> applicables = resolveApplicables(
            candidates,
            argTypes
        );

        final List<JvmOperation> mostSpecificMethods = mostSpecificApplicables(
            applicables.collect(Collectors.toList()));

        final Maybe<JvmOperation> operation;
        if (mostSpecificMethods.isEmpty()) {
            operation = Maybe.nothing();
        } else {
            operation = Maybe.some(mostSpecificMethods.get(0));
        }

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        final String methodCompiled;
        final List<String> adaptedArgs;
        if (operation.isPresent()) {
            methodCompiled = operation.toNullable().getQualifiedName('.');

            final List<IJadescriptType> paramTypes =
                operation.toNullable().getParameters().stream()
                    .filter(Objects::nonNull)
                    .map(JvmFormalParameter::getParameterType)
                    .map(typeSolver::fromJvmTypeReference)
                    .map(TypeArgument::ignoreBound)
                    .collect(Collectors.toList());

            adaptedArgs = compilationHelper.implicitConversionsOnRValueList(
                compiledArgs,
                argTypes,
                paramTypes
            );

        } else {
            methodCompiled = classFQName + "." + methodName;
            adaptedArgs = compiledArgs;
        }

        String expression = methodCompiled + "(" +
            String.join(", ", adaptedArgs) + ")";

        if (!input.__(NativeCall::hasTypeClause).orElse(false)) {
            return expression;
        }

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);

        final IJadescriptType explicitReturnType =
            tes.toJadescriptType(input.__(NativeCall::getType));

        return "(" + explicitReturnType.compileAsJavaCast() + " "
            + expression + ")";
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<NativeCall> input,
        StaticState state
    ) {
        if (isDynamicResolution(input)) {
            return inferTypeWithExplicitTypeClause(input);
        } else {
            return inferTypeStaticResolution(input, state);
        }
    }


    private IJadescriptType inferTypeWithExplicitTypeClause(
        Maybe<NativeCall> input
    ) {
        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);

        return tes.toJadescriptType(input.__(NativeCall::getType));
    }


    private IJadescriptType inferTypeStaticResolution(
        Maybe<NativeCall> input,
        StaticState state
    ) {
        final TypeSolver typeSolver = module.get(TypeSolver.class);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        boolean hasTypeClause =
            input.__(NativeCall::hasTypeClause).orElse(false);

        if (hasTypeClause) {
            return inferTypeWithExplicitTypeClause(input);
        }


        MaybeList<RValueExpression> args = input
            .__(NativeCall::getSimpleArguments)
            .__toList(SimpleArgumentList::getExpressions);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);


        final Maybe<JavaFullyQualifiedName> javaFQN =
            input.__(NativeCall::getJavaFQName);


        final MaybeList<String> fqnSegments =
            javaFQN.__toList(JavaFullyQualifiedName::getSegments);


        if (fqnSegments.size() < 2) {
            return builtins.any(
                "Error: not enough segments in fully-qualified method name"
            );
        }


        if (fqnSegments.stream()
            .anyMatch(m -> m.isNothing() || m.toNullable().isBlank())) {
            return builtins.any(
                "Error: invalid fully-qualified method name"
            );
        }

        String methodName =
            fqnSegments.get(fqnSegments.size() - 1).orElse("");

        String classFQName =
            fqnSegments.subList(0, fqnSegments.size() - 1).stream()
                .map(Maybe::toNullable)
                .collect(Collectors.joining("."));


        final IJadescriptType containingClass =
            typeSolver.fromFullyQualifiedName(classFQName);

        StaticState newState = state;
        int argsSize = args.size();

        List<IJadescriptType> argTypes = new ArrayList<>(argsSize);
        for (int i = 0; i < argsSize; i++) {
            Maybe<RValueExpression> arg = args.get(i);
            if (arg.isNothing()) {
                continue;
            }

            argTypes.add(rves.inferType(arg, newState));

            if (i < argsSize - 1) {
                newState = rves.advance(arg, newState);
            }
        }

        final JvmTypeNamespace jvmNamespace = containingClass.jvmNamespace();

        final Stream<JvmOperation> candidates =
            resolveCandidates(jvmNamespace, methodName, argsSize);


        final Stream<JvmOperation> applicables =
            resolveApplicables(candidates, argTypes);

        final List<JvmOperation> mostSpecific =
            mostSpecificApplicables(applicables.collect(Collectors.toList()));

        if (mostSpecific.isEmpty()) {
            return builtins.any(
                "Could not resolve Java method " + methodName + "(" +
                    argTypes.stream()
                        .map(TypeArgument::compileToJavaTypeReference)
                        .collect(Collectors.joining(", ")) + ") in class '" +
                    classFQName + "'."
            );
        }

        return jvmNamespace.resolveType(
            mostSpecific.get(0).getReturnType()
        ).ignoreBound();
    }


    @Override
    protected boolean validateInternal(
        Maybe<NativeCall> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (isDynamicResolution(input)) {
            return validateDynamicResolution(input, state, acceptor);
        } else {
            return validateStaticResolution(input, state, acceptor);
        }
    }


    private boolean validateDynamicResolution(
        Maybe<NativeCall> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {

        MaybeList<RValueExpression> args = input
            .__(NativeCall::getSimpleArguments)
            .__toList(SimpleArgumentList::getExpressions);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        StaticState newState = state;

        final Maybe<RValueExpression> stringExpr =
            input.__(NativeCall::getNameExpr);

        boolean nameValidation = rves.validate(stringExpr, newState, acceptor);

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        if (nameValidation == VALID) {
            nameValidation = validationHelper.assertExpectedType(
                builtins.text(),
                rves.inferType(stringExpr, newState),
                "InvalidNativeCall",
                stringExpr,
                acceptor
            );
        }

        if (nameValidation == VALID) {
            newState = rves.advance(stringExpr, newState);
        }

        boolean argsValidation = VALID;

        for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
            Maybe<RValueExpression> arg = args.get(i);
            if (arg.isNothing()) {
                continue;
            }


            boolean argValidation = rves.validate(
                arg,
                newState,
                acceptor
            );

            argsValidation = argsValidation && argValidation;

            if (argValidation && i < argsSize - 1) {
                newState = rves.advance(arg, newState);
            }
        }

        boolean hasTypeClause =
            input.__(NativeCall::hasTypeClause).orElse(false);

        boolean isStatement =
            input.__(NativeCall::isStatement).orElse(true);

        final Maybe<TypeExpression> typeExpression =
            input.__(NativeCall::getType);


        if (!isStatement && !hasTypeClause) {
            return validationHelper.emitError(
                "InvalidNativeCall",
                "The 'as' type clause at the end is required for" +
                    " dynamically-resolved native method invocations as " +
                    "expressions.",
                input,
                acceptor
            );
        }

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);

        IJadescriptType type =
            tes.toJadescriptType(input.__(NativeCall::getType));

        boolean typeValidation = type.validateType(
            typeExpression,
            acceptor
        );


        return nameValidation && argsValidation && typeValidation;
    }


    private boolean validateStaticResolution(
        Maybe<NativeCall> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {

        MaybeList<RValueExpression> args = input
            .__(NativeCall::getSimpleArguments)
            .__toList(SimpleArgumentList::getExpressions);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);


        final Maybe<JavaFullyQualifiedName> javaFQN =
            input.__(NativeCall::getJavaFQName);


        final MaybeList<String> fqnSegments =
            javaFQN.__toList(JavaFullyQualifiedName::getSegments);

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        boolean fqnValidation = VALID;
        if (fqnSegments.size() < 2) {
            fqnValidation = validationHelper.emitError(
                "InvalidNativeCall",
                "Not enough segments in fully-qualified method name (at least" +
                    "a class name and a method name are required).",
                javaFQN,
                acceptor
            );
        }


        if (fqnValidation == INVALID) {
            return INVALID;
        }

        if (fqnSegments.stream()
            .anyMatch(m -> m.isNothing() || m.toNullable().isBlank())) {
            fqnValidation = validationHelper.emitError(
                "InvalidNativeCall",
                "Invalid fully-qualified method name.",
                javaFQN,
                acceptor
            );
        }

        if (fqnValidation == INVALID) {
            return INVALID;
        }

        String methodName = fqnSegments
            .get(fqnSegments.size() - 1)
            .orElse("");

        String classFQName = fqnSegments
            .subList(0, fqnSegments.size() - 1).stream()
            .map(Maybe::toNullable)
            .collect(Collectors.joining("."));

        final TypeSolver typeSolver = module.get(TypeSolver.class);
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);

        final IJadescriptType containingClass =
            typeSolver.fromFullyQualifiedName(classFQName);

        int argsSize = args.size();

        final JvmTypeNamespace jvmNamespace = containingClass.jvmNamespace();

        List<JvmOperation> candidates = resolveCandidates(
            jvmNamespace,
            methodName,
            argsSize
        ).collect(Collectors.toList());

        boolean thereAreCandidates = VALID;

        if (candidates.isEmpty()) {
            thereAreCandidates = validationHelper.emitError(
                "InvalidNativeCall",
                "Could not resolve method '" + methodName + "' in " +
                    "class " + classFQName + " with arity " + argsSize,
                input,
                acceptor
            );
        }


        StaticState newState = state;

        boolean argsValidation = VALID;
        List<IJadescriptType> argTypes = new ArrayList<>(argsSize);

        for (int i = 0; i < argsSize; i++) {
            Maybe<RValueExpression> arg = args.get(i);
            if (arg.isNothing()) {
                continue;
            }

            boolean argValidation = rves.validate(arg, newState, acceptor);

            argsValidation = argsValidation && argValidation;

            if (argValidation == INVALID) {
                continue;
            }

            argTypes.add(rves.inferType(arg, newState));

            if (i < argsSize - 1) {
                newState = rves.advance(arg, newState);
            }
        }

        if (argsValidation == INVALID) {
            return INVALID;
        }

        if (thereAreCandidates == INVALID) {
            return INVALID;
        }

        List<JvmOperation> applicables = resolveApplicables(
            candidates.stream(),
            argTypes
        ).collect(Collectors.toList());


        boolean thereAreApplicables = VALID;

        if (applicables.isEmpty()) {
            thereAreApplicables = validationHelper.emitError(
                "InvalidNativeCall",
                "Could not find applicable method " + methodName + "(" +
                    argTypes.stream()
                        .map(TypeArgument::compileToJavaTypeReference)
                        .collect(Collectors.joining(", ")) +
                    ") in class " + classFQName,
                input,
                acceptor
            );
        }

        if (thereAreApplicables == INVALID) {
            return INVALID;
        }

        final Maybe<TypeExpression> typeExpression =
            input.__(NativeCall::getType);
        boolean hasTypeClause =
            input.__(NativeCall::hasTypeClause).orElse(false);

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);


        if (applicables.size() > 1) {
            // Attempt to refine the set of candidates to the most specific
            // method(s).
            applicables = mostSpecificApplicables(applicables);
        }

        if (applicables.isEmpty()) {
            return validationHelper.emitError(
                "InvalidNativeCall",
                "Could not resolve method " + methodName + "(" +
                    argTypes.stream()
                        .map(TypeArgument::compileToJavaTypeReference)
                        .collect(Collectors.joining(", ")) +
                    ") in class " + classFQName,
                input,
                acceptor
            );
        } else if (applicables.size() > 1) {
            String candidatesString =
                applicables.stream()
                    .map(o ->
                        o.getReturnType().getQualifiedName('.') + " " +
                            o.getSimpleName() + "(" +
                            o.getParameters().stream()
                                .map(p -> p.getParameterType()
                                    .getQualifiedName('.') + " " +
                                    p.getName())
                                .collect(Collectors.joining(", ")) + ")"
                    ).collect(Collectors.joining("\n - "));

            return validationHelper.emitError(
                "InvalidNativeCall",
                "Ambiguous method call. Candidates:\n" + candidatesString,
                input,
                acceptor
            );
        } else {
            // We can assume that candidates.size() == 1
            final JvmOperation winner = candidates.get(0);
            final List<IJadescriptType> paramTypes =
                winner.getParameters().stream()
                    .filter(Objects::nonNull)
                    .map(JvmFormalParameter::getParameterType)
                    .map(typeSolver::fromJvmTypeReference)
                    .map(TypeArgument::ignoreBound)
                    .collect(Collectors.toList());

            int assumedSize = Math.min(paramTypes.size(), argsSize);

            boolean argsTypesValidation = VALID;

            for (int i = 0; i < assumedSize; i++) {
                final IJadescriptType paramType = paramTypes.get(i);
                final IJadescriptType argType = argTypes.get(i);
                final Maybe<RValueExpression> arg = args.get(i);

                boolean typeValidation = validationHelper.assertExpectedType(
                    paramType,
                    argType,
                    "InvalidNativeCall",
                    arg,
                    acceptor
                );

                argsTypesValidation = argsTypesValidation && typeValidation;
            }

            if (argsTypesValidation == INVALID) {
                //noinspection ConstantValue
                return INVALID;
            }


            if (hasTypeClause) {
                final IJadescriptType explicitReturnType =
                    tes.toJadescriptType(typeExpression);

                if (!explicitReturnType.validateType(
                    typeExpression,
                    acceptor
                )) {
                    return INVALID;
                }

                if (!jvm.isAssignable(
                    explicitReturnType.asJvmTypeReference(),
                    winner.getReturnType(),
                    false
                ) && !jvm.isAssignable(
                    winner.getReturnType(),
                    explicitReturnType.asJvmTypeReference(),
                    false
                )) {
                    // if the two types are not convertible, emit error
                    return validationHelper.emitError(
                        "InvalidNativeCall",
                        "Types " +
                            explicitReturnType.compileToJavaTypeReference() +
                            " and " + winner.getReturnType()
                            .getQualifiedName('.') + " are not convertible.",
                        typeExpression,
                        acceptor
                    );
                }
            }


        }


        return VALID;
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<NativeCall> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<NativeCall> input,
        StaticState state
    ) {
        boolean resolveDynamically = isDynamicResolution(input);

        MaybeList<RValueExpression> args = input
            .__(NativeCall::getSimpleArguments)
            .__toList(SimpleArgumentList::getExpressions);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        StaticState newState;
        if (resolveDynamically) {
            final Maybe<RValueExpression> stringExpr =
                input.__(NativeCall::getNameExpr);

            newState = rves.advance(stringExpr, state);
        } else {
            newState = state;
        }

        for (Maybe<RValueExpression> arg : args) {
            newState = rves.advance(arg, newState);
        }

        return newState;
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<NativeCall> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<NativeCall> input,
        StaticState state
    ) {
        // Native operations are always assumed to be with side effects
        return false;
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<NativeCall> input) {
        //CANNOT ASSIGN TO AN INVOKE-EXPRESSION
        return false;
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<NativeCall> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<NativeCall> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<NativeCall> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<NativeCall> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<NativeCall> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<NativeCall> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<NativeCall> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<NativeCall> input) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<NativeCall> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<NativeCall> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<NativeCall> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<NativeCall> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }

}
