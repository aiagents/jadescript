package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery;

public class PatternMatchMode {

    /* Requirements on the pattern and the input value: */
    private final HolesAndGroundness holesAndGroundness;
    private final TypeRelationshipQuery typeRelationshipRequirement;
    private final RequiresSuccessfulMatch requiresSuccessfulMatch;
    private final PatternApplicationSideEffects patternApplicationSideEffects;
    private final Reassignment reassignment;
    /* Expectations on the pattern match evaluation results: */
    private final Unification unification;
    private final NarrowsTypeOfInput narrowsTypeOfInput;
    /* Additional input information: */
    private final PatternLocation patternLocation;


    public PatternMatchMode(
        HolesAndGroundness holesAndGroundness,
        TypeRelationshipQuery typeRelationshipRequirement,
        RequiresSuccessfulMatch requiresSuccessfulMatch,
        PatternApplicationSideEffects patternApplicationSideEffects,
        Reassignment reassignment,
        Unification unification,
        NarrowsTypeOfInput narrowsTypeOfInput,
        PatternLocation patternLocation
    ) {
        this.holesAndGroundness = holesAndGroundness;
        this.typeRelationshipRequirement = typeRelationshipRequirement;
        this.requiresSuccessfulMatch = requiresSuccessfulMatch;
        this.patternApplicationSideEffects = patternApplicationSideEffects;
        this.reassignment = reassignment;
        this.unification = unification;
        this.narrowsTypeOfInput = narrowsTypeOfInput;
        this.patternLocation = patternLocation;
    }


    public HolesAndGroundness getHolesAndGroundness() {
        return holesAndGroundness;
    }


    /**
     * Identifies the type of subtyping relationship that the type of the
     * input of the pattern is required to have with
     * the type of the input value at compile time.
     */
    public TypeRelationshipQuery getTypeRelationshipRequirement() {
        return typeRelationshipRequirement;
    }


    public PatternApplicationSideEffects getPatternApplicationPurity() {
        return patternApplicationSideEffects;
    }


    public Unification getUnification() {
        return unification;
    }


    public NarrowsTypeOfInput getNarrowsTypeOfInput() {
        return narrowsTypeOfInput;
    }


    public PatternLocation getPatternLocation() {
        return patternLocation;
    }


    public Reassignment getReassignment() {
        return reassignment;
    }


    public RequiresSuccessfulMatch getRequiresSuccessfulMatch() {
        return requiresSuccessfulMatch;
    }


    public enum HolesAndGroundness {
        /**
         * The pattern can have variables not bounded to any value
         * (and also other types of holes).
         */
        ACCEPTS_ANY_HOLE,
        /**
         * The pattern can have holes which are not free variables.
         * (e.g., the '_' placeholder).
         * Any not-resolved name in the pattern should be marked as error.
         */
        ACCEPTS_NONVAR_HOLES_ONLY,
        /**
         * The pattern cannot contain any kind of hole.
         */
        DOES_NOT_ACCEPT_HOLES,
        /**
         * The pattern is required to have at least one unbound variable.
         * For example, a for-destructuring to a completely bound pattern is
         * a useless operation and should be marked as error
         * (for-destructuring cannot reassign values in the outer scope).
         */
        REQUIRES_FREE_VARS,
        /**
         * The pattern is required to have at least one unbound or assignable
         * variable.
         * For example, a destructuring assignment to a pattern which does
         * not declare any variable or does not reassign any L-Expression, is a
         * useless operation and should be marked as error.
         */
        REQUIRES_FREE_OR_ASSIGNABLE_VARS
    }


    public enum Unification {
        /**
         * This pattern matching either produces a context, or enriches a
         * preexisting context,
         * with new declared variables (destructuring).
         */
        WITH_VAR_DECLARATION,
        /**
         * This pattern matching does not declare new variables.
         */
        WITHOUT_VAR_DECLARATION
    }


    public enum NarrowsTypeOfInput {
        /**
         * This pattern matching can contribute to produce a context, or to
         * modify a preexisting context, with additional information about
         * the type of the input expression (a.k.a. flow-typing, narrowing,
         * smart-casting).
         */
        NARROWS_TYPE,
        /**
         * This pattern matching does not provide additional information
         * about the type of the input expression.
         */
        DOES_NOT_NARROW_TYPE
    }


