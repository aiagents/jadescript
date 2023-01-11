package it.unipr.ailab.jadescript.semantics.context.staticstate;

import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.utils.ImmutableList;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public interface PatternDescriptor {

    public class ComposedPattern implements PatternDescriptor {

        private final int hashId;
        private final ImmutableList<PatternDescriptor> subPatterns;


        public ComposedPattern(
            int hashId,
            ImmutableList<PatternDescriptor> subPatterns
        ) {
            this.hashId = hashId;
            this.subPatterns = subPatterns;
        }


        public ComposedPattern(
            int hashId,
            List<PatternDescriptor> subPatterns
        ) {
            this(hashId, ImmutableList.from(subPatterns));
        }




        public int getHashId() {
            return hashId;
        }


        public ImmutableList<PatternDescriptor> getSubPatterns() {
            return subPatterns;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ComposedPattern)) return false;

            ComposedPattern that = (ComposedPattern) o;

            if (getHashId() != that.getHashId()) return false;
            return getSubPatterns() != null ?
                getSubPatterns().equals(that.getSubPatterns()) :
                that.getSubPatterns() == null;
        }


        @Override
        public int hashCode() {
            int result = getHashId();
            result = 31 * result + (getSubPatterns() != null ? getSubPatterns().hashCode() : 0);
            return result;
        }

    }

    public class IdentifierPattern implements PatternDescriptor {

        private final String identifier;


        public IdentifierPattern(String identifier) {
            this.identifier = identifier;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IdentifierPattern)) return false;

            IdentifierPattern that = (IdentifierPattern) o;

            return Objects.equals(identifier, that.identifier);
        }


        @Override
        public int hashCode() {
            return identifier != null ? identifier.hashCode() : 0;
        }

    }

}
