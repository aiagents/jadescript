package it.unipr.ailab.jadescript.semantics.context.staticstate;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.List;
import java.util.Objects;

public interface ExpressionDescriptor {

    public static class TypeCheck implements ExpressionDescriptor {
        private final ExpressionDescriptor expressionChecked;
        private final IJadescriptType type;

        public TypeCheck(
            ExpressionDescriptor expressionChecked,
            IJadescriptType type
        ) {
            this.expressionChecked = expressionChecked;
            this.type = type;
        }

        public ExpressionDescriptor getExpressionChecked() {
            return expressionChecked;
        }

        public IJadescriptType getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TypeCheck)) return false;

            TypeCheck tc = (TypeCheck) o;

            if (getExpressionChecked() != null
                ? !getExpressionChecked().equals(tc.getExpressionChecked())
                : tc.getExpressionChecked() != null)
                return false;
            return getType() != null ? getType().equals(tc.getType())
                : tc.getType() == null;
        }

        @Override
        public int hashCode() {
            int result = getExpressionChecked() != null ?
                getExpressionChecked().hashCode() : 0;
            result = 31 * result + (getType() != null ? getType().hashCode()
                : 0);
            return result;
        }
    }





    public static class PropertyChain implements ExpressionDescriptor {
        private final List<String> properties;


        public PropertyChain(List<String> properties) {
            if (properties.isEmpty()) {
                throw new IllegalArgumentException(
                    "Property chain cannot be empty."
                );
            }
            this.properties = properties;
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

    }
}
