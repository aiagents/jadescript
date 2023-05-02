package it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship;

public final class TypeRelationshipQuery {

    private final Class<? extends TypeRelationshipInternal> relationshipClz;
    private final String humanReadable;


    private TypeRelationshipQuery(
        Class<? extends TypeRelationshipInternal> clz,
        String humanReadable
    ) {
        this.relationshipClz = clz;
        this.humanReadable = humanReadable;
    }


    private static final TypeRelationshipQuery RELATED =
        new TypeRelationshipQuery(
            TypeRelationshipInternal.Related.class,
            "related to"
        );

    private static final TypeRelationshipQuery NOT_RELATED =
        new TypeRelationshipQuery(
            TypeRelationshipInternal.NotRelated.class,
            "not related to"
        );

    private static final TypeRelationshipQuery SUBTYPE_OR_EQUAL =
        new TypeRelationshipQuery(
            TypeRelationshipInternal.SubtypeOrEqual.class,
            "a subtype of or equal to"
        );

    private static final TypeRelationshipQuery SUPERTYPE_OR_EQUAL =
        new TypeRelationshipQuery(
            TypeRelationshipInternal.SupertypeOrEqual.class,
            "a supertype of or equal to"
        );

    private static final TypeRelationshipQuery EQUAL =
        new TypeRelationshipQuery(
            TypeRelationshipInternal.Equal.class,
            "equal to"
        );

    private static final TypeRelationshipQuery STRICT_SUPERTYPE =
        new TypeRelationshipQuery(
            TypeRelationshipInternal.StrictSupertype.class,
            "a strict supertype of"
        );

    private static final TypeRelationshipQuery STRICT_SUBTYPE =
        new TypeRelationshipQuery(
            TypeRelationshipInternal.StrictSubtype.class,
            "a strict subtype of"
        );


    public String getHumanReadableString() {
        return humanReadable;
    }


    public static TypeRelationshipQuery related() {
        return RELATED;
    }


    public static TypeRelationshipQuery notRelated() {
        return NOT_RELATED;
    }


    public static TypeRelationshipQuery subTypeOrEqual() {
        return SUBTYPE_OR_EQUAL;
    }


    public static TypeRelationshipQuery superTypeOrEqual() {
        return SUPERTYPE_OR_EQUAL;
    }


    public static TypeRelationshipQuery equal() {
        return EQUAL;
    }


    public static TypeRelationshipQuery strictSuperType() {
        return STRICT_SUPERTYPE;
    }


    public static TypeRelationshipQuery strictSubType() {
        return STRICT_SUBTYPE;
    }


    /*package-private*/ boolean matches(TypeRelationshipInternal internal) {
        return this.relationshipClz.isInstance(internal);
    }


    public boolean matches(TypeRelationship relationship) {
        return this.matches(relationship.getInternal());
    }


}
