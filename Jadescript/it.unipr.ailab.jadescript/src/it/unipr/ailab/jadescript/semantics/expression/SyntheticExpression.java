package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts.VALID;

/**
 * Created on 01/11/2018.
 *
 */
public class SyntheticExpression implements RValueExpression {

    public enum SyntheticType{
        CUSTOM
    }

    public static class SemanticsMethods{
        public List<? extends ExpressionSemantics<?>> getSubExpressions() {
            // Override if needed
            return Collections.emptyList();
        }

        @SuppressWarnings("SameReturnValue")
        public String compile() {
            // Override if needed
            return "";
        }

        public IJadescriptType inferType(TypeHelper typeHelper) {
            // Override if needed
            return typeHelper.ANY;
        }

        @SuppressWarnings("SameReturnValue")
        public boolean mustTraverse() {
            // Override if needed
            return false;
        }

        public Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traverse() {
            // Override if needed
            return Optional.empty();
        }

        @SuppressWarnings({"EmptyMethod", "SameReturnValue"})
        public boolean validate(ValidationMessageAcceptor acceptor) {
            // Override if needed
            return VALID;
        }

        @SuppressWarnings("SameReturnValue")
        public boolean isPatternEvaluationPure() {
            // Override if needed
            return true;
        }
    }

    private final SyntheticType syntheticType;
    private SemanticsMethods customSemantics = null;

    public SyntheticExpression(SyntheticType syntheticType) {
        this.syntheticType = syntheticType;
    }

    public SyntheticExpression(SemanticsMethods custom) {
        this.syntheticType = SyntheticType.CUSTOM;
        this.customSemantics = custom;
    }

    public SemanticsMethods getSemanticsMethods(){
        return customSemantics;
    }

    public SyntheticType getSyntheticType() {
        return syntheticType;
    }

    @Override
    public EClass eClass() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource eResource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EObject eContainer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EStructuralFeature eContainingFeature() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EReference eContainmentFeature() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EList<EObject> eContents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TreeIterator<EObject> eAllContents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean eIsProxy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EList<EObject> eCrossReferences() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object eGet(EStructuralFeature eStructuralFeature) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object eGet(EStructuralFeature eStructuralFeature, boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void eSet(EStructuralFeature eStructuralFeature, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean eIsSet(EStructuralFeature eStructuralFeature) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void eUnset(EStructuralFeature eStructuralFeature) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object eInvoke(EOperation eOperation, EList<?> eList) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EList<Adapter> eAdapters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean eDeliver() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void eSetDeliver(boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void eNotify(Notification notification) {
        throw new UnsupportedOperationException();
    }
}
