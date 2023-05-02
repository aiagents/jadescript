package it.unipr.ailab.jadescript.semantics.proxyeobjects;

import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public class SingleIdentifier extends ProxyEObject {
    private final String ident;

    public SingleIdentifier(String ident, EObject eObject) {
        super(eObject);
        this.ident = ident;
    }

    public static Maybe<SingleIdentifier> singleIdentifier(
        Maybe<String> ident,
        Maybe<? extends EObject> eObject
    ){
        if(ident.isPresent() && eObject.isPresent()){
            return some(new SingleIdentifier(
                ident.toNullable(),
                eObject.toNullable()
            ));
        }else{
            return nothing();
        }
    }

    public static Maybe<SingleIdentifier> singleIdentifier(
        String ident,
        Maybe<? extends EObject> eObject
    ){
        if(eObject.isPresent()){
            return some(new SingleIdentifier(ident, eObject.toNullable()));
        }else{
            return nothing();
        }
    }

    public static Maybe<SingleIdentifier> singleIdentifier(
        Maybe<String> ident,
        EObject eObject
    ) {
        if(ident.isPresent() ){
            return some(new SingleIdentifier(ident.toNullable(), eObject));
        }else{
            return nothing();
        }
    }

    public String getIdent() {
        return ident;
    }




}
