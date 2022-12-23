package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.Semantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeRelationship;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created on 27/12/16.
 */
@Singleton
public abstract class ExpressionSemantics<T> extends Semantics {

    public ExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    /**
     * Returns true if the input expression is just a node of the AST intended to contain some other sub-expression.
     * As a matter of fact, the syntax of Jadescript matches the following pattern when defining expressions:
     * Expr: SubExpr (op SubExpr)*;
     * SubExpr: ...;
     * <p>
     * When no 'op' is present, the produced Expr node in the AST is just a container for a SubExpr.
     * This pattern can be repeated recursively until literal and constants are reached at the leaves of the AST.
     * <p>
     * If this method returns true, the actual logic of the semantics of the expression are not actually defined here,
     * but rather in some of the semantics objects for its subexpressions, and therefore, this node should be traversed
     * (see {@link ExpressionSemantics#traverse(Maybe)}).
     */
    protected abstract boolean mustTraverse(Maybe<T> input);

    /**
     * Returns a {@link SemanticsBoundToExpression} instance if the input expression is just a node of the AST intended
     * to contain some other sub-expression, or {@link Optional#empty()} otherwise.
     * As a matter of fact, the syntax of Jadescript matches the following pattern when defining expressions:
     * Expr: SubExpr (op SubExpr)*;
     * SubExpr: ...;
     * <p>
     * When no 'op' is present, the produced Expr node in the AST is just a container for a SubExpr.
     * This pattern can be repeated recursively until literal and constants are reached at the leaves of the AST.
     * <p>
     * The instance contains a pair (e, s) where e is the sub-expression resulting from the traverse operation, and s is
     * its corresponding semantics.
     */
    protected abstract Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traverse(Maybe<T> input);

    /**
     * Given a Maybe(input), it provides a list of pairs (e, s), where e is a subexpression of the input and s the
     * associated semantics.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final Stream<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<T> input) {
        return traverse(input)
                .map(x -> x.getSemantics().getSubExpressions((Maybe) x.getInput()))
                .orElseGet(() -> getSubExpressionsInternal(input));
    }

    protected abstract Stream<SemanticsBoundToExpression<?>>
    getSubExpressionsInternal(Maybe<T> input);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public final <TT> boolean subExpressionsAllMatch(
            Maybe<T> input,
            BiPredicate<ExpressionSemantics<TT>, Maybe<TT>> predicate
    ) {
        return getSubExpressions(input)
                .allMatch(sbte -> ((BiPredicate) predicate).test(sbte.getSemantics(), sbte.getInput()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public final <TT> boolean subExpressionsAnyMatch(
            Maybe<T> input,
            BiPredicate<ExpressionSemantics<TT>, Maybe<TT>> predicate
    ) {
        return getSubExpressions(input)
                .anyMatch(sbte -> ((BiPredicate) predicate).test(sbte.getSemantics(), sbte.getInput()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public final <TT> boolean subExpressionsNoneMatch(
            Maybe<T> input,
            BiPredicate<ExpressionSemantics<TT>, Maybe<TT>> predicate
    ) {
        return getSubExpressions(input)
                .noneMatch(sbte -> ((BiPredicate) predicate).test(sbte.getSemantics(), sbte.getInput()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public final <TT, R> Stream<R> mapSubExpressions(
            Maybe<T> input,
            BiFunction<ExpressionSemantics<TT>, Maybe<TT>, R> function
    ) {
        return getSubExpressions(input)
                .map(sbte -> (R) ((BiFunction) function).apply(sbte.getSemantics(), sbte.getInput()));
    }

    /**
     * Applies a function to the input AST node and to each of its children (ea sunt, the input expression and its
     * sub-expressions) and collects the values resulting from the applications into a List.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final <R> List<? extends R> collectFromAllNodes(
            Maybe<T> input,
            BiFunction<Maybe<?>, ExpressionSemantics<?>, R> function
    ) {
        if (mustTraverse(input)) {
            Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                //noinspection unchecked,rawtypes
                return traversed.get().getSemantics().collectFromAllNodes((Maybe) traversed.get().getInput(), function);
            }
        }

        List<R> result = new ArrayList<>();
        result.add(function.apply(input, this));

        getSubExpressions(input).forEach(bounds -> {
            //noinspection unchecked,rawtypes
            result.addAll(bounds.getSemantics().collectFromAllNodes((Maybe) bounds.getInput(), function));
        });

        return result;
    }

    /**
     * Produces a String resulting from the direct compilation of the input expression.
     *
     * @param input    the input expression
     * @param acceptor an acceptor that can be used to produce additional statements which will be added as auxiliary
     *                 statements for the expression evaluation
     * @return the corresponding Java expression
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final String compile(Maybe<T> input, CompilationOutputAcceptor acceptor) {
        return traverse(input)
                .map(x -> x.getSemantics().compile((Maybe) x.getInput(), acceptor))
                .orElseGet(() -> compileInternal(input, acceptor));

    }


    //TODO the compile method could return more stuff (in one sweep, which is faster). In particular:
    // - the string of the compiled expression (ECR)
    // - the flow-typing implications (ECR)
    // - the auxiliary statements (acceptor)
    // - a simple effect-analysis (this could maybe use the acceptor pattern too...)
    // - the context-generation implications (acceptor pattern too?)

    /**
     * @see ExpressionSemantics#compile(Maybe, CompilationOutputAcceptor)
     */
    protected abstract String compileInternal(Maybe<T> input, CompilationOutputAcceptor acceptor);

