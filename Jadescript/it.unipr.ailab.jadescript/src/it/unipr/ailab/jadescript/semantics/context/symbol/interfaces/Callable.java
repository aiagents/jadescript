package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface Callable extends Located {

    String name();

    IJadescriptType returnType();

    Map<String, IJadescriptType> parameterTypesByName();

    List<String> parameterNames();

    List<IJadescriptType> parameterTypes();

    boolean isWithoutSideEffects();

    default int arity() {
        return Math.min(parameterNames().size(), parameterTypesByName().size());
    }

    default Signature getSignature(){
        return new Signature(
            name(),
            returnType(),
            parameterTypes()
        );
    }

    default void debugDumpCallable(SourceCodeBuilder scb) {
        scb.open("Callable(concrete class=" + this.getClass().getName() +
            ") {");
        scb.line("sourceLocation = " + sourceLocation());
        scb.line("name =" + name());
        scb.line("returnType = " + returnType().getDebugPrint());

        scb.open("parameters (arity=" + arity() + ") = [");
        parameterNames().stream()
            .map(name -> name + ": " +
                parameterTypesByName().get(name).getDebugPrint())
            .forEach(scb::line);
        scb.close("]");
        scb.close("}");
    }

    public static class Signature implements BaseSignature{

        protected final String name;
        protected final IJadescriptType returnType;
        protected final List<IJadescriptType> parameterTypes;


        public Signature(
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

            Signature that = (Signature) o;

            if (getName() != null
                ? !getName().equals(that.getName())
                : that.getName() != null) {
                return false;
            }

            if (getReturnType() != null) {
                if(!TypeComparator.rawEquals(
                    this.getReturnType(),
                    that.getReturnType()
                )) {
                    return false;
                }
            } else {
                if (that.getReturnType() != null) {
                    return false;
                }
            }

            return getParameterTypes() != null
                ? (Streams.zip(
                getParameterTypes().stream(),
                that.getParameterTypes().stream(),
                TypeComparator::rawEquals
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


}
