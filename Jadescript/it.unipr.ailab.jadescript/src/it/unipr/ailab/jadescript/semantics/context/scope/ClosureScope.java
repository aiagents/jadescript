package it.unipr.ailab.jadescript.semantics.context.scope;

import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.UserVariable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ClosureScope extends ChildProceduralScope {

    private final Consumer<UserVariable> capturedVariableListener;

    public ClosureScope(
            ProceduralScope outer,
            Consumer<UserVariable> capturedVariableListener
    ) {
        super(outer);
        this.capturedVariableListener = capturedVariableListener;
    }

    @Override
    public Stream<? extends NamedSymbol> searchName(
            Predicate<String> name,
            Predicate<IJadescriptType> readingType,
            Predicate<Boolean> canWrite
    ) {
        return super.searchName(name, readingType, canWrite).map(ns -> {
            if (ns instanceof UserVariable) {
                final UserVariable variable = UserVariable.asCaptured(((UserVariable) ns));
                capturedVariableListener.accept(variable);
                return variable;
            } else {
                return ns;
            }
        });
    }

}
