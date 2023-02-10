package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.DereferencedNamedCell;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberNamedCell;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import org.eclipse.xtext.util.Strings;

import java.util.function.BiFunction;
import java.util.function.Function;

public class Property implements MemberNamedCell {

    protected final Function<String, String> readCompile;
    private final boolean canWrite;
    private final String name;
    private final IJadescriptType type;
    private final SearchLocation location;
    protected BiFunction<String, String, String> writeCompile;


    public Property(
        boolean canWrite,
        String name,
        IJadescriptType type,
        SearchLocation location,
        Function<String, String> readCompile,
        BiFunction<String, String, String> writeCompile
    ) {
        this.canWrite = canWrite;
        this.name = name;
        this.type = type;
        this.location = location;
        this.readCompile = readCompile;
        this.writeCompile = writeCompile;
    }


    public static Property readonlyProperty(
        String name,
        IJadescriptType type,
        SearchLocation location,
        Function<String, String> readCompile
    ) {
        return new Property(
            false,
            name,
            type,
            location,
            readCompile,
            (ow, rx) -> ow + "./* Error: readonly property */" +
                name + " = " + rx
        );
    }


    public static BiFunction<String, String, String> compileWithJVMSetter(
        String name
    ) {
        return (owner, rexpr) -> owner + ".set" + Strings.toFirstUpper(name) +
            "(" + rexpr + ")";
    }


    public static Function<String, String> compileWithJVMGetter(String name) {
        return (owner) -> owner + ".get" + Strings.toFirstUpper(name) + "()";
    }


    public static Function<String, String> compileGetWithCustomMethod(
        String methodName
    ) {
        return o -> o + "." + methodName + "()";
    }


    public static BiFunction<String, String, String> compileSetWithCustomMethod(
        String methodName
    ) {
        return (o, r) -> o + "." + methodName + "(" + r + ")";
    }


    @Override
    public DereferencedNamedCell dereference(String compiledOwner) {
        return new DereferencedProperty(
            compiledOwner,
            this
        );
    }


    @Override
    public SearchLocation sourceLocation() {
        return this.location;
    }


    @Override
    public String name() {
        return this.name;
    }


    @Override
    public IJadescriptType readingType() {
        return type;
    }


    @Override
    public boolean canWrite() {
        return this.canWrite;
    }

}
