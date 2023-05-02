package it.unipr.ailab.jadescript.semantics.context.clashing;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.FQNameLocation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.Callable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationship;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery;
import org.eclipse.xtext.util.Strings;

import java.util.List;
import java.util.stream.Stream;

public interface AutoCallableClashValidator extends CallableClashValidator {

    /**
     * Returns true if {@code toBeAdded} is a declaration that overrides
     * {@code alreadyPresent}.
     */
    static boolean isOverriding(
        SemanticsModule module,
        Callable alreadyPresent,
        Callable toBeAdded
    ) {
        final SearchLocation alreadyPresentLocation =
            alreadyPresent.sourceLocation();
        final SearchLocation toBeAddedLocation =
            toBeAdded.sourceLocation();
        if (alreadyPresentLocation instanceof FQNameLocation
            && toBeAddedLocation instanceof FQNameLocation) {
            final IJadescriptType alreadyPresentType =
                ((FQNameLocation) alreadyPresentLocation)
                    .extractType(module);
            final IJadescriptType toBeAddedType =
                ((FQNameLocation) toBeAddedLocation)
                    .extractType(module);

            final TypeComparator comparator = module.get(TypeComparator.class);

            final TypeRelationship comparison = comparator.compare(
                alreadyPresentType, toBeAddedType
            );

            return TypeRelationshipQuery.strictSuperType().matches(comparison)
                && isSignatureCompatibleForOverriding(
                module,
                alreadyPresent,
                toBeAdded
            );
        }
        return false;
    }

    static boolean isSignatureCompatibleForOverriding(
        SemanticsModule module,
        Callable alreadyPresent,
        Callable toBeAdded
    ) {
        if (alreadyPresent.arity() != toBeAdded.arity()) {
            return false;
        }

        final TypeComparator comparator = module.get(TypeComparator.class);

        if (!TypeRelationshipQuery.superTypeOrEqual().matches(
            comparator.compare(
                alreadyPresent.returnType(),
                toBeAdded.returnType()
            ))
        ) {
            return false;
        }

        final List<IJadescriptType> apTypes = alreadyPresent.parameterTypes();
        final List<IJadescriptType> tbaTypes = toBeAdded.parameterTypes();
        for (int i = 0; i < apTypes.size(); i++) {
            if (!TypeRelationshipQuery.equal().matches(comparator.compare(
                apTypes.get(i), tbaTypes.get(i)
            ))) {
                return false;
            }
        }

        return true;
    }

    @Override
    default Stream<DefinitionClash> checkCallableClash(
        SemanticsModule module,
        Callable toBeAdded
    ) {
        Stream<DefinitionClash> fromCallables;
        if (this instanceof CompilableCallable.Namespace) {
            fromCallables = ((CompilableCallable.Namespace) this)
                .compilableCallables(toBeAdded.name())
                .filter(mc -> mc.arity() == toBeAdded.arity())
                .filter(alreadyPresent -> !isOverriding(
                    module,
                    alreadyPresent,
                    toBeAdded
                ))
                .map(alreadyPresent -> new DefinitionClash(
                    toBeAdded,
                    alreadyPresent
                ));
            if (toBeAdded.parameterTypes().size() == 0) {
                fromCallables = Streams.concat(
                    fromCallables,
                    ((CompilableCallable.Namespace) this)
                        .compilableCallables(
                            "get" + Strings.toFirstUpper(toBeAdded.name())
                        )
                        .filter(mc -> mc.arity() == 0)
                        .map(alreadyPresent -> new DefinitionClash(
                            toBeAdded,
                            alreadyPresent
                        ))
                );
            }
            if (toBeAdded.parameterTypes().size() == 1) {
                fromCallables = Streams.concat(
                    fromCallables,
                    ((CompilableCallable.Namespace) this)
                        .compilableCallables("set" +
                            Strings.toFirstUpper(toBeAdded.name()))
                        .filter(mc -> mc.arity() == 1)
                        .map(alreadyPresent -> new DefinitionClash(
                            toBeAdded,
                            alreadyPresent
                        ))
                );
            }
        } else {
            fromCallables = Stream.empty();
        }

        Stream<DefinitionClash> fromNameds;
        if (this instanceof CompilableName.Namespace
            && toBeAdded.parameterNames().size() == 0) {
            fromNameds = ((CompilableName.Namespace) this)
                .compilableNames(toBeAdded.name())
                .map(alreadyPresent -> new DefinitionClash(
                    toBeAdded,
                    alreadyPresent
                ));
        } else {
            fromNameds = Stream.empty();
        }

        return Streams.concat(
            fromNameds,
            fromCallables
        );
    }

}
