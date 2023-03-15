package it.unipr.ailab.jadescript.semantics.context.clashing;


import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.Located;

import java.util.List;

public class DefinitionClash {

    private final Located toBeAdded;
    private final Located alreadyPresent;


    public DefinitionClash(Located toBeAdded, Located alreadyPresent) {
        this.toBeAdded = toBeAdded;
        this.alreadyPresent = alreadyPresent;
    }

    public Located getToBeAddedSymbol() {
        return toBeAdded;
    }

    public Located getAlreadyPresentSymbol() {
        return alreadyPresent;
    }

    @Override
    public String toString() {
        final Located toBeAdded = this.getToBeAddedSymbol();
        final Located alreadyPresent = getAlreadyPresentSymbol();
        return toBeAdded.getSignature() + " in "
            + toBeAdded.sourceLocation() +
                " clashes with " +
                alreadyPresent.getSignature() +
            " in " + alreadyPresent.sourceLocation();
    }

    public static String clashListToString(List<DefinitionClash> clashes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clashes.size(); i++) {
            DefinitionClash clash = clashes.get(i);
            sb.append(" • ");
            sb.append(clash.toString());
            if (i != clashes.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
