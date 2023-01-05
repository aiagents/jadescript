package it.unipr.ailab.jadescript.semantics.proxyeobjects;

import it.unipr.ailab.jadescript.jadescript.NamedArgumentList;
import it.unipr.ailab.jadescript.jadescript.SimpleArgumentList;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public class MethodCall extends ProxyEObject {
    private final String name;
    private final Maybe<SimpleArgumentList> simpleArgs;
    private final Maybe<NamedArgumentList> namedArgs;
    private final boolean isProcedure;

    private MethodCall(
            EObject input,
            String name,
            Maybe<SimpleArgumentList> simpleArgs,
            Maybe<NamedArgumentList> namedArgs,
            boolean isProcedure
    ) {
        super(input instanceof ProxyEObject ? ((ProxyEObject) input).getProxyEObject() : input);
        this.name = name;
        this.simpleArgs = simpleArgs;
        this.namedArgs = namedArgs;
        this.isProcedure = isProcedure;
    }

    public static Maybe<MethodCall> methodCall(
            Maybe<? extends EObject> input,
            Maybe<? extends String> name,
            Maybe<SimpleArgumentList> simpleArgs,
            Maybe<NamedArgumentList> namedArgs,
            boolean isProcedure
    ) {
        if (input.isPresent() && name.isPresent()) {
            return some(new MethodCall(input.toNullable(), name.toNullable(), simpleArgs, namedArgs, isProcedure));
        } else {
            return nothing();
        }
    }

    public static Maybe<MethodCall> methodCall(
            Maybe<SingleIdentifier> fromIdentifier
    ){
        return methodCall(
                fromIdentifier.__(ProxyEObject::getProxyEObject),
                fromIdentifier.__(SingleIdentifier::getIdent),
                nothing(),
                nothing(),
                false
        );
    }

    public static Maybe<MethodCall> methodCall(
            EObject input,
            Maybe<? extends String> name,
            Maybe<SimpleArgumentList> simpleArgs,
            Maybe<NamedArgumentList> namedArgs,
            boolean isProcedure
    ) {
        if (name.isPresent()) {
            return some(new MethodCall(input, name.toNullable(), simpleArgs, namedArgs, isProcedure));
        } else {
            return nothing();
        }
    }

    public static Maybe<MethodCall> methodCall(
            Maybe<? extends EObject> input,
            String name,
            Maybe<SimpleArgumentList> simpleArgs,
            Maybe<NamedArgumentList> namedArgs,
            boolean isProcedure
    ) {
        if (input.isPresent()) {
            return some(new MethodCall(input.toNullable(), name, simpleArgs, namedArgs, isProcedure));
        } else {
            return nothing();
        }
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
