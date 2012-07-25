package org.fusesource.hawtdispatch.internal;

import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.EventAggregators;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.hawtdispatch.internal.util.RunnableCountDownLatch;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: cposta
 * Date: 6/28/12
 * Time: 4:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class HawtCustomDispatchSourceTest {

    private HawtDispatcher dispatcher;

    @Before
    public void setUp() {
        dispatcher = new DispatcherConfig().createDispatcher();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResumeNoEventHandler() {
        DispatchQueue queue = dispatcher.createQueue("test");
        HawtCustomDispatchSource<Integer,Integer> source =
                new HawtCustomDispatchSource(dispatcher, EventAggregators.INTEGER_ADD, queue);

        source.resume();
    }

    @Test
    public void testMergeFromNonDispatchThread() throws InterruptedException {
        DispatchQueue queue = dispatcher.createQueue("test");
        final HawtCustomDispatchSource<Integer,Integer> source =
                new HawtCustomDispatchSource(dispatcher, EventAggregators.INTEGER_ADD, queue);

        final ArrayList<Integer> callList = new ArrayList<Integer>();
        final CountDownLatch latch = new CountDownLatch(2);
        source.setEventHandler(new Task() {

            @Override
            public void run() {
                callList.add(source.getData());
                latch.countDown();
            }
        });
        source.resume();

        source.merge(4);
        source.merge(5);

        latch.await(1, TimeUnit.SECONDS);

        // events are not merged if they are not from the same dispatch thread
        // they are treated as individual events
        assertEquals(new Integer(4), callList.get(0));
        assertEquals(new Integer(5), callList.get(1));
    }

    @Test
    public void testMergeFromDispatchThread() throws InterruptedException {
        DispatchQueue queue = dispatcher.createQueue("test");
        final HawtCustomDispatchSource<Integer,Integer> source =
                new HawtCustomDispatchSource(dispatcher, EventAggregators.INTEGER_ADD, queue);

        RunnableCountDownLatch eventHandler = new RunnableCountDownLatch(1){
            @Override
            public void run() {
                assertEquals(new Integer(9), source.getData());
                super.run();    //To change body of overridden methods use File | Settings | File Templates.
            }
        };

        source.setEventHandler(eventHandler);

        source.resume();

        DispatchQueue globalQueue = dispatcher.getGlobalQueue();
        globalQueue.execute(new Task() {
            @Override
            public void run() {
                source.merge(4);
                source.merge(5);
            }
        });

        assertTrue(eventHandler.await(1, TimeUnit.SECONDS));

    }

    @Test
    public void testResumeFromDifferentThread() throws InterruptedException {
        DispatchQueue queue = dispatcher.createQueue("test");
        final HawtCustomDispatchSource<Integer, Integer> source =
                new HawtCustomDispatchSource<Integer, Integer>(dispatcher, EventAggregators.INTEGER_ADD, queue);

        RunnableCountDownLatch eventHandler = new RunnableCountDownLatch(1){
            @Override
            public void run() {
                assertEquals(new Integer(9), source.getData());
                super.run();    //To change body of overridden methods use File | Settings | File Templates.
            }
        };

        source.setEventHandler(eventHandler);

        DispatchQueue globalQueue = dispatcher.getGlobalQueue();
        globalQueue.execute(new Task() {
            @Override
            public void run() {
                source.resume();
            }
        });

        globalQueue.execute(new Task() {
            @Override
            public void run() {
                source.merge(4);
                source.merge(5);
            }
        });

        assertTrue(eventHandler.await(1, TimeUnit.SECONDS));

    }
}