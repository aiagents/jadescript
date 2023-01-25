package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OntologyElementStructuralPattern implements PatternSymbol {

    private final String name;
    private final IJadescriptType inputType;
    private final List<String> termNames;
    private final Map<String, IJadescriptType> termTypesByName;
    private final SearchLocation location;


    public OntologyElementStructuralPattern(
        String name,
        IJadescriptType inputType,
        List<String> termNames,
        Map<String, IJadescriptType> termTypesByName,
        SearchLocation location
    ) {
        this.name = name;
        this.inputType = inputType;
        this.termNames = termNames;
        this.termTypesByName = termTypesByName;
        this.location = location;
    }


    @Override
    public String name() {
        return name;
    }


    @Override
    public IJadescriptType inputType() {
        return inputType;
    }


    @Override
    public Map<String, IJadescriptType> termTypesByName() {
        return termTypesByName;
    }


    @Override
    public List<String> termNames() {
        return termNames;
    }


    @Override
    public List<IJadescriptType> termTypes() {
        final Map<String, IJadescriptType> ttbn = this.termTypesByName();
        return termNames().stream()
            .filter(ttbn::containsKey)
            .map(ttbn::get)
            .collect(Collectors.toList());
    }


    @Override
    public boolean isWithoutSideEffects() {
        return true;
    }


    @Override
    public SearchLocation sourceLocation() {
        return location;
    }

}
