package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;

import java.util.List;

public class PatternMatchOutput<
        C extends PatternMatchOutput.Compile,
        U extends PatternMatchOutput.Unification,
        N extends PatternMatchOutput.TypeNarrowing> {

    public interface Compile {
    }

    public interface Unification {
    }

    public interface TypeNarrowing {
    }

    public enum IsValidation implements Compile {
        INSTANCE
    }

    public static class IsCompilation implements Compile {
        private final Maybe<String> matchExpressionOutput;
        private final List<PatternMatcher> patternMatchers;

        public IsCompilation(Maybe<String> matchExpressionOutput, List<PatternMatcher> patternMatchers) {
            this.matchExpressionOutput = matchExpressionOutput;
            this.patternMatchers = patternMatchers;
        }

        public Maybe<String> getMatchExpressionOutput() {
            return matchExpressionOutput;
        }

        public List<PatternMatcher> getPatternMatchers() {
            return patternMatchers;
        }
    }

    public enum NoUnification implements Unification {
        INSTANCE
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


}
