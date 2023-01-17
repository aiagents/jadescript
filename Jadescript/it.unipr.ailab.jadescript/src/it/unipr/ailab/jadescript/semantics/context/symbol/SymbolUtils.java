package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.empty;
import static java.util.Optional.of;


public class SymbolUtils {

    private SymbolUtils() {
    } // do not instantiate


    public static NamedSymbol setDereferenceByVariable(
        NamedSymbol namedElement,
        String var
    ) {
        return new DereferencedByVarNamedSymbol(
            namedElement, var
        );
    }


    public static CallableSymbol changeLocation(
        CallableSymbol input,
        SearchLocation location
    ) {
        return new ChangedLocationCallableSymbol(
            input,
            location
        );
    }


    public static CallableSymbol setDereferenceByVariable(
        CallableSymbol callableElement,
        String var
    ) {
        return new DereferencedByVarCallableSymbol(
            callableElement,
            var
        );
    }


    public static CallableSymbol setDereferenceByCtor(
        CallableSymbol input,
        IJadescriptType inModuleType
    ) {
        return new DereferencedByCtorCallableSymbol(
            input,
            inModuleType.compileToJavaTypeReference()
        );
    }


    public static CallableSymbol setDereferenceByExternalClass(
        CallableSymbol input,
        IJadescriptType inModuleType
    ) {
        return new DereferencedByExternalClassCallableSymbol(
            input,
            inModuleType.compileToJavaTypeReference()
        );
    }


    public static Optional<NamedSymbol> intersectNamedSymbols(
        Collection<? extends NamedSymbol> nss,
        SemanticsModule module
    ) {

        if (nss.isEmpty()) {
            return empty();
        }

        if(nss.size() == 1){
            for (NamedSymbol namedSymbol : nss) {
                return of(namedSymbol);
            }
        }

        TypeHelper typeHelper = module.get(TypeHelper.class);
        //All names must be the same
        String name = null;
        IJadescriptType lub = null;
        for (NamedSymbol ns : nss) {
            if (name == null) {
                name = ns.name();
                lub = ns.readingType();
            } else {
                if (!name.equals(ns.name())) {
                    return empty();
                }
                lub = typeHelper.getLUB(lub, ns.readingType());
            }
        }

        NamedSymbolSignature signature = null;
        boolean allSameSignature = true;
        for (NamedSymbol ns : nss) {
            if (signature == null) {
                signature = ns.getSignature();
            } else {
                if (!signature.equals(ns.getSignature())) {
                    allSameSignature = false;
                    break;
                }
            }
        }


        if (allSameSignature) {
            for (NamedSymbol namedSymbol : nss) {
                return of(namedSymbol);
            }
        }

        SearchLocation location = null;
        boolean allSameLocation = true;
        for (NamedSymbol ns : nss) {
            if (location == null) {
                location = ns.sourceLocation();
            } else {
                if (!location.equals(ns.sourceLocation())) {
                    allSameLocation = false;
                    break;
                }
            }
        }

        if (allSameLocation) {
            return of(new IntersectedNamedSymbol(name, lub, location));
        }


        return of(new IntersectedNamedSymbol(
            name,
            lub,
            new IntersectedLocation(
                nss.stream()
                    .map(NamedSymbol::sourceLocation)
                    .collect(Collectors.toSet())
            )
        ));
    }


    public static class IntersectedLocation extends SearchLocation {

        private final Set<SearchLocation> locs;


        public IntersectedLocation(Set<SearchLocation> x) {
            this.locs = new HashSet<>(flatten(x));
        }


        public static Set<SearchLocation> flatten(Set<SearchLocation> x) {
            return x.stream()
                .flatMap(sl -> {
                    if (sl instanceof IntersectedLocation) {
                        return flatten(((IntersectedLocation) sl).locs)
                            .stream();
                    } else {
                        return Stream.of(sl);
                    }
                }).collect(Collectors.toSet());
        }


        @Override
        public boolean equals(Object obj) {
            if (obj instanceof IntersectedLocation) {
                return this.locs.equals(((IntersectedLocation) obj).locs);
            }

            if (obj instanceof SearchLocation) {
                if (this.locs.size() == 1) {
                    for (SearchLocation sub : this.locs) {
                        return sub.equals(obj);
                    }
                }
            }

            return false;
        }


        @Override
        public int hashCode() {
            return locs.hashCode();
        }


        @Override
        public String toString() {
            return "(Intersected from other declarations. Locations: " +
                locs.stream()
                    .map(SearchLocation::toString)
                    .collect(Collectors.joining(", ")) +
                ")";
        }

    }

    public static class IntersectedNamedSymbol implements NamedSymbol {

        private final String name;
        private final IJadescriptType type;
        private final SearchLocation location;


        public IntersectedNamedSymbol(
            String name,
            IJadescriptType type,
            SearchLocation location
        ) {
            this.name = name;
            this.type = type;
            this.location = location;
        }


        @Override
        public String name() {
            return name;
        }


        @Override
        public IJadescriptType readingType() {
            return type;
        }


        @Override
        public boolean canWrite() {
            return false;
        }


        @Override
        public SearchLocation sourceLocation() {
            return location;
        }

    }

}