    /**
     * Computes the type of the input expression.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final IJadescriptType inferType(Maybe<T> input) {
        return traverse(input)
                .map(x -> x.getSemantics().inferType((Maybe) x.getInput()))
                .orElseGet(() -> inferTypeInternal(input));
    }

    /**
     * @see ExpressionSemantics#inferType(Maybe)
     */
    protected abstract IJadescriptType inferTypeInternal(Maybe<T> input);

    //TODO Javadoc
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final boolean validate(Maybe<T> input, ValidationMessageAcceptor acceptor) {
        return traverse(input)
                .map(x -> x.getSemantics().validate((Maybe) x.getInput(), acceptor))
                .orElseGet(() -> validateInternal(input, acceptor));
    }

    /**
     * @see ExpressionSemantics#validate(Maybe, ValidationMessageAcceptor)
     */
    protected abstract boolean validateInternal(Maybe<T> input, ValidationMessageAcceptor acceptor);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public final List<String> propertyChain(Maybe<T> input) {
        return traverse(input)
                .map(x -> x.getSemantics().propertyChain((Maybe) x.getInput()))
                .orElseGet(() -> propertyChainInternal(input));
    }

    protected abstract List<String> propertyChainInternal(Maybe<T> input);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public final ExpressionTypeKB computeKB(Maybe<T> input) {
        return traverse(input)
                .map(x -> x.getSemantics().computeKB((Maybe) x.getInput()))
                .orElseGet(() -> computeKBInternal(input));

    }

    protected abstract ExpressionTypeKB computeKBInternal(Maybe<T> input);

