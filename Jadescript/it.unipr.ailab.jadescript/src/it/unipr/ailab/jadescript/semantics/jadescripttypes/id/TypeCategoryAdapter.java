package it.unipr.ailab.jadescript.semantics.jadescripttypes.id;

public class TypeCategoryAdapter implements TypeCategory{

    @Override
    public boolean isAny() {
        return false;
    }


    @Override
    public boolean isNothing() {
        return false;
    }


    @Override
    public boolean isBasicType() {
        return false;
    }


    @Override
    public boolean isCollection() {
        return false;
    }


    @Override
    public boolean isAgent() {
        return false;
    }


    @Override
    public boolean isBehaviour() {
        return false;
    }


    @Override
    public boolean isMessage() {
        return false;
    }


    @Override
    public boolean isOntoContent() {
        return false;
    }


    @Override
    public boolean isAgentEnv() {
        return false;
    }


    @Override
    public boolean isOntology() {
        return false;
    }


    @Override
    public boolean isUnknownJVM() {
        return false;
    }


    @Override
    public boolean isJavaVoid() {
        return false;
    }


    @Override
    public boolean isTuple() {
        return false;
    }


    @Override
    public boolean isList() {
        return false;
    }


    @Override
    public boolean isMap() {
        return false;
    }


    @Override
    public boolean isSet() {
        return false;
    }


    @Override
    public boolean isSideEffectFlag() {
        return false;
    }

}
