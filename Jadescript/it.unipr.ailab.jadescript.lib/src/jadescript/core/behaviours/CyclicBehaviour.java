package jadescript.core.behaviours;


public class CyclicBehaviour<A extends jadescript.core.Agent>
        extends Behaviour<A> implements Cyclic {
    @Override
    protected ExecutionType __executionType() {
        return ExecutionType.Cyclic;
    }

    public CyclicBehaviour() {
        super();
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

    @SuppressWarnings("rawtypes")
    public static class EmptyCyclicBehaviour extends jadescript.core.behaviours.CyclicBehaviour {
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
