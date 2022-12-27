package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.Semantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.Optional;

/**
 * Created on 26/04/18.
 */
@Singleton
public abstract class NamedEntitySemantics<T extends NamedElement> extends Semantics {

    public NamedEntitySemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    public void validate(Maybe<T> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        Maybe<String> name = input.__(NamedElement::getName).nullIf(String::isBlank);
        if (nameShouldStartWithCapital()) {

            module.get(ValidationHelper.class).advice(
                    name.__(String::charAt, 0).__(Character::isUpperCase),
                    "LowerCaseElementName",
                    "Names here should start with a capital letter",
                    input,
                    JadescriptPackage.eINSTANCE.getNamedElement_Name(),
                    acceptor
            );
        }

        module.get(ValidationHelper.class).assertNotReservedName(
                name,
                input,
                JadescriptPackage.eINSTANCE.getNamedElement_Name(),
                acceptor
        );
    }

    public boolean nameShouldStartWithCapital() {
        return true;
    }

    @SuppressWarnings("SameReturnValue")
    public boolean isNameAlwaysRequired() {
        return true;
    }


    public void generateDeclaredTypes(Maybe<T> input, IJvmDeclaredTypeAcceptor acceptor, boolean isPreIndexingPhase) {


        if (input == null) return;
        Maybe<String> name = input.__(NamedElement::getName);
        if (isNameAlwaysRequired() && name.isNothing()) {
            return;
        }
        Optional<T> inputSafe = input.toOpt();

        if (inputSafe.isPresent()) {
            Optional<QualifiedName> fullyQualifiedName = input.__(module.get(CompilationHelper.class)::getFullyQualifiedName).toOpt();
            fullyQualifiedName.ifPresent(qualifiedName -> acceptor.accept(module.get(JvmTypesBuilder.class).toClass(
                    inputSafe.get(),
                    qualifiedName,
                    itClass -> {
                        populateMainSuperTypes(input, itClass.getSuperTypes());

                        if (!isPreIndexingPhase) {
                            populateMainMembers(input, itClass.getMembers(), itClass);
                        }

                    }
            )));
        }
    }


    public void populateMainMembers(Maybe<T> input, EList<JvmMember> members, JvmDeclaredType beingDeclared) {
        //do nothing
    }

    public void populateMainSuperTypes(Maybe<T> input, EList<JvmTypeReference> superTypes) {
        //do nothing
    }
}
