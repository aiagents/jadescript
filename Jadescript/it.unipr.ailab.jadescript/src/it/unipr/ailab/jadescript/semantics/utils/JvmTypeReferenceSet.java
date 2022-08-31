package it.unipr.ailab.jadescript.semantics.utils;

import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmTypeReference;


import java.util.HashMap;
import java.util.Iterator;

/**
 * Created on 21/02/2019.
 *
 */
public class JvmTypeReferenceSet implements Iterable<String> {

    private final HashMap<String, JvmTypeReference> privateMap = new HashMap<>();

    public static JvmTypeReferenceSet generateAllSupertypesSet(JvmDeclaredType input) {
        JvmTypeReferenceSet result = new JvmTypeReferenceSet();
        for (JvmTypeReference directSuperType : input.getSuperTypes()) {
            result.add(directSuperType);
            if (directSuperType.getType() instanceof JvmDeclaredType) {
                result.addAll(generateAllSupertypesSet((JvmDeclaredType) directSuperType.getType()));
            }
        }
        return result;
    }

    private void addAll(JvmTypeReferenceSet jvmTypeReferenceSet) {
        this.privateMap.putAll(jvmTypeReferenceSet.privateMap);
    }

    public boolean contains(JvmTypeReference jvmTypeReference) {
        return privateMap.containsKey(uniqueId(jvmTypeReference));
    }

    public void add(JvmTypeReference jvmTypeReference) {
        privateMap.put(uniqueId(jvmTypeReference), jvmTypeReference);
    }


    @Override
    public Iterator<String> iterator() {
        return privateMap.keySet().iterator();
    }

    public static String uniqueId(JvmTypeReference typeReference) {
        String result = "";
        if (typeReference instanceof JvmParameterizedTypeReference && typeReference.getType()
                instanceof JvmDeclaredType) {
            JvmDeclaredType type = (JvmDeclaredType) typeReference.getType();
            result += (type.getPackageName()!=null?(type.getPackageName() + "."):"") + type.getSimpleName();
        } else {
            result = typeReference.getQualifiedName();
        }
        return result;
    }

}