    /**
     * Determines if the pattern can be a failing pattern (i.e., it can
     * contain run-time conditions that cannot be predicted to match at
     * compile-time) or if it is required to be guaranteed, at compile time,
     * to always match if the type - which should be also checked at compile
     * time - is compatible.
     * In the REQUIRES_SUCCESSFUL_MATCH mode, patterns which match with
     * runtime-only conditions cannot be used as pattern, and the validator
     * should check this.
     * For example, list, map and set patterns which have terms before
     * the pipe, or which do not contain the pipe, cannot guarantee at compile
     * time to match even if the input type is compatible, because the result
     * of the matching depends on the size of the input collection, which is
     * only known at runtime. On the other hand, structural
     * patterns and tuples are always successful if the provided type is
     * correct. Therefore, the compiler can ensure that the pattern matches at
     * compile-time.
     */
    public enum RequiresSuccessfulMatch {
        REQUIRES_SUCCESSFUL_MATCH,
        CAN_FAIL
    }


    /**
     * Determines how to interpret not-holed expressions.
     * Pattern-matching of holed expressions are not influenced by this.
     */
    public enum Reassignment {
        /**
         * If this not-holed-expression is a valid L-Expression, just perform
         * an assignment, otherwise, mark as error at compile time.
         */
        REQUIRE_REASSIGN,
        /**
         * This not-holed-expression is always considered an R-Expression,
         * whose resulting value is checked for equality against the
         * corresponding input.
         */
        CHECK_EQUALITY
    }


    public enum PatternApplicationSideEffects {
        /**
         * Requires the pattern matching operation to be without side effects.
         */
        HAS_TO_BE_WITHOUT_SIDE_EFFECTS,
        /**
         * The pattern matching operation can cause side effects.
         */
        CAN_HAVE_SIDE_EFFECTS
    }


    public interface PatternLocation {

        /**
         * The pattern is used to match against the "content" value of an
         * event handler in its header.
         * Please note that this case concerns both content-patterns and
         * 'matches' operators in when-expressions of event handlers.
         */
        FeatureHeader FEATURE_HEADER = new FeatureHeader() {
        };
        /**
         * This pattern matching is part of a statement guard (i.e.,
         * when-matches statement).
         */
        StatementGuard STATEMENT_GUARD =
            new StatementGuard() {
            };
        /**
         * This pattern matching is a generic boolean expression.
         */
        BooleanExpression BOOLEAN_EXPRESSION =
            new BooleanExpression() {
            };
        /**
         * This pattern is the root of an assigned expression e.g., the
         * expression at the left of the '=' operator in a
         * declaration/assignment statement.
         */
        RootOfAssignedExpression
            ROOT_OF_ASSIGNED_EXPRESSION = new RootOfAssignedExpression() {
        };
        /**
         * This pattern is part of another pattern.
         */
        SubPattern SUB_PATTERN = new SubPattern() {
        };

        static String describeLocation(PatternMatchInput<?> input) {
            final PatternLocation loc = input.getMode().getPatternLocation();
            if (loc instanceof FeatureHeader) {
                return "in the header of a member feature";
            } else if (loc instanceof StatementGuard) {
                return "in the guard of a control flow statement";
            } else if (loc instanceof BooleanExpression) {
                return "in a boolean expression";
            } else if (loc instanceof RootOfAssignedExpression) {
                return "at the left of an assignment/declaration operation";
            } else if (loc instanceof SubPattern && input instanceof
                PatternMatchInput.SubPattern) {
                return "as part of another pattern " + describeLocation(
                    ((PatternMatchInput.SubPattern<?, ?>) input).getRootInput()
                );
            } else if (loc instanceof Expression) {
                return "in an expression";
            } else {
                return "";
            }
        }

        interface FeatureHeader extends PatternLocation {

        }

        interface StatementGuard extends PatternLocation {

        }


        interface Expression extends PatternLocation {

        }

        interface BooleanExpression extends Expression {

        }

        interface RootOfAssignedExpression extends Expression {

        }

        interface SubPattern extends Expression {

        }

    }

}
