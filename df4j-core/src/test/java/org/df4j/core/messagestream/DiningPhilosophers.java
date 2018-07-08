package org.df4j.core.messagestream;

import org.df4j.core.connector.messagescalar.ScalarInput;
import org.df4j.core.connector.messagestream.StreamSubscriber;
import org.df4j.core.node.Action;
import org.df4j.core.node.AsyncActionTask;
import org.df4j.core.node.AsyncTask;
import org.df4j.core.node.messagestream.PickPoint;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * Demonstrates usage of class {@link PickPoint} to model common places for tokens.
 */
public class DiningPhilosophers {
    private static final int num = 5;

    @Ignore // TODO implement state machine with AsyncTask.controlLock
    @Test
    public void test() throws InterruptedException {
        ForkPlace[] forkPlaces = new ForkPlace[num];
        CountDownLatch counter = new CountDownLatch(num);
        Philosopher[] philosophers = new Philosopher[num];
    	// create places for forks with 1 fork in each
        for (int k=0; k < num; k++) {
            ForkPlace forkPlace = new ForkPlace(k);
            forkPlace.post(new Fork(k));
            forkPlaces[k]=forkPlace;
        }
        // create philosophers
        for (int k=0; k<num; k++) {
            philosophers[k] = new Philosopher(k, forkPlaces, counter);
        }
        // animate all philosophers
        for (int k=0; k<num; k++) {
            philosophers[k].start();
        }
        assertTrue(counter.await(2000, TimeUnit.MILLISECONDS));
    }

    static class Fork  {
        public final String id;

        Fork(int id) {
            this.id = "Fork_"+id;
        }

        @Override
        public String toString() {
            return id;
        }
    }
    
    static class ForkPlace extends PickPoint<Fork> {
        int id;
        String label;

        public ForkPlace(int k) {
            id = k;
            label="Forkplace_"+id;
        }

        @Override
        protected void fire() {
            super.fire();
        }

        @Override
        public void postFailure(Throwable t) {
            super.postFailure(t);
        }

        @Override
        public void post(Fork resource) {
            System.out.println(label+": put "+resource.toString());
            super.post(resource);
        }
    }

    /**
     * while ordinary {@link org.df4j.core.node.Actor} is a single {@link AsyncTask}
     * which restarts itself,
     * this class comprises of several {@link AsyncTask}s which activate each other cyclically.
     */
    static class Philosopher {
        Random rand=new Random();
        int id;
        CountDownLatch counter;
		ForkPlace firstPlace, secondPlace;
        Fork first, second;
        String indent;
        int rounds = 0;

        public Philosopher(int id, ForkPlace[] forkPlaces, CountDownLatch counter) {
            this.id = id;
            this.counter = counter;
            // to avoid deadlocks, allocate resource with lower number first
            if (id == num-1) {
                firstPlace = forkPlaces[0];
                secondPlace= forkPlaces[id];
            } else {
                firstPlace = forkPlaces[id];
                secondPlace = forkPlaces[id+1];
            }

            StringBuffer sb = new StringBuffer();
            sb.append(id).append(":");
            for (int k = 0; k<=id; k++) sb.append("  ");
            indent = sb.toString();
            println("first place ("+firstPlace.id+") second place ("+secondPlace.id+")");
        }

        Hungry hungry = new Hungry();
        Replete replete = new Replete();
        AsyncTask think = new DelayedAsyncTask(hungry);
        AsyncTask eat = new DelayedAsyncTask(replete);

        public void start() {
            think.start();
        }

        private void println(String s) {
            System.out.println(indent+s);
        }

        private class DelayedAsyncTask extends AsyncActionTask {
            final AsyncTask next;

            private DelayedAsyncTask(AsyncTask next) {
                this.next = next;
            }

            @Action
            public void act() throws InterruptedException {
                Thread.sleep(rand.nextLong()%11+11);
                next.start();
            }
        }

        /**
         * collects forks one by one
         */
        private class Hungry extends AsyncActionTask {
            ScalarInput<Fork> input = new ScalarInput<>(this);

            @Override
            public void start() {
                println("Request first (" + firstPlace.id + ")");
                StreamSubscriber<? super Fork> input= (StreamSubscriber<? super Fork>) this.input;
                firstPlace.subscribe(input);
                super.start();
            }

            @Action
            protected void act(Fork fork) {
                if (first == null) {
                    first = fork;
                    println("Request second (" + secondPlace.id + ")");
                    StreamSubscriber<? super Fork> input= (StreamSubscriber<? super Fork>) this.input;
                    secondPlace.subscribe(input);
                    super.start();
                } else  {
                    second = fork;
                    eat.start();
                }
            }
        }

        /** return forks
         *
         */
        private class Replete extends AsyncActionTask {

            @Action
            protected void act() {
                println("Release first (" + firstPlace.id + ")");
                firstPlace.post(first);
                println("Release second (" + secondPlace.id + ")");
                secondPlace.post(second);
                rounds++;
                if (rounds < 10) {
                    think.start();
                } else {
                    counter.countDown();
                }
            }
        }
    }
}
