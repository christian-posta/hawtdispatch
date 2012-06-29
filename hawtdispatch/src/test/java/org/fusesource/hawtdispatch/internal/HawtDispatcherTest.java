package org.fusesource.hawtdispatch.internal;

import org.fusesource.hawtdispatch.DispatchPriority;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.hawtdispatch.internal.pool.SimplePool;
import org.fusesource.hawtdispatch.internal.pool.SimpleThread;
import org.fusesource.hawtdispatch.internal.util.RunnableCountDownLatch;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: cposta
 * Date: 6/28/12
 * Time: 3:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class HawtDispatcherTest {

    private static final int NUM_PROCESSORS = Runtime.getRuntime().availableProcessors();
    private HawtDispatcher dispatcher;

    @Before
    public void setUp() {
        dispatcher = new DispatcherConfig().createDispatcher();

    }

    @Test
    public void testDefaultGlobalQueue() {
        GlobalDispatchQueue queue = (GlobalDispatchQueue) dispatcher.getGlobalQueue();
        assertEquals(DispatchPriority.DEFAULT, queue.getPriority());
        assertEquals(queue.workers.getThreads().length, NUM_PROCESSORS);

        // shouldn't a "isXXX" method return boolean?
        assertTrue(queue.isSerialDispatchQueue() == null);
        assertTrue(queue.isThreadDispatchQueue() == null);
        assertEquals(queue, queue.isGlobalDispatchQueue());
        assertEquals(null, queue.getTargetQueue());
        assertEquals(DispatchQueue.QueueType.GLOBAL_QUEUE, queue.getQueueType());

        DispatchQueue[] threadedQueues = queue.getThreadQueues();
        assertEquals(NUM_PROCESSORS, threadedQueues.length);
        for (DispatchQueue threadQueue : threadedQueues) {
            assertTrue(threadQueue instanceof ThreadDispatchQueue);
        }

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCannotResume() {
        dispatcher.getGlobalQueue().resume();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCannotSuspend() {
        dispatcher.getGlobalQueue().suspend();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCannotSetTargetQueue() {
        dispatcher.getGlobalQueue().setTargetQueue(null);
    }

    @Test
    public void testCurrentDispatchQueue() throws InterruptedException {
        final DispatchQueue queue = dispatcher.getCurrentQueue();
        assertNull(queue);

        final DispatchQueue serialDispatchQueue = dispatcher.createQueue("test");

        RunnableCountDownLatch runner = new RunnableCountDownLatch(1){
            @Override
            public void run() {
                DispatchQueue queue1 = dispatcher.getCurrentQueue();
                assertNotNull(queue1);
                assertEquals(serialDispatchQueue, queue1);
                super.run();
            }
        };
        serialDispatchQueue.execute(runner);
        runner.await(1, TimeUnit.SECONDS);

    }


    @Test
    public void testPriorityGlobalQueues() {
        GlobalDispatchQueue queue = dispatcher.getGlobalQueue(DispatchPriority.DEFAULT);
        assertEquals(DispatchPriority.DEFAULT, queue.getPriority());
        assertThreadPriority(queue, Thread.NORM_PRIORITY);


        queue = dispatcher.getGlobalQueue(DispatchPriority.HIGH);
        assertEquals(DispatchPriority.HIGH, queue.getPriority());
        assertThreadPriority(queue, Thread.MAX_PRIORITY);


        queue = dispatcher.getGlobalQueue(DispatchPriority.LOW);
        assertEquals(DispatchPriority.LOW, queue.getPriority());
        assertThreadPriority(queue, Thread.MIN_PRIORITY);


    }

    private void assertThreadPriority(GlobalDispatchQueue queue, int priority) {
        WorkerPool pool = queue.workers;
        for (WorkerThread thread : pool.getThreads()) {
            SimpleThread simpleThread = (SimpleThread) thread;
            assertEquals(priority, simpleThread.getPriority());
        }
    }


}
