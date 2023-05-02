package it.unipr.ailab.jadescript.semantics.proxyeobjects;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

public class ProxyEObject implements EObject {

    @SuppressWarnings("unchecked")
    public static <T extends EObject> T reflectiveProxyEObject(
        T input1,
        EObject input2,
        Class<?> targetInterface
    ) {
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(
            ProxyEObject.class.getClassLoader(),
            new Class[]{EObject.class, targetInterface},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "eClass":
                        return input1.eClass();
                    case "eResource":
                        return input2.eResource();
                    case "eContainer":
                        return input2.eContainer();
                    case "eContainingFeature":
                        return input2.eContainingFeature();
                    case "eContainmentFeature":
                        return input1.eContainmentFeature();
                    case "eContents":
                        return input1.eContents();
                    case "eAllContents":
                        return input1.eAllContents();
                    case "eIsProxy":
                        return input2.eIsProxy();
                    case "eCrossReferences":
                        return input2.eCrossReferences();
                    case "eGet":
                        if (args.length == 1) {
                            return input2.eGet(
                                (EStructuralFeature) args[0]
                            );
                        } else {
                            return input2.eGet(
                                (EStructuralFeature) args[0],
                                (Boolean) args[1]
                            );
                        }
                    case "eSet":
                        input2.eSet(
                            ((EStructuralFeature) args[0]),
                            args[1]
                        );
                        return null;
                    case "eIsSet":
                        return input2.eIsSet((EStructuralFeature) args[0]);
                    case "eUnset":
                        input2.eUnset(((EStructuralFeature) args[0]));
                        return null;
                    case "eInvoke":
                        return input2.eInvoke(
                            (EOperation) args[0],
                            (EList<?>) args[1]
                        );
                    case "eAdapters":
                        return input2.eAdapters();
                    case "eDeliver":
                        return input2.eDeliver();
                    case "eSetDeliver":
                        input2.eSetDeliver((Boolean) args[0]);
                        return null;
                    case "eNotify":
                        input2.eNotify((Notification) args[0]);
                        return null;
                    default:
                        return input1.getClass().getMethod(
                            method.getName(),
                            method.getParameterTypes()
                        ).invoke(input1, args);
                }
            }
        );
    }


    protected final EObject input;


    public ProxyEObject(EObject input) {
        this.input = input;
    }


    @Override
    public EClass eClass() {
        return input.eClass();
    }


    @Override
    public Resource eResource() {
        return input.eResource();
    }


    @Override
    public EObject eContainer() {
        return input.eContainer();
    }


    @Override
    public EStructuralFeature eContainingFeature() {
        return input.eContainingFeature();
    }


    @Override
    public EReference eContainmentFeature() {
        return input.eContainmentFeature();
    }


    @Override
    public EList<EObject> eContents() {
        return input.eContents();
    }


    @Override
    public TreeIterator<EObject> eAllContents() {
        return input.eAllContents();
    }


    @Override
    public boolean eIsProxy() {
        return input.eIsProxy();
    }


    @Override
    public EList<EObject> eCrossReferences() {
        return input.eCrossReferences();
    }


    @Override
    public Object eGet(EStructuralFeature eStructuralFeature) {
        return input.eGet(eStructuralFeature);
    }


    @Override
    public Object eGet(EStructuralFeature eStructuralFeature, boolean b) {
        return input.eGet(eStructuralFeature, b);
    }


    @Override
    public void eSet(EStructuralFeature eStructuralFeature, Object o) {
        input.eSet(eStructuralFeature, o);
    }


    @Override
    public boolean eIsSet(EStructuralFeature eStructuralFeature) {
        return input.eIsSet(eStructuralFeature);
    }


    @Override
    public void eUnset(EStructuralFeature eStructuralFeature) {
        input.eUnset(eStructuralFeature);
    }


    @Override
    public Object eInvoke(
        EOperation eOperation,
        EList<?> eList
    ) throws InvocationTargetException {
        return input.eInvoke(eOperation, eList);
    }


    @Override
    public EList<Adapter> eAdapters() {
        return input.eAdapters();
    }


    @Override
    public boolean eDeliver() {
        return input.eDeliver();
    }


    @Override
    public void eSetDeliver(boolean b) {
        input.eSetDeliver(b);
    }


    @Override
    public void eNotify(Notification notification) {
        input.eNotify(notification);
    }


    public EObject getProxyEObject() {
        return input;
    }

}
