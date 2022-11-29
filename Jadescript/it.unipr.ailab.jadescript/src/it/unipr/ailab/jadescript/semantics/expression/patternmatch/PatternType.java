package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public interface PatternType {
    public static class SimplePatternType implements PatternType{
        private final IJadescriptType type;

        public SimplePatternType(IJadescriptType type) {
            this.type = type;
        }

        public IJadescriptType getType() {
            return type;
        }
    }
}
