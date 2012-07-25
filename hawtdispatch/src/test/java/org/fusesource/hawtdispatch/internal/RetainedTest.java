package org.fusesource.hawtdispatch.internal;

import org.fusesource.hawtdispatch.BaseRetained;
import org.fusesource.hawtdispatch.internal.util.RunnableCountDownLatch;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: cposta
 * Date: 7/5/12
 * Time: 10:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class RetainedTest {

    class UnderTestRetained extends BaseRetained {

    }

    private UnderTestRetained retainedObject;


    @Before
    public void setUp() {
        retainedObject = new UnderTestRetained();
    }

    @Test
    public void testInitialRetainedValue() {
        assertEquals(1, retainedObject.retained());
    }

    @Test
    public void testIncrementRetained() {
        retainedObject.retain();
        retainedObject.retain();
        retainedObject.retain();

        assertEquals(4, retainedObject.retained());

    }

    @Test
    public void testDecrementRetained() {
        assertEquals(1, retainedObject.retained());

        retainedObject.retain();
        retainedObject.retain();
        retainedObject.retain();

        assertEquals(4, retainedObject.retained());

        retainedObject.release();
        retainedObject.release();
        retainedObject.release();

        assertEquals(1, retainedObject.retained());

    }

    @Test
    public void testDisposalWhenZero() throws InterruptedException {
        assertEquals(1, retainedObject.retained());
        RunnableCountDownLatch disposer = new RunnableCountDownLatch(1);
        retainedObject.setDisposer(disposer);
        retainedObject.release();
        assertEquals(0, retainedObject.retained());
        assertTrue(disposer.await(1, TimeUnit.SECONDS));
    }

}
