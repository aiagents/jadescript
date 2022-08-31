package it.unipr.ailab.jadescript.semantics.context.clashing;

import it.unipr.ailab.jadescript.semantics.context.symbol.Symbol;

import java.util.List;

public class DefinitionClash {

    private final Symbol toBeAddedSymbol;
    private final Symbol alreadyPresentSymbol;


    public DefinitionClash(Symbol toBeAdded, Symbol alreadyPresent) {
        this.toBeAddedSymbol = toBeAdded;
        this.alreadyPresentSymbol = alreadyPresent;
    }

    public Symbol getToBeAddedSymbol() {
        return toBeAddedSymbol;
    }

    public Symbol getAlreadyPresentSymbol() {
        return alreadyPresentSymbol;
    }

    @Override
    public String toString() {
        final Symbol toBeAddedSymbol = this.getToBeAddedSymbol();
        final Symbol alreadyPresentSymbol = getAlreadyPresentSymbol();
        return toBeAddedSymbol.getSignature() + " in " + toBeAddedSymbol.sourceLocation() +
                " clashes with " +
                alreadyPresentSymbol.getSignature() + " in " + alreadyPresentSymbol.sourceLocation();
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