    /**
     * This returns true <i>only if</i> the input expression can be evaluated without causing side-effects.
     * This is used, most importantly, to determine if an expression can be used as condition for handler activation (in
     * the when-clause, or as part of the content pattern).
     * Please remember that the evaluation of such conditions should not cause side-effects.
     * <p></p>
     * This is usually decided taking into account the purity of sub-expressions.
     * @see ExpressionSemantics#subExpressionsAllAlwaysPure(Maybe)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})

    public final boolean isAlwaysPure(Maybe<T> input) {
        return traverse(input)
                .map(x -> x.getSemantics().isAlwaysPure((Maybe) x.getInput()))
                .orElseGet(() -> isAlwaysPureInternal(input));
    }

    protected abstract boolean isAlwaysPureInternal(Maybe<T> input);

    public final boolean subExpressionsAllAlwaysPure(Maybe<T> input){
        return subExpressionsAllMatch(input, ExpressionSemantics::isAlwaysPure);
    }



    /**
     * Returns true iff the input expression can be syntactically used as L-Expression, i.e., an assignable expression
     * that when evaluated at the left of an assignment produces a value that represents a writeable cell.
     * <p></p>
     * This is usually decided locally.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final boolean isValidLExpr(Maybe<T> input) {
        return traverse(input)
                .map(x -> x.getSemantics().isValidLExpr((Maybe) x.getInput()))
                .orElseGet(() -> isValidLExprInternal(input));
    }

    protected abstract boolean isValidLExprInternal(Maybe<T> input);


    @SuppressWarnings({"unchecked", "rawtypes"})
    public final boolean isPatternEvaluationPure(Maybe<T> input) {
        return traverse(input)
                .map(x -> x.getSemantics().isPatternEvaluationPure((Maybe) x.getInput()))
                .orElseGet(() -> isPatternEvaluationPureInternal(input));
    }

    protected abstract boolean isPatternEvaluationPureInternal(Maybe<T> input);

    public final boolean subPatternEvaluationsAllPure(Maybe<T> input){
        return subExpressionsAllMatch(input, ExpressionSemantics::isPatternEvaluationPure);
    }

    /**
     * Returns true if this expression contains holes in it e.g., unbounded identifiers or '_' placeholders.
     * In {@link ExpressionSemantics} there is a default implementation that returns false unless a traversing is
     * required.
     * Overridden by semantics classes which implement semantics of expressions which can present holes (without
     * traversing).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final boolean isHoled(Maybe<T> input) {
        return traverse(input)
                .map(x -> x.getSemantics().isHoled((Maybe) x.getInput()))
                .orElseGet(() -> isHoledInternal(input));
    }

    protected abstract boolean isHoledInternal(Maybe<T> input);

    public final boolean subExpressionsAnyHoled(Maybe<T> input){
        return subExpressionsAnyMatch(input, ExpressionSemantics::isHoled);
    }

    /**
     * Similar to {@link ExpressionSemantics#isHoled(Maybe)}, but used only by the process of type inferring of
     * patterns.
     * Being "typely holed" is a special case of being "holed".
     * In fact, some holed patterns can still provide complete information about their type at compile time.
     * One example is the functional-notation pattern. Its argument patterns can be holed, but the type of the whole
     * pattern is completely known at compile-time, and it is the one related to the return type of the function
     * referred by the pattern.
     * By design, if this method returns true for a given pattern, then
     * {@link ExpressionSemantics#inferPatternType(Maybe, PatternMatchMode)} should return a
     * {@link PatternType.HoledPatternType}, otherwise a {@link PatternType.SimplePatternType} is expected.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final boolean isTypelyHoled(Maybe<T> input) {
        return traverse(input)
                .map(x -> x.getSemantics().isTypelyHoled((Maybe) x.getInput()))
                .orElseGet(() -> isTypelyHoledInternal(input));
    }

    protected abstract boolean isTypelyHoledInternal(Maybe<T> input);

    public final boolean subExpressionsAnyTypelyHoled(Maybe<T> input){
        return subExpressionsAnyMatch(input, ExpressionSemantics::isTypelyHoled);
    }

    /**
     * Returns true if this expression contains unbounded names in it.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final boolean isUnbound(Maybe<T> input) {
        return traverse(input)
                .map(x -> x.getSemantics().isUnbound((Maybe) x.getInput()))
                .orElseGet(() -> isUnboundInternal(input));
    }

    protected abstract boolean isUnboundInternal(Maybe<T> input);

    public final boolean subExpressionsAnyUnbound(Maybe<T> input){
        return subExpressionsAnyMatch(input, ExpressionSemantics::isUnbound);
    }

    /**
     * Returns true if this kind of expression can contain holes in order to form a pattern.
     * For example, a subscript operation cannot be holed.
     * <p></p>
     * Decided locally.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final boolean canBeHoled(Maybe<T> input) {
        return traverse(input)
                .map(x -> x.getSemantics().canBeHoled((Maybe) x.getInput()))
                .orElseGet(() -> canBeHoledInternal(input));
    }

    protected abstract boolean canBeHoledInternal(Maybe<T> input);

    /**
     * Returns true if this kind of expression contains assignable parts (i.e., parts that are bound/resolved/not-holed
     * but, at the same time, can be at the left of the assignment since they represent a writeable cell).
     * <p></p>
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final boolean containsNotHoledAssignableParts(Maybe<T> input) {
        return traverse(input)
                .map(x -> x.getSemantics().containsNotHoledAssignableParts((Maybe) x.getInput()))
                .orElseGet(() -> containsNotHoledAssignablePartsInternal(input));
    }

    protected abstract boolean containsNotHoledAssignablePartsInternal(Maybe<T> input);

    public final boolean subExpressionsAnyContainsNotHoledAssignableParts(Maybe<T> input){
        return subExpressionsAnyMatch(input, ExpressionSemantics::containsNotHoledAssignableParts);
    }

    private boolean isSubPatternGroundForEquality(PatternMatchInput<T, ?, ?> patternMatchInput) {
        return isSubPatternGroundForEquality(patternMatchInput.getPattern(), patternMatchInput.getMode());
    }

    private boolean isSubPatternGroundForAssignment(PatternMatchInput<T, ?, ?> patternMatchInput) {
        return patternMatchInput.getMode().getPatternLocation() == PatternMatchMode.PatternLocation.SUB_PATTERN
                && !isHoled(patternMatchInput.getPattern())
                && isValidLExpr(patternMatchInput.getPattern());
    }

    private boolean isSubPatternGroundForEquality(Maybe<T> pattern, PatternMatchMode mode) {
        return isSubPatternGroundForEquality(pattern, mode.getPatternLocation());
    }

    private boolean isSubPatternGroundForEquality(Maybe<T> pattern, PatternMatchMode.PatternLocation location) {
        return location == PatternMatchMode.PatternLocation.SUB_PATTERN && !isHoled(pattern);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public final <U extends PatternMatchOutput.Unification, N extends PatternMatchOutput.TypeNarrowing>
    PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, U, N> compilePatternMatch(
            PatternMatchInput<T, U, N> input,
            CompilationOutputAcceptor acceptor
    ) {
        return traverse(input.getPattern())
                .map(x -> x.getSemantics().compilePatternMatch(input.replacePattern((Maybe) x.getInput()), acceptor))
                .orElseGet(() -> prepareCompilePatternMatch(input, acceptor));
    }

    @SuppressWarnings({"unchecked"})
    private <U extends PatternMatchOutput.Unification, N extends PatternMatchOutput.TypeNarrowing>
    PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, U, N> prepareCompilePatternMatch(
            PatternMatchInput<T, U, N> input,
            CompilationOutputAcceptor acceptor
    ) {
        if (isSubPatternGroundForEquality(input)) {
            return (PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation, U, N>)
                    compileExpressionEqualityPatternMatch(input, acceptor);
        } else {
            return (PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation, U, N>)
                    compilePatternMatchInternal(input, acceptor);
        }
    }

    public abstract PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<T, ?, ?> input, CompilationOutputAcceptor acceptor);


    @SuppressWarnings({"rawtypes", "unchecked"})
    public final PatternType inferPatternType(
            Maybe<T> input, PatternMatchMode mode
    ) {
        return traverse(input)
                .map(x -> x.getSemantics().inferPatternType((Maybe) x.getInput(), mode))
                .orElseGet(() -> prepareInferPatternType(input, mode));
    }

    private PatternType prepareInferPatternType(
            Maybe<T> input, PatternMatchMode mode
    ) {
        if (isSubPatternGroundForEquality(input, mode)) {
            return PatternType.simple(inferType(input));
        } else {
            return inferPatternTypeInternal(input);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public final PatternType inferSubPatternType(Maybe<T> input) {
        return traverse(input)
                .map(x -> x.getSemantics().inferSubPatternType((Maybe) x.getInput()))
                .orElseGet(() -> prepareInferSubPatternType(input));
    }

    private PatternType prepareInferSubPatternType(Maybe<T> input) {
        if (isSubPatternGroundForEquality(input, PatternMatchMode.PatternLocation.SUB_PATTERN)) {
            return PatternType.simple(inferType(input));
        } else {
            return inferPatternTypeInternal(input);
        }
    }

    public abstract PatternType inferPatternTypeInternal(Maybe<T> input);


    @SuppressWarnings({"unchecked", "rawtypes"})
    public final <U extends PatternMatchOutput.Unification, N extends PatternMatchOutput.TypeNarrowing>
    PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, U, N> validatePatternMatch(
            PatternMatchInput<T, U, N> input,
            ValidationMessageAcceptor acceptor
    ) {
        return traverse(input.getPattern())
                .map(x -> x.getSemantics().validatePatternMatch(input.replacePattern((Maybe) x.getInput()), acceptor))
                .orElseGet(() -> prepareValidatePatternMatch(input, acceptor));
    }

    @SuppressWarnings("unchecked")
    private <U extends PatternMatchOutput.Unification, N extends PatternMatchOutput.TypeNarrowing>
    PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, U, N> prepareValidatePatternMatch(
            PatternMatchInput<T, U, N> input,
            ValidationMessageAcceptor acceptor
    ) {

        PatternMatchMode.HolesAndGroundness holesAndGroundnessRequirement = input.getMode().getHolesAndGroundness();
        PatternMatchMode.PatternApplicationPurity purityRequirement = input.getMode().getPatternApplicationPurity();
        PatternMatchMode.RequiresSuccessfulMatch successfulMatchRequirement
                = input.getMode().getRequiresSuccessfulMatch();
        PatternMatchMode.Reassignment reassignmentRequirement = input.getMode().getReassignment();
        final boolean patternGroundForEquality = isSubPatternGroundForEquality(input);
        final boolean isHoled = isHoled(input.getPattern());
        final boolean isUnbound = isUnbound(input.getPattern());
        final boolean containsNotHoledAssignableParts = containsNotHoledAssignableParts(input.getPattern());

        String describedLocation = PatternMatchMode.PatternLocation.describeLocation(input);
        if (!describedLocation.isBlank()) {
            describedLocation = "(" + describedLocation + ") ";
        }
        final Maybe<? extends EObject> eObject = Util.extractEObject(input.getPattern());
        switch (holesAndGroundnessRequirement) {
            case DOES_NOT_ACCEPT_HOLES:
                module.get(ValidationHelper.class).assertion(
                        !isHoled,
                        "InvalidPattern",
                        "A pattern in this location " + describedLocation + "cannot contain holes.",
                        eObject,
                        acceptor
                );

                break;
            case ACCEPTS_NONVAR_HOLES_ONLY:
                module.get(ValidationHelper.class).assertion(
                        !isUnbound,
                        "InvalidPattern",
                        "A pattern in this location " + describedLocation + "can only include holes " +
                                "that are not free variables.",
                        eObject,
                        acceptor
                );
                break;
            case REQUIRES_FREE_VARS:
                module.get(ValidationHelper.class).assertion(
                        isUnbound,
                        "InvalidPattern",
                        "A pattern in this location " + describedLocation + "requires at least one free variable in it.",
                        eObject,
                        acceptor
                );

                break;

            case REQUIRES_FREE_OR_ASSIGNABLE_VARS:
                module.get(ValidationHelper.class).assertion(
                        isUnbound || containsNotHoledAssignableParts,
                        "InvalidPattern",
                        "A pattern in this location " + describedLocation + "requires at least one free variable or " +
                                "assignable expression in it.",
                        eObject,
                        acceptor
                );
                break;

            case ACCEPTS_ANY_HOLE:
                // Do nothing
                break;
        }

        switch (purityRequirement) {
            case HAS_TO_BE_PURE:
                module.get(ValidationHelper.class).assertion(
                        isPatternEvaluationPure(input.getPattern()),
                        "InvalidPattern",
                        "A pattern in this location " + describedLocation + "cannot produce side-effects during its " +
                                "evaluation.",
                        eObject,
                        acceptor
                );
                break;
            case IMPURE_OK:
                break;
        }


        //TODO check reassignment mode!!
        //TODO check successful match!!


        InterceptAcceptor ia = new InterceptAcceptor(acceptor);
        if (patternGroundForEquality) {
            // This is a non-holed sub pattern: validate it as expression.
            validate(input.getPattern(), ia);
            if (!ia.thereAreErrors()) {
                // Infer its type as expression, then validate the result type
                final IJadescriptType patternType = inferType(input.getPattern());
                patternType.validateType(eObject, ia);
                if (!ia.thereAreErrors()) {
                    // Check that the type relationship requirement is met
                    validatePatternTypeRelationshipRequirement(
                            input,
                            patternType,
                            ia
                    );
                }
            }
            if (!ia.thereAreErrors()) {
                // Validate as equality condition
                return (PatternMatchOutput<PatternMatchSemanticsProcess.IsValidation, U, N>)
                        validateExpressionEqualityPatternMatch((PatternMatchInput.SubPattern<T, ?, ?, ?>) input);
            } else {
                return (PatternMatchOutput<PatternMatchSemanticsProcess.IsValidation, U, N>)
                        input.createEmptyValidationOutput();
            }

        } else {

            if (!canBeHoled(input.getPattern())) {
                module.get(ValidationHelper.class).assertion(
                        !isHoled(input.getPattern()),
                        "InvalidPattern",
                        "This kind of expression cannot contain holes to produce a pattern.",
                        eObject,
                        ia
                );
            }

            if (!ia.thereAreErrors()) {
                final IJadescriptType patternType = inferPatternType(input.getPattern(), input.getMode())
                        .solve(input.getProvidedInputType());
                patternType.validateType(eObject, ia);
                validatePatternTypeRelationshipRequirement(input, ia);
            }

            if (!ia.thereAreErrors()) {
                return (PatternMatchOutput<PatternMatchSemanticsProcess.IsValidation, U, N>)
                        validatePatternMatchInternal(input, acceptor);
            } else {
                return (PatternMatchOutput<PatternMatchSemanticsProcess.IsValidation, U, N>)
                        input.createEmptyValidationOutput();
            }
        }
    }

    public abstract PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?>
    validatePatternMatchInternal(PatternMatchInput<T, ?, ?> input, ValidationMessageAcceptor acceptor);

    private void validatePatternTypeRelationshipRequirement(
            Maybe<T> pattern,
            Class<? extends TypeRelationship> requirement,
            TypeRelationship actualRelationship,
            IJadescriptType providedInputType,
            IJadescriptType solvedPatternType,
            ValidationMessageAcceptor acceptor
    ) {
        module.get(ValidationHelper.class).assertion(
                requirement.isInstance(actualRelationship),
                "InvalidProvidedInput",
                "Cannot apply here an input of type " + providedInputType.getJadescriptName()
                        + " to a pattern which expects an input of type " + solvedPatternType.getJadescriptName(),
                Util.extractEObject(pattern),
                acceptor
        );
    }

    private <U extends PatternMatchOutput.Unification, N extends PatternMatchOutput.TypeNarrowing>
    void validatePatternTypeRelationshipRequirement(
            PatternMatchInput<T, U, N> input,
            IJadescriptType solvedType,
            ValidationMessageAcceptor acceptor
    ) {
        validatePatternTypeRelationshipRequirement(
                input.getPattern(),
                input.getMode().getTypeRelationshipRequirement(),
                module.get(TypeHelper.class).getTypeRelationship(solvedType, input.getProvidedInputType()),
                input.getProvidedInputType(),
                solvedType,
                acceptor
        );
    }

    private <U extends PatternMatchOutput.Unification, N extends PatternMatchOutput.TypeNarrowing>
    void validatePatternTypeRelationshipRequirement(
            PatternMatchInput<T, U, N> input,
            ValidationMessageAcceptor acceptor
    ) {
        validatePatternTypeRelationshipRequirement(
                input,
                inferPatternType(input.getPattern(), input.getMode()).solve(input.getProvidedInputType()),
                acceptor
        );
    }

    protected final PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?>
    validateExpressionEqualityPatternMatch(
            PatternMatchInput.SubPattern<T, ?, ?, ?> input
    ) {
        IJadescriptType patternType = inferPatternType(input.getPattern(), input.getMode()).solve(input.getProvidedInputType());
        return input.createValidationOutput(
                () -> PatternMatchOutput.EMPTY_UNIFICATION,
                () -> new PatternMatchOutput.WithTypeNarrowing(patternType)
        );
    }

    /**
     * Handles the compilation in the case where the pattern is a non-holed subpattern expression, and therefore it
     * should be treated as expression. In these cases, at runtime the pattern matches if the corresponding (part of
     * the) input value is equal to the value resulting from the pattern's sub-expression evaluation.
     *
     * @param input    the subpattern.
     * @param acceptor
     * @return a pattern matching component that simply checks if the evaluated input expression equals to the evaluated
     * expression given as subpattern.
     */
    protected final PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compileExpressionEqualityPatternMatch(
            PatternMatchInput<T, ?, ?> input,
            CompilationOutputAcceptor acceptor
    ) {
        IJadescriptType solvedPatternType = inferPatternType(input.getPattern(), input.getMode())
                .solve(input.getProvidedInputType());
        return input.createSingleConditionMethodOutput(
                solvedPatternType,
                "java.util.Objects.equals(__x, " + compile(input.getPattern(), acceptor) + ")",
                () -> PatternMatchOutput.EMPTY_UNIFICATION,
                () -> new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
        );
    }

