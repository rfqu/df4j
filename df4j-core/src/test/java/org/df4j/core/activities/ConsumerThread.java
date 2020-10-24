package org.df4j.core.activities;

import org.df4j.core.connector.AsyncArrayBlockingQueue;
import org.df4j.core.actor.ActivityThread;
import org.df4j.core.util.LoggerFactory;
import org.junit.Assert;
import org.slf4j.Logger;

import java.util.concurrent.CompletionException;

public class ConsumerThread extends ActivityThread {
    public final Logger logger = LoggerFactory.getLogger(this);
    AsyncArrayBlockingQueue<Long> queue;
    final int delay;
    Long in = null;

    public ConsumerThread(AsyncArrayBlockingQueue<Long> queue, int delay) {
        this.queue = queue;
        this.delay = delay;
    }

    @Override
    public void run() {
        logger.info(" Consumer thread started");
        Throwable cause;
        try {
            for (;;) {
                logger.info(" Consumer thread: take()");
                Long in = queue.take();
                logger.info(" Consumer thread got: " + in);
                if (this.in != null) {
                    Assert.assertEquals(this.in.intValue() - 1, in.intValue());
                }
                this.in = in;
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
            cause = e;
        } catch (CompletionException e) {
            cause = e.getCause();
        }
        logger.info(" Consumer thread completed with: " + cause);
    }
}
