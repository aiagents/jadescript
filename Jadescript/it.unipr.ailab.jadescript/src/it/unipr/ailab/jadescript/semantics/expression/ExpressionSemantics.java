package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.Semantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeRelationship;
import it.unipr.ailab.jadescript.semantics.statement.StatementCompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Created on 27/12/16.
 */
@Singleton
public abstract class ExpressionSemantics<T> extends Semantics<T> {

    public ExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
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

    /**
     * Given a Maybe(input), it provides a list of pairs (e, s), where e is a subexpression of the input and s the
     * associated semantics.
     */
    public abstract List<ExpressionSemantics.SemanticsBoundToExpression<?>> getSubExpressions(Maybe<T> input);


    //TODO the compile method could return more stuff (in one sweep, which is faster). In particular:
    // - the string of the compiled expression (ECR)
    // - the flow-typing implications (ECR)
    // - the auxiliary statements (the acceptor pattern is ok, however)
    // - a simple effect-analysis (this could maybe use the acceptor pattern too...)
    // - the context-generation implications (acceptor pattern too?)
    /**
     * Produces a String resulting from the direct compilation of the input expression.
     *
     * @param input    the input expression
     * @param acceptor an acceptor that can be used to produce additional statements which will be added as auxiliary
     *                 statements for the expression evaluation
     * @return the corresponding Java expression
     */
    public abstract ExpressionCompilationResult compile(Maybe<T> input, StatementCompilationOutputAcceptor acceptor);


