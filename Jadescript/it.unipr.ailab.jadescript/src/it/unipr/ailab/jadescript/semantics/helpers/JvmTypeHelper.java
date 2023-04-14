package it.unipr.ailab.jadescript.semantics.helpers;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.implicit.ImplicitConversionsHelper;
import it.unipr.ailab.jadescript.semantics.utils.JvmTypeQualifiedNameParser;
import it.unipr.ailab.jadescript.semantics.utils.JvmTypeReferenceSet;
import it.unipr.ailab.maybe.utils.LazyInit;
import jadescript.lang.Tuple;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.*;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public class JvmTypeHelper {

    private final SemanticsModule module;

    private final LazyInit<ImplicitConversionsHelper> conversions;


    public JvmTypeHelper(SemanticsModule module) {
        this.module = module;
        this.conversions = LazyInit.lazyInit(() -> {
            return module.get(ImplicitConversionsHelper.class);
        });
    }


    public List<JvmTypeReference> getTypeArgumentsOfParent(
        JvmTypeReference type,
        JvmTypeReference targetParentNoParams
    ) {
        for (JvmTypeReference x : getParentChainIncluded(type)) {
            if (x instanceof JvmParameterizedTypeReference
                && x.getType().getQualifiedName().equals(
                targetParentNoParams.getQualifiedName()
            )
            ) {
                return ((JvmParameterizedTypeReference) x).getArguments();
            }
        }
        return new ArrayList<>();
    }


    public Stream<JvmTypeReference> getParentClasses(
        @NotNull JvmTypeReference x
    ) {
        final JvmType referenced = x.getType();
        if (!(referenced instanceof JvmDeclaredType)) {
            return Stream.empty();
        }

        JvmDeclaredType declared = (JvmDeclaredType) referenced;

        final JvmTypeReference extendedClass = declared.getExtendedClass();
        if (extendedClass == null) {
            return Stream.empty();
        }

        Map<String, JvmTypeReference> typeParametersAssigned = null;

        if (x instanceof JvmParameterizedTypeReference
            && referenced instanceof JvmGenericType) {
            JvmParameterizedTypeReference parameterized =
                (JvmParameterizedTypeReference) x;
            JvmGenericType generic = (JvmGenericType) referenced;

            final EList<JvmTypeReference> arguments =
                parameterized.getArguments();
            final EList<JvmTypeParameter> typeParameters =
                generic.getTypeParameters();


            int size = Math.min(arguments.size(), typeParameters.size());

            typeParametersAssigned = new HashMap<>(size);

            for (int i = 0; i < size; i++) {
                final JvmTypeReference arg = arguments.get(i);
                final String parName = typeParameters.get(i).getName();
                typeParametersAssigned.put(parName, arg);
            }
        }


        if (typeParametersAssigned == null) {
            return Stream.concat(
                Stream.of(extendedClass),
                getParentClasses(extendedClass)
            );
        }

        if (extendedClass.getType() instanceof JvmTypeParameter) {
            final JvmTypeReference argument =
                typeParametersAssigned.get(
                    ((JvmTypeParameter) extendedClass.getType()
                    ).getName());

            if (argument == null) {
                return Stream.concat(
                    Stream.of(extendedClass),
                    getParentClasses(extendedClass)
                );
            }

            return Stream.concat(
                Stream.of(argument),
                getParentClasses(argument)
            );

        } else if (extendedClass instanceof JvmParameterizedTypeReference
            && extendedClass.getType() instanceof JvmDeclaredType) {

            JvmParameterizedTypeReference extendendParameterized =
                (JvmParameterizedTypeReference) extendedClass;

            final EList<JvmTypeReference> arguments =
                extendendParameterized.getArguments();

            final List<JvmTypeReference> newArguments =
                new ArrayList<>(arguments);

            for (int i = 0; i < newArguments.size(); i++) {
                final JvmTypeReference arg = newArguments.get(i);
                if (!(arg.getType() instanceof JvmTypeParameter)) {
                    continue;
                }

                JvmTypeParameter param = (JvmTypeParameter) arg.getType();
                final JvmTypeReference resolvedArg =
                    typeParametersAssigned.get(param.getName());

                if (resolvedArg == null) {
                    continue;
                }

                newArguments.set(i, resolvedArg);
            }

            final JvmTypeReference ref =
                typeRef(extendedClass.getType(), newArguments);
            return Stream.concat(
                Stream.of(ref),
                getParentClasses(ref)
            );
        } else {
            return Stream.concat(
                Stream.of(extendedClass),
                getParentClasses(extendedClass)
            );
        }


    }


    public List<JvmTypeReference> getParentChainIncluded(JvmTypeReference x) {
        List<JvmTypeReference> result = new ArrayList<>();
        result.add(x);
        if (x.getType() instanceof JvmDeclaredType) {
            if (((JvmDeclaredType) x.getType()).getExtendedClass() != null) {
                result.addAll(getParentChainIncluded(
                    ((JvmDeclaredType) x.getType()).getExtendedClass()
                ));
            }
        } else {
            result.add(typeRef(Object.class));
        }
        return result;
    }


    private static String boxedName(String input) {
        switch (input) {
            case SemanticsConsts.JAVA_PRIMITIVE_int:
                return SemanticsConsts.JAVA_TYPE_Integer;
            case SemanticsConsts.JAVA_PRIMITIVE_float:
                return SemanticsConsts.JAVA_TYPE_Float;
            case SemanticsConsts.JAVA_PRIMITIVE_long:
                return SemanticsConsts.JAVA_TYPE_Long;
            case SemanticsConsts.JAVA_PRIMITIVE_char:
                return SemanticsConsts.JAVA_TYPE_Character;
            case SemanticsConsts.JAVA_PRIMITIVE_short:
                return SemanticsConsts.JAVA_TYPE_Short;
            case SemanticsConsts.JAVA_PRIMITIVE_double:
                return SemanticsConsts.JAVA_TYPE_Double;
            case SemanticsConsts.JAVA_PRIMITIVE_boolean:
                return SemanticsConsts.JAVA_TYPE_Boolean;
            case SemanticsConsts.JAVA_PRIMITIVE_byte:
                return SemanticsConsts.JAVA_TYPE_Byte;
            default:
                return input;
        }
    }


    @SuppressWarnings("unused")
    public static String unBoxedName(String input) {
        switch (input) {
            case SemanticsConsts.JAVA_TYPE_Integer:
                return SemanticsConsts.JAVA_PRIMITIVE_int;
            case SemanticsConsts.JAVA_TYPE_Float:
                return SemanticsConsts.JAVA_PRIMITIVE_float;
            case SemanticsConsts.JAVA_TYPE_Long:
                return SemanticsConsts.JAVA_PRIMITIVE_long;
            case SemanticsConsts.JAVA_TYPE_Character:
                return SemanticsConsts.JAVA_PRIMITIVE_char;
            case SemanticsConsts.JAVA_TYPE_Short:
                return SemanticsConsts.JAVA_PRIMITIVE_short;
            case SemanticsConsts.JAVA_TYPE_Double:
                return SemanticsConsts.JAVA_PRIMITIVE_double;
            case SemanticsConsts.JAVA_TYPE_Boolean:
                return SemanticsConsts.JAVA_PRIMITIVE_boolean;
            case SemanticsConsts.JAVA_TYPE_Byte:
                return SemanticsConsts.JAVA_PRIMITIVE_byte;
            default:
                return input;
        }
    }


    public static boolean typeReferenceRawEquals(
        JvmTypeReference a,
        JvmTypeReference b
    ) {
        return Objects.equals(
            boxedName(noGenericsTypeName(a.getQualifiedName('.'))),
            boxedName(noGenericsTypeName(b.getQualifiedName('.')))
        );
    }


    public static boolean typeReferenceEquals(
        JvmTypeReference a,
        JvmTypeReference b
    ) {
        return Objects.equals(
            boxedName(a.getQualifiedName('.')),
            boxedName(b.getQualifiedName('.'))
        );
    }


    public boolean isAssignable(Class<?> toType, JvmTypeReference fromType) {
        return isAssignableRaw(typeRef(toType), fromType);
    }


    public JvmTypeReference objectTypeRef() {
        return module.get(JvmTypeReferenceBuilder.class).typeRef(Object.class);
    }


    public boolean isAssignableRaw(
        Class<?> toType,
        JvmTypeReference fromType
    ) {
        return isAssignableRaw(
            typeRef(toType),
            fromType
        );
    }


    public boolean isAssignableRaw(
        JvmTypeReference toType,
        JvmTypeReference fromType
    ) {
        JvmTypeReference left = toType;
        JvmTypeReference right = fromType;
        if (isNullOrVoid(right)) {
            return false;
        }

        if (isNullOrVoid(right)) {
            return false;
        }

        if (conversions.get().isJVMPrimitiveWideningViable(fromType, toType)) {
            return true;
        }

        if (toType.getType() instanceof JvmPrimitiveType) {
            left = conversions.get().boxedReference(
                (JvmPrimitiveType) toType.getType()
            );
        }
        if (fromType.getType() instanceof JvmPrimitiveType) {
            right = conversions.get().boxedReference(
                (JvmPrimitiveType) fromType.getType()
            );
        }

        if (JvmTypeHelper.typeReferenceRawEquals(left, right)) {
            return true;
        }


        final String tupleInterfaceName = Tuple.class.getName();
        final String tupleInterfaceSimpleName = Tuple.class.getSimpleName();
        if (Objects.equals(
            JvmTypeHelper.noGenericsTypeName(left.getQualifiedName('.')),
            tupleInterfaceName
        )) {
            //ad hoc fix for tuple types
            final String noGenericsFQN = JvmTypeHelper.noGenericsTypeName(
                right.getQualifiedName('.')
            );
            final String noGenericsSN = JvmTypeHelper.noGenericsTypeName(
                right.getSimpleName()
            );
            return noGenericsFQN.startsWith(tupleInterfaceName)
                && noGenericsSN.startsWith(tupleInterfaceSimpleName);
        }


        if (left.getType() instanceof JvmDeclaredType
            && right.getType() instanceof JvmDeclaredType) {

            if (left.getType().equals(right.getType())) {
                return true;
            }

            final JvmTypeReferenceSet superSet =
                JvmTypeReferenceSet.generateAllSupertypesSet(
                    (JvmDeclaredType) right.getType()
                );

            return superSet.containsRaw(left);

        }

        if (left instanceof JvmGenericArrayTypeReference
            && right instanceof JvmGenericArrayTypeReference) {
            //if left and right are array types, just see if their component
            // types matches isAssignableRaw
            return isAssignableRaw(
                ((JvmGenericArrayTypeReference) left).getComponentType(),
                ((JvmGenericArrayTypeReference) right).getComponentType()
            );
        }

        //noinspection IfStatementWithIdenticalBranches
        if (left instanceof JvmGenericArrayTypeReference
            && right.getType() instanceof JvmDeclaredType
            || left.getType() instanceof JvmDeclaredType
            && right instanceof JvmGenericArrayTypeReference) {
            //one is array, the other is not: not assignable
            return false;
        }

        return false;
    }


    private boolean isNullOrVoid(JvmTypeReference jvmTypeReference) {
        return jvmTypeReference == null
            || jvmTypeReference.getIdentifier() == null
            || jvmTypeReference.getIdentifier().equals("void");
    }


    public boolean isAssignableGeneric(
        JvmTypeReference toType,
        JvmTypeReference fromType
    ) {
        JvmTypeReference left = toType;
        JvmTypeReference right = fromType;
        if (isNullOrVoid(left) || isNullOrVoid(right)) {
            return false;
        }

        if (conversions.get().isJVMPrimitiveWideningViable(fromType, toType)) {
            return true;
        }

        left = conversions.get().boxedReferenceIfPrimitive(left);
        right = conversions.get().boxedReferenceIfPrimitive(right);

        if (JvmTypeHelper.typeReferenceEquals(left, right)) {
            return true;
        }

        if (left.getQualifiedName().equals("jadescript.lang.Tuple")) {
            //ad hoc fix for tuple types
            return right.getQualifiedName('.')
                .startsWith("jadescript.lang.Tuple")
                && right.getSimpleName().startsWith("Tuple");
        }

        if (left.getType() instanceof JvmDeclaredType
            && right.getType() instanceof JvmDeclaredType) {

            if (!(left instanceof JvmParameterizedTypeReference)) {
                return left.getType().equals(right.getType())
                    || JvmTypeReferenceSet.generateAllSupertypesSet(
                    (JvmDeclaredType) right.getType()
                ).contains(left);
            }

            JvmParameterizedTypeReference leftJvmptr =
                (JvmParameterizedTypeReference) left;

            if (!(right instanceof JvmParameterizedTypeReference)) {
                return false;
            } else {
                JvmParameterizedTypeReference rightJvmptr =
                    (JvmParameterizedTypeReference) right;
                if (leftJvmptr.getArguments().size()
                    != rightJvmptr.getArguments().size()) {
                    return false;
                } else {
                    for (int i = 0; i < leftJvmptr.getArguments().size(); ++i) {
                        if (!JvmTypeHelper.typeReferenceEquals(
                            leftJvmptr.getArguments().get(i),
                            rightJvmptr.getArguments().get(i)
                        )) {
                            return false;
                        }
                    }
                    //all type parameters are the same
                }
            }

            return left.getType().equals(right.getType())
                || JvmTypeReferenceSet.generateAllSupertypesSet(
                (JvmDeclaredType) right.getType()
            ).contains(left);

        } else if (left instanceof JvmGenericArrayTypeReference
            && right instanceof JvmGenericArrayTypeReference) {
            //if left and right are array types, just see if their component
            // types matches isAssignable
            return isAssignableGeneric(
                ((JvmGenericArrayTypeReference) left).getComponentType(),
                ((JvmGenericArrayTypeReference) right).getComponentType()
            );
        } else if (left instanceof JvmGenericArrayTypeReference
            && right.getType() instanceof JvmDeclaredType
            || left.getType() instanceof JvmDeclaredType
            && right instanceof JvmGenericArrayTypeReference) {
            //one is array, the other is not: not assignable
            return false;
        } else {
            return false;
        }
    }


    /**
     * Determines if the class or interface represented by the
     * <code>toType</code> type reference
     * is either the same as, or is a superclass or superinterface of, the
     * class or interface
     * represented by <code>fromType</code> type reference. It tries to
     * respond to the question,
     * in the context of the JVM type system, "can a Java value of the
     * type referenced by
     * <code>fromType</code> be assigned to a Java variable of the type
     * referenced by
     * <code>toType</code>?".
     */
    public boolean isAssignable(
        JvmTypeReference toType,
        JvmTypeReference fromType,
        boolean rawComparison
    ) {
        if (rawComparison) {
            return isAssignableRaw(toType, fromType);
        }

        return isAssignableGeneric(toType, fromType);
    }


    public static String extractPackageName(JvmTypeReference jtr) {
        String[] split = jtr.getQualifiedName().split("\\.");
        StringBuilder packageName = new StringBuilder();
        for (int i = 0; i < split.length - 1; i++) {
            String s = split[i];
            packageName.append(s);
        }
        return packageName.toString();
    }


    public static String noGenericsTypeName(String type) {
        if (type == null) {
            return "";
        }
        int endIndex = type.indexOf('<');
        if (endIndex < 0) {
            return type;
        }
        return type.substring(0, endIndex);
    }


    public JvmTypeReference typeRef(
        Class<?> objectClass,
        JvmTypeReference... typeParameters
    ) {
        return module.get(JvmTypeReferenceBuilder.class).typeRef(
            objectClass,
            typeParameters
        );
    }


    public JvmTypeReference typeRef(
        Class<?> objectClass,
        List<JvmTypeReference> typeParameters
    ) {
        return module.get(JvmTypeReferenceBuilder.class).typeRef(
            objectClass,
            typeParameters.toArray(new JvmTypeReference[0])
        );
    }


    public JvmTypeReference typeRef(
        JvmType componentType,
        JvmTypeReference... typeArgs
    ) {
        return module.get(JvmTypeReferenceBuilder.class).typeRef(
            componentType,
            typeArgs
        );
    }


    public JvmTypeReference typeRef(
        JvmType componentType,
        List<JvmTypeReference> typeArgs
    ) {
        return module.get(JvmTypeReferenceBuilder.class).typeRef(
            componentType,
            typeArgs.toArray(new JvmTypeReference[0])
        );
    }


    public JvmTypeReference typeRef(
        String ident,
        JvmTypeReference... typeArgs
    ) {
        return module.get(JvmTypeReferenceBuilder.class).typeRef(
            ident,
            typeArgs
        );
    }


    public JvmTypeReference attemptResolveTypeRef(
        JvmTypeReference typeReference
    ) {
        final JvmTypeQualifiedNameParser.GenericType type =
            JvmTypeQualifiedNameParser
                .parseJvmGenerics(typeReference.getIdentifier());
        if (type == null) {
            return typeReference;
        }
        final JvmTypeReferenceBuilder jvmtrb = module.get(
            JvmTypeReferenceBuilder.class);
        return type.convertToTypeRef(
            jvmtrb::typeRef,
            jvmtrb::typeRef
        );
    }

}
