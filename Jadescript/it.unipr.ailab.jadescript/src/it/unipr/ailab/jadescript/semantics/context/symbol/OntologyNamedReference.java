package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public class OntologyNamedReference implements GlobalName {

    private final IJadescriptType ontologyType;
    private final String name;


    public OntologyNamedReference(IJadescriptType ontologyType, String name) {
        this.ontologyType = ontologyType;
        this.name = name;
    }


    @Override
    public String compileRead(BlockElementAcceptor acceptor) {
        final String ontoTypeCompiled =
            ontologyType.compileToJavaTypeReference();
        return "((" + ontoTypeCompiled + ") " +
            ontoTypeCompiled + ".getInstance())";
    }


    @Override
    public void compileWrite(String rexpr, BlockElementAcceptor acceptor) {
        acceptor.accept(w.simpleStmt("/*internal error*/"
            + compileRead(acceptor)));
    }


    @Override
    public SearchLocation sourceLocation() {
        return ontologyType.getLocation();
    }


    @Override
    public String name() {
        return name;
    }


    @Override
    public IJadescriptType readingType() {
        return ontologyType;
    }


    @Override
    public boolean canWrite() {
        return false;
    }

}
