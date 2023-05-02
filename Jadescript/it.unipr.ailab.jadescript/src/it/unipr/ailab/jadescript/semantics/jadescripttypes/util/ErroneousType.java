package it.unipr.ailab.jadescript.semantics.jadescripttypes.util;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

public abstract class ErroneousType extends UtilityType {

    private final String errorMessage;


    public ErroneousType(
        SemanticsModule module,
        String typeID,
        String simpleName,
        JvmTypeReference jvmType,
        String errorMessage
    ) {
        super(module, typeID, simpleName, jvmType);
        this.errorMessage = errorMessage;
    }


    @Override
    public boolean validateType(
        Maybe<? extends EObject> input,
        ValidationMessageAcceptor acceptor
    ) {
        if (this.errorMessage.isBlank()) {
            super.validateType(input, acceptor);
        }

        return module.get(ValidationHelper.class).emitError(
            "InvalidType",
            this.errorMessage,
            input,
            acceptor
        );
    }


    @Override
    public boolean isSendable() {
        return false;
    }


    @Override
    public boolean isErroneous() {
        return true;
    }


}
