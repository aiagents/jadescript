package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import org.eclipse.xtext.util.Strings;

import java.util.function.BiFunction;
import java.util.function.Function;

public class Property implements NamedSymbol {

    private final String name;
    private final IJadescriptType type;
    private final boolean readOnly;
    private final SearchLocation location;
    private Function<String, String> readCompile;
    private BiFunction<String, String, String> writeCompile;


    public Property(
        String name,
        IJadescriptType type,
        boolean readOnly,
        SearchLocation location
    ) {
        this.name = name;
        this.type = type;
        this.readOnly = readOnly;
        this.readCompile = e -> e + name + "()";
        this.writeCompile = (e, re) -> e + name + "(" + re + ")";
        this.location = location;
    }


    public Property setCompileByJVMAccessors() {
        this.readCompile = e -> e + "get" + Strings.toFirstUpper(name) +
            "()";
        this.writeCompile = (e, re) -> e + "set" + Strings.toFirstUpper(name) +
            "(" + re + ")";
        return this;
    }


    public Property setCompileByCustomJVMMethod(
        String readMethod,
        String writeMethod
    ) {
        this.readCompile = e -> e + readMethod + "()";
        this.writeCompile = (e, re) -> e + writeMethod + "(" + re + ")";
        return this;
    }


    public Property setCustomCompile(
        Function<String, String> customReadCompile,
        BiFunction<String, String, String> customWriteCompile
    ) {
        this.readCompile = customReadCompile;
        this.writeCompile = customWriteCompile;
        return this;
    }


    @Override
    public String compileRead(String dereferencePrefix) {
        return readCompile.apply(dereferencePrefix);
    }


    @Override
    public IJadescriptType readingType() {
        return type;
    }


    @Override
    public String compileWrite(String dereferencePrefix, String rExpr) {
        return writeCompile.apply(dereferencePrefix, rExpr);
    }


    @Override
    public String name() {
        return name;
    }


    public boolean canWrite() {
        return !readOnly;
    }


    @Override
    public SearchLocation sourceLocation() {
        return location;
    }

}
