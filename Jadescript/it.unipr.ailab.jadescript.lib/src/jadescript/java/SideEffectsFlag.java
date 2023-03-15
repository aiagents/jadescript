package jadescript.java;

public interface SideEffectsFlag {

    interface WithSideEffects extends SideEffectsFlag {

    }

    interface NoSideEffects extends SideEffectsFlag {

    }

    interface AnySideEffectFlag extends WithSideEffects, NoSideEffects {

    }


}
