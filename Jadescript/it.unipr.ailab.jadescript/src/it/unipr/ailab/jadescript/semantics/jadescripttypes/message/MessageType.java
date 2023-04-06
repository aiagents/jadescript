package it.unipr.ailab.jadescript.semantics.jadescripttypes.message;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;
import it.unipr.ailab.jadescript.semantics.namespace.MessageTypeNamespace;

public interface MessageType
    extends IJadescriptType, EmptyCreatable {

    final TypeCategory CATEGORY = new TypeCategoryAdapter() {
        @Override
        public boolean isMessage() {
            return true;
        }
    };

    IJadescriptType getContentType();

    @Override
    MessageTypeNamespace namespace();

    @Override
    default TypeCategory category(){
        return CATEGORY;
    }

}
