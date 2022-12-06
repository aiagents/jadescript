package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class PatternMatchOutput<
        P extends PatternMatchSemanticsProcess,
        U extends PatternMatchOutput.Unification,
        N extends PatternMatchOutput.TypeNarrowing> {

    private final P processInfo;
    private final U unificationInfo;
    private final N typeNarrowingInfo;

    PatternMatchOutput(
            P processInfo,
            U unificationInfo,
            N typeNarrowingInfo
    ) {
        this.processInfo = processInfo;
        this.unificationInfo = unificationInfo;
        this.typeNarrowingInfo = typeNarrowingInfo;
    }



    public PatternMatchOutput<P, U, N> mapIfUnifies(Function<DoesUnification, U> func) {
        if (this.getUnificationInfo() instanceof DoesUnification) {
            return new PatternMatchOutput<>(
                    getProcessInfo(),
                    func.apply(((DoesUnification) this.getUnificationInfo())),
                    getTypeNarrowingInfo()
            );
        } else {
            return this;
        }
    }

    public interface Unification {
    }

    public interface TypeNarrowing {
    }

    public P getProcessInfo() {
        return processInfo;
    }

    public U getUnificationInfo() {
        return unificationInfo;
    }

    public N getTypeNarrowingInfo() {
        return typeNarrowingInfo;
    }


    public enum NoUnification implements Unification {
        INSTANCE
    }

    public static <T extends PatternMatchSemanticsProcess>
    DoesUnification collectUnificationResults(List<PatternMatchOutput<? extends T, ?, ?>> subUnifications) {
        List<NamedSymbol> result = new ArrayList<>();
        for (PatternMatchOutput<?, ?, ?> subUnification : subUnifications) {
            final Unification unificationInfo = subUnification.getUnificationInfo();
            if (unificationInfo instanceof DoesUnification) {
                result.addAll(((DoesUnification) unificationInfo).unifiedVariables);
            }
        }
        return new DoesUnification(result);
    }

    public static class DoesUnification implements Unification {

        private final List<NamedSymbol> unifiedVariables;

        public DoesUnification(List<NamedSymbol> unifiedVariables) {
            this.unifiedVariables = unifiedVariables;
        }

        public List<NamedSymbol> getUnifiedVariables() {
            return unifiedVariables;
        }
    }

    public enum NoNarrowing implements TypeNarrowing {
        INSTANCE
    }


    public static class WithTypeNarrowing implements TypeNarrowing {
        private final IJadescriptType narrowedType;

        public WithTypeNarrowing(IJadescriptType narrowedType) {
            this.narrowedType = narrowedType;
        }

        public IJadescriptType getNarrowedType() {
            return narrowedType;
        }
    }


    public static final DoesUnification EMPTY_UNIFICATION = new DoesUnification(List.of());

}
