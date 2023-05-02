package it.unipr.ailab.jadescript.semantics.proxyeobjects;

import it.unipr.ailab.jadescript.jadescript.Behaviour;
import it.unipr.ailab.jadescript.jadescript.Feature;
import it.unipr.ailab.jadescript.jadescript.MemberBehaviour;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour.BehaviourType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static it.unipr.ailab.maybe.Maybe.some;

public class BehaviourDeclaration extends ProxyEObject
    implements Behaviour, MemberBehaviour {

    private final EObject input;
    private final Maybe<EList<JvmTypeReference>> ontologies;
    private final Maybe<EList<JvmParameterizedTypeReference>> superTypes;
    private final Maybe<EList<Feature>> features;
    private final boolean isMemberBehaviour;
    private Maybe<String> behaviourType;
    private Maybe<String> name;
    private Maybe<JvmTypeReference> forAgent;


    private BehaviourDeclaration(
        EObject input,
        Maybe<String> behaviourType,
        Maybe<String> name,
        Maybe<JvmTypeReference> forAgent,
        Maybe<EList<JvmTypeReference>> ontologies,
        Maybe<EList<JvmParameterizedTypeReference>> superTypes,
        Maybe<EList<Feature>> features,
        boolean isMemberBehaviour
    ) {
        super(input);
        this.name = name;
        this.forAgent = forAgent;
        this.input = input;
        this.behaviourType = behaviourType;
        this.ontologies = ontologies;
        this.superTypes = superTypes;
        this.features = features;
        this.isMemberBehaviour = isMemberBehaviour;
    }


    public static Maybe<BehaviourDeclaration> memberBehaviour(
        Maybe<? extends EObject> input,
        Maybe<String> behaviourType,
        Maybe<String> name,
        Maybe<JvmTypeReference> forAgent,
        Maybe<EList<JvmTypeReference>> ontologies,
        Maybe<EList<JvmParameterizedTypeReference>> superTypes,
        Maybe<EList<Feature>> features
    ) {
        return input.__(inputSafe ->
            new BehaviourDeclaration(
                inputSafe,
                behaviourType,
                name,
                forAgent,
                ontologies,
                superTypes,
                features,
                true
            )
        );
    }


    public static Maybe<BehaviourDeclaration> topLevelBehaviour(
        Maybe<Behaviour> behaviour
    ) {
        return behaviour.__(bsafe -> new BehaviourDeclaration(
            bsafe,
            behaviour.__(Behaviour::getType),
            behaviour.__(Behaviour::getName),
            behaviour.__(Behaviour::getAgent),
            behaviour.__(Behaviour::getOntologies),
            behaviour.__(Behaviour::getSuperTypes),
            behaviour.__(Behaviour::getFeatures),
            false
        ));
    }


    public BehaviourType.Kind getKind() {
        String type = this.getType();
        if (type == null || type.isBlank()) {
            return BehaviourType.Kind.Base;
        }

        type = type.toLowerCase();

        if (Objects.equals(type, "cyclic")) {
            return BehaviourType.Kind.Cyclic;
        }

        if (Objects.equals(type, "one shot")
            || Objects.equals(type, "oneshot")) {
            return BehaviourType.Kind.OneShot;
        }

        return BehaviourType.Kind.Base;
    }


    public boolean isMemberBehaviour() {
        return isMemberBehaviour;
    }


    public Maybe<JvmTypeReference> getForAgent() {
        return forAgent;
    }


    public EObject getUsesOntologyElement() {
        return input;
    }


    @Override
    public EList<Feature> getFeatures() {
        return features.toNullable();
    }


    @Override
    public EList<JvmParameterizedTypeReference> getSuperTypes() {
        return superTypes.toNullable();
    }


    @Override
    public String getName() {
        return name.orElse("");
    }


    @Override
    public void setName(String value) {
        this.name = some(value);
    }


    @Override
    public @Nullable JvmTypeReference getAgent() {
        return forAgent.toNullable();
    }


    @Override
    public void setAgent(JvmTypeReference value) {
        this.forAgent = Maybe.some(value);
    }


    @Override
    public String getType() {
        return behaviourType.toNullable();
    }


    @Override
    public void setType(String value) {
        this.behaviourType = some(value);
    }


    @Override
    public EList<JvmTypeReference> getOntologies() {
        return ontologies.toNullable();
    }


}
