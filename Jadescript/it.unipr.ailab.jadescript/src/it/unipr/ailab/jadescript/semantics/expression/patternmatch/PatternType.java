package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.function.Function;

public interface PatternType {
    IJadescriptType solve(IJadescriptType providedInputType);

    public static class SimplePatternType implements PatternType {
        private final IJadescriptType type;

        public SimplePatternType(IJadescriptType type) {
            this.type = type;
        }

        public IJadescriptType getType() {
            return type;
        }

        @Override
        public IJadescriptType solve(IJadescriptType ignored) {
            return type;
        }
    }

    public static class HoledPatternType implements PatternType {
        private final Function<? super IJadescriptType, ? extends IJadescriptType> solvingFunction;

        public HoledPatternType(Function<? super IJadescriptType, ? extends IJadescriptType> solvingFunction) {
            this.solvingFunction = solvingFunction;
        }

        @Override
        public IJadescriptType solve(IJadescriptType providedInputType) {
            return solvingFunction.apply(providedInputType);
        }
    }

    public static HoledPatternType holed(Function<? super IJadescriptType, ? extends IJadescriptType> solvingFunction) {
        return new HoledPatternType(solvingFunction);
    }

    public static SimplePatternType simple(IJadescriptType type) {
        return new SimplePatternType(type);
    }

    public static SimplePatternType empty(SemanticsModule module) {
        return new SimplePatternType(module.get(TypeHelper.class).NOTHING);
    }
}
