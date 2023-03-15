package jadescript.core.behaviours;


import jadescript.java.AgentEnv;
import jadescript.java.SideEffectsFlag;

public class CyclicBehaviour<A extends jadescript.core.Agent>
        extends Behaviour<A> implements Cyclic {
    @Override
    protected ExecutionType __executionType() {
        return ExecutionType.Cyclic;
    }

    public CyclicBehaviour(
        AgentEnv<? extends A, SideEffectsFlag.AnySideEffectFlag> _agentEnv
    ) {
        super(_agentEnv);
    }

    @Override
    public void doAction(int _tickCount) {

    }

    @Override
    public void doOnActivate() {

    }

    @Override
    public void doOnDeactivate() {

    }

    @Override
    public void doOnDestroy() {

    }

    @SuppressWarnings("rawtypes")
    public static CyclicBehaviour __createEmpty(){
        return new EmptyCyclicBehaviour();
    }

    @SuppressWarnings({ "rawtypes", "serial" })
    public static class EmptyCyclicBehaviour extends jadescript.core.behaviours.CyclicBehaviour {
    	
        public EmptyCyclicBehaviour() {
			super(null);
		}

		@Override
        public void doAction(int _tickCount) {
            throw new UninitializedBehaviourException();
        }

        @Override
        public void doOnActivate() {
            throw new UninitializedBehaviourException();
        }
    }

}