    protected final PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?>
    validateExpressionEqualityPatternMatch(
            PatternMatchInput<T, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        IJadescriptType solvedPatternType = inferPatternType(input.getPattern(), input.getMode())
                .solve(input.getProvidedInputType());
        validate(input.getPattern(), acceptor); //TODO check type relationship?
        return input.createValidationOutput(
                () -> PatternMatchOutput.EMPTY_UNIFICATION,
                () -> new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
        );
    }



    /**
     * Validates the input expression ensuring that it can be used as condition for the execution of an event handler.
     */
    public final boolean validateUsageAsHandlerCondition(
            Maybe<T> input,
            Maybe<? extends EObject> refObject,
            ValidationMessageAcceptor acceptor
    ) {
        return module.get(ValidationHelper.class).assertion(
                isAlwaysPure(input),
                "InvalidHandlerCondition",
                "Only expressions without side effects can be used as conditions to execute an event handler",
                refObject,
                acceptor
        );
    }

    /**
     * Auxiliary class used to identify a pair (e, s) where e is an expression and s is its associated semantics
     * instance.
     */
    public static class SemanticsBoundToExpression<T extends EObject> {
        private final ExpressionSemantics<T> semantics;
        private final Maybe<T> input;

        public SemanticsBoundToExpression(ExpressionSemantics<T> semantics, Maybe<T> input) {
            this.semantics = semantics;
            this.input = input;
        }

        public ExpressionSemantics<T> getSemantics() {
            return semantics;
        }

        public Maybe<T> getInput() {
            return input;
        }

        public void doWithBinding(BiConsumer<ExpressionSemantics<T>, Maybe<T>> consumer) {
            consumer.accept(semantics, input);
        }

        public <R> R doWithBinding(BiFunction<ExpressionSemantics<T>, Maybe<T>, R> function) {
            return function.apply(semantics, input);
        }
    }


    //ALSO TODO ensure that each pattern matching is evaluated inside its own subscope
    //          this is because a pattern could introduce variables that are used in the same pattern
    //          - then remember to populate the resulting context with the variables given by the output object

}
