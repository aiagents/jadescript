package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;

import java.util.function.Function;

public interface PatternType {

    static HoledPatternType holed(
        Function<? super IJadescriptType, ?
            extends IJadescriptType> solvingFunction
    ) {
        return new HoledPatternType(solvingFunction);
    }

    static SimplePatternType simple(IJadescriptType type) {
        return new SimplePatternType(type);
    }

    static SimplePatternType empty(SemanticsModule module) {
        return new SimplePatternType(module.get(BuiltinTypeProvider.class)
            .nothing(
                "Empty pattern type escaped."
            ));
    }

    IJadescriptType solve(IJadescriptType providedInputType);

    class SimplePatternType implements PatternType {

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


        @Override
        public String toString() {
            return "SimplePatternType{type=" + this.type + "}";
        }

    }

    class HoledPatternType implements PatternType {

        private final Function<? super IJadescriptType, ?
            extends IJadescriptType> solvingFunction;


        public HoledPatternType(
            Function<? super IJadescriptType, ?
                extends IJadescriptType> solvingFunction
        ) {
            this.solvingFunction = solvingFunction;
        }


        @Override
        public IJadescriptType solve(IJadescriptType providedInputType) {
            return solvingFunction.apply(providedInputType);
        }


        @Override
        public String toString() {
            return "HoledType{funcClass=" +
                this.solvingFunction.getClass().getName() + "}";
        }

    }

}
