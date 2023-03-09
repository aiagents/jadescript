package jadescript.core.behaviours;

import jade.content.ContentManager;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jadescript.content.JadescriptProposition;
import jadescript.core.exception.JadescriptException;
import jadescript.java.AgentEnv;
import jadescript.java.SideEffectsFlag;
import jadescript.lang.Duration;
import jadescript.lang.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public abstract class Behaviour<A extends jadescript.core.Agent>
    extends SimpleBehaviour implements Base {

    //Fixed period -> the next wake-up time is obtained as
    // __startTime + (an integer multiple of period)

    //Not fixed period -> just wait (period)ms from the end of last execution
    private static final boolean __fixedPeriod = false;


    //Set of requests to wait
    // (for an external event or for a time instant to be elapsed).
    private final List<Waiting> __waitings = new ArrayList<>();

    //Behaviour property:
    private long period = 0;

    private MacroState macroState;
    private long __JADEstartTime;
    private long __startTime;
    private long __expirationTime = 0;

    //not counting state adjustments & on activate:
    private int __effectiveExecutions = 0;

    private Waiting __ensureWait = Waiting.doNotWait();




    public Behaviour(
        AgentEnv<? extends A, SideEffectsFlag.AnySideEffectFlag> _agentEnv
    ) {
        super(_agentEnv.getAgent());
        this.macroState = MacroState.NOT_ACTIVE;

    }


    @SuppressWarnings("rawtypes")
    public static Behaviour __createEmpty() {
        return new EmptyBehaviour();
    }





    protected abstract ExecutionType __executionType();


    /**
     * invoked when macroState does the transition ACTIVE -> NOT_ACTIVE
     */
    public void reset() {
        super.reset();
        __waitings.clear();
        __clearEnsureWaiting();
        __JADEstartTime = 0;
        __startTime = 0;
        __effectiveExecutions = 0;
    }


    private boolean __hasExpirationTime() {
        return __expirationTime > 0;
    }


    private boolean __hasEnsureWakeUpTime() {
        return __ensureWait.hasWakeUpTime();
    }


    private void __setEnsureWaiting(Waiting waiting) {
        this.__ensureWait = waiting;
    }


    private void __clearEnsureWaiting() {
        __setEnsureWaiting(Waiting.doNotWait());
    }


    public void __unblock() {
        restart();
        __clearEnsureWaiting();
    }


    private Optional<Waiting> __getCollapsedWaiting() {
        if (__waitings.size() > 1) {
            Waiting w = __waitings.get(0);
            for (int i = 1; i < __waitings.size(); i++) {
                w = w.computeOverride(__waitings.get(i));
            }
            __waitings.clear();
        }
        if (__waitings.isEmpty()) {
            return Optional.empty();
        } else {
            final Waiting value = __waitings.get(0);
            __waitings.clear();
            return Optional.of(value);
        }
    }


    //Used by generated behaviours, when they detect that no event handler
    // fired in an execution step, to put the
    // behaviour to sleep until a new event (message/percept) occurs.
    public void __awaitForEvents() {
        __waitings.add(Waiting.waitForEvents());

    }


    public void __awaitDelayedActivation(long delay) {
        __waitings.add(Waiting.waitActivation(System.currentTimeMillis() + delay));
    }


    public void __noMessageHandled() {
        if (myAgent != null && myAgent instanceof jadescript.core.Agent) {
            ((jadescript.core.Agent) myAgent).__setAllMessagesIgnored(this);
        }
    }


    public void __awaitNextTick() {
        if (period > 0) {
            final long now = System.currentTimeMillis();
            final long nextEpoch;
            if (__fixedPeriod) {
                nextEpoch = __startTime + (__effectiveExecutions + 1) * period;
            } else {
                nextEpoch = now + period;
            }
            __waitings.add(Waiting.waitPeriodic(nextEpoch));
        }
    }


    public Duration getPeriod() {
        return Duration.of(period);
    }


    public void setPeriod(Duration period) {
        this.period = period.getSecondsLong() * 1000L + period.getMillis();
    }


    public abstract void doAction(int _tickCount);

    @SuppressWarnings("EmptyMethod")
    public abstract void doOnActivate();

    @SuppressWarnings("EmptyMethod")
    public abstract void doOnDeactivate();

    @SuppressWarnings("EmptyMethod")
    public abstract void doOnDestroy();


    @Override
    public final boolean done() {
        return false;
    }


    @Override
    public final void action() {
        // ActivateNow, ActivateDelayed, DeactivateDelayed, FailDelayed
        // and Destroy can ONLY be issued manually.
        // DeactivateNow and FailNow can be issued manually, but also can be
        // automatically issued by the system (e.g., one-shot behaviours
        // deactivate when they are done, or behaviours deactivated with a
        // delay, or behaviours failing for an escalated exception).
        // Execute can only be issued by the system.

        // Now we need to answer the question:
        // "Why action() has been invoked here?"

        // This block of code infers the type of event from the state and
        // provides one input of type (DeactivateNow | Execute).
        // It may even choose to reblock the behaviour automatically if the
        // behaviour was woken up early.
        final long now = System.currentTimeMillis();
        if (__executionType() == ExecutionType.OneShot && __effectiveExecutions >= 1) {
            //If it is oneshot, and it is done, deactivate.
            __feedInput(DeactivateNow.INSTANCE);
        } else if (__hasExpirationTime() && __expirationTime <= now) {
            //If it is expired, deactivate.
            __feedInput(DeactivateNow.INSTANCE);
        } else {
            if (__hasEnsureWakeUpTime() && __ensureWait.wakeUpTime > now) {
                // We woke up because a message arrived.
                // However, we had to wait a little more.
                __ensureWait.doBlock(this);
            } else {

                if (__hasEnsureWakeUpTime()) {
                    //We needed to wait a specific amount of time, but that
                    // time elapsed.
                    // We can clear the ensure-field now.
                    __clearEnsureWaiting();
                }
                __feedInput(Execute.INSTANCE);
                //Detect any internally issued waitings, and execute them.
                __getCollapsedWaiting().ifPresent(it -> it.doBlock(this));
            }

        }
    }


    private void __feedInput(Input input) {

        switch (macroState) {
            case FAILED:
            case NOT_ACTIVE: {
                if (input instanceof ActivateNow) {
                    __startActivatingInternal(((ActivateNow) input).getAgent());
                    macroState = MacroState.ACTIVATING;
                } else if (input instanceof ActivateDelayed) {
                    final long delay = ((ActivateDelayed) input).getDelay();
                    __awaitDelayedActivation(delay);
                    __startActivatingInternal(((ActivateDelayed) input).getAgent());
                    macroState = MacroState.ACTIVATING;
                } else if (input instanceof Execute) {
                    throw new RuntimeException(
                        "Invalid behaviour state (cannot execute if not " +
                            "active).");
                } else if (input instanceof DeactivateNow) {
                    // Deactivation is idempotent, do nothing.
                    // Just remove expiration time (deactivating and
                    // reactivating has to behave like a 'reset').
                    __expirationTime = 0;
                    // Staying in NOT_ACTIVE.
                } else if (input instanceof DeactivateDelayed) {
                    // Deactivation is idempotent, do nothing.
                    // Just set expiration time.
                    __expirationTime =
                        System.currentTimeMillis() + ((DeactivateDelayed) input).getDelay();
                    // Staying in NOT_ACTIVE.
                } else if (input instanceof FailNow) {
                    // Failure when not active -> put in failed state,
                    // dispatch failure event
                    // remove expiration time (deactivating and reactivating
                    // has to behave like a 'reset').
                    //noinspection unchecked
                    final A agent = (A) getAgent();
                    __expirationTime = 0;
                    macroState = MacroState.FAILED;
                    __dispatchFailure(agent, ((FailNow) input).getReason());
                    // Staying in NOT_ACTIVE.
                } else if (input instanceof Destroy) {
                    __destroyInternal();
                    macroState = MacroState.DESTROYED;
                }

            }
            break;
            case ACTIVATING: {
                if (input instanceof ActivateNow) {
                    __anticipateActivationInternal();
                    // Staying in ACTIVATING.
                } else if (input instanceof ActivateDelayed) {
                    __postponeActivationInternal(((ActivateDelayed) input).getDelay());
                    // Staying in ACTIVATING.
                } else if (input instanceof Execute) {
                    macroState = MacroState.ACTIVE;
                    __executeOnActivateInternal();
                    __executeHandlersInternal();
                    __awaitNextTick();
                } else if (input instanceof DeactivateNow) {
                    __cancelActivatingInternal();
                    macroState = MacroState.NOT_ACTIVE;
                } else if (input instanceof DeactivateDelayed) {
                    //sets an expiration time
                    //It will race with the activation time
                    __doRaceDeactivationWithActivationInternal(((DeactivateDelayed) input).getDelay());
                    // Staying in ACTIVATING.
                } else if (input instanceof FailNow) {
                    // Failure when activating -> cancel activating, put in
                    // failed state, dispatch failure event
                    //noinspection unchecked
                    final A agent = (A) getAgent();
                    __cancelActivatingInternal();
                    macroState = MacroState.FAILED;
                    __dispatchFailure(agent, ((FailNow) input).getReason());
                } else if (input instanceof Destroy) {
                    __cancelActivatingInternal();
                    __destroyInternal();
                    macroState = MacroState.DESTROYED;
                }
            }
            break;
            case ACTIVE: {
                if (input instanceof ActivateNow) {
                    // Activate is idempotent.
                    // Just remove expiration time.
                    __expirationTime = 0;
                    // Staying in ACTIVE.
                } else if (input instanceof ActivateDelayed) {
                    __pauseInternal(((ActivateDelayed) input).getDelay());
                    // Staying in ACTIVE.
                } else if (input instanceof Execute) {
                    __executeHandlersInternal();
                    __awaitNextTick();
                    // Staying in ACTIVE.
                } else if (input instanceof DeactivateNow) {
                    __deactivateInternal(false);
                    macroState = MacroState.NOT_ACTIVE;
                } else if (input instanceof DeactivateDelayed) {
                    __expirationTime =
                        System.currentTimeMillis() + ((DeactivateDelayed) input).getDelay();
                    // Staying in ACTIVE.
                } else if (input instanceof FailNow) {
                    // Failure when active -> deactivate, put in failed
                    // state, dispatch failure event
                    // remove expiration time (deactivating and reactivating
                    // has to behave like a 'reset').
                    //noinspection unchecked
                    final A agent = (A) getAgent();
                    __expirationTime = 0;
                    __deactivateInternal(true);
                    macroState = MacroState.FAILED;
                    __dispatchFailure(agent, ((FailNow) input).getReason());
                    // Staying in NOT_ACTIVE.
                } else if (input instanceof Destroy) {
                    __deactivateInternal(false);
                    __destroyInternal();
                    macroState = MacroState.DESTROYED;
                }
            }
            break;
            case DESTROYED: {
                if (!(input instanceof Destroy)) { // destroy statement is
                    // idempotent
                    throw new DestroyedBehaviourException(this);
                }
            }
            break;
        }

        final Optional<Waiting> externallyIssued = __getCollapsedWaiting();
        externallyIssued.ifPresent(waiting -> waiting.doBlock(this));
    }


    private void __pauseInternal(long delay) {
        __awaitDelayedActivation(delay);
    }


    private void __cancelActivatingInternal() {
        //Like __deactivateInternal(), but without calling on-deactivate
        // (because on-activate has never executed)
        __waitings.clear();
        if (myAgent != null) {
            __unblock();
            myAgent.removeBehaviour(this);
            setAgent(null);
        }
    }


    private void __postponeActivationInternal(long delay) {
        if (delay <= 0) {
            __anticipateActivationInternal();
        } else {
            __waitings.clear();
            __unblock();
            __awaitDelayedActivation(delay);
        }
    }


    private void __anticipateActivationInternal() {
        __waitings.clear();
        __unblock();
    }


    private void __doRaceDeactivationWithActivationInternal(long delay) {
        __expirationTime = System.currentTimeMillis() + delay;
    }


    private void __startActivatingInternal(Agent toAgent) {
        if (this.getAgent() != null) {
            this.getAgent().removeBehaviour(this);
        }
        toAgent.addBehaviour(this);
        this.setAgent(toAgent);
    }


    private void __executeOnActivateInternal() {
        __startTime = System.currentTimeMillis();
        final Agent agent = getAgent();
        if (agent != null) {
            final ContentManager cm = agent.getContentManager();
            if (cm != null) {
                __registerOntologies(cm);
            }
        }
        doOnActivate();
    }


    private void __executeHandlersInternal() {
        doAction(__effectiveExecutions);
        __effectiveExecutions++;
    }


    private void __deactivateInternal(boolean skipOnDeactivate) {
        __waitings.clear();
        if (myAgent != null) {
            reset();
            if (!skipOnDeactivate) doOnDeactivate();
            myAgent.removeBehaviour(this);
            setAgent(null);
        }
    }


    private void __destroyInternal() {
        doOnDestroy();
    }


    protected void __registerOntologies(ContentManager cm) {
        cm.registerOntology(jadescript.content.onto.Ontology.getInstance());
    }


    public final void onStart() {
        __JADEstartTime = System.currentTimeMillis();
    }


    public final void activate_after_every(
        Agent agent,
        Duration delay,
        Duration period
    ) {

        long delayMillis;
        long periodMillis;
        if (delay != null) {
            delayMillis = delay.getSecondsLong() * 1000L + delay.getMillis();
        } else {
            delayMillis = 0;
        }
        if (period != null) {
            periodMillis = period.getSecondsLong() * 1000L + period.getMillis();
        } else {
            periodMillis = 0;
        }
        this.period = periodMillis;
        if (delayMillis > 0) {

            __feedInput(new ActivateDelayed(agent, delayMillis));
        } else {
            __feedInput(new ActivateNow(agent));
        }
    }


    public final void activate_at_every(
        Agent agent,
        Timestamp start,
        Duration period
    ) {
        activate_after_every(
            agent,
            Timestamp.subtract(start, Timestamp.now()),
            period
        );
    }


    public final void activate_at(Agent agent, Timestamp start) {
        activate_at_every(agent, start, null);
    }


    public final void activate_every(Agent agent, Duration period) {
        activate_after_every(agent, null, period);
    }


    public final void activate_after(Agent agent, Duration delay) {
        activate_after_every(agent, delay, null);
    }


    public final void activate(Agent agent) {
        activate_after_every(agent, null, null);
    }


    public final void deactivate_after_millis(long delay) {
        __feedInput(new DeactivateDelayed(delay));
    }


    public final void deactivate_after(Duration delay) {
        deactivate_after_millis(delay.getSecondsLong() * 1000L + delay.getMillis());
    }


    public final void deactivate_at(Timestamp end) {

        deactivate_after(Timestamp.subtract(end, Timestamp.now()));
    }


    public final boolean isActive() {
        return macroState == MacroState.ACTIVE;
    }


    public final void deactivate() {
        __feedInput(DeactivateNow.INSTANCE);
    }


    public final void destroy() {
        __feedInput(Destroy.INSTANCE);
    }


    @Override
    public final int onEnd() {
        deactivate();
        return super.onEnd();
    }


    public void __escalateException(
        JadescriptException exception
    ) {
        __failBehaviour(exception);
    }


    public final void __failBehaviour(JadescriptException reason) {
        __failBehaviour(reason.getReason());
    }


    public final void __failBehaviour(JadescriptProposition reason) {
        __feedInput(new FailNow(reason));
    }


    public final void __dispatchFailure(
        jadescript.core.Agent agent,
        JadescriptProposition reason
    ) {
        if (agent != null) {
            agent.__handleBehaviourFailure(this, reason);
        }
    }


    public Boolean __hasStaleMessageHandler() {
        // Is overriden by generated subclasses.
        return false;
    }


    @SuppressWarnings("unchecked")
    public A getJadescriptAgent() {
        final Agent agent = getAgent();
        if (agent == null) {
            throw new InactiveBehaviourException(this);
        }
        //noinspection unchecked
        return (A) agent;
    }


    public A __theAgent() {
        return getJadescriptAgent();
    }


    private enum MacroState {
        NOT_ACTIVE,
        ACTIVATING,
        ACTIVE,
        FAILED,
        DESTROYED,
    }

    public enum ExecutionType {
        Cyclic,
        OneShot
    }


    /*
        What issues a waiting?
        Waitings priority: (when a waiting is issued, the ones on the top
                override the ones on the bottom, never the opposite).
        AD) Activation Delay / Postponed Activation / User-issued Pause
        EW) Event Wait / Indefinite wait (only waiting for events)
        PW) Periodic wait. Between a tick and the next for behaviours with
                period > 0.
        NO) No wait
     */
    private enum WaitingType {
        ActivationDelay, EventWait, PeriodicWait, NoWait
    }

    private interface Input {

    }

    private static class ActivateNow implements Input {

        private final jade.core.Agent agent;


        private ActivateNow(jade.core.Agent agent) {
            this.agent = agent;
        }


        public jade.core.Agent getAgent() {
            return agent;
        }


        @Override
        public String toString() {
            return "ActivateNow{}";
        }

    }

    private static class ActivateDelayed implements Input {

        private final jade.core.Agent agent;
        private final long delay;


        private ActivateDelayed(jade.core.Agent agent, long delay) {
            this.agent = agent;
            this.delay = delay;
        }


        public long getDelay() {
            return delay;
        }


        public jade.core.Agent getAgent() {
            return agent;
        }


        @Override
        public String toString() {
            return "ActivateDelayed{" +
                "delay=" + Duration.of(delay) +
                '}';
        }

    }

    private static class DeactivateNow implements Input {

        public static final DeactivateNow INSTANCE = new DeactivateNow();


        @Override
        public String toString() {
            return "DeactivateNow{}";
        }

    }

    private static class DeactivateDelayed implements Input {

        private final long delay;


        private DeactivateDelayed(long delay) {
            this.delay = delay;
        }


        public long getDelay() {
            return delay;
        }


        @Override
        public String toString() {
            return "DeactivateDelayed{" +
                "delay=" + Duration.of(delay) +
                '}';
        }

    }

    private static class FailNow implements Input {

        private final JadescriptProposition reason;


        private FailNow(JadescriptProposition reason) {
            this.reason = reason;
        }


        public JadescriptProposition getReason() {
            return reason;
        }


        @Override
        public String toString() {
            return "FailNow{}";
        }

    }

    private static class Destroy implements Input {

        public static final Destroy INSTANCE = new Destroy();


        @Override
        public String toString() {
            return "Destroy{}";
        }

    }

    private static class Execute implements Input {

        public static final Execute INSTANCE = new Execute();


        @Override
        public String toString() {
            return "Execute{}";
        }

    }

    private static class Waiting {

        private final WaitingType waitingType;
        private final long wakeUpTime;


        private Waiting(long wakeUpTime, WaitingType waitingType) {
            this.wakeUpTime = wakeUpTime;
            this.waitingType = waitingType;
        }


        public static Waiting doNotWait() {
            return new Waiting(0, WaitingType.NoWait);
        }


        public static Waiting waitForEvents() {
            return new Waiting(0, WaitingType.EventWait);
        }


        public static Waiting waitActivation(long wakeUpTime) {
            return new Waiting(wakeUpTime, WaitingType.ActivationDelay);
        }


        public static Waiting waitPeriodic(long wakeUpTime) {
            return new Waiting(wakeUpTime, WaitingType.PeriodicWait);
        }


        @Override
        public String toString() {
            String wut = "";
            if (wakeUpTime > 0) {
                wut = "wut=" + Timestamp.fromEpochMillis(wakeUpTime) + ", ";
            }

            return "Waiting{" + wut + "type=" + waitingType.name() + '}';
        }


        public boolean hasWakeUpTime() {
            return wakeUpTime != 0;
        }


        public Waiting computeOverride(Waiting w) {
            // Higher-priority waitings overwrite other waitings.
            if (this.waitingType.ordinal() < w.waitingType.ordinal()) {
                return this;
            } else { //if(this.waitingType.ordinal() >= w.waitingType.ordinal
                // ())//
                return w;
            }
        }


        public void doBlock(jade.core.behaviours.Behaviour b) {
            if (b.getAgent() != null) {
                switch (waitingType) {
                    case ActivationDelay:
                    case PeriodicWait:
                        long now = System.currentTimeMillis();
                        long millis = wakeUpTime - now;
                        if (millis > 0) {
                            b.block(millis);
                            if (b instanceof Behaviour) {
                                ((Behaviour<?>) b).__setEnsureWaiting(this);
                            }
                        }
                        break;
                    case EventWait:
                        b.block();
                        break;
                    case NoWait:
                        break;
                }
            }
        }

    }

    public static class UninitializedBehaviourException extends RuntimeException {

        public UninitializedBehaviourException() {
            super("Attempted to executed an uninitialized behaviour.");
        }

    }

    public static class DestroyedBehaviourException extends RuntimeException {

        public DestroyedBehaviourException(Behaviour<?> b) {
            super("Cannot activate destroyed behaviour '" + b.getBehaviourName() + "'.");
        }

    }

    public static class InactiveBehaviourException extends RuntimeException {

        public InactiveBehaviourException(Behaviour<?> b) {
            super("Cannot access agent state from non-active behaviour '" + b.getBehaviourName() + "'.");
        }

    }

    @SuppressWarnings("rawtypes")
    public static class EmptyBehaviour extends Behaviour {

        public EmptyBehaviour() {
            super(null);
        }


        @Override
        protected ExecutionType __executionType() {
            return null;
        }


        @Override
        public void doAction(int _tickCount) {
            throw new UninitializedBehaviourException();
        }


        @Override
        public void doOnActivate() {
            throw new UninitializedBehaviourException();
        }


        @Override
        public void doOnDeactivate() {

        }


        @Override
        public void doOnDestroy() {

        }

    }

}
