package it.unipr.ailab.jadescript.semantics;

import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

public class DroppingAcceptor implements ValidationMessageAcceptor, BlockElementAcceptor {
    @Override
    public void acceptError(
            String message,
            EObject object,
            EStructuralFeature feature,
            int index,
            String code,
            String... issueData
    ) {
        // Do nothing.
    }

    @Override
    public void acceptError(
            String message,
            EObject object,
            int offset,
            int length,
            String code,
            String... issueData
    ) {
        // Do nothing.
    }

    @Override
    public void acceptWarning(
            String message,
            EObject object,
            EStructuralFeature feature,
            int index,
            String code,
            String... issueData
    ) {
        // Do nothing.
    }

    @Override
    public void acceptWarning(
            String message,
            EObject object,
            int offset,
            int length,
            String code,
            String... issueData
    ) {
        // Do nothing.
    }

    @Override
    public void acceptInfo(
            String message,
            EObject object,
            EStructuralFeature feature,
            int index,
            String code,
            String... issueData
    ) {
        // Do nothing.
    }

    @Override
    public void acceptInfo(
            String message,
            EObject object,
            int offset,
            int length,
            String code,
            String... issueData
    ) {
        // Do nothing.
    }

    @Override
    public void accept(BlockWriterElement element) {
        // Do nothing.
    }
}
