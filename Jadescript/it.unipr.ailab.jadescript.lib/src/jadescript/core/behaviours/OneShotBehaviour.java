package jadescript.core.behaviours;



public class OneShotBehaviour<A extends jadescript.core.Agent>
        extends Behaviour<A> implements OneShot {
    @Override
    protected Behaviour.ExecutionType __executionType() {
        return ExecutionType.OneShot;
    }

    public OneShotBehaviour() {
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
    public static OneShotBehaviour __createEmpty(){
        return new EmptyOneShotBehaviour();
    }

    @SuppressWarnings("rawtypes")
    public static class EmptyOneShotBehaviour extends OneShotBehaviour {
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
