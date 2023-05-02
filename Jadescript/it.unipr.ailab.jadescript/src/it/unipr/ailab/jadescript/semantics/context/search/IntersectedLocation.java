package it.unipr.ailab.jadescript.semantics.context.search;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IntersectedLocation extends SearchLocation {

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
