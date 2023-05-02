package it.unipr.ailab.jadescript.semantics;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

/**
 * Created on 30/04/18.
 *
 */
public class InterceptAcceptor implements ValidationMessageAcceptor{
    private final ValidationMessageAcceptor input;
    private boolean thereAreErrors = false;

    public InterceptAcceptor(ValidationMessageAcceptor input){
        this.input = input;
    }

    @Override
    public void acceptError(
        String s,
        EObject eObject,
        EStructuralFeature eStructuralFeature,
        int i,
        String s1,
        String... strings
    ) {
        thereAreErrors = true;
        input.acceptError(s, eObject, eStructuralFeature, i, s1, strings);
    }

    @Override
    public void acceptError(
        String s,
        EObject eObject,
        int i,
        int i1,
        String s1,
        String... strings
    ) {
        thereAreErrors = true;
        input.acceptError(s, eObject, i, i1, s1, strings);
    }

    @Override
    public void acceptWarning(
        String s,
        EObject eObject,
        EStructuralFeature eStructuralFeature,
        int i,
        String s1,
        String... strings
    ) {
        input.acceptWarning(s, eObject, eStructuralFeature, i, s1, strings);
    }

    @Override
    public void acceptWarning(
        String s,
        EObject eObject,
        int i,
        int i1,
        String s1,
        String... strings
    ) {
        input.acceptWarning(s, eObject, i, i1, s1, strings);
    }

    @Override
    public void acceptInfo(
        String s,
        EObject eObject,
        EStructuralFeature eStructuralFeature,
        int i,
        String s1,
        String... strings
    ) {
        input.acceptInfo(s, eObject, eStructuralFeature, i, s1, strings);
    }

    @Override
    public void acceptInfo(
        String s,
        EObject eObject,
        int i,
        int i1,
        String s1,
        String... strings
    ) {
        input.acceptInfo(s, eObject, i, i1, s1, strings);
    }

    public boolean thereAreErrors(){
        return thereAreErrors;
    }
}
