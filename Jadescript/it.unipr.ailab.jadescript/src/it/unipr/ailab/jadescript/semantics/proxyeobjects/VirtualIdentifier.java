package it.unipr.ailab.jadescript.semantics.proxyeobjects;

import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.of;

public class VirtualIdentifier extends ProxyEObject {
    private final String ident;

    public VirtualIdentifier(String ident, EObject eObject) {
        super(eObject);
        this.ident = ident;
    }

    public static Maybe<VirtualIdentifier> virtualIdentifier(Maybe<String> ident, Maybe<? extends EObject> eObject){
        if(ident.isPresent() && eObject.isPresent()){
            return of(new VirtualIdentifier(ident.toNullable(), eObject.toNullable()));
        }else{
            return nothing();
        }
    }

    public static Maybe<VirtualIdentifier> virtualIdentifier(String ident, Maybe<? extends EObject> eObject){
        if(eObject.isPresent()){
            return of(new VirtualIdentifier(ident, eObject.toNullable()));
        }else{
            return nothing();
        }
    }

    public static Maybe<VirtualIdentifier> virtualIdentifier(Maybe<String> ident, EObject eObject) {
        if(ident.isPresent() ){
            return of(new VirtualIdentifier(ident.toNullable(), eObject));
        }else{
            return nothing();
        }
    }

    public String getIdent() {
        return ident;
    }




}
