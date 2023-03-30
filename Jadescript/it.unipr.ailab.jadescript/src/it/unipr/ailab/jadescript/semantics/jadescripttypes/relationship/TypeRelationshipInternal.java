package it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship;


/**
 * Identifies the type of subtyping relationship that a type has w.r.t. to another.
 */
interface TypeRelationshipInternal {



    interface Related extends TypeRelationshipInternal {
    }

    interface NotRelated extends TypeRelationshipInternal {
    }

    NotRelated NOT_RELATED = new NotRelated() {
        @Override
        public String toString() {
            return "NotRelated";
        }
    };

    interface SubtypeOrEqual extends Related {
    }

    interface SupertypeOrEqual extends Related {
    }

    interface Equal extends SupertypeOrEqual, SubtypeOrEqual {
    }

    Equal EQUAL = new Equal() {
        @Override
        public String toString() {
            return "Equal";
        }
    };

    interface StrictSubtype extends SubtypeOrEqual {
    }

    StrictSubtype STRICT_SUBTYPE = new StrictSubtype() {
        @Override
        public String toString() {
            return "StricSubtype";
        }
    };

    interface StrictSupertype extends SupertypeOrEqual {
    }

    StrictSupertype STRICT_SUPERTYPE = new StrictSupertype() {
        @Override
        public String toString() {
            return "StrictSupertype";
        }
    };

}
