package it.unipr.ailab.jadescript.semantics.context.staticstate;

import it.unipr.ailab.jadescript.semantics.utils.ImmutableList;

import java.util.Objects;
import java.util.stream.Collectors;

public interface ExpressionDescriptor {

    class PropertyChain implements ExpressionDescriptor {

        private final ImmutableList<String> properties;


        public PropertyChain(ImmutableList<String> properties) {
            if (properties.isEmpty()) {
                throw new IllegalArgumentException(
                    "Property chain cannot be empty."
                );
            }
            this.properties = properties;
        }


        public PropertyChain(String... properties) {
            if (properties.length == 0) {
                throw new IllegalArgumentException(
                    "Property chain cannot be empty."
                );
            }
            this.properties = ImmutableList.of(properties);
        }


        public ImmutableList<String> getProperties() {
            return properties;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PropertyChain)) return false;

            PropertyChain that = (PropertyChain) o;

            return Objects.equals(properties, that.properties);
        }


        @Override
        public int hashCode() {
            return properties != null ? properties.hashCode() : 0;
        }


        @Override
        public String toString() {
            return "PropertyChain["
                + properties.stream()
                .collect(Collectors.joining(", "))
                + "]";
        }

    }

}
