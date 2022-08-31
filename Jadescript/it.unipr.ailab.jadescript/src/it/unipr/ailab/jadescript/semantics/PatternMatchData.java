package it.unipr.ailab.jadescript.semantics;

import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmField;

import java.util.List;

public class PatternMatchData {

    private final String compiledExpression;
    private final List<JvmDeclaredType> patternMatchClasses;
    private final List<JvmField> patternMatchFields;
    private final List<NamedSymbol> autoDeclaredVariables;
    private final IJadescriptType inferredContentType;

    public PatternMatchData(
            String compiledExpression,
            List<JvmDeclaredType> patternMatchClasses,
            List<JvmField> patternMatchFields,
            List<NamedSymbol> autoDeclaredVariables,
            IJadescriptType inferredContentType
    ) {
        this.compiledExpression = compiledExpression;
        this.patternMatchClasses = patternMatchClasses;
        this.patternMatchFields = patternMatchFields;
        this.autoDeclaredVariables = autoDeclaredVariables;
        this.inferredContentType = inferredContentType;
    }

    public String getCompiledExpression() {
        return compiledExpression;
    }

    public List<JvmDeclaredType> getPatternMatchClasses() {
        return patternMatchClasses;
    }

    public List<JvmField> getPatternMatchFields() {
        return patternMatchFields;
    }

    public List<NamedSymbol> getAutoDeclaredVariables() {
        return autoDeclaredVariables;
    }

    public IJadescriptType getInferredContentType() {
        return inferredContentType;
    }
}
