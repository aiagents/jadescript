package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.sonneteer.classmember.FieldWriter;
import it.unipr.ailab.sonneteer.classmember.MethodWriter;

import java.util.ArrayList;
import java.util.List;

public interface PatternMatchSemanticsProcess {
    enum IsValidation implements PatternMatchSemanticsProcess {
        INSTANCE
    }

    class IsCompilation implements PatternMatchSemanticsProcess {

        private final List<MethodWriter> methodWriters = new ArrayList<>();
        private final List<FieldWriter> fieldWriters = new ArrayList<>();
        private final String entryMethodName;

        public IsCompilation(String entryMethodName) {
            this.entryMethodName = entryMethodName;
        }

        public IsCompilation addWriter(MethodWriter methodWriter) {
            methodWriters.add(methodWriter);
            return this;
        }

        public IsCompilation addWriter(FieldWriter fieldWriter) {
            fieldWriters.add(fieldWriter);
            return this;
        }

        public List<MethodWriter> getMethodWriters() {
            return methodWriters;
        }

        public List<FieldWriter> getFieldWriters() {
            return fieldWriters;
        }

        public String compileOperationInvocation(String input){
            return entryMethodName + "(" + input + ")";
        }
    }
}
