package it.unipr.ailab.jadescript.semantics.proxyeobjects;

import it.unipr.ailab.jadescript.jadescript.NamedArgumentList;
import it.unipr.ailab.jadescript.jadescript.SimpleArgumentList;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public class Call extends ProxyEObject {

    public static final boolean IS_FUNCTION = false;
    public static final boolean IS_PROCEDURE = true;

    private final String name;
    private final Maybe<SimpleArgumentList> simpleArgs;
    private final Maybe<NamedArgumentList> namedArgs;
    private final boolean isProcedure;


    private Call(
        EObject input,
        String name,
        Maybe<SimpleArgumentList> simpleArgs,
        Maybe<NamedArgumentList> namedArgs,
        boolean isProcedure
    ) {
        super(
            input instanceof ProxyEObject ?
                ((ProxyEObject) input).getProxyEObject()
                : input
        );
        this.name = name;
        this.simpleArgs = simpleArgs;
        this.namedArgs = namedArgs;
        this.isProcedure = isProcedure;
    }


    public static Maybe<Call> call(
        Maybe<? extends EObject> input,
        Maybe<? extends String> name,
        Maybe<SimpleArgumentList> simpleArgs,
        Maybe<NamedArgumentList> namedArgs,
        boolean isProcedure
    ) {
        if (input.isPresent() && name.isPresent()) {
            return some(new Call(
                input.toNullable(),
                name.toNullable(),
                simpleArgs,
                namedArgs,
                isProcedure
            ));
        } else {
            return nothing();
        }
    }


    public static Maybe<Call> call(
        Maybe<SingleIdentifier> fromIdentifier
    ) {
        return call(
            fromIdentifier.__(ProxyEObject::getProxyEObject),
            fromIdentifier.__(SingleIdentifier::getIdent),
            nothing(),
            nothing(),
            Call.IS_FUNCTION
        );
    }


    public String getName() {
        return name;
    }


    public Maybe<SimpleArgumentList> getSimpleArgs() {
        return simpleArgs;
    }


    public Maybe<NamedArgumentList> getNamedArgs() {
        return namedArgs;
    }


    public boolean isProcedure() {
        return isProcedure;
    }

}
