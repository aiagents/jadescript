package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public class NamedSymbolSignature extends SymbolSignature {

    private final String name;
    private final IJadescriptType type;
    private final boolean canWrite;


    public NamedSymbolSignature(
        String name,
        IJadescriptType type,
        boolean canWrite
    ) {
        this.name = name;
        this.type = type;
        this.canWrite = canWrite;
    }


    public String getName() {
        return name;
    }


    public IJadescriptType getType() {
        return type;
    }


    public boolean canWrite() {
        return canWrite;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NamedSymbolSignature that = (NamedSymbolSignature) o;

        if (canWrite != that.canWrite) return false;

        if (getName() != null
            ? !getName().equals(that.getName())
            : that.getName() != null) return false;

        return getType() != null
            ? getType().typeEquals(that.getType())
            : that.getType() == null;
    }


    @Override
    public String toString() {
        return getName() + ": " + getType() + (
            canWrite()
                ? ""
                : " (read-only)"
        );
    }


    @Override
    public int hashCode() {
        int result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + (getType() != null ?
            getType().getID().hashCode() : 0);
        result = 31 * result + (canWrite ? 1 : 0);
        return result;
    }


}