    /**
     * Computes the type of the input expression.
     */
    public abstract IJadescriptType inferType(Maybe<T> input);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ExpressionTypeKB extractFlowTypeTruths(Maybe<T> input) {
        if (mustTraverse(input)) {
            Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                //noinspection unchecked,rawtypes
                return traversed.get().getSemantics().extractFlowTypeTruths((Maybe) traversed.get().getInput());
            }
        }
        return ExpressionTypeKB.empty();
    }



    /**
     * This should return true <i>only if</i> the input expression can be evaluated without causing side-effects.
     * This is used, most importantly, to determine if an expression can be used as condition for handler activation (in
     * the when-clause, or as part of the content pattern).
     * Please remember that the evaluation of such conditions should not cause side-effects.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean isAlwaysPure(Maybe<T> input) {
        if (mustTraverse(input)) {
            Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                //noinspection unchecked,rawtypes
                return traversed.get().getSemantics().isAlwaysPure((Maybe) traversed.get().getInput());
            }
        }
        return true;//expressions are mostly pure
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
    public abstract boolean mustTraverse(Maybe<T> input);

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
    public abstract Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traverse(Maybe<T> input);


    //TODO move in compile, using visitor/acceptor pattern
    /**
     * Recursively navigates the AST starting from the input expression, producing a list of {@link StatementWriter}s,
     * where each writer compiles to a statement that has to be added before the compilation of the evaluation of the
     * expression.
     * This is used for those expressions (e.g, pattern matching) that require some preparative steps in generated
     * Java code in order to be evaluated.
     */

    /**
     * Applies a function to the input AST node and to each of its children (ea sunt, the input expression and its
     * sub-expressions) and collects the values resulting from the applications into a List.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R> List<? extends R> collectFromAllNodes(
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
     * Validates the input expression ensuring that it can be used as condition for the execution of an event handler.
     */
    public void validateUsageAsHandlerCondition(
            Maybe<T> input,
            Maybe<? extends EObject> refObject,
            ValidationMessageAcceptor acceptor
    ) {
        module.get(ValidationHelper.class).assertion(
                isAlwaysPure(input),
                "InvalidHandlerCondition",
                "Only expressions without side effects can be used as conditions to execute an event handler",
                refObject,
                acceptor
        );
    }


    /**
     * Returns true iff the input expression can be syntactically used as L-Expression, i.e., an assignable expression
     * that when evaluated at the left of an assignment produces a value that represents a writeable cell.
     */
    public boolean isValidLExpr(Maybe<T> input) {
        return false;
    }


    public abstract boolean isPatternEvaluationPure(Maybe<T> input);

    /**
     * Returns true if this expression contains holes in it e.g., unbounded identifiers or '_' placeholders.
     * In {@link ExpressionSemantics} there is a default implementation that returns false unless a traversing is
     * required.
     * Overridden by semantics classes which implement semantics of expressions which can present holes (without
     * traversing).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean isHoled(Maybe<T> input) {
        if (mustTraverse(input)) {
            final Optional<SemanticsBoundToExpression<?>> traverse = traverse(input);
            if (traverse.isPresent()) {
                final ExpressionSemantics<?> semantics = traverse.get().getSemantics();
                final Maybe traversedInput = traverse.get().getInput();
                return semantics.isHoled(traversedInput);
            }
        }

        return false;
    }

    /**
     * Similar to {@link ExpressionSemantics#isHoled(Maybe)} , but used only by the process of type inferring of
     * patterns.
     * Being "typely holed" is a special case of being "holed".
     * In fact, some holed patterns can still provide complete information about their type at compile time.
     * One example is the functional-notation pattern. Its argument patterns can be holed, but the type of the whole
     * pattern is completely known at compile-time, and it is the one related to the return type of the function
     * referred by the pattern.
     * By design, if this method returns true for a given pattern, then
     * {@link ExpressionSemantics#inferPatternType(Maybe, PatternMatchMode)} should return a
     * {@link PatternType.HoledPatternType}, otherwise a {@link PatternType.SimplePatternType} is expected.
     * The default implementation attempts to traverse the expression tree, and when this is not possible, it returns
     * the same value as {@link ExpressionSemantics#isHoled(Maybe)}, but this must be overridden by special cases like
     * the one in the example mentioned above.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean isTypelyHoled(Maybe<T> input) {
        if (mustTraverse(input)) {
            final Optional<SemanticsBoundToExpression<?>> traverse = traverse(input);
            if (traverse.isPresent()) {
                final ExpressionSemantics<?> semantics = traverse.get().getSemantics();
                final Maybe traversedInput = traverse.get().getInput();
                return semantics.isTypelyHoled(traversedInput);
            }
        }

        return isHoled(input);
    }

    /**
     * Returns true if this expression contains unbounded names in it.
     * In {@link ExpressionSemantics} there is a default implementation that returns false unless a traversing is
     * required.
     * Overridden by semantics classes which implement semantics of expressions which can present unbound names (without
     * traversing).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean isUnbound(Maybe<T> input) {
        if (mustTraverse(input)) {
            final Optional<SemanticsBoundToExpression<?>> traverse = traverse(input);
            if (traverse.isPresent()) {
                final ExpressionSemantics<?> semantics = traverse.get().getSemantics();
                final Maybe traversedInput = traverse.get().getInput();
                return semantics.isUnbound(traversedInput);
            }
        }

        return false;
    }

    /**
     * Returns true if this kind of expression can contain holes in order to form a pattern.
     * For example, a subscript operation cannot be holed.
     */
    protected boolean canBeHoled(Maybe<T> input) {
        //TODO ?
    }

    /**
     * Returns true if this kind of expression contains assignable parts (i.e., parts that are bound/resolved/not-holed
     * but, at the same time, can be at the left of the assignment since they represent a writeable cell).
     */
    protected boolean containsNotHoledAssignableParts(Maybe<T> input) {

    }


    protected boolean isSubPatternGroundForEquality(PatternMatchInput<T, ?, ?> patternMatchInput) {
        return isSubPatternGroundForEquality(patternMatchInput.getPattern(), patternMatchInput.getMode());
    }

    protected boolean isSubPatternGroundForAssignment(PatternMatchInput<T, ?, ?> patternMatchInput) {
        return patternMatchInput.getMode().getPatternLocation() == PatternMatchMode.PatternLocation.SUB_PATTERN
                && !isHoled(patternMatchInput.getPattern())
                && isValidLExpr(patternMatchInput.getPattern());
    }

    protected boolean isSubPatternGroundForEquality(Maybe<T> pattern, PatternMatchMode mode) {
        return isSubPatternGroundForEquality(pattern, mode.getPatternLocation());
    }

    protected boolean isSubPatternGroundForEquality(Maybe<T> pattern, PatternMatchMode.PatternLocation location) {
        return location == PatternMatchMode.PatternLocation.SUB_PATTERN && !isHoled(pattern);
    }

    @SuppressWarnings("unchecked")
    public <U extends PatternMatchOutput.Unification, N extends PatternMatchOutput.TypeNarrowing>
    PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, U, N> compilePatternMatch(
            PatternMatchInput<T, U, N> input,
            StatementCompilationOutputAcceptor acceptor
    ) {
        if (isSubPatternGroundForEquality(input)) {
            return (PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation, U, N>)
                    compileExpressionEqualityPatternMatch(input, acceptor);
        } else {
            return (PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation, U, N>)
                    compilePatternMatchInternal(input, acceptor);
        }
    }

    public PatternType inferPatternType(
            Maybe<T> input, PatternMatchMode mode
    ) {
        if (isSubPatternGroundForEquality(input, mode)) {
            return PatternType.simple(inferType(input));
        } else {
            return inferPatternTypeInternal(input);
        }
    }

    public PatternType inferSubPatternType(Maybe<T> input) {
        if (isSubPatternGroundForEquality(input, PatternMatchMode.PatternLocation.SUB_PATTERN)) {
            return PatternType.simple(inferType(input));
        } else {
            return inferPatternTypeInternal(input);
        }
    }


    @SuppressWarnings("unchecked")
    public <U extends PatternMatchOutput.Unification, N extends PatternMatchOutput.TypeNarrowing>
    PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, U, N> validatePatternMatch(
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

    protected void validatePatternTypeRelationshipRequirement(
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

    protected <U extends PatternMatchOutput.Unification, N extends PatternMatchOutput.TypeNarrowing>
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

    protected <U extends PatternMatchOutput.Unification, N extends PatternMatchOutput.TypeNarrowing>
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


    private PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?>
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
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compileExpressionEqualityPatternMatch(
            PatternMatchInput<T, ?, ?> input,
            StatementCompilationOutputAcceptor acceptor
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

    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?>
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


    public abstract PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<T, ?, ?> input, StatementCompilationOutputAcceptor acceptor);

    public abstract PatternType inferPatternTypeInternal(Maybe<T> input);

    public abstract PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?>
    validatePatternMatchInternal(PatternMatchInput<T, ?, ?> input, ValidationMessageAcceptor acceptor);


    //ALSO TODO ensure that each pattern matching is evaluated inside its own subscope
    //          this is because a pattern could introduce variables that are used in the same pattern
    //          - then remember to populate the resulting context with the variables given by the output object
    //ALSO TODO refactor compile-expression to provide auxiliary statements in one sweep
}
