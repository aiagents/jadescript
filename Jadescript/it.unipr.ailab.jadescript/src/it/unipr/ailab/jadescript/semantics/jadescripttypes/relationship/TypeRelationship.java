package it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship;

public abstract class TypeRelationship {

    private final TypeRelationshipInternal relationship;


    private TypeRelationship(
        TypeRelationshipInternal internal
    ) {
        this.relationship = internal;
    }


    public boolean is(TypeRelationshipQuery query) {
        return query.matches(this);
    }


    public String getHumanReadableString() {
        return relationship.toString();
    }


    private static final TypeRelationship EQUAL =
        new TypeRelationship(TypeRelationshipInternal.EQUAL) {
            @Override
            public TypeRelationship filterSuperOrEqual() {
                return this;
            }


            @Override
            public TypeRelationship filterSubOrEqual() {
                return this;
            }


            @Override
            public TypeRelationship filterEqual() {
                return this;
            }


            @Override
            public TypeRelationship filterStrictlySuper() {
                return notRelated();
            }


            @Override
            public TypeRelationship filterStrictlySub() {
                return notRelated();
            }


            @Override
            public TypeRelationship flip() {
                return this;
            }
        };

    private static final TypeRelationship SUPERTYPE =
        new TypeRelationship(TypeRelationshipInternal.STRICT_SUPERTYPE) {
            @Override
            public TypeRelationship filterSuperOrEqual() {
                return this;
            }


            @Override
            public TypeRelationship filterSubOrEqual() {
                return TypeRelationship.notRelated();
            }


            @Override
            public TypeRelationship filterEqual() {
                return TypeRelationship.notRelated();
            }


            @Override
            public TypeRelationship filterStrictlySuper() {
                return this;
            }


            @Override
            public TypeRelationship filterStrictlySub() {
                return TypeRelationship.notRelated();
            }


            @Override
            public TypeRelationship flip() {
                return subType();
            }
        };

    private static final TypeRelationship SUBTYPE =
        new TypeRelationship(TypeRelationshipInternal.STRICT_SUBTYPE) {
            @Override
            public TypeRelationship filterSuperOrEqual() {
                return TypeRelationship.notRelated();
            }


            @Override
            public TypeRelationship filterSubOrEqual() {
                return this;
            }


            @Override
            public TypeRelationship filterEqual() {
                return TypeRelationship.notRelated();
            }


            @Override
            public TypeRelationship filterStrictlySuper() {
                return TypeRelationship.notRelated();
            }


            @Override
            public TypeRelationship filterStrictlySub() {
                return this;
            }


            @Override
            public TypeRelationship flip() {
                return superType();
            }
        };

    private static final TypeRelationship NOT_RELATED =
        new TypeRelationship(TypeRelationshipInternal.NOT_RELATED) {
            @Override
            public TypeRelationship filterSuperOrEqual() {
                return this;
            }


            @Override
            public TypeRelationship filterSubOrEqual() {
                return this;
            }


            @Override
            public TypeRelationship filterEqual() {
                return this;
            }


            @Override
            public TypeRelationship filterStrictlySuper() {
                return this;
            }


            @Override
            public TypeRelationship filterStrictlySub() {
                return this;
            }


            @Override
            public TypeRelationship flip() {
                return this;
            }
        };


    public static TypeRelationship equal() {
        return EQUAL;
    }


    public static TypeRelationship superType() {
        return SUPERTYPE;
    }


    public static TypeRelationship subType() {
        return SUBTYPE;
    }


    public static TypeRelationship notRelated() {
        return NOT_RELATED;
    }


    /*package-private*/ TypeRelationshipInternal getInternal() {
        return this.relationship;
    }


    @Override
    public String toString() {
        return getInternal().toString();
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TypeRelationship) {
            return getInternal().equals(
                ((TypeRelationship) obj).getInternal()
            );
        }

        return super.equals(obj);
    }


    @Override
    public int hashCode() {
        return getInternal().hashCode();
    }


    public abstract TypeRelationship filterSuperOrEqual();

    public abstract TypeRelationship filterSubOrEqual();

    public abstract TypeRelationship filterEqual();

    public abstract TypeRelationship filterStrictlySuper();

    public abstract TypeRelationship filterStrictlySub();

    public abstract TypeRelationship flip();

}
