package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeRelationship;

public class PatternMatchMode {

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
         * The pattern requires each term to be bound.
         */
        DOES_NOT_ACCEPT_HOLES,
        /**
         * The pattern is required to have at least one unbound variable.
         * For example, a destructuring assignment to a completely bound pattern is a useless operation and should be
         * marked as error.
         */
        REQUIRES_FREE_VARS
    }


    public enum Unification {
        /**
         * This pattern matching either produces a context, or enriches a preexisting context,
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
         * This pattern matching can contribute to produce a context, or to modify a preexisting
         * context, with additional information about the type of the input expression
         * (a.k.a. flow-typing, narrowing, smart-casting).
         */
        NARROWS_TYPE,
        /**
         * This pattern matching does not provide additional information about the type of the
         * input expression.
         */
        DOES_NOT_NARROW_TYPE
    }

    public enum PatternApplicationPurity {
        /**
         * Requires the pattern matching operation to be pure, i.e., without causing side effects.
         */
        HAS_TO_BE_PURE,
        /**
         * The pattern matching operation can cause side effects.
         */
        IMPURE_OK
    }


    public interface PatternLocation {

        static String describeLocation(PatternMatchInput<?,?,?> input){
            final PatternLocation loc = input.getMode().getPatternLocation();
            if(loc instanceof FeatureHeader){
                return "in the header of a member feature";
            }else if(loc instanceof StatementGuard){
                return "in the guard of a control flow statement";
            }else if(loc instanceof BooleanExpression){
                return "in a boolean expression";
            }else if(loc instanceof RootOfAssignedExpression){
                return "at the left of an assignment/declaration operation";
            }else if(loc instanceof SubPattern && input instanceof PatternMatchInput.SubPattern){
                return "as part of another pattern "
                        +describeLocation(((PatternMatchInput.SubPattern<?, ?, ?, ?>) input).getRootInput());
            }else if(loc instanceof Expression){
                return "in an expression";
            }else{
                return "";
            }
        }
        public interface FeatureHeader extends PatternLocation {
        }

        /**
         * The pattern is used to match against the "content" value of an event handler in its header.
         * Please note that this case concerns both content-patterns and and when-expressions.
         */
        public static final FeatureHeader FEATURE_HEADER = new FeatureHeader() {
        };

        public interface StatementGuard extends PatternLocation {
        }

        /**
         * This pattern matching is part of a statement guard (i.e., when-matches statement).
         */
        public static final StatementGuard STATEMENT_GUARD = new StatementGuard() {
        };

        public interface Expression extends PatternLocation {
        }

        public interface BooleanExpression extends Expression {
        }

        /**
         * This pattern matching is a generic boolean expression.
         */
        public static final BooleanExpression BOOLEAN_EXPRESSION = new BooleanExpression() {
        };


        public interface RootOfAssignedExpression extends Expression {
        }

        /**
         * This pattern is the root of an assigned expression e.g., the expression at the left
         * of the '=' operator in a declaration/assignment statement.
         */
        public static final RootOfAssignedExpression ROOT_OF_ASSIGNED_EXPRESSION = new RootOfAssignedExpression() {
        };

        public interface SubPattern extends Expression {
        }

        /**
         * This pattern is part of another pattern.
         */
        public static final SubPattern SUB_PATTERN = new SubPattern() {
        };
    }




    private final HolesAndGroundness holesAndGroundness;
    private final Class<? extends TypeRelationship> typeRelationshipRequirement;
    private final PatternApplicationPurity patternApplicationPurity;
    private final Unification unification;
    private final NarrowsTypeOfInput narrowsTypeOfInput;
    private final PatternLocation patternLocation;


    public PatternMatchMode(
            HolesAndGroundness holesAndGroundness,
            Class<? extends TypeRelationship> typeRelationshipRequirement,
            PatternApplicationPurity patternApplicationPurity,
            Unification unification,
            NarrowsTypeOfInput narrowsTypeOfInput,
            PatternLocation patternLocation
    ) {
        this.holesAndGroundness = holesAndGroundness;
        this.typeRelationshipRequirement = typeRelationshipRequirement;
        this.patternApplicationPurity = patternApplicationPurity;
        this.unification = unification;
        this.narrowsTypeOfInput = narrowsTypeOfInput;
        this.patternLocation = patternLocation;
    }


    public HolesAndGroundness getHolesAndGroundness() {
        return holesAndGroundness;
    }

    /**
     * Identifies the type of subtyping relationship that the type of the input of the pattern is required to have with
     * the type of the input value at compile time.
     */
    public Class<? extends TypeRelationship> getTypeRelationshipRequirement() {
        return typeRelationshipRequirement;
    }

    public PatternApplicationPurity getPatternApplicationPurity() {
        return patternApplicationPurity;
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
}
