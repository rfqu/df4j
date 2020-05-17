package org.df4j.core.dataflow;

import org.df4j.core.port.InpFlood;
import org.df4j.core.port.OutMessagePort;

/**
 * Actor according to Carl Hewitt
 * Has a predefined input port.
 * Additional ports can be added, though.
 * @param <T> Type of input messages
 */
public abstract class ClassicActor<T> extends Actor implements OutMessagePort<T> {
    protected InpFlood<T> inp = new InpFlood<>(this);
    private volatile MessageAction<T> nextMessageAction;

    public void onNext(T message) {
        inp.onNext(message);
    }

    public void onComplete() {
        inp.onComplete();
    }

    public void onError(Throwable t) {
        inp.onError(t);
    }

    @Override
    protected void runAction() throws Throwable {
        if (inp.isCompleted()) {
            complete();
        } else {
            runAction(inp.remove());
        }
    }

    protected void runAction2() throws Throwable {
        if (inp.isCompleted()) {
            complete();
        } else {
            nextMessageAction.runAction(inp.remove());
        }
    }

    protected void nextMessageAction(MessageAction<T> messageAction) {
        nextMessageAction = messageAction;
        nextAction(this::runAction2);
    }

    protected abstract void runAction(T message) throws Throwable;

    @FunctionalInterface
    public interface MessageAction<T> {
        void runAction(T message) throws Throwable;
    }
}
