package it.unipr.ailab.jadescript.semantics.namespace.jvm;

import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.function.Function;

public class JvmFieldSymbol implements NamedSymbol,
    JvmSymbol {

    private final JvmField jvmField;
    private final Function<JvmTypeReference, IJadescriptType> typeResolver;
    private final boolean isStatic;
    private final Maybe<JvmType> declaringType;


    public JvmFieldSymbol(
        JvmField jvmField,
        Function<JvmTypeReference, IJadescriptType> typeResolver,
        boolean isStatic,
        Maybe<JvmType> declaringType
    ) {
        this.jvmField = jvmField;
        this.typeResolver = typeResolver;
        this.isStatic = isStatic;
        this.declaringType = declaringType;
    }


    public boolean isStatic() {
        return isStatic;
    }


    @Override
    public Maybe<JvmType> declaringType() {
        return declaringType;
    }


    @Override
    public String name() {
        return jvmField.getSimpleName();
    }


    @Override
    public String compileRead(String dereferencePrefix) {
        return dereferencePrefix + jvmField.getSimpleName();
    }


    @Override
    public IJadescriptType readingType() {
        return typeResolver.apply(jvmField.getType());
    }


    @Override
    public boolean canWrite() {
        return !jvmField.isFinal();
    }


    @Override
    public String compileWrite(String dereferencePrefix, String rexpr) {
        return dereferencePrefix + jvmField.getSimpleName() + " = " + rexpr;
    }


    @Override
    public void debugDumpNamedSymbol(SourceCodeBuilder scb) {
        NamedSymbol.super.debugDumpNamedSymbol(scb);
        scb.indent().line("--> (" + name() + " is also JvmFieldSymbol; " +
            "isStatic=" + isStatic() + ")").dedent();
    }

}
