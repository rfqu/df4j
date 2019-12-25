package org.df4j.protocol;

/************************************************************************
 * Licensed under Public Domain (CC0)                                    *
 *                                                                       *
 * To the extent possible under law, the person who associated CC0 with  *
 * this code has waived all copyright and related or neighboring         *
 * rights to this code.                                                  *
 *                                                                       *
 * You should have received a copy of the CC0 legalcode along with this  *
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.*
 ************************************************************************/
/**
 * interfaces copied from package org.reactivestreams
 * @see <a href="https://www.reactive-streams.org/">https://www.reactive-streams.org/</a>
 */
public final class Flow {

    private Flow() {} // uninstantiable

    /**
     * A {@link Publisher} is a provider of a potentially unbounded number of sequenced elements, publishing them according to
     * the demand received from its {@link Subscriber}(s).
     * <p>
     * A {@link Publisher} can serve multiple {@link Subscriber}s subscribed {@link #subscribe(Subscriber)} dynamically
     * at various points in time.
     *
     * @param <T> the type of element signaled.
     */
    public interface Publisher<T> {

        /**
         * Request {@link Publisher} to start streaming data.
         * <p>
         * This is a "factory method" and can be called multiple times, each time starting a new {@link FlowSubscription}.
         * <p>
         * Each {@link FlowSubscription} will work for only a single {@link Subscriber}.
         * <p>
         * A {@link Subscriber} should only subscribe once to a single {@link Publisher}.
         * <p>
         * If the {@link Publisher} rejects the subscription attempt or otherwise fails it will
         * signal the error via {@link Subscriber#onError}.
         *
         * @param s the {@link Subscriber} that will consume signals from this {@link Publisher}
         */
        void subscribe(Subscriber<? super T> s);
    }

    /**
     * Will receive call to {@link #onSubscribe(FlowSubscription)} once after passing an instance of {@link Subscriber} to {@link Publisher#subscribe(Subscriber)}.
     * <p>
     * No further notifications will be received until {@link FlowSubscription#request(long)} is called.
     * <p>
     * After signaling demand:
     * <ul>
     * <li>One or more invocations of {@link #onNext(Object)} up to the maximum number defined by {@link FlowSubscription#request(long)}</li>
     * <li>Single invocation of {@link #onError(Throwable)} or {@link Subscriber#onComplete()} which signals a terminal state after which no further events will be sent.
     * </ul>
     * <p>
     * Demand can be signaled via {@link FlowSubscription#request(long)} whenever the {@link Subscriber} instance is capable of handling more.
     *
     * @param <T> the type of element signaled.
     */
    public interface Subscriber<T> {
        /**
         * Invoked after calling {@link Publisher#subscribe(Subscriber)}.
         * <p>
         * No data will start flowing until {@link FlowSubscription#request(long)} is invoked.
         * <p>
         * It is the responsibility of this {@link Subscriber} instance to call {@link FlowSubscription#request(long)} whenever more data is wanted.
         * <p>
         * The {@link Publisher} will send notifications only in response to {@link FlowSubscription#request(long)}.
         *
         * @param s
         *            {@link FlowSubscription} that allows requesting data via {@link FlowSubscription#request(long)}
         */
        public void onSubscribe(FlowSubscription s);

        /**
         * Data notification sent by the {@link Publisher} in response to requests to {@link FlowSubscription#request(long)}.
         *
         * @param t the element signaled
         */
        public void onNext(T t);

        /**
         * Failed terminal state.
         * <p>
         * No further events will be sent even if {@link FlowSubscription#request(long)} is invoked again.
         *
         * @param t the throwable signaled
         */
        public void onError(Throwable t);

        /**
         * Successful terminal state.
         * <p>
         * No further events will be sent even if {@link FlowSubscription#request(long)} is invoked again.
         */
        public void onComplete();
    }

}
