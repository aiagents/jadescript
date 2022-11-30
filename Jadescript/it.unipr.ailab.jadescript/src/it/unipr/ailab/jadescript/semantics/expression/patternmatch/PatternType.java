package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public interface PatternType {
    IJadescriptType solve(IJadescriptType providedInputType);

    public static class SimplePatternType implements PatternType{
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

    public static SimplePatternType empty(SemanticsModule module){
        return new SimplePatternType(module.get(TypeHelper.class).NOTHING);
    }
}
