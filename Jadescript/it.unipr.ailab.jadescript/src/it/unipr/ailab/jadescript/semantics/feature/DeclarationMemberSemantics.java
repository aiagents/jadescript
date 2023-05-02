package it.unipr.ailab.jadescript.semantics.feature;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Feature;
import it.unipr.ailab.jadescript.jadescript.FeatureContainer;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.Semantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.associations.SelfAssociated;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.controlflow.TryCatchWriter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;


/**
 * Created on 26/04/18.
 */
@Singleton
public abstract class DeclarationMemberSemantics<T extends Feature>
    extends Semantics {


    public DeclarationMemberSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    public abstract void generateJvmMembers(
            Maybe<T> input,
            Maybe<FeatureContainer> featureContainer,
            EList<JvmMember> members,
            JvmDeclaredType beingDeclared,
            BlockElementAcceptor fieldInitializationAcceptor
    );


    protected TryCatchWriter encloseInGeneralHandlerTryCatch(
        SourceCodeBuilder scb
    ) {
        return encloseInGeneralHandlerTryCatch(
            new BlockWriter() {
                @Override
                public void writeSonnet(SourceCodeBuilder s) {
                    s.add(scb);
                }
            }
        );
    }


    protected TryCatchWriter encloseInGeneralHandlerTryCatch(
        BlockWriter insideTry
    ) {
        final String throwable = "__throwable";
        return w.tryCatch(insideTry)
            .addCatchBranch(
                "jadescript.core.exception.JadescriptException",
                throwable,
                w.block()
                    .addStatement(w.callStmnt(
                        EXCEPTION_HANDLER_METHOD_NAME,
                        w.expr(throwable)
                    ))
            ).addCatchBranch("java.lang.Throwable", throwable,
                w.block()
                    .addStatement(w.callStmnt(
                        EXCEPTION_HANDLER_METHOD_NAME,
                        w.expr(
                            "jadescript.core.exception.JadescriptException" +
                                ".wrap(" + throwable + ")"
                        )
                    ))
            );
    }


    public abstract void validateOnEdit(
            Maybe<T> input,
            Maybe<FeatureContainer> container,
            ValidationMessageAcceptor acceptor
    );

    public abstract void validateOnSave(
        Maybe<T> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    );

    protected SearchLocation getLocationOfThis() {
        return module.get(ContextManager.class).currentContext()
                .actAs(SelfAssociated.class)
                .findFirst()
                .flatMap(sac -> sac.computeCurrentSelfAssociations()
                    .findFirst())
                .map(sa -> sa.getAssociatedType().namespace().currentLocation())
                .orElse(module.get(ContextManager.class).currentContext()
                    .currentLocation());
    }
}
