package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.stream;
import static it.unipr.ailab.maybe.Maybe.toListOfMaybes;

/**
 * Created on 2019-05-20.
 */
@Singleton
//TODO ICAART23 updates
public class NativeExpressionSemantics
    extends AssignableExpressionSemantics<NativeExpression> {


    public NativeExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    public static Maybe<String> getJavaFQName(
        Maybe<JavaFullyQualifiedName> fqN
    ) {
        return CompilationHelper.sourceToText(fqN);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<NativeExpression> input
    ) {
        final Maybe<SimpleArgumentList> simpleArguments =
            input.__(NativeExpression::getSimpleArguments);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        return Stream.concat(
                Stream.of(input.__(NativeExpression::getNameExpr)),
                stream(simpleArguments.__(
                    SimpleArgumentList::getExpressions))
            ).filter(Maybe::isPresent)
            .map(i -> new SemanticsBoundToExpression<>(rves, i));
    }


    @Override
    public void compileAssignmentInternal(
        Maybe<NativeExpression> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        //CANNOT ASSIGN TO AN INVOKE EXPRESSION
    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<NativeExpression> input,
        IJadescriptType rightType,
        StaticState state
    ) {
        //CANNOT ASSIGN TO AN INVOKE EXPRESSION
        return state;
    }


    @Override
    public boolean validateAssignmentInternal(
        Maybe<NativeExpression> input,
        Maybe<RValueExpression> expression,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        //CANNOT ASSIGN TO AN INVOKE-EXPRESSION
        //this is actually never called because of prior check via
        // syntacticValidateLValue(...)
        return errorNotLvalue(input, acceptor);
    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<NativeExpression> input,
        ValidationMessageAcceptor acceptor
    ) {
        return errorNotLvalue(input, acceptor);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<NativeExpression> input) {
        //CANNOT ASSIGN TO AN INVOKE-EXPRESSION
        return false;
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<NativeExpression> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<NativeExpression> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<NativeExpression> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<NativeExpression> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<NativeExpression> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<NativeExpression> input,
        StaticState state
    ) {

        boolean resolveDynamically = isDynamicResolution(input);


        final Maybe<SimpleArgumentList> simpleArguments =
            input.__(NativeExpression::getSimpleArguments);

        List<Maybe<RValueExpression>> args = Maybe.toListOfMaybes(
            simpleArguments.__(SimpleArgumentList::getExpressions)
        );

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        StaticState newState;
        if (resolveDynamically) {
            final Maybe<RValueExpression> stringExpr =
                input.__(NativeExpression::getNameExpr);

            newState = rves.advance(stringExpr, state);
        } else {
            newState = state;
        }


        for (Maybe<RValueExpression> arg : args) {
            newState = rves.advance(arg, newState);
        }

        return newState;
    }


    private boolean isDynamicResolution(Maybe<NativeExpression> input) {
        return input.__(NativeExpression::isResolveDynamically).orElse(false)
            && input.__(NativeExpression::getNameExpr).isPresent();
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<NativeExpression> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected String compileInternal(
        Maybe<NativeExpression> input,
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
        Maybe<NativeExpression> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final Maybe<SimpleArgumentList> simpleArguments =
            input.__(NativeExpression::getSimpleArguments);

        List<Maybe<RValueExpression>> args = Maybe.toListOfMaybes(
            simpleArguments.__(SimpleArgumentList::getExpressions)
        );

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        StaticState newState = state;
        String fqName;
        final Maybe<RValueExpression> stringExpr =
            input.__(NativeExpression::getNameExpr);
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
            tes.toJadescriptType(input.__(NativeExpression::getType));
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

        final TypeHelper typeHelper = module.get(TypeHelper.class);
        int argsSize = argTypes.size();
        return candidates.filter(op -> {
            final EList<JvmFormalParameter> parameters = op.getParameters();
            for (int i = 0; i < argsSize; i++) {
                if (!typeHelper.isAssignable(
                    parameters.get(i).getParameterType(),
                    argTypes.get(i).asJvmTypeReference()
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
        final TypeHelper typeHelper = module.get(TypeHelper.class);

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
                if (typeHelper.isAssignable(aType, bType)) {
                    allSubtypeOrEqual = false;
                    continue;
                }

                // A <: B
                if (typeHelper.isAssignable(bType, aType)) {
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
        Maybe<NativeExpression> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final Maybe<SimpleArgumentList> simpleArguments =
            input.__(NativeExpression::getSimpleArguments);

        List<Maybe<RValueExpression>> args = Maybe.toListOfMaybes(
            simpleArguments.__(SimpleArgumentList::getExpressions)
        );

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);


        final Maybe<JavaFullyQualifiedName> javaFQN =
            input.__(NativeExpression::getJavaFQName);


        final List<Maybe<String>> fqnSegments = toListOfMaybes(
            javaFQN.__(JavaFullyQualifiedName::getSegments)
        );

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


        final TypeHelper typeHelper = module.get(TypeHelper.class);

        final IJadescriptType containingClass =
            typeHelper.jtFromFullyQualifiedName(classFQName);

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
                    .map(typeHelper::jtFromJvmTypeRef)
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

        boolean hasTypeClause = input.__(NativeExpression::getType).isPresent()
            && input.__(NativeExpression::isWithAsClause).orElse(false);


        String expression = methodCompiled + "(" +
            String.join(", ", adaptedArgs) + ")";

        if (!hasTypeClause) {
            return expression;
        }

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);

        final IJadescriptType explicitReturnType =
            tes.toJadescriptType(input.__(NativeExpression::getType));

        return "(" + explicitReturnType.compileAsJavaCast() + " "
            + expression + ")";
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<NativeExpression> input,
        StaticState state
    ) {
        if (isDynamicResolution(input)) {
            return inferTypeWithExplicitTypeClause(input);
        } else {
            return inferTypeStaticResolution(input, state);
        }
    }


    private IJadescriptType inferTypeWithExplicitTypeClause(
        Maybe<NativeExpression> input
    ) {
        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);

        return tes.toJadescriptType(input.__(NativeExpression::getType));
    }


    private IJadescriptType inferTypeStaticResolution(
        Maybe<NativeExpression> input,
        StaticState state
    ) {

        boolean hasTypeClause = input.__(NativeExpression::getType).isPresent()
            && input.__(NativeExpression::isWithAsClause).orElse(false);

        if (hasTypeClause) {
            return inferTypeWithExplicitTypeClause(input);
        }


        final Maybe<SimpleArgumentList> simpleArguments =
            input.__(NativeExpression::getSimpleArguments);

        List<Maybe<RValueExpression>> args = Maybe.toListOfMaybes(
            simpleArguments.__(SimpleArgumentList::getExpressions)
        );

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);


        final Maybe<JavaFullyQualifiedName> javaFQN =
            input.__(NativeExpression::getJavaFQName);


        final List<Maybe<String>> fqnSegments = toListOfMaybes(
            javaFQN.__(JavaFullyQualifiedName::getSegments)
        );

        final TypeHelper typeHelper = module.get(TypeHelper.class);


        if (fqnSegments.size() < 2) {
            return typeHelper.TOP.apply(
                "Error: not enough segments in fully-qualified method name"
            );
        }


        if (fqnSegments.stream()
            .anyMatch(m -> m.isNothing() || m.toNullable().isBlank())) {
            return typeHelper.TOP.apply(
                "Error: invalid fully-qualified method name"
            );
        }

        String methodName = fqnSegments.get(fqnSegments.size() - 1)
            .orElse("");

        String classFQName = fqnSegments
            .subList(0, fqnSegments.size() - 1).stream()
            .map(Maybe::toNullable)
            .collect(Collectors.joining("."));


        final IJadescriptType containingClass =
            typeHelper.jtFromFullyQualifiedName(classFQName);

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
        final Optional<JvmOperation> operation = jvmNamespace
            .searchJvmOperation()
            .filter(Objects::nonNull)
            .filter(JvmOperation::isStatic)
            .filter(op -> Objects.equals(methodName, op.getSimpleName()))
            .filter(op -> argsSize == op.getParameters().size())
            .filter(op -> {
                final EList<JvmFormalParameter> parameters = op.getParameters();
                for (int i = 0; i < argsSize; i++) {
                    if (!typeHelper.isAssignable(
                        parameters.get(i).getParameterType(),
                        argTypes.get(i).asJvmTypeReference()
                    )) {
                        return false;
                    }
                }
                return true;
            }).findAny();


        final IJadescriptType returnedType;

        if (operation.isPresent()) {
            returnedType = jvmNamespace.resolveType(
                operation.get().getReturnType()
            );
        } else {
            returnedType = typeHelper.TOP.apply(
                "Could not resolve Java method " + methodName + "(" +
                    argTypes.stream()
                        .map(TypeArgument::compileToJavaTypeReference)
                        .collect(Collectors.joining(", ")) + ") in class '" +
                    classFQName + "'."
            );
        }


        return returnedType;

    }


    @Override
    protected boolean mustTraverse(Maybe<NativeExpression> input) {
        return false;
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(Maybe<NativeExpression> input) {
        return Optional.empty();
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<NativeExpression> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<NativeExpression> input
        , StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<NativeExpression> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean validateInternal(
        Maybe<NativeExpression> input,
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
        Maybe<NativeExpression> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final Maybe<SimpleArgumentList> simpleArguments =
            input.__(NativeExpression::getSimpleArguments);

        List<Maybe<RValueExpression>> args = Maybe.toListOfMaybes(
            simpleArguments.__(SimpleArgumentList::getExpressions)
        );

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        StaticState newState = state;

        final Maybe<RValueExpression> stringExpr =
            input.__(NativeExpression::getNameExpr);

        boolean nameValidation = rves.validate(
            stringExpr,
            newState,
            acceptor
        );

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


        final Maybe<TypeExpression> typeExpression =
            input.__(NativeExpression::getType);
        boolean hasTypeClause = typeExpression.isPresent()
            && input.__(NativeExpression::isWithAsClause).orElse(false);

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);
        if(!hasTypeClause){
            return validationHelper.emitError(
                "InvalidNativeCall",
                "The 'as' type clause at the end is required for" +
                    " dynamically-resolved native method invocations.",
                input,
                acceptor
            );
        }

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);

        IJadescriptType type =
            tes.toJadescriptType(input.__(NativeExpression::getType));

        boolean typeValidation = type.validateType(
            typeExpression,
            acceptor
        );


        return nameValidation && argsValidation && typeValidation;
    }


    private boolean validateStaticResolution(
        Maybe<NativeExpression> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final Maybe<SimpleArgumentList> simpleArguments =
            input.__(NativeExpression::getSimpleArguments);

        List<Maybe<RValueExpression>> args = Maybe.toListOfMaybes(
            simpleArguments.__(SimpleArgumentList::getExpressions)
        );

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);


        final Maybe<JavaFullyQualifiedName> javaFQN =
            input.__(NativeExpression::getJavaFQName);


        final List<Maybe<String>> fqnSegments = toListOfMaybes(
            javaFQN.__(JavaFullyQualifiedName::getSegments)
        );

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        if (fqnSegments.size() < 2) {
            validationHelper.emitError(
                "InvalidNativeCall",
                "Not enough segments in fully-qualified method name (at least" +
                    "a class name and a method name are required).",
                javaFQN,
                acceptor
            );
        }


        if (fqnSegments.stream()
            .anyMatch(m -> m.isNothing() || m.toNullable().isBlank())) {
            validationHelper.emitError(
                "InvalidNativeCall",
                "Invalid fully-qualified method name.",
                javaFQN,
                acceptor
            );
        }

        String methodName = fqnSegments
            .get(fqnSegments.size() - 1)
            .orElse("");

        String classFQName = fqnSegments
            .subList(0, fqnSegments.size() - 1).stream()
            .map(Maybe::toNullable)
            .collect(Collectors.joining("."));

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        final IJadescriptType containingClass =
            typeHelper.jtFromFullyQualifiedName(classFQName);

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
                "Could not resolve method " + methodName + "(" +
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
            input.__(NativeExpression::getType);
        boolean hasTypeClause = typeExpression.isPresent()
            && input.__(NativeExpression::isWithAsClause).orElse(false);

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
                    .map(typeHelper::jtFromJvmTypeRef)
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

                if(!explicitReturnType.validateType(
                    typeExpression,
                    acceptor
                )){
                    return INVALID;
                }

                if (!typeHelper.isAssignable(
                    explicitReturnType.asJvmTypeReference(),
                    winner.getReturnType()
                ) && !typeHelper.isAssignable(
                    winner.getReturnType(),
                    explicitReturnType.asJvmTypeReference()
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
    protected boolean isWithoutSideEffectsInternal(
        Maybe<NativeExpression> input,
        StaticState state
    ) {
        // Native operations are always assumed to be with side effects
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<NativeExpression> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<NativeExpression> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<NativeExpression> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<NativeExpression> input) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<NativeExpression> input,
        StaticState state
    ) {
        return false;
    }


}
