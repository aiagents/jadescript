package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.NamedElement;
import it.unipr.ailab.jadescript.semantics.Semantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
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

/**
 * Created on 26/04/18.
 */
@Singleton
public abstract class NamedTopLevelDeclarationSemantics<T extends NamedElement>
    extends Semantics {

    public NamedTopLevelDeclarationSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    public void validateOnEdit(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return;
        }

        Maybe<String> name = input.__(NamedElement::getName)
            .nullIf(String::isBlank);

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);


        validationHelper.assertNotReservedName(
            name,
            input,
            JadescriptPackage.eINSTANCE.getNamedElement_Name(),
            acceptor
        );
    }


    public void validateOnSave(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {

        Maybe<String> name = input.__(NamedElement::getName)
            .nullIf(String::isBlank);

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        if (nameShouldStartWithCapital()) {
            validationHelper.advice(
                name.__partial2(String::charAt, 0).__(Character::isUpperCase),
                "LowerCaseElementName",
                "Names here should start with a capital letter",
                input,
                JadescriptPackage.eINSTANCE.getNamedElement_Name(),
                acceptor
            );
        }
    }


    public void validateOnRequest(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {
        // Overridden by subclasses.
    }


    public boolean nameShouldStartWithCapital() {
        return true;
    }


    @SuppressWarnings("SameReturnValue")
    public boolean isNameAlwaysRequired() {
        return true;
    }


    public void generateDeclaredTypes(
        Maybe<T> input,
        IJvmDeclaredTypeAcceptor acceptor,
        boolean isPreIndexingPhase
    ) {


        if (input == null) {
            return;
        }
        Maybe<String> name = input.__(NamedElement::getName);
        if (isNameAlwaysRequired() && name.isNothing()) {
            return;
        }

        if (input.isNothing()) {
            return;
        }

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        Maybe<QualifiedName> fullyQualifiedName =
            input.__(compilationHelper::getFullyQualifiedName);

        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);

        final T inputSafe = input.toNullable();

        if (fullyQualifiedName.isNothing()) {
            return;
        }

        final QualifiedName fqnSafe = fullyQualifiedName.toNullable();

        acceptor.accept(
            jvmTB.toClass(inputSafe, fqnSafe, itClass -> {
                populateMainSuperTypes(input, itClass.getSuperTypes());
            }),
            itClass -> {
                if (mainGeneratedClassIsAbstract(input)) {
                    itClass.setAbstract(true);
                }

                populateMainMembers(
                    input,
                    itClass.getMembers(),
                    itClass
                );
            }
        );
    }


    public boolean mainGeneratedClassIsAbstract(Maybe<T> input) {
        return false;
    }


    public void populateMainMembers(
        Maybe<T> input,
        EList<JvmMember> members,
        JvmDeclaredType beingDeclared
    ) {
        //do nothing
    }


    public void populateMainSuperTypes(
        Maybe<T> input,
        EList<JvmTypeReference> superTypes
    ) {
        //do nothing
    }

}
