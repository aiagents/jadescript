package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.jadescript.ExtendingFeature;
import it.unipr.ailab.jadescript.jadescript.FeatureWithSlots;
import it.unipr.ailab.jadescript.jadescript.SlotDeclaration;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalPattern;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.some;

public class OntologyElementStructuralPattern implements GlobalPattern {

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


    public static Maybe<OntologyElementStructuralPattern> fromFeature(
        SemanticsModule module,
        Maybe<ExtendingFeature> f,
        SearchLocation currentLocation
    ) {
        if (f.isNothing()) {
            return Maybe.nothing();
        }

        final ExtendingFeature ontologyElement = f.toNullable();

        final List<String> termNames;
        final Map<String, IJadescriptType> termTypesByName;
        if (ontologyElement instanceof FeatureWithSlots) {

            final TypeExpressionSemantics typeExpressionSemantics =
                module.get(TypeExpressionSemantics.class);
            final List<Maybe<SlotDeclaration>> slots =
                Maybe.toListOfMaybes(f.__(
                    i -> ((FeatureWithSlots) i).getSlots()
                ));
            termNames = new ArrayList<>(slots.size());
            termTypesByName = new HashMap<>(slots.size());

            for (Maybe<SlotDeclaration> slot : slots) {
                String name = slot.__(SlotDeclaration::getName).orElse("");
                Maybe<TypeExpression> typeExpr =
                    slot.__(SlotDeclaration::getType);
                if (typeExpr.isPresent() && !name.isBlank()) {
                    termNames.add(name);
                    termTypesByName.put(
                        name,
                        typeExpressionSemantics.toJadescriptType(typeExpr)
                    );
                }
            }
        } else {
            termNames = List.of();
            termTypesByName = Map.of();
        }

        final TypeHelper typeHelper = module.get(TypeHelper.class);
        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);
        return some(new OntologyElementStructuralPattern(
            ontologyElement.getName() == null ? "" : ontologyElement.getName(),
            typeHelper.jtFromJvmTypeRef(
                typeHelper.typeRef(
                    compilationHelper.getFullyQualifiedName(ontologyElement)
                        .toString(".")
                )
            ),
            termNames,
            termTypesByName,
            currentLocation
        ));
    }


    @Override
    public SearchLocation sourceLocation() {
        return location;
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
        final Map<String, IJadescriptType> map = termTypesByName();
        return termNames().stream()
            .filter(map::containsKey)
            .map(map::get)
            .collect(Collectors.toList());
    }


    @Override
    public boolean isWithoutSideEffects() {
        return true;
    }

}
