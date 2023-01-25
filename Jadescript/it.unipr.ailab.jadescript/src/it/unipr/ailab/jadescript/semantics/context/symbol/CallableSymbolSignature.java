package it.unipr.ailab.jadescript.semantics.context.symbol;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CallableSymbolSignature extends SymbolSignature {

    protected final String name;
    protected final IJadescriptType returnType;
    protected final List<IJadescriptType> parameterTypes;


    public CallableSymbolSignature(
        String name,
        IJadescriptType returnType,
        List<IJadescriptType> parameterTypes
    ) {
        this.name = name;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
    }


    public String getName() {
        return name;
    }


    public IJadescriptType getReturnType() {
        return returnType;
    }


    public List<IJadescriptType> getParameterTypes() {
        return parameterTypes;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CallableSymbolSignature that = (CallableSymbolSignature) o;

        if (getName() != null
            ? !getName().equals(that.getName())
            : that.getName() != null) {
            return false;
        }

        if (getReturnType() != null
            ? !getReturnType().typeEquals(that.getReturnType())
            : that.getReturnType() != null) {
            return false;
        }

        return getParameterTypes() != null
            ? (Streams.zip(
            getParameterTypes().stream(),
            that.getParameterTypes().stream(),
            IJadescriptType::typeEquals
        ).allMatch(it -> it))
            : that.getParameterTypes() == null;
    }


    @Override
    public String toString() {
        final String parametersPrint = getParameterTypes().stream()
            .map(Object::toString)
            .collect(Collectors.joining(", "));
        if (getReturnType().getID().equals(TypeHelper.VOID_TYPEID)) {
            return "procedure " + getName() + " with " + parametersPrint;
        } else {
            return "function " + getName() + "(" + parametersPrint +
                ") as " + getReturnType();
        }
    }


    @Override
    public int hashCode() {
        int result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + (getReturnType() != null ?
            getReturnType().getID().hashCode() : 0);
        result = 31 * result + (getParameterTypes() != null ?
            ((Supplier<Integer>) (() -> {
                int paramListHash = 1;
                for (IJadescriptType e : getParameterTypes()) {
                    paramListHash = 31 * paramListHash + (e == null ? 0 :
                        e.getID().hashCode());
                }
                return paramListHash;
            })).get() : 0);
        return result;
    }

}
