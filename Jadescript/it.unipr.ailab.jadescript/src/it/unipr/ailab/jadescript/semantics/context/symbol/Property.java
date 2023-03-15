package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.DereferencedName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Functional.TriConsumer;
import org.eclipse.xtext.util.Strings;

import java.util.function.BiFunction;
import java.util.function.Function;

public class Property implements MemberName {

    protected final BiFunction<String, BlockElementAcceptor, String> read;
    protected final TriConsumer<String, String, BlockElementAcceptor> write;
    private final boolean canWrite;
    private final String name;
    private final IJadescriptType type;
    private final SearchLocation location;


    public Property(
        boolean canWrite,
        String name,
        IJadescriptType type,
        SearchLocation location,
        BiFunction<String, BlockElementAcceptor, String> read,
        TriConsumer<String, String, BlockElementAcceptor> write
    ) {
        this.canWrite = canWrite;
        this.name = name;
        this.type = type;
        this.location = location;
        this.read = read;
        this.write = write;
    }


    public static Property readonlyProperty(
        String name,
        IJadescriptType type,
        SearchLocation location,
        BiFunction<String, BlockElementAcceptor, String> readCompile
    ) {
        return new Property(
            false,
            name,
            type,
            location,
            readCompile,
            (ow, rx, acc) -> acc.accept(
                w.assign(ow + "./* Error: readonly property */" +
                    name, w.expr(rx))
            )
        );
    }


    public static TriConsumer<String, String, BlockElementAcceptor>
    compileWithJVMSetter(
        String name
    ) {
        return (o, r, a) -> a.accept(
            w.callStmnt(o + ".set" + Strings.toFirstUpper(name),
                w.expr(r))
        );
    }


    public static TriConsumer<String, String, BlockElementAcceptor>
    compileSetWithCustomMethod(
        String methodName
    ) {
        return (o, r, a) -> a.accept(
            w.callStmnt(o + "." + methodName, w.expr(r))
        );
    }


    public static BiFunction<String, BlockElementAcceptor, String>
    compileWithJVMGetter(String name) {
        return (o,a) -> o + ".get" + Strings.toFirstUpper(name) + "()";
    }


    public static BiFunction<String, BlockElementAcceptor, String>
    compileGetWithCustomMethod(
        String methodName
    ) {
        return (o, a) -> o + "." + methodName + "()";
    }




    @Override
    public DereferencedName dereference(Function<BlockElementAcceptor,
        String> ownerCompiler) {
        return new DereferencedProperty(
            ownerCompiler,
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
