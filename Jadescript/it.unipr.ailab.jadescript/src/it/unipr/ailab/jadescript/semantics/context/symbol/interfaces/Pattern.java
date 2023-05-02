package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface Pattern extends Located {

    String name();

    //For now, all functional-notation-like patterns are NOT typely-holed,
    // meaning that the input type and the term types can be represented by
    // solved types, and not by potentially holed PatternTypes
    IJadescriptType inputType();

    Map<String, IJadescriptType> termTypesByName();

    List<String> termNames();

    List<IJadescriptType> termTypes();

    @SuppressWarnings("SameReturnValue")
    boolean isWithoutSideEffects();

    default int termCount() {
        return Math.min(termNames().size(), termTypesByName().size());
    }

    @Override
    default BaseSignature getSignature() {
        return new Signature(
            name(),
            inputType(),
            termTypes()
        );
    }

    public static class Signature implements BaseSignature {

        protected final String name;
        protected final IJadescriptType inputType;
        protected final List<IJadescriptType> termTypes;


        public Signature(
            String name,
            IJadescriptType inputType,
            List<IJadescriptType> termTypes
        ) {
            this.name = name;
            this.inputType = inputType;
            this.termTypes = termTypes;
        }


        public String getName() {
            return name;
        }


        public IJadescriptType getInputType() {
            return inputType;
        }


        public List<IJadescriptType> getTermTypes() {
            return termTypes;
        }


        @Override
        public String toString() {
            final String parametersPrint = getTermTypes().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));

            return "pattern " + getName() + "(" + parametersPrint + ") " +
                "taking " + getInputType();

        }


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Signature)) {
                return false;
            }

            Signature that = (Signature) o;

            if (getName() != null ? !getName().equals(that.getName()) :
                that.getName() != null) {
                return false;
            }
            if (getInputType() != null ?
                !getInputType().equals(that.getInputType()) :
                that.getInputType() != null) {
                return false;
            }
            return getTermTypes() != null ?
                getTermTypes().equals(that.getTermTypes())
                : that.getTermTypes() == null;
        }


        @Override
        public int hashCode() {
            int result = getName() != null ? getName().hashCode() : 0;
            result = 31 * result +
                (getInputType() != null ? getInputType().hashCode() : 0);
            result = 31 * result +
                (getTermTypes() != null ? getTermTypes().hashCode() : 0);
            return result;
        }

    }

}
