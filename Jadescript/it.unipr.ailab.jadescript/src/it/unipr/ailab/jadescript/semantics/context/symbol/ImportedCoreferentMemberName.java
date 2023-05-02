package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.FlowSensitiveSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.function.Function;

public class ImportedCoreferentMemberName
    implements CompilableName, FlowSensitiveSymbol {

    private final ExpressionDescriptor ownerDescriptor;
    private final Function<BlockElementAcceptor, String> ownerCompiler;
    private final MemberName memberName;


    public ImportedCoreferentMemberName(
        ExpressionDescriptor ownerDescriptor,
        Function<BlockElementAcceptor, String> ownerCompiler,
        MemberName memberName
    ) {
        this.ownerDescriptor = ownerDescriptor;
        this.ownerCompiler = ownerCompiler;
        this.memberName = memberName;
    }


    @Override
    public String compileRead(BlockElementAcceptor acceptor) {
        return memberName.dereference(ownerCompiler).compileRead(acceptor);
    }


    @Override
    public void compileWrite(String rexpr, BlockElementAcceptor acceptor) {
        memberName.dereference(ownerCompiler).compileWrite(rexpr, acceptor);
    }


    @Override
    public ExpressionDescriptor descriptor() {
        return ownerDescriptor.descriptorOfMemberProperty(memberName.name());
    }


    @Override
    public SearchLocation sourceLocation() {
        return memberName.sourceLocation();
    }


    @Override
    public String name() {
        return memberName.name();
    }


    @Override
    public IJadescriptType readingType() {
        return memberName.readingType();
    }


    @Override
    public IJadescriptType writingType() {
        return memberName.writingType();
    }


    @Override
    public boolean canWrite() {
        return memberName.canWrite();
    }

}
