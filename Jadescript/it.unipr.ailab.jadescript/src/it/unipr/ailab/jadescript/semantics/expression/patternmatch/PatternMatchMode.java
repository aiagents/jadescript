package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

public class PatternMatchMode {

    public enum HolesAndGroundness {
        /**
         * The pattern can have variables not bounded to any value
         * (and also other types of holes).
         */
        ACCEPTS_FREE_VARS,
        /**
         * The pattern can have holes which are not free variables.
         * (e.g., the '_' placeholder).
         * Any not-resolved name in the pattern should be marked as error.
         */
        ACCEPTS_NONVAR_HOLES_ONLY,
        /**
         * The pattern requires each term to be bound.
         */
        DOES_NOT_ACCEPT_HOLES
    }

    /**
     * Identifies the type of subtyping relationship the type of the input of the pattern is required to have with the
     * type of the input value.
     */
    public interface TypeRelationship {

        public interface Related extends TypeRelationship {
        }

        public static final Related RELATED = new Related() {
        };

        public interface NotRelated extends TypeRelationship {
        }

        public static final NotRelated NOT_RELATED = new NotRelated() {
        };

        public interface SubtypeOrEqual extends Related {
        }

        public static final SubtypeOrEqual SUBTYPE_OR_EQUAL = new SubtypeOrEqual() {
        };

        public interface SupertypeOrEqual extends Related {
        }

        public static final SupertypeOrEqual SUPERTYPE_OR_EQUAL = new SupertypeOrEqual() {
        };

        public interface Equal extends SupertypeOrEqual, SubtypeOrEqual {
        }

        public static final Equal EQUAL = new Equal() {
        };

        public interface StrictSubtype extends SubtypeOrEqual {
        }

        public static final StrictSubtype STRICT_SUBTYPE = new StrictSubtype() {
        };

        public interface StrictSupertype extends SupertypeOrEqual {
        }

        public static final StrictSupertype STRICT_SUPERTYPE = new StrictSupertype() {
        };

    }


    public enum Deconstruction {
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


    public interface PatternLocation {
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

        public interface AssignedExpression extends Expression {
        }

        public interface RootOfAssignedExpression extends AssignedExpression {
        }

        /**
         * This pattern is the root of an assigned expression e.g., the expression at the left
         * of the '=' operator in a declaration/assignment statement.
         */
        public static final RootOfAssignedExpression ROOT_OF_ASSIGNED_EXPRESSION = new RootOfAssignedExpression() {
        };

        public interface PartOfAssignedExpression extends AssignedExpression {
        }

        /**
         * This pattern is part of an assigned expression, but it is not the root of it.
         */
        public static final PartOfAssignedExpression PART_OF_ASSIGNED_EXPRESSION = new PartOfAssignedExpression() {
        };
    }


    private final HolesAndGroundness holesAndGroundness;
    private final Class<? extends TypeRelationship> typeRelationshipRequirement;
    private final Deconstruction deconstruction;
    private final NarrowsTypeOfInput narrowsTypeOfInput;
    private final PatternLocation patternLocation;


    public PatternMatchMode(
            HolesAndGroundness holesAndGroundness,
            Class<? extends TypeRelationship> typeRelationshipRequirement,
            Deconstruction deconstruction,
            NarrowsTypeOfInput narrowsTypeOfInput,
            PatternLocation patternLocation
    ) {
        this.holesAndGroundness = holesAndGroundness;
        this.typeRelationshipRequirement = typeRelationshipRequirement;
        this.deconstruction = deconstruction;
        this.narrowsTypeOfInput = narrowsTypeOfInput;
        this.patternLocation = patternLocation;
    }


    public HolesAndGroundness getHolesAndBoundness() {
        return holesAndGroundness;
    }

    public Class<? extends TypeRelationship> getTypeRelationshipRequirement() {
        return typeRelationshipRequirement;
    }

    public Deconstruction getDeconstruction() {
        return deconstruction;
    }

    public NarrowsTypeOfInput getNarrowsTypeOfInput() {
        return narrowsTypeOfInput;
    }

    public PatternLocation getPatternLocation() {
        return patternLocation;
    }
}
