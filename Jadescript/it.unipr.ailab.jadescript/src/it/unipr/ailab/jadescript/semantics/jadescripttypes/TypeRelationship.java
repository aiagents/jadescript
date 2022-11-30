package it.unipr.ailab.jadescript.semantics.jadescripttypes;


/**
 * Identifies the type of subtyping relationship that a type has w.r.t. to another.
 */
public interface TypeRelationship {

    public interface Related extends TypeRelationship {
    }

    public interface NotRelated extends TypeRelationship {
    }

    public static final NotRelated NOT_RELATED = new NotRelated() {
    };

    public interface SubtypeOrEqual extends Related {
    }

    public interface SupertypeOrEqual extends Related {
    }

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
