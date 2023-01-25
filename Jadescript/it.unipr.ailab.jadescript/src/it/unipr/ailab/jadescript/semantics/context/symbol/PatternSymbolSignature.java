package it.unipr.ailab.jadescript.semantics.context.symbol;


import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.List;
import java.util.stream.Collectors;

public class PatternSymbolSignature extends SymbolSignature{

    protected final String name;
    protected final IJadescriptType inputType;
    protected final List<IJadescriptType> termTypes;


    public PatternSymbolSignature(
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
        if (this == o) return true;
        if (!(o instanceof PatternSymbolSignature)) return false;

        PatternSymbolSignature that = (PatternSymbolSignature) o;

        if (getName() != null ? !getName().equals(that.getName()) :
            that.getName() != null)
            return false;
        if (getInputType() != null ?
            !getInputType().equals(that.getInputType()) :
            that.getInputType() != null)
            return false;
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
