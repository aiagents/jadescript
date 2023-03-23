package it.unipr.ailab.jadescript.semantics.context.staticstate;

import it.unipr.ailab.maybe.utils.ImmutableList;

import java.util.Objects;
import java.util.stream.Collectors;

public interface ExpressionDescriptor {

    final PropertyChain agentReference = new PropertyChain("agent");
    final PropertyChain thisReference = new PropertyChain("this");
    final PropertyChain messageReference = new PropertyChain("message");
    final PropertyChain nativeEventReference = new PropertyChain("event");
    final PropertyChain contentOfMessageReference =
        new PropertyChain("content", "message");
    final PropertyChain failureReasonReference =
        new PropertyChain("failureReason");
    final PropertyChain exceptionReference = new PropertyChain("exception");


    ExpressionDescriptor descriptorOfMemberProperty(String propertyName);

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


        @Override
        public ExpressionDescriptor descriptorOfMemberProperty(
            String propertyName
        ) {
            return new PropertyChain(this.properties.insert(0, propertyName));

        }

    }

}
