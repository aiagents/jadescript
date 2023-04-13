package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ExtendingElement;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;

/**
 * Created on 27/04/18.
 */
@Singleton
public abstract class ExtendingTopLevelDeclarationSemantics
    <T extends ExtendingElement>
    extends MemberContainerTopLevelDeclarationSemantics<T> {

    public ExtendingTopLevelDeclarationSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    public Maybe<IJadescriptType> getExtendedType(Maybe<T> input) {

        final Maybe<EList<JvmParameterizedTypeReference>> eListMaybe =
            input.__(ExtendingElement::getSuperTypes);

        if (eListMaybe.__(List::isEmpty).orElse(true)) {
            return Maybe.nothing();
        }

        return eListMaybe
            .__(elist -> elist.get(0))
            .__(module.get(TypeSolver.class)::fromJvmTypeReference);
    }


    @Override
    public void validateOnEdit(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return;
        }


        super.validateOnEdit(input, acceptor);
    }


    @Override
    public void validateOnSave(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return;
        }

        List<IJadescriptType> allowedSuperTypes =
            allowedIndirectSupertypes(input);

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

            final TypeSolver typeSolver = module.get(TypeSolver.class);
            final TypeComparator comparator = module.get(TypeComparator.class);

        if (!allowedSuperTypes.isEmpty()) {
            for (Maybe<JvmParameterizedTypeReference> declaredSuperType :
                Maybe.iterate(input.__(ExtendingElement::getSuperTypes))) {

                if (declaredSuperType.isNothing()) {
                    continue;
                }

                final JvmParameterizedTypeReference declaredSuperTypeSafe =
                    declaredSuperType.toNullable();

                validationHelper.asserting(
                    allowedSuperTypes.stream().anyMatch(sup -> {
                        final IJadescriptType sub = typeSolver
                            .fromJvmTypeReference(declaredSuperTypeSafe);

                        return comparator.compare(sup, sub)
                            .is(superTypeOrEqual());
                    }),
                    "InvalidSupertype",
                    "Here is expected a subtype of or same type as " +
                        namesOfAllowedSuperTypes(allowedSuperTypes),
                    declaredSuperType,
                    acceptor
                );
            }
        }
        super.validateOnSave(input, acceptor);
    }


    private String namesOfAllowedSuperTypes(
        List<IJadescriptType> allowedSuperTypes
    ) {
        if (allowedSuperTypes.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(allowedSuperTypes.get(0).getFullJadescriptName());

        if (allowedSuperTypes.size() > 1) {
            for (int i = 1; i < allowedSuperTypes.size() - 1; i++) {
                sb.append(", ")
                    .append(allowedSuperTypes.get(i).getFullJadescriptName());
            }
            sb.append(" or ")
                .append(allowedSuperTypes.get(allowedSuperTypes.size() - 1)
                    .getFullJadescriptName());
        }

        return sb.toString();
    }


    @Override
    public void populateMainSuperTypes(
        Maybe<T> input,
        EList<JvmTypeReference> superTypes
    ) {
        if (!input.__(ExtendingElement::getSuperTypes)
            .__(EList::isEmpty).orElse(true)) {

            final JvmTypesBuilder jvmTB = module.get(JvmTypesBuilder.class);

            superTypes.addAll(
                Maybe.someStream(input.__(ExtendingElement::getSuperTypes))
                    .map(j -> j.__(jvmTB::cloneWithProxies))
                    .filter(Maybe::isPresent)
                    .map(Maybe::toNullable)
                    .collect(Collectors.toList())
            );

        } else {
            defaultSuperType(input)
                .map(IJadescriptType::asJvmTypeReference)
                .ifPresent(superTypes::add);
        }

        super.populateMainSuperTypes(input, superTypes);
    }


    public Optional<IJadescriptType> defaultSuperType(Maybe<T> input) {
        return Optional.empty();
    }


    public abstract List<IJadescriptType> allowedIndirectSupertypes(
        Maybe<T> input
    );

}
