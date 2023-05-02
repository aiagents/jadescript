package it.unipr.ailab.jadescript.semantics.utils;

import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created on 21/02/2019.
 */
public class JvmTypeReferenceSet implements Iterable<String> {

    private final Set<String> genericsIDs = new HashSet<>();
    private final Set<String> rawIDs = new HashSet<>();


    public static JvmTypeReferenceSet generateAllSupertypesSet(
        JvmDeclaredType input
    ) {
        JvmTypeReferenceSet result = new JvmTypeReferenceSet();
        for (JvmTypeReference directSuperType : input.getSuperTypes()) {
            result.add(directSuperType);
            if (directSuperType.getType() instanceof JvmDeclaredType) {
                result.addAll(generateAllSupertypesSet(
                    (JvmDeclaredType) directSuperType.getType()
                ));
            }
        }
        return result;
    }


    public static String rawID(JvmTypeReference typeReference) {
        return typeReference.getQualifiedName();
    }


    public static String genericsID(JvmTypeReference typeReference) {
        String result = "";
        if (typeReference instanceof JvmParameterizedTypeReference
            && typeReference.getType() instanceof JvmDeclaredType) {
            JvmDeclaredType type = (JvmDeclaredType) typeReference.getType();
            result += (
                type.getPackageName() != null
                    ? (type.getPackageName() + ".")
                    : ""
            ) + type.getSimpleName();
        } else {
            result = typeReference.getQualifiedName();
        }
        return result;
    }


    private void addAll(JvmTypeReferenceSet other) {
        this.genericsIDs.addAll(other.genericsIDs);
        this.rawIDs.addAll(other.rawIDs);
    }


    public boolean contains(JvmTypeReference ref) {
        return genericsIDs.contains(genericsID(ref));
    }


    public boolean containsRaw(JvmTypeReference ref) {
        return rawIDs.contains(rawID(ref));
    }


    public void add(JvmTypeReference jvmTypeReference) {
        this.genericsIDs.add(genericsID(jvmTypeReference));
        this.rawIDs.add(rawID(jvmTypeReference));
    }


    @Override
    public Iterator<String> iterator() {
        return genericsIDs.iterator();
    }

}
